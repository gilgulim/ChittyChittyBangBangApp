package ccbb.example.com.ccbb2;

import java.util.ArrayList;
import java.util.List;

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
import android.widget.ToggleButton;

import ccbb.example.com.ccbb2.enums.ROIType;

public class DetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG = "OCVSample::Activity";
    private Mat                 mHierarchy;
    private Mat                 mRgba;
    private Mat                 mGray;
    private Mat                 mHsv;
    private Mat                 erodeElement;
    private Mat                 dilateElement;
    private Mat                 threshold;
    private Mat                 contoursMat;
    private List<MatOfPoint>    contoursList = new ArrayList<>();
    private static final int    CONTOUR_SIZE_THRESHOLD = 85;
    private Scalar              hsvMin;
    private Scalar              hsvMax;
    private Scalar              CONTOUR_COLOR;
    private boolean             isHSVState = false;
    private boolean             isMorph = false;
    private boolean             isTracked = false;
    private static final boolean TRACK_LANE=true;

    private static final int    SEEK_BAR_MAX_VALUE = 256;
    private int                 hMin = 0;
    private int                 hMax = 256;
    private int                 sMin = 0;
    private int                 sMax = 256;
    private int                 vMin = 0;
    private int                 vMax = 256;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
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

        initComponents();
    }

    private void initComponents() {
        ToggleButton colorModelToggleButton = (ToggleButton) findViewById(R.id.colorModel);
        colorModelToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isHSVState = isChecked;
            }
        });

        SeekBar hMinSeekBar = (SeekBar) findViewById(R.id.seekBarHMin);
        hMinSeekBar.setMax(SEEK_BAR_MAX_VALUE);
        hMinSeekBar.setProgress(hMin);
        hMinSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                hMin = i;
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
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Switch switchMorph = (Switch) findViewById(R.id.switchMorph);
        switchMorph.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isMorph = b;
            }
        });

        Switch switchTracked = (Switch) findViewById(R.id.switchTrack);
        switchTracked.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                isTracked = b;
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
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat();
        mHsv = new Mat();
        threshold = new Mat();
        mHierarchy = new Mat();
        erodeElement = new Mat();
        dilateElement = new Mat();

        contoursMat = new Mat();
        contoursList = new ArrayList<>();
        CONTOUR_COLOR = new Scalar(255,0,0,255);

        erodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(3, 3));
        dilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(8, 8));
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        if(!TRACK_LANE){
            mRgba = inputFrame.rgba();

            // Convert input frame to HSV in order to displays it back to screen
            filterHSVRange();
            if (isMorph){
                morphOps();
            }

            if (isTracked){
                trackFilteredObject();
            }

            if(isHSVState){
                return threshold;
            }else{
                return mRgba;
            }
        }else{
            mGray = inputFrame.gray();
            getROI(ROIType.Lane);

            return mGray;
        }
    }

    private void getROI(ROIType roiType) {
        if(roiType.name().equals(ROIType.Lane.name())){
            
        }
    }

    private void trackFilteredObject() {
        threshold.copyTo(contoursMat);
        contoursList.clear();
        Imgproc.findContours(contoursMat, contoursList, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        if (!isHSVState){
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

    private void morphOps() {
        Imgproc.erode(threshold, threshold, erodeElement);
        Imgproc.erode(threshold, threshold, erodeElement);
        Imgproc.dilate(threshold, threshold, dilateElement);
        Imgproc.dilate(threshold, threshold, dilateElement);
    }

    private void filterHSVRange() {
        Imgproc.cvtColor(mRgba, mHsv, Imgproc.COLOR_RGB2HSV_FULL);
        hsvMin = new Scalar(hMin,sMin,vMin);
        hsvMax = new Scalar(hMax,sMax,vMax);
        Core.inRange(mHsv,hsvMin,hsvMax,threshold);
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