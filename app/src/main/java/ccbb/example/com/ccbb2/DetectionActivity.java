package ccbb.example.com.ccbb2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.squirrelframework.foundation.fsm.UntypedStateMachine;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import ccbb.example.com.ccbb2.Helpers.CollectionHelper;
import ccbb.example.com.ccbb2.bluetooth.BluetoothCommandService;
import ccbb.example.com.ccbb2.dataobjects.Circle;
import ccbb.example.com.ccbb2.dataobjects.Line;
import ccbb.example.com.ccbb2.dataobjects.PairOfPoints;
import ccbb.example.com.ccbb2.enums.Action;
import ccbb.example.com.ccbb2.fsm.CarDecisionFsm;
import ccbb.example.com.ccbb2.fsm.FsmManager;

public class DetectionActivity extends Activity implements CvCameraViewListener2 {
    private static final String  TAG = "CCBB::";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothCommandService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for Bluetooth Command Service

    public static  BluetoothCommandService mCommandService = null;
    private Switch switchConnectBT;
    //sign detection by color
    private Mat mHierarchy;
    private Mat mRgba;
    private Mat mSignColorThreshold;
    private Mat mHsv;
    private MatOfPoint2f contoursListf;
    private List<MatOfPoint> contoursList;
    private List<MatOfPoint> contoursListSpeedUp;
    private List<MatOfPoint> contoursListSpeedDown;
    private List<MatOfPoint> contoursListStop;
    private Scalar hsvMin;
    private Scalar hsvMax;
    private Map<String, List<Integer>> contourMeanSizes;
    private int cycleCounterStopSign;
    private int cycleCounterSpeedUpSign;
    private int cycleCounterSpeedDownSign;
    private long currentTimeMillis;

    //lane detection by shape
    private Mat mGrayLane;
    private Mat mLaneThreshold;
    private Mat laneHoughLines;

    //sign detection by shape
    private Mat mGraySign;
    private Mat mSignShapeThreshold;
    private Mat signHoughCircles;
    private Circle meanCircleResult;

    //generic
    private Mat mCalibration;
    private Mat mDetectionResult;
    private Mat genericErodeElement;
    private Mat genericDilateElement;
    private boolean laneShapeDetectionOn;
    private boolean signShapeDetectionOn;
    private boolean signColorDetectionOn;
    private boolean hsvThresholdOn;
    private Spinner laneOptionSpinner;
    private String spinnerValue;

    private int                 hMin = 0;
    private int                 hMax = 256;
    private int                 sMin = 0;
    private int                 sMax = 256;
    private int                 vMin = 0;
    private int                 vMax = 256;
    private Pair<Scalar, Scalar> stopSignHsvPair;
    private Pair<Scalar, Scalar> speedUpSignHsvPair;
    private Pair<Scalar, Scalar> speedDownSignHsvPair;
    private int                 screenWidth;
    private int                 screenHeight;


    private Rect laneRoi;
    private Rect signRoi;

    //opencv
    private CameraBridgeViewBase mOpenCvCameraView;
    private BaseLoaderCallback  mLoaderCallback;

    //state machine
    private UntypedStateMachine fsm;
    private CarDecisionFsm carDecisionFsm;

    public DetectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.detection_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.enableFpsMeter();

        mLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                    {
                        mOpenCvCameraView.enableView();
                    } break;
                    default:
                    {
                        super.onManagerConnected(status);
                    } break;
                }
            }
        };

        initComponents();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        carDecisionFsm = new CarDecisionFsm();

        fsm = carDecisionFsm.getBuilder().newStateMachine(Action.Forward);
    }

    private void initComponents() {
        laneOptionSpinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, getResources()
                .getStringArray(R.array.laneDetection));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        laneOptionSpinner.setAdapter(adapter);
        laneOptionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                spinnerValue = adapterView.getItemAtPosition(i).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        } );

        switchConnectBT = (Switch) findViewById(R.id.switchDetectionBTConnect);
        switchConnectBT.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                try {
                    if (b) {
                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(BluetoothCommandService.address);
                        // Attempt to connect to the device
                        mCommandService.connect(device);

                    }
                } catch (Exception ex) {
                    switchConnectBT.setChecked(false);
                    Log.e(TAG, "Error connecting to BT device", ex);
                }
            }
        });
        final TextView textViewSignHMin = (TextView) findViewById(R.id.textViewHMin);
        textViewSignHMin.setText("HMin " + hMin);
        final TextView textViewSignHMax = (TextView) findViewById(R.id.textViewHMax);
        textViewSignHMax.setText("HMax " + hMin);
        final TextView textViewSignSMin = (TextView) findViewById(R.id.textViewSMin);
        textViewSignSMin.setText("SMin " + sMin);
        final TextView textViewSignSMax = (TextView) findViewById(R.id.textViewSMax);
        textViewSignSMax.setText("SMax " + sMax);
        final TextView textViewSignVMin = (TextView) findViewById(R.id.textViewVMin);
        textViewSignVMin.setText("VMin " + vMin);
        final TextView textViewSignVMax = (TextView) findViewById(R.id.textViewVMax);
        textViewSignVMax.setText("VMax " + vMax);

        initToggleButtons();
        initHsvSeekBars(textViewSignHMin, textViewSignHMax, textViewSignSMin, textViewSignSMax, textViewSignVMin, textViewSignVMax);
        initHsvThresholds();
    }

    private void initHsvThresholds() {
        final Button speedUpButton = (Button) findViewById(R.id.buttonSpeedUp);
        speedUpButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                speedUpSignHsvPair = new Pair<>(new Scalar(hMin, sMin, vMin), new Scalar(hMax, sMax, vMax));
                Toast.makeText(getApplicationContext(),
                        "HSV SpeedUp Set:{(" +
                                (int) speedUpSignHsvPair.first.val[0] + "," +
                                (int) speedUpSignHsvPair.first.val[1] + "," +
                                (int) speedUpSignHsvPair.first.val[2] + "),(" +
                                (int) speedUpSignHsvPair.second.val[0] + "," +
                                (int) speedUpSignHsvPair.second.val[1] + "," +
                                (int) speedUpSignHsvPair.second.val[2] + ")}"
                        , Toast.LENGTH_SHORT).show();
            }
        });

        final Button speedDownButton = (Button) findViewById(R.id.buttonSpeedDown);
        speedDownButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                speedDownSignHsvPair = new Pair<>(new Scalar(hMin, sMin, vMin), new Scalar(hMax, sMax, vMax));
                Toast.makeText(getApplicationContext(),
                        "HSV SpeedDown Set:{" +
                                (int) speedDownSignHsvPair.first.val[0] + "," +
                                (int) speedDownSignHsvPair.first.val[1] + "," +
                                (int) speedDownSignHsvPair.first.val[2] + "),(" +
                                (int) speedDownSignHsvPair.second.val[0] + "," +
                                (int) speedDownSignHsvPair.second.val[1] + "," +
                                (int) speedDownSignHsvPair.second.val[2] + ")}"
                        , Toast.LENGTH_SHORT).show();
            }
        });

        final Button stopButton = (Button) findViewById(R.id.buttonStop);
        stopButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSignHsvPair = new Pair<>(new Scalar(hMin, sMin, vMin), new Scalar(hMax, sMax, vMax));
                Toast.makeText(getApplicationContext(),
                        "HSV Stop Set:{" +
                                (int) stopSignHsvPair.first.val[0] + "," +
                                (int) stopSignHsvPair.first.val[1] + "," +
                                (int) stopSignHsvPair.first.val[2] + "),(" +
                                (int) stopSignHsvPair.second.val[0] + "," +
                                (int) stopSignHsvPair.second.val[1] + "," +
                                (int) stopSignHsvPair.second.val[2] + ")}"
                        , Toast.LENGTH_SHORT).show();
            }
        });

        final Switch hsvThdSwitch = (Switch) findViewById(R.id.switchHsvThreshold);
        hsvThdSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                hsvThresholdOn = b;
            }
        });
    }

    private void initToggleButtons() {
        Button stopBTbutton = (Button) findViewById(R.id.buttonDetectionBTStop);
        stopBTbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCommandService.write("d".getBytes());
            }
        });
        ToggleButton laneShapeDetectionToggleButton = (ToggleButton) findViewById(R.id.laneShapeToggleButton);
        laneShapeDetectionToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                laneOptionSpinner.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                laneShapeDetectionOn = isChecked;
            }

        });
        laneShapeDetectionToggleButton.setChecked(false);
        
        ToggleButton signShapeDetectionToggleButton = (ToggleButton) findViewById(R.id.signShapeToggleButton);
        signShapeDetectionToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                signShapeDetectionOn = isChecked;
            }
        });

        ToggleButton signColorDetectionToggleButton = (ToggleButton) findViewById(R.id.signColorToggleButton);
        signColorDetectionToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                signColorDetectionOn = isChecked;
            }
        });
    }

    private void initHsvSeekBars(final TextView textViewSignHMin, final TextView textViewSignHMax, final TextView textViewSignSMin, final TextView textViewSignSMax, final TextView textViewSignVMin, final TextView textViewSignVMax) {
        SeekBar hMinSeekBar = (SeekBar) findViewById(R.id.seekBarHMin);
        hMinSeekBar.setMax(ConfigConstants.SEEK_BAR_MAX_VALUE);
        hMinSeekBar.setProgress(hMin);
        hMinSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                hMin = i;
                textViewSignHMin.setText(hMin + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBar hMaxSeekBar = (SeekBar) findViewById(R.id.seekBarHMax);
        hMaxSeekBar.setMax(ConfigConstants.SEEK_BAR_MAX_VALUE);
        hMaxSeekBar.setProgress(hMax);
        hMaxSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                hMax = i;
                textViewSignHMax.setText(hMax + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBar sMinSeekBar = (SeekBar) findViewById(R.id.seekBarSMin);
        sMinSeekBar.setMax(ConfigConstants.SEEK_BAR_MAX_VALUE);
        sMinSeekBar.setProgress(sMin);
        sMinSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sMin = i;
                textViewSignSMin.setText(sMin + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBar sMaxSeekBar = (SeekBar) findViewById(R.id.seekBarSMax);
        sMaxSeekBar.setMax(ConfigConstants.SEEK_BAR_MAX_VALUE);
        sMaxSeekBar.setProgress(sMax);
        sMaxSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sMax = i;
                textViewSignSMax.setText(sMax + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBar vMinSeekBar = (SeekBar) findViewById(R.id.seekBarVMin);
        vMinSeekBar.setMax(ConfigConstants.SEEK_BAR_MAX_VALUE);
        vMinSeekBar.setProgress(vMin);
        vMinSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                vMin = i;
                textViewSignVMin.setText(vMin + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBar vMaxSeekBar = (SeekBar) findViewById(R.id.seekBarVMax);
        vMaxSeekBar.setMax(ConfigConstants.SEEK_BAR_MAX_VALUE);
        vMaxSeekBar.setProgress(vMax);
        vMaxSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                vMax = i;
                textViewSignVMax.setText(vMax + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        // If BT is not on, request that it be enabled.
        // setupCommand() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        // otherwise set up the command service
        else {
            if (mCommandService==null)
                setupCommand();
        }
    }

    private void setupCommand() {
        // Initialize the BluetoothChatService to perform bluetooth connections
        mCommandService = new BluetoothCommandService(this, mHandler);
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothCommandService.STATE_CONNECTED:
                            switchConnectBT.setText("connected ");
                            switchConnectBT.append(mConnectedDeviceName);
                            break;
                        case BluetoothCommandService.STATE_CONNECTING:
                            switchConnectBT.setText("connecting");
                            break;
                        case BluetoothCommandService.STATE_LISTEN:
                        case BluetoothCommandService.STATE_NONE:
                            switchConnectBT.setText("not connected");
                            break;
                    }
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        if (mCommandService != null) {
            if (mCommandService.getState() == BluetoothCommandService.STATE_NONE) {
                mCommandService.start();
            }
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        screenHeight = height;
        screenWidth = width;
        laneRoi = new Rect(0, height/2, width, height/2);
        signRoi = new Rect(width/2, 0, width/2, height);
        mRgba = new Mat();
        mDetectionResult = new Mat(height, width, CvType.CV_8UC4);
        mLaneThreshold = new Mat(height, width, CvType.CV_8UC1, new Scalar(0));
        mSignShapeThreshold = new Mat(height, width, CvType.CV_8UC1, new Scalar(0));
        mCalibration = new Mat(height, width, CvType.CV_8UC1, new Scalar(0));
        mSignColorThreshold = new Mat();
        laneHoughLines = new Mat();
        signHoughCircles = new Mat();
        mGrayLane = new Mat();
        mGraySign = new Mat();
        mHsv = new Mat();
        hsvMin = new Scalar(20,20,20);
        hsvMax = new Scalar(150,180,120);
        mHierarchy = new Mat();
        contoursListf = new MatOfPoint2f();
        contoursList = new ArrayList<>();
        contoursListSpeedUp = new ArrayList<>();
        contoursListSpeedDown = new ArrayList<>();
        contoursListStop = new ArrayList<>();

        stopSignHsvPair = new Pair<>(new Scalar(164, 170, 109), new Scalar(255, 255, 255));
        speedUpSignHsvPair = new Pair<>(new Scalar(94, 148, 57), new Scalar(114, 255, 255));
        speedDownSignHsvPair = new Pair<>(new Scalar(27, 138, 149), new Scalar(57, 255, 255));
        genericErodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(2, 2));
        genericDilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
        contourMeanSizes = new HashMap<>();
        currentTimeMillis = System.currentTimeMillis();
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mDetectionResult.release();
        mLaneThreshold.release();
        mSignShapeThreshold.release();
        mCalibration.release();
        mSignColorThreshold.release();
        laneHoughLines.release();
        signHoughCircles.release();
        mGrayLane.release();
        mGraySign.release();
        mHsv.release();
        mHierarchy.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mDetectionResult = inputFrame.rgba();
        //calibration
        if(hsvThresholdOn){
            calibrateHsvValues(inputFrame);
            return mCalibration;
        }else{
            if(signColorDetectionOn){
                analyzeSignByColor(inputFrame);
            }

            if(laneShapeDetectionOn){
                analyzeLaneByShape(inputFrame);
            }

            if(signShapeDetectionOn) {
//                Integer i = 0;
//                Log.i(TAG, "INDEX:" + i);
//                new LaneDetection(inputFrame).execute();
//                Log.i(TAG, "INDEX:" + i);
                analyzeSignByShape(inputFrame);
            }

            if(signColorDetectionOn && signShapeDetectionOn){
                calculateSignResults();
            }
        }
        switch(spinnerValue){
            case "adaptive threshold":
            case "erode":
            case "canny contours":
            case "hough lines":
                return mLaneThreshold;
            case "result":
            default:
                return mDetectionResult;
        }

    }

    private long getElapsedTime(){
        return System.currentTimeMillis() - currentTimeMillis;
    }

    private void calculateSignResults(){
        Action action = getSignDetectionAction();
        if (action != null){
            printToScreen(action.name(), 3);
        }
        contoursListSpeedDown.clear();
        contoursListSpeedUp.clear();
        contoursListStop.clear();
    }

    private Action getSignDetectionAction() {
        if(meanCircleResult != null){
            for (MatOfPoint element : contoursListSpeedUp) {
                element.convertTo(contoursListf, CvType.CV_32F);
                if(Imgproc.pointPolygonTest(contoursListf, meanCircleResult.getCenter(), false)> 0){
                    return Action.SpeedUp;
                }
            }
            meanCircleResult = null;
        }
        return null;
    }
    private void calibrateHsvValues(CvCameraViewFrame inputFrame){
        mRgba = inputFrame.rgba();
        Imgproc.cvtColor(mRgba.submat(signRoi), mHsv, Imgproc.COLOR_RGB2HSV_FULL);

        hsvMin = new Scalar(hMin, sMin, vMin);
        hsvMax = new Scalar(hMax, sMax, sMax);
        Core.inRange(mHsv, hsvMin, hsvMax, mCalibration.submat(signRoi));
        Imgproc.erode(mCalibration.submat(signRoi), mCalibration.submat(signRoi), Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3)));
        Imgproc.dilate(mCalibration.submat(signRoi), mCalibration.submat(signRoi), Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(8, 8)));
//        Imgproc.findContours(mCalibration.submat(signRoi), contoursList, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//        Imgproc.drawContours(mDetectionResult.submat(signRoi), contoursList, -1, ConfigConstants.BLUE_COLOR);

        contoursList.clear();
    }

    private void analyzeSignByColor(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        Imgproc.cvtColor(mRgba.submat(signRoi), mHsv, Imgproc.COLOR_RGB2HSV_FULL);
        Core.inRange(mHsv, speedUpSignHsvPair.first, speedUpSignHsvPair.second, mSignColorThreshold);
        Imgproc.erode(mSignColorThreshold, mSignColorThreshold, Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3)));
        Imgproc.dilate(mSignColorThreshold, mSignColorThreshold, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(8, 8)));
        Imgproc.findContours(mSignColorThreshold, contoursListSpeedUp, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.drawContours(mDetectionResult.submat(signRoi), contoursListSpeedUp, -1, ConfigConstants.BLUE_COLOR);

        if(contoursListSpeedUp.size() > 0){
            cycleCounterSpeedUpSign++;
            if(cycleCounterSpeedUpSign >ConfigConstants.COUNTER_MAX_CYCLES){
                double mopMax=0;
                int mopMaxIndex=0;
                for (int i=0; i < contoursListSpeedUp.size(); i++) {
                    double area = Imgproc.contourArea(contoursListSpeedUp.get(i));
                    if(area > mopMax){
                        mopMax = area;
                        mopMaxIndex=i;
                    }
                }
                int edges = calcNumOfEdges(contoursListSpeedUp.get(mopMaxIndex));
                changeState(Action.None, Action.SpeedUp);
                printToScreen(Action.SpeedUp.name() + "|Ed-" +
                        edges + "|Sz-" +
                        + (int)mopMax +
                        "|Qy" + contoursListSpeedUp.size(), 2);
            }
        }else{
            cycleCounterSpeedUpSign =0;
        }

        Imgproc.cvtColor(mRgba.submat(signRoi), mHsv, Imgproc.COLOR_RGB2HSV_FULL);
        Core.inRange(mHsv, speedDownSignHsvPair.first, speedDownSignHsvPair.second, mSignColorThreshold);
        Imgproc.erode(mSignColorThreshold, mSignColorThreshold, Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3)));
        Imgproc.dilate(mSignColorThreshold, mSignColorThreshold, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(8, 8)));
        Imgproc.findContours(mSignColorThreshold, contoursListSpeedDown, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.drawContours(mDetectionResult.submat(signRoi), contoursListSpeedDown, -1, ConfigConstants.GREEN_COLOR);

        if(contoursListSpeedDown.size() > 0){
            cycleCounterSpeedDownSign++;
            if(cycleCounterSpeedDownSign >ConfigConstants.COUNTER_MAX_CYCLES){
                double mopMax=0;
                int mopMaxIndex=0;
                for (int i=0; i < contoursListSpeedDown.size(); i++) {
                    double area = Imgproc.contourArea(contoursListSpeedDown.get(i));
                    if(area > mopMax){
                        mopMax = area;
                        mopMaxIndex=i;
                    }
                }
                int edges = calcNumOfEdges(contoursListSpeedDown.get(mopMaxIndex));
                changeState(Action.None, Action.SpeedDown);
                printToScreen(Action.SpeedDown.name() + "|Ed-" +
                        edges + "|Sz-" +
                        + (int)mopMax +
                        "|Qy"  +
                        contoursListSpeedDown.size(), 3);
            }
        }else{
            cycleCounterSpeedDownSign = 0;
        }

        Imgproc.cvtColor(mRgba.submat(signRoi), mHsv, Imgproc.COLOR_RGB2HSV_FULL);
        Core.inRange(mHsv, stopSignHsvPair.first, stopSignHsvPair.second, mSignColorThreshold);
        Imgproc.erode(mSignColorThreshold, mSignColorThreshold, Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3)));
        Imgproc.dilate(mSignColorThreshold, mSignColorThreshold, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(8, 8)));
        Imgproc.findContours(mSignColorThreshold, contoursListStop, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.drawContours(mDetectionResult.submat(signRoi), contoursListStop, -1, ConfigConstants.RED_COLOR);

        if(contoursListStop.size() > 0){
            cycleCounterStopSign++;
            if(cycleCounterStopSign >ConfigConstants.COUNTER_MAX_CYCLES){
                double mopMax=0;
                int mopMaxIndex=0;
                for (int i=0; i < contoursListStop.size(); i++) {
                    double area = Imgproc.contourArea(contoursListStop.get(i));
                    if(area > mopMax){
                        mopMax = area;
                        mopMaxIndex=i;
                    }
                }
                int edges = calcNumOfEdges(contoursListStop.get(mopMaxIndex));
                changeState(Action.Stop);
                printToScreen(Action.Stop.name() + "|Ed-" +
                        edges + "|Sz-" +
                        + (int)mopMax +
                        "|Qy"  +
                        contoursListStop.size(), 4);
            }
        }else{
            cycleCounterStopSign =0;
        }

        contoursListSpeedDown.clear();
        contoursListSpeedUp.clear();
        contoursListStop.clear();
    }

    public int calcNumOfEdges(MatOfPoint thisContour) {
        Rect ret = null;

        MatOfPoint2f thisContour2f = new MatOfPoint2f();
        MatOfPoint approxContour = new MatOfPoint();
        MatOfPoint2f approxContour2f = new MatOfPoint2f();

        thisContour.convertTo(thisContour2f, CvType.CV_32FC2);

        Imgproc.approxPolyDP(thisContour2f, approxContour2f, Imgproc.arcLength(thisContour2f, true) * 0.02, true);

        approxContour2f.convertTo(approxContour, CvType.CV_32S);

            ret = Imgproc.boundingRect(approxContour);

        return ret != null? (int)approxContour.size().height : -1;
    }

    private void analyzeSignByShape(CvCameraViewFrame inputFrame) {
        //extract ROI
        mGraySign = inputFrame.gray().submat(signRoi);
        mGraySign.copyTo(mSignShapeThreshold.submat(signRoi));

        //perform adaptive threshold
        Imgproc.adaptiveThreshold(
                mGraySign,
                mGraySign,
                ConfigConstants.SIGN_DETECTION_BY_SHAPE_THRESHOLD_MAX_VALUE,
                Imgproc.ADAPTIVE_THRESH_MEAN_C,
                Imgproc.THRESH_BINARY,
                ConfigConstants.SIGN_DETECTION_BY_SHAPE_THRESHOLD_BLOCK_SIZE,
                ConfigConstants.SIGN_DETECTION_BY_SHAPE_THRESHOLD_C);
        mGraySign.copyTo(mSignShapeThreshold.submat(signRoi));

        //perform erode
        Imgproc.erode(mGraySign, mGraySign, genericErodeElement);
        mGraySign.copyTo(mSignShapeThreshold.submat(signRoi));

        //find contours
        double colorMean = Core.mean(mSignShapeThreshold.submat(signRoi)).val[0];
        Imgproc.Canny(
                mGraySign,
                mGraySign,
                colorMean * ConfigConstants.SIGN_DETECTION_BY_SHAPE_CANNY_THRESHOLD_1,
                colorMean * ConfigConstants.SIGN_DETECTION_BY_SHAPE_CANNY_THRESHOLD_2);

        //copy without frame (canny identify then as contours)
        mGraySign.colRange(1, screenWidth / 2 - 1).copyTo(mSignShapeThreshold.submat(0, screenHeight, screenWidth / 2 + 1, screenWidth - 1));

        //find circles
        Imgproc.HoughCircles(
                mSignShapeThreshold,
                signHoughCircles,
                Imgproc.CV_HOUGH_GRADIENT,
                ConfigConstants.SIGN_DETECTION_BY_SHAPE_HOUGH_CIRCLE_DP,
                ConfigConstants.SIGN_DETECTION_BY_SHAPE_HOUGH_CIRCLE_MIN_DISTANCE,
                ConfigConstants.SIGN_DETECTION_BY_SHAPE_HOUGH_CIRCLE_PARAM_1,
                ConfigConstants.SIGN_DETECTION_BY_SHAPE_HOUGH_CIRCLE_PARAM_2,
                ConfigConstants.SIGN_DETECTION_BY_SHAPE_HOUGH_CIRCLE_MIN_RADIUS,
                ConfigConstants.SIGN_DETECTION_BY_SHAPE_HOUGH_CIRCLE_MAX_RADIUS);

        int numberOfCircles = (signHoughCircles.rows() == 0) ? 0 : signHoughCircles.cols();
        Set<Circle> circleSet = new TreeSet<>();
        for (int i=0; i<numberOfCircles; i++) {
            // get the circle details, circleCoordinates[0, 1, 2] = (x,y,r)
            double[] circleCoordinates = signHoughCircles.get(0, i);
            circleSet.add(new Circle((int) circleCoordinates[2], new Point((int) circleCoordinates[0], (int) circleCoordinates[1])));
        }
        if(circleSet.size() > 0){
            int meanX = 0;
            int meanY = 0;
            int meanR = 0;
            for (Circle circle : circleSet) {
                meanX += circle.getCenter().x;
                meanY += circle.getCenter().y;
                meanR += circle.getRadius();
            }
            meanX = meanX/circleSet.size();
            meanY = meanY/circleSet.size();
            meanR = meanR/circleSet.size();
            meanCircleResult = new Circle(meanR, new Point(meanX, meanY));
            Imgproc.circle(mDetectionResult, new Point(meanX, meanY), meanR+20, ConfigConstants.GREEN_COLOR, ConfigConstants.THICKNESS_THICKER);
            Imgproc.rectangle(mDetectionResult, new Point(meanX - 5, meanY - 5),
                    new Point(meanX + 5, meanY + 5),
                    ConfigConstants.BLUE_COLOR, ConfigConstants.THICKNESS_FILL_SHAPE);
        }else{
            meanCircleResult = null;
            printToScreen(Action.None.name(), 2);
        }
/*
        //find rectangles
        //todo temp use of contourList & hierarchy
        Imgproc.findContours(mSignShapeThreshold, contoursListSpeedUp,mHierarchy,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint2f approxMop2f  = new MatOfPoint2f();
        MatOfPoint2f conMop2f;
        for (MatOfPoint con : contoursListSpeedUp) {
            conMop2f  = new MatOfPoint2f(con.toArray());
            Imgproc.approxPolyDP(conMop2f, approxMop2f, Imgproc.arcLength(conMop2f, true) * 0.02, true);
            if(Imgproc.contourArea(con) < 100 || Imgproc.isContourConvex(new MatOfPoint(approxMop2f.toArray()))){
                continue;
            }

            //if(approxMop2f.elemSize() == 3){
                Imgproc.drawContours(mDetectionResult, Collections.singletonList(new MatOfPoint(approxMop2f.toArray())), -1, ConfigConstants.BLACK_COLOR, ConfigConstants.THICKNESS_THICK);
            //}
        }

        //Imgproc.drawContours(mDetectionResult, contoursListSpeedUp, -1, ConfigConstants.BLACK_COLOR, ConfigConstants.THICKNESS_THICK);
        contoursListSpeedUp.clear();
*/
    }

    private void analyzeLaneByShape(CvCameraViewFrame inputFrame) {
        //extract ROI
        mGrayLane = inputFrame.gray().submat(laneRoi);
        //perform adaptive threshold
        Imgproc.adaptiveThreshold(
                mGrayLane,
                mGrayLane,
                ConfigConstants.LANE_DETECTION_BY_SHAPE_THRESHOLD_MAX_VALUE,
                Imgproc.ADAPTIVE_THRESH_MEAN_C,
                Imgproc.THRESH_BINARY,
                ConfigConstants.LANE_DETECTION_BY_SHAPE_THRESHOLD_BLOCK_SIZE,
                ConfigConstants.LANE_DETECTION_BY_SHAPE_THRESHOLD_C);
        if(spinnerValue.equals("adaptive threshold")){
            mGrayLane.rowRange(1, screenHeight / 2 - 1).copyTo(mLaneThreshold.submat(screenHeight / 2 + 1, screenHeight - 1, 0, screenWidth));
            return;
        }
        //perform erode
        Imgproc.erode(mGrayLane, mGrayLane, genericErodeElement);
        Imgproc.dilate(mGrayLane, mGrayLane, genericDilateElement);
        if(spinnerValue.equals("erode")){
            mGrayLane.rowRange(1, screenHeight / 2 - 1).copyTo(mLaneThreshold.submat(screenHeight / 2 + 1, screenHeight - 1, 0, screenWidth));
            return;
        }
        //find contours
        double laneMean = Core.mean(inputFrame.gray()).val[0];
        Imgproc.Canny(
                mGrayLane,
                mGrayLane,
                laneMean * ConfigConstants.LANE_DETECTION_BY_SHAPE_CANNY_THRESHOLD_1,
                laneMean * ConfigConstants.LANE_DETECTION_BY_SHAPE_CANNY_THRESHOLD_2);
        long initTime = System.currentTimeMillis();
        if(spinnerValue.equals("canny contours")){
            mGrayLane.rowRange(1, screenHeight / 2 - 1).copyTo(mLaneThreshold.submat(screenHeight / 2 + 1, screenHeight - 1, 0, screenWidth));
            return;
        }
        //find lines based on hough algorithm
        mGrayLane.rowRange(1, screenHeight / 2 - 1).copyTo(mLaneThreshold.submat(screenHeight / 2 + 1, screenHeight - 1, 0, screenWidth));
        Imgproc.HoughLinesP(
                mLaneThreshold,
                laneHoughLines,
                ConfigConstants.LANE_DETECTION_BY_SHAPE_HOUGH_LINE_P_RHO,
                ConfigConstants.LANE_DETECTION_BY_SHAPE_HOUGH_LINE_P_THETA,
                ConfigConstants.LANE_DETECTION_BY_SHAPE_HOUGH_LINE_P_THRESHOLD,
                ConfigConstants.LANE_DETECTION_BY_SHAPE_HOUGH_LINE_P_MIN_LINE_SIZE,
                ConfigConstants.LANE_DETECTION_BY_SHAPE_HOUGH_LINE_P_LINE_GAP);
        Log.i(TAG, "TIMECHECK:" + (System.currentTimeMillis() - initTime));
        Set<PairOfPoints> leftLines = new TreeSet<>();
        Set<PairOfPoints> rightLines = new TreeSet<>();
        Set<Line> lines= new HashSet<>();

        for (int i = 0; i < laneHoughLines.rows(); i++) {
            for (int x = 0; x < laneHoughLines.cols(); x++) {
                double[] vec = laneHoughLines.get(i, x);
                double x1 = vec[0],
                        y1 = vec[1],
                        x2 = vec[2],
                        y2 = vec[3];

                //discard horizontal and vertical lines
                if(Math.abs(y1-y2) < 30 || Math.abs(x1-x2) < 10){
                    continue;
                }

                //add lines to treeSets (lower point first)
                Point start;
                Point end;
                if(y1 > y2){
                    start = new Point(x1, y1);
                    end = new Point(x2, y2);
                }else{
                    end = new Point(x1, y1);
                    start = new Point(x2, y2);
                }
                //draw all lines to screen
                // Imgproc.line(mDetectionResult, start, end, ConfigConstants.BLUE_COLOR, ConfigConstants.THICKNESS_THICK);
                lines.add(new Line(new PairOfPoints(start, end),(start.y - end.y)/(start.x-end.x)));

//                if (x1 > screenWidth/2){
//                    rightLines.add(new PairOfPoints(start, end));
//                }else{
//                    leftLines.add(new PairOfPoints(start, end));
//                }
            }
        }

        //calc lines gradient
        double gradient;
        List<Line> linesList = CollectionHelper.toList(lines);
        Map<Double, List<Line>> lineGroup = new HashMap<>();
        for (Line line : linesList) {
            if(line.isLineGroup()){continue;}
            gradient = line.getGradient();
            for (Line innerLine : linesList) {
                if(line.equals(innerLine)||innerLine.isLineGroup()){continue;}
                if(Math.abs(gradient - innerLine.getGradient()) < 0.1){
                    if(!lineGroup.containsKey(gradient)){
                        lineGroup.put(gradient, new ArrayList<Line>());
                    }
                    lineGroup.get(gradient).add(innerLine);
                    innerLine.setIsLineGroup(true);
                }
            }
        }

        List<PairOfPoints> aggLines = new ArrayList<>();
        int groupSize;
        double avgPtLowX,avgPtLowY,avgPtHighX,avgPtHighY;
        for (List<Line> lineGroupList : lineGroup.values()) {
            groupSize = lineGroupList.size();
            avgPtLowX=0; avgPtLowY=0; avgPtHighX=0; avgPtHighY=0;
            for (Line lineGroupElement : lineGroupList) {
                avgPtLowX+=lineGroupElement.getPoints().getPtLow().x;
                avgPtLowY+=lineGroupElement.getPoints().getPtLow().y;
                avgPtHighX+=lineGroupElement.getPoints().getPtHigh().x;
                avgPtHighY+=lineGroupElement.getPoints().getPtHigh().y;
            }
            avgPtLowX /= groupSize;
            avgPtLowY /= groupSize;
            avgPtHighX /= groupSize;
            avgPtHighY /= groupSize;
            aggLines.add(new PairOfPoints(new Point(avgPtLowX, avgPtLowY), new Point(avgPtHighX, avgPtHighY)));
        }

        for (PairOfPoints aggLine : aggLines) {
            if (aggLine.getPtHigh().x > screenWidth/2){
                rightLines.add(aggLine);
            }else{
                leftLines.add(aggLine);
            }
            //draw agg lines
            Imgproc.line(mDetectionResult, aggLine.getPtLow(), aggLine.getPtHigh(), ConfigConstants.BLUE_COLOR, ConfigConstants.THICKNESS_THICK);
        }
        //filter only the lane lines
        laneHoughLines.empty();
        Point intersectionPoint;
        Point centerPoint = new Point(0,0);
        PairOfPoints roadRightLane = new PairOfPoints(new Point(screenWidth, screenHeight), new Point(screenWidth,0));
        PairOfPoints roadLeftLane = new PairOfPoints(new Point(0, screenHeight), new Point(0,0));
        //compare all right lines with left lines to find intersection points
        if(rightLines.size() > 0 || leftLines.size() > 0){
            for (PairOfPoints rightLine : rightLines) {
                for (PairOfPoints leftLine : leftLines) {
                    intersectionPoint = intersection(
                            (int) rightLine.getPtLow().x,
                            (int) rightLine.getPtLow().y,
                            (int) rightLine.getPtHigh().x,
                            (int) rightLine.getPtHigh().y,
                            (int) leftLine.getPtLow().x,
                            (int) leftLine.getPtLow().y,
                            (int) leftLine.getPtHigh().x,
                            (int) leftLine.getPtHigh().y);

                    //interPoint may be not relevant
                    if(intersectionPoint != null && intersectionPoint.y <(screenHeight)){//was screenHeight/2 but if turn hard will miss
                        Imgproc.circle(mDetectionResult, intersectionPoint, 5, ConfigConstants.WHITE_COLOR, ConfigConstants.THICKNESS_THICK);
                        if(Math.abs(intersectionPoint.x - screenWidth / 2) < Math.abs(centerPoint.x - screenWidth/2)){
                            centerPoint = intersectionPoint;
                            roadRightLane = rightLine;
                            roadLeftLane = leftLine;
                        }
                    }
                }
            }
            if(rightLines.size()==0 || leftLines.size()==0) {//no lines in one of sides
                if (rightLines.size() > 0) {
                    //should turn left
                    changeState(Action.TurnLeft);
                    printToScreen(Action.TurnLeft.name() + "|Qy-R" + rightLines.size() + " L" + leftLines.size());
                    Imgproc.line(mDetectionResult, roadRightLane.getPtLow(), roadRightLane.getPtHigh(), ConfigConstants.GREEN_COLOR, ConfigConstants.THICKNESS_THICKER);
                    Imgproc.line(mDetectionResult, roadLeftLane.getPtLow(), roadLeftLane.getPtHigh(), ConfigConstants.RED_COLOR, ConfigConstants.THICKNESS_THICKER);
                } else {
                    //should turn right
                    changeState(Action.TurnRight);
                    printToScreen(Action.TurnRight.name() + "|Qy-R" + rightLines.size() + " L" + leftLines.size());
                    Imgproc.line(mDetectionResult, roadRightLane.getPtLow(), roadRightLane.getPtHigh(), ConfigConstants.RED_COLOR, ConfigConstants.THICKNESS_THICKER);
                    Imgproc.line(mDetectionResult, roadLeftLane.getPtLow(), roadLeftLane.getPtHigh(), ConfigConstants.GREEN_COLOR, ConfigConstants.THICKNESS_THICKER);

                }
            }else if(Math.abs(centerPoint.x - screenWidth / 2) > ConfigConstants.LANE_DETECTION_BY_SHAPE_CAR_LANE_DEVIATION_THRESHOLD && centerPoint.x != 0){ //center point may remains 0,0
                if(centerPoint.x > screenWidth/2){
                    //should turn right
                    changeState(Action.TurnRight);
                    printToScreen(Action.TurnRight.name()+"|Qy-R"+rightLines.size()+ " L" + leftLines.size());
                    Imgproc.line(mDetectionResult, roadRightLane.getPtLow(), roadRightLane.getPtHigh(), ConfigConstants.RED_COLOR, ConfigConstants.THICKNESS_THICKER);
                    Imgproc.line(mDetectionResult, roadLeftLane.getPtLow(), roadLeftLane.getPtHigh(), ConfigConstants.GREEN_COLOR, ConfigConstants.THICKNESS_THICKER);
                }else{
                    //should turn left
                    changeState(Action.TurnLeft);
                    printToScreen(Action.TurnLeft.name()+"|Qy-R"+rightLines.size()+ " L" + leftLines.size());
                    Imgproc.line(mDetectionResult, roadRightLane.getPtLow(), roadRightLane.getPtHigh(), ConfigConstants.GREEN_COLOR, ConfigConstants.THICKNESS_THICKER);
                    Imgproc.line(mDetectionResult, roadLeftLane.getPtLow(), roadLeftLane.getPtHigh(), ConfigConstants.RED_COLOR, ConfigConstants.THICKNESS_THICKER);
                }
            }else{
                //strait line
                changeState(Action.Forward);
                printToScreen(Action.Forward.name()+"|Qy-R"+rightLines.size()+ " L" + leftLines.size());
                Imgproc.line(mDetectionResult, roadRightLane.getPtLow(), roadRightLane.getPtHigh(), ConfigConstants.GREEN_COLOR, ConfigConstants.THICKNESS_THICKER);
                Imgproc.line(mDetectionResult, roadLeftLane.getPtLow(), roadLeftLane.getPtHigh(), ConfigConstants.GREEN_COLOR, ConfigConstants.THICKNESS_THICKER);
            }
        }
    }

    private Point intersection(int p1x1, int p1y1, int p1x2, int p1y2, int p2x1, int p2y1, int p2x2, int p2y2) {
        int d = (p1x1-p1x2)*(p2y1-p2y2) - (p1y1-p1y2)*(p2x1-p2x2);
        if (d == 0) return null;

        int xi = ((p2x1-p2x2)*(p1x1*p1y2-p1y1*p1x2)-(p1x1-p1x2)*(p2x1*p2y2-p2y1*p2x2))/d;
        int yi = ((p2y1-p2y2)*(p1x1*p1y2-p1y1*p1x2)-(p1y1-p1y2)*(p2x1*p2y2-p2y1*p2x2))/d;

        return new Point(xi,yi);
    }

    private void printToScreen(String str){
        printToScreen(str, 1);
    }

    private void printToScreen(String str, int row){
        Imgproc.putText(
                mDetectionResult,
                str,
                new Point(40, 40 * row),
                ConfigConstants.DEFAULT_FONT,
                ConfigConstants.SCALE_SIZE_MEDIUM,
                ConfigConstants.GREEN_COLOR,
                ConfigConstants.THICKNESS_THICK);
    }

    private void changeState(Action action, Action speedAction){
        if(speedAction != null && action.equals(Action.None)){
            DetectionActivity.mCommandService.write(speedAction.getSignal().getBytes());
        }
        /*
            ToA("o={1, 2, 4};l={0}", "a"),   //o={1, 2, 4};l={0} Forward
            ToB("o={1, 2, 4};l={1}", "b"),   //o={1, 2, 4};l={1} Left
            ToC("o={1, 2, 4};l={2}", "c"),   //o={1, 2, 4};l={2} Right
            ToD("o={3};l={*}", "d"),         //o={3};l={*}       Stop
            ToE("o={0};l={*}", "e");         //o={0};l={*}       Wait
         */
        switch (action){
            case Forward:
                fsm.fire(FsmManager.FSMEvent.ToA, speedAction);
                break;
            case TurnLeft:
                fsm.fire(FsmManager.FSMEvent.ToB, speedAction);
                break;
            case TurnRight:
                fsm.fire(FsmManager.FSMEvent.ToC, speedAction);
                break;
            case Stop:
                fsm.fire(FsmManager.FSMEvent.ToD, speedAction);
                break;
            case Wait:
                fsm.fire(FsmManager.FSMEvent.ToE, speedAction);
                break;
        }
    }
    private void changeState(Action action){
        changeState(action, null);
    }
}