package ccbb.example.com.ccbb2;

import java.util.ArrayList;
import java.util.List;
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
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;
import org.squirrelframework.foundation.fsm.UntypedStateMachine;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import ccbb.example.com.ccbb2.dataobjects.PairOfPoints;
import ccbb.example.com.ccbb2.enums.Action;
import ccbb.example.com.ccbb2.fsm.CarDecisionFsm;

public class DetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG = "OCVSample::Activity";
    private Mat                 mHierarchy;
    private Mat                 mRgba;
    private Mat                 mGrayLane;
    private Mat                 mGraySign;
    private Mat                 mLaneThreshold;
    private Mat                 mSignColorThreshold;
    private Mat                 mSignShapeThreshold;
    private Mat                 mLaneResult;
    private Mat                 mHsv;
    private Mat                 erodeElement;
    private Mat genericErodeElement;
    private Mat                 dilateElement;
    private Mat                 contoursMat;
    private Mat                 laneHoughLines;
    private List<MatOfPoint>    contoursList = new ArrayList<>();
    private static final int    CONTOUR_SIZE_THRESHOLD = 85;
    private Scalar              hsvMin;
    private Scalar              hsvMax;
    private Scalar              CONTOUR_COLOR;

    //sign tracking
    private boolean             isSignRoi = false;
    private boolean             isSignHSV = false;
    private boolean             isSignMRP = false;
    private boolean             isTracked = false;

    //lane tracking
    private boolean             isLaneTrack = true;
    private boolean             isLaneRoi = false;
    private boolean             isLaneThd = false;
    private boolean             isLaneErd = false;
    private boolean             isLaneCtr = false;
    private boolean             isLaneHug = false;

    private double              laneThdConst = -2;
    private double              laneCtrMinConst = 0.66;
    private double              laneCtrMaxConst = 1.33;
    private double              laneMean;
    private static final int    SEEK_BAR_MAX_VALUE = 256;
    private int                 hMin = 0;
    private int                 hMax = 256;
    private int                 sMin = 0;
    private int                 sMax = 256;
    private int                 vMin = 0;
    private int                 vMax = 256;
    private int                 screenWidth;
    private int                 screenHeight;

    private Rect                laneRoi;
    private Rect                signRoi;

    private CameraBridgeViewBase mOpenCvCameraView;
    private BaseLoaderCallback  mLoaderCallback;
    private UntypedStateMachine fsm;
    private CarDecisionFsm      carDecisionFsm;
    public DetectionActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
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
                        //mOpenCvCameraView.setOnTouchListener(DetectionActivity.this);
                    } break;
                    default:
                    {
                        super.onManagerConnected(status);
                    } break;
                }
            }
        };

        initComponents();
        carDecisionFsm = new CarDecisionFsm();
        fsm = carDecisionFsm.getBuilder().newStateMachine(Action.Forward);
        //fsm.fire(FsmManager.FSMEvent.ToA, Action.None);
    }

    private void initComponents() {
        final TextView textViewLaneThdValue = (TextView) findViewById(R.id.textViewLaneThd);
        textViewLaneThdValue.setText(laneThdConst+"");
        final TextView textViewLaneCtrMinValue = (TextView) findViewById(R.id.textViewLaneCtrMin);
        textViewLaneCtrMinValue.setText(laneCtrMinConst+"");
        final TextView textViewLaneCtrMaxValue = (TextView) findViewById(R.id.textViewLaneCtrMax);
        textViewLaneCtrMaxValue.setText(laneCtrMaxConst+"");

        final TextView textViewSignHMin = (TextView) findViewById(R.id.textViewHMin);
        textViewSignHMin.setText("HMin "+ hMin);
        final TextView textViewSignHMax = (TextView) findViewById(R.id.textViewHMax);
        textViewSignHMax.setText("HMax "+ hMin);
        final TextView textViewSignSMin = (TextView) findViewById(R.id.textViewSMin);
        textViewSignSMin.setText("SMin "+ sMin);
        final TextView textViewSignSMax = (TextView) findViewById(R.id.textViewSMax);
        textViewSignSMax.setText("SMax "+ sMax);
        final TextView textViewSignVMin = (TextView) findViewById(R.id.textViewVMin);
        textViewSignVMin.setText("VMin "+ vMin);
        final TextView textViewSignVMax = (TextView) findViewById(R.id.textViewVMax);
        textViewSignVMax.setText("VMax "+ vMax);

        ToggleButton colorModelToggleButton = (ToggleButton) findViewById(R.id.colorModel);
        colorModelToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isSignHSV = isChecked;
            }
        });

        ToggleButton objectTypeToggleButton = (ToggleButton) findViewById(R.id.objType);
        objectTypeToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isLaneTrack = isChecked;
            }
        });

        SeekBar hMinSeekBar = (SeekBar) findViewById(R.id.seekBarHMin);
        hMinSeekBar.setMax(SEEK_BAR_MAX_VALUE);
        hMinSeekBar.setProgress(hMin);
        hMinSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                hMin = i;
                textViewSignHMin.setText(hMin+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBar hMaxSeekBar = (SeekBar) findViewById(R.id.seekBarHMax);
        hMaxSeekBar.setMax(SEEK_BAR_MAX_VALUE);
        hMaxSeekBar.setProgress(hMax);
        hMaxSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                hMax = i;
                textViewSignHMax.setText(hMax+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBar sMinSeekBar = (SeekBar) findViewById(R.id.seekBarSMin);
        sMinSeekBar.setMax(SEEK_BAR_MAX_VALUE);
        sMinSeekBar.setProgress(sMin);
        sMinSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sMin = i;
                textViewSignSMin.setText(sMin+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBar sMaxSeekBar = (SeekBar) findViewById(R.id.seekBarSMax);
        sMaxSeekBar.setMax(SEEK_BAR_MAX_VALUE);
        sMaxSeekBar.setProgress(sMax);
        sMaxSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                sMax = i;
                textViewSignSMax.setText(sMax+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBar vMinSeekBar = (SeekBar) findViewById(R.id.seekBarVMin);
        vMinSeekBar.setMax(SEEK_BAR_MAX_VALUE);
        vMinSeekBar.setProgress(vMin);
        vMinSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                vMin = i;
                textViewSignVMin.setText(vMin+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBar vMaxSeekBar = (SeekBar) findViewById(R.id.seekBarVMax);
        vMaxSeekBar.setMax(SEEK_BAR_MAX_VALUE);
        vMaxSeekBar.setProgress(vMax);
        vMaxSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                vMax = i;
                textViewSignVMax.setText(vMax+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBar laneThdSeekBar = (SeekBar) findViewById(R.id.seekBarThd);
        laneThdSeekBar.setProgress((int)laneThdConst*(-25));
        laneThdSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                laneThdConst = (double) i / -25.0;
                vMax = i;
                textViewLaneThdValue.setText(laneThdConst+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        final SeekBar laneCtrMinSeekBar = (SeekBar) findViewById(R.id.seekBarCtrMin);
        laneCtrMinSeekBar.setProgress((int)laneCtrMinConst*50);
        laneCtrMinSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                laneCtrMinConst = (double) i / 50.0;
                textViewLaneCtrMinValue.setText(laneCtrMinConst+"");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SeekBar laneCtrMaxSeekBar = (SeekBar) findViewById(R.id.seekBarCtrMax);
        laneCtrMaxSeekBar.setProgress((int) laneCtrMaxConst * 50);
        laneCtrMaxSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                laneCtrMaxConst = (double) i / 50.0;
                textViewLaneCtrMaxValue.setText(laneCtrMaxConst + "");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        Switch switchSignRoi = (Switch) findViewById(R.id.switchSignRoi);
        switchSignRoi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isSignRoi = b;
            }
        });

        Switch switchMorph = (Switch) findViewById(R.id.switchLaneMorph);
        switchMorph.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isSignMRP = b;
            }
        });

        Switch switchTracked = (Switch) findViewById(R.id.switchTrack);
        switchTracked.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isTracked = b;
            }
        });

        Switch switchRoi = (Switch) findViewById(R.id.switchRoi);
        switchRoi.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isLaneRoi = b;
            }
        });

        Switch switchThd = (Switch) findViewById(R.id.switchThd);
        switchThd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isLaneThd = b;
            }
        });

        Switch switchErd = (Switch) findViewById(R.id.switchErd);
        switchErd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isLaneErd = b;
            }
        });

        Switch switchCtr = (Switch) findViewById(R.id.switchCtr);
        switchCtr.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isLaneCtr = b;
            }
        });

        Switch switchHug = (Switch) findViewById(R.id.switchHug);
        switchHug.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isLaneHug = b;
            }
        });
    }

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
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mLaneResult = new Mat(height, width, CvType.CV_8UC4);
        mLaneThreshold = new Mat(height, width, CvType.CV_8UC1, new Scalar(0));
        mSignShapeThreshold = new Mat(height, width, CvType.CV_8UC1, new Scalar(0));
        mSignColorThreshold = new Mat(height, width, CvType.CV_8UC4, new Scalar(0));
        laneHoughLines = new Mat();
        mGrayLane = new Mat();
        mGraySign = new Mat();
        mHsv = new Mat();

        mHierarchy = new Mat();
        contoursMat = new Mat();
        contoursList = new ArrayList<>();
        CONTOUR_COLOR = new Scalar(255,0,0,255);

        genericErodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(2, 2));
        erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3));
        dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(8, 8));
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        if(isLaneTrack){
            return analyzeRoadByShape(inputFrame);
        }else {
            //return analyzeSignByColor(inputFrame);
            return analyzeSignByShape(inputFrame);
        }
    }

    private Mat analyzeSignByColor(CvCameraViewFrame inputFrame) {
        if(isSignRoi){
            mRgba = inputFrame.rgba().submat(signRoi);;
            mRgba.copyTo(mSignColorThreshold.submat(signRoi));
            if (isSignHSV){
                Imgproc.cvtColor(mRgba, mRgba, Imgproc.COLOR_RGB2HSV_FULL);
                hsvMin = new Scalar(hMin,sMin,vMin);
                hsvMax = new Scalar(hMax,sMax, vMax);
                Core.inRange(mRgba, hsvMin, hsvMax, mRgba);
                mRgba.copyTo(mSignColorThreshold);
                if (isSignMRP) {
                    Imgproc.erode(mRgba, mRgba, erodeElement);
                    Imgproc.erode(mRgba, mRgba, erodeElement);
                    Imgproc.dilate(mRgba, mRgba, dilateElement);
                    Imgproc.dilate(mRgba, mRgba, dilateElement);
                    mRgba.copyTo(mSignColorThreshold);
                    if (isTracked) {
                        //trackFilteredObject();
                        Imgproc.findContours(mRgba, contoursList, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
                    }
                }
            }
            return mSignColorThreshold;
        }
        return inputFrame.rgba();
    }

    private Mat analyzeSignByShape(CvCameraViewFrame inputFrame) {
        //extract ROI
        mGraySign = inputFrame.gray().submat(signRoi);
        mGraySign.copyTo(mSignShapeThreshold.submat(signRoi));
        //perform adaptive threshold
        Imgproc.adaptiveThreshold(mGraySign, mGraySign, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 3, laneThdConst);
        mGraySign.copyTo(mSignShapeThreshold.submat(signRoi));
        //perform erode
        Imgproc.erode(mGraySign, mGraySign, genericErodeElement);
        mGraySign.copyTo(mSignShapeThreshold.submat(signRoi));
        //find contours
        double colorMean = Core.mean(mSignShapeThreshold.submat(signRoi)).val[0];
        Imgproc.Canny(mGraySign, mGraySign, colorMean * 0.66, colorMean * 1.33);
        mGraySign.rowRange( 1, screenHeight / 2 - 1).copyTo(mLaneThreshold.submat(screenHeight / 2 + 1, screenHeight - 1, 0, screenWidth));
        return mSignShapeThreshold; //todo change
    }

    private Mat analyzeRoadByShape(CvCameraViewFrame inputFrame) {
        mLaneResult = inputFrame.rgba();
        //extract ROI
        mGrayLane = inputFrame.gray().submat(laneRoi);
        mGrayLane.copyTo(mLaneThreshold.submat(laneRoi));
        //perform adaptive threshold
        Imgproc.adaptiveThreshold(mGrayLane, mGrayLane, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 3, laneThdConst);
        mGrayLane.copyTo(mLaneThreshold.submat(laneRoi));
        //perform erode
        Imgproc.erode(mGrayLane, mGrayLane, genericErodeElement);
        mGrayLane.copyTo(mLaneThreshold.submat(laneRoi));
        //find contours
        laneMean = Core.mean(inputFrame.gray()).val[0];
        Imgproc.Canny(mGrayLane, mGrayLane, laneMean * laneCtrMinConst, laneMean * laneCtrMaxConst);
        mGrayLane.rowRange(1, screenHeight / 2 - 1).copyTo(mLaneThreshold.submat(screenHeight / 2 + 1, screenHeight - 1, 0, screenWidth));
        //find lines based on hough algorithm
        Set<PairOfPoints> leftLines = new TreeSet<>();
        Set<PairOfPoints> rightLines = new TreeSet<>();
        int threshold = 50;
        int minLineSize = 50;
        int lineGap = 20;

        Imgproc.HoughLinesP(mLaneThreshold, laneHoughLines, 1, Math.PI / 180, threshold, minLineSize, lineGap);
        for (int i = 0; i < laneHoughLines.rows(); i++) {
            for (int x = 0; x < laneHoughLines.cols(); x++) {
                double[] vec = laneHoughLines.get(i, x);
                double x1 = vec[0],
                        y1 = vec[1],
                        x2 = vec[2],
                        y2 = vec[3];

                //discard horizontal lines
                if(Math.abs(y1-y2) < 30){
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
                if (x1 > screenWidth/2){
                    rightLines.add(new PairOfPoints(start, end));
                }else{
                    leftLines.add(new PairOfPoints(start, end));
                }
            }
        }
        laneHoughLines.empty();
        Point intersectionPoint;
        Point centerPoint = new Point(0,0);
        PairOfPoints roadRightLane = new PairOfPoints(new Point(screenWidth, screenHeight), new Point(screenWidth,0));
        PairOfPoints roadLeftLane = new PairOfPoints(new Point(0, screenHeight), new Point(0,0));

        if(rightLines.size() > 0 || leftLines.size() > 0){//todo:test case when only one line is visible
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

                    if(intersectionPoint != null && intersectionPoint.y <(screenHeight/2)){//interPoint may be not relevant
                        if(Math.abs(intersectionPoint.x - screenWidth/2) < Math.abs(centerPoint.x - screenWidth/2)){
                            centerPoint = intersectionPoint;
                            roadRightLane = rightLine;
                            roadLeftLane = leftLine;
                        }
                    }
                }
            }

            int deviationThreshold = 40;
            if(Math.abs(centerPoint.x - screenWidth/2) > deviationThreshold && centerPoint.x != 0){//center point may remains 0,0
                if(centerPoint.x > screenWidth/2){
                    //should turn right
                    Imgproc.line(mLaneResult, roadRightLane.getPtLow(), roadRightLane.getPtHigh(), new Scalar(0, 255, 0), 3);
                    Imgproc.line(mLaneResult, roadLeftLane.getPtLow(), roadLeftLane.getPtHigh(), new Scalar( 255, 0, 0), 3);
                }else{
                    //should turn left
                    Imgproc.line(mLaneResult, roadRightLane.getPtLow(), roadRightLane.getPtHigh(), new Scalar(255, 0, 0), 3);
                    Imgproc.line(mLaneResult, roadLeftLane.getPtLow(), roadLeftLane.getPtHigh(), new Scalar( 0, 255, 0), 3);
                }
            }else{
                //strait line
                Imgproc.line(mLaneResult, roadRightLane.getPtLow(), roadRightLane.getPtHigh(), new Scalar(255, 0, 0), 3);
                Imgproc.line(mLaneResult, roadLeftLane.getPtLow(), roadLeftLane.getPtHigh(), new Scalar(255, 0, 0), 3);
            }
        }
        return mLaneResult;
    }

    private Point intersection(int p1x1, int p1y1, int p1x2, int p1y2, int p2x1, int p2y1, int p2x2, int p2y2) {
        int d = (p1x1-p1x2)*(p2y1-p2y2) - (p1y1-p1y2)*(p2x1-p2x2);
        if (d == 0) return null;

        int xi = ((p2x1-p2x2)*(p1x1*p1y2-p1y1*p1x2)-(p1x1-p1x2)*(p2x1*p2y2-p2y1*p2x2))/d;
        int yi = ((p2y1-p2y2)*(p1x1*p1y2-p1y1*p1x2)-(p1y1-p1y2)*(p2x1*p2y2-p2y1*p2x2))/d;

        return new Point(xi,yi);
    }

    private Mat getROI(CvCameraViewFrame inputFrame) {
//        printLog(mSignColorThreshold.get(5, 5), "before 1");
//        printLog(mSignColorThreshold.get((int) mGrayLane.size().height - 5, (int) mGrayLane.size().width - 5), "before 2");
        Rect roi = new Rect(0,(int) mGrayLane.size().height / 2, (int) mGrayLane.size().width,(int) mGrayLane.size().height / 2);
//        mSignColorThreshold = mGrayLane.submat(roi);
//        printLog(mSignColorThreshold.get(5, 5), "after copy 1");
//        printLog(mSignColorThreshold.get((int) mGrayLane.size().height - 5, (int) mGrayLane.size().width - 5), "after copy 2");

        return new Mat(inputFrame.gray(),roi);
    }

    private void printLog(double[] doubles, String m){
        if(doubles != null){
            for (double  d : doubles) {
                Log.d(TAG, m + " : " + d);
            }
        }else{
            Log.d(TAG, m + " : null");
        }

    }
    private void trackFilteredObject() {
        mSignColorThreshold.copyTo(contoursMat);
        contoursList.clear();
        Imgproc.findContours(contoursMat, contoursList, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        if (!isSignHSV){
            findAndDrawCenterMassContour();
        }
        Imgproc.drawContours(mRgba, contoursList, -1, CONTOUR_COLOR);
    }

    private void findAndDrawCenterMassContour() {
        for (MatOfPoint contourMOP : contoursList) {
            if(contourMOP.toArray().length > CONTOUR_SIZE_THRESHOLD){
                Point centerPoint = getCenterPoint(contourMOP.toArray());
                Imgproc.drawMarker(mRgba,centerPoint,new Scalar(3));
            }
        }
    }

    private Point getCenterPoint(Point[] contourMOP) {
        Point pMin = new Point();
        Point pMax = new Point();
        pMin.x = pMax.x = contourMOP[0].x;
        pMin.y = pMax.y = contourMOP[0].y;

        for (int i = 1; i < contourMOP.length; i++) {
            if(contourMOP[i].x > pMax.x){
                pMax.x = contourMOP[i].x;
            }
            if(contourMOP[i].y > pMax.y){
                pMax.y = contourMOP[i].y;
            }
            if(contourMOP[i].x < pMin.x){
                pMin.x = contourMOP[i].x;
            }
            if(contourMOP[i].y < pMin.y){
                pMin.y = contourMOP[i].y;
            }
        }

        Point p = new Point();
        p.x = pMin.x + ((pMax.x - pMin.x) / 2);
        p.y = pMin.y + ((pMax.y - pMin.y) / 2);
        Log.i(TAG, "Center:" + p.toString() + ", Max:" + pMax.toString() + ", Min:" + pMin.toString());
        return p;
    }


    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }
}

//not in use...
    /*


    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

//        mDetector.setHsvColor(mBlobColorHsv);

//        Imgproc.resize(mDetector.getSpectrum(), mSpectrum, SPECTRUM_SIZE);


        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }
 */