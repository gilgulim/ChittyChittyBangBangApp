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
import android.widget.TextView;
import android.widget.ToggleButton;

import ccbb.example.com.ccbb2.dataobjects.Circle;
import ccbb.example.com.ccbb2.dataobjects.PairOfPoints;
import ccbb.example.com.ccbb2.enums.Action;
import ccbb.example.com.ccbb2.fsm.CarDecisionFsm;

public class DetectionActivity extends Activity implements OnTouchListener, CvCameraViewListener2 {
    private static final String  TAG = "CCBB::";

    //sign detection by color
    private Mat mHierarchy;
    private Mat mRgba;
    private Mat mSignColorThreshold;
    private Mat mHsv;
    private List<MatOfPoint> contoursList = new ArrayList<>();
    private Scalar hsvMin;
    private Scalar hsvMax;

    //lane detection by shape
    private Mat mGrayLane;
    private Mat mLaneThreshold;
    private Mat laneHoughLines;

    //sign detection by shape
    private Mat mGraySign;
    private Mat mSignShapeThreshold;
    private Mat signHoughCircles;

    //generic
    private Mat mDetectionResult;
    private Mat genericErodeElement;
    private boolean laneShapeDetectionOn;
    private boolean signShapeDetectionOn;
    private boolean signColorDetectionOn;

    private int                 hMin = 0;
    private int                 hMax = 256;
    private int                 sMin = 0;
    private int                 sMax = 256;
    private int                 vMin = 0;
    private int                 vMax = 256;
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
    }

    private void initToggleButtons() {
        ToggleButton laneShapeDetectionToggleButton = (ToggleButton) findViewById(R.id.laneShapeToggleButton);
        laneShapeDetectionToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                laneShapeDetectionOn = isChecked;
            }
        });

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
        mRgba = new Mat();
        mDetectionResult = new Mat(height, width, CvType.CV_8UC4);
        mLaneThreshold = new Mat(height, width, CvType.CV_8UC1, new Scalar(0));
        mSignShapeThreshold = new Mat(height, width, CvType.CV_8UC1, new Scalar(0));
        mSignColorThreshold = new Mat();
        laneHoughLines = new Mat();
        signHoughCircles = new Mat();
        mGrayLane = new Mat();
        mGraySign = new Mat();
        mHsv = new Mat();
        hsvMin = new Scalar(20,20,20);
        hsvMax = new Scalar(150,180,120);
        mHierarchy = new Mat();
        contoursList = new ArrayList<>();

        genericErodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(2, 2));
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mDetectionResult.release();
        mLaneThreshold.release();
        mSignShapeThreshold.release();
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
        if(signColorDetectionOn){
            analyzeSignByColor(inputFrame);
        }

        if(laneShapeDetectionOn){
            analyzeLaneByShape(inputFrame);
        }

        if(signShapeDetectionOn) {
            analyzeSignByShape(inputFrame);
        }


            return mDetectionResult;
    }

    private void analyzeSignByColor(CvCameraViewFrame inputFrame){
        mRgba = inputFrame.rgba();
        Imgproc.cvtColor(mRgba.submat(signRoi), mHsv, Imgproc.COLOR_RGB2HSV_FULL);

        hsvMin = new Scalar(hMin,sMin,vMin);
        hsvMax = new Scalar(hMax,sMax,vMax);
        Core.inRange(mHsv, hsvMin, hsvMax, mSignColorThreshold);
        
        Imgproc.findContours(mSignColorThreshold, contoursList, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        Imgproc.drawContours(mDetectionResult.submat(signRoi), contoursList, -1, ConfigConstants.BLUE_COLOR);
        contoursList.clear();
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

            Imgproc.circle(mDetectionResult, new Point(meanX, meanY), meanR+20, ConfigConstants.GREEN_COLOR, ConfigConstants.THICKNESS_THICKER);
            Imgproc.rectangle(mDetectionResult, new Point(meanX - 5, meanY - 5),
                    new Point(meanX + 5, meanY + 5),
                    ConfigConstants.BLUE_COLOR, ConfigConstants.THICKNESS_FILL_SHAPE);
        }else{
            printActionToScreen(Action.None, 2);
        }
/*
        //find rectangles
        //todo temp use of contourList & hierarchy
        Imgproc.findContours(mSignShapeThreshold, contoursList,mHierarchy,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint2f approxMop2f  = new MatOfPoint2f();
        MatOfPoint2f conMop2f;
        for (MatOfPoint con : contoursList) {
            conMop2f  = new MatOfPoint2f(con.toArray());
            Imgproc.approxPolyDP(conMop2f, approxMop2f, Imgproc.arcLength(conMop2f, true) * 0.02, true);
            if(Imgproc.contourArea(con) < 100 || Imgproc.isContourConvex(new MatOfPoint(approxMop2f.toArray()))){
                continue;
            }

            //if(approxMop2f.elemSize() == 3){
                Imgproc.drawContours(mDetectionResult, Collections.singletonList(new MatOfPoint(approxMop2f.toArray())), -1, ConfigConstants.BLACK_COLOR, ConfigConstants.THICKNESS_THICK);
            //}
        }

        //Imgproc.drawContours(mDetectionResult, contoursList, -1, ConfigConstants.BLACK_COLOR, ConfigConstants.THICKNESS_THICK);
        contoursList.clear();
*/
    }

    private void analyzeLaneByShape(CvCameraViewFrame inputFrame) {
        //extract ROI
        mGrayLane = inputFrame.gray().submat(laneRoi);
        mGrayLane.copyTo(mLaneThreshold.submat(laneRoi));

        //perform adaptive threshold
        Imgproc.adaptiveThreshold(
                mGrayLane,
                mGrayLane,
                ConfigConstants.LANE_DETECTION_BY_SHAPE_THRESHOLD_MAX_VALUE,
                Imgproc.ADAPTIVE_THRESH_MEAN_C,
                Imgproc.THRESH_BINARY,
                ConfigConstants.LANE_DETECTION_BY_SHAPE_THRESHOLD_BLOCK_SIZE,
                ConfigConstants.LANE_DETECTION_BY_SHAPE_THRESHOLD_C);
        mGrayLane.copyTo(mLaneThreshold.submat(laneRoi));

        //perform erode
        Imgproc.erode(mGrayLane, mGrayLane, genericErodeElement);
        //mGrayLane.copyTo(mLaneThreshold.submat(laneRoi));

        //find contours
        double laneMean = Core.mean(inputFrame.gray()).val[0];
        Imgproc.Canny(
                mGrayLane,
                mGrayLane,
                laneMean * ConfigConstants.LANE_DETECTION_BY_SHAPE_CANNY_THRESHOLD_1,
                laneMean * ConfigConstants.LANE_DETECTION_BY_SHAPE_CANNY_THRESHOLD_2);
        mGrayLane.rowRange(1, screenHeight / 2 - 1).copyTo(mLaneThreshold.submat(screenHeight / 2 + 1, screenHeight - 1, 0, screenWidth));

        //find lines based on hough algorithm
        Imgproc.HoughLinesP(
                mLaneThreshold,
                laneHoughLines,
                ConfigConstants.LANE_DETECTION_BY_SHAPE_HOUGH_LINE_P_RHO,
                ConfigConstants.LANE_DETECTION_BY_SHAPE_HOUGH_LINE_P_THETA,
                ConfigConstants.LANE_DETECTION_BY_SHAPE_HOUGH_LINE_P_THRESHOLD,
                ConfigConstants.LANE_DETECTION_BY_SHAPE_HOUGH_LINE_P_MIN_LINE_SIZE,
                ConfigConstants.LANE_DETECTION_BY_SHAPE_HOUGH_LINE_P_LINE_GAP);

        Set<PairOfPoints> leftLines = new TreeSet<>();
        Set<PairOfPoints> rightLines = new TreeSet<>();

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
                    if(intersectionPoint != null && intersectionPoint.y <(screenHeight/2)){
                        if(Math.abs(intersectionPoint.x - screenWidth/2) < Math.abs(centerPoint.x - screenWidth/2)){
                            centerPoint = intersectionPoint;
                            roadRightLane = rightLine;
                            roadLeftLane = leftLine;
                        }
                    }
                }
            }

            //center point may remains 0,0
            if(Math.abs(centerPoint.x - screenWidth/2) > ConfigConstants.LANE_DETECTION_BY_SHAPE_CAR_LANE_DEVIATION_THRESHOLD && centerPoint.x != 0){
                if(centerPoint.x > screenWidth/2){
                    //should turn right
                    printActionToScreen(Action.TurnRight);
                    Imgproc.line(mDetectionResult, roadRightLane.getPtLow(), roadRightLane.getPtHigh(), ConfigConstants.RED_COLOR, ConfigConstants.THICKNESS_THICKER);
                    Imgproc.line(mDetectionResult, roadLeftLane.getPtLow(), roadLeftLane.getPtHigh(), ConfigConstants.GREEN_COLOR, ConfigConstants.THICKNESS_THICKER);
                }else{
                    //should turn left
                    printActionToScreen(Action.TurnLeft);
                    Imgproc.line(mDetectionResult, roadRightLane.getPtLow(), roadRightLane.getPtHigh(), ConfigConstants.GREEN_COLOR, ConfigConstants.THICKNESS_THICKER);
                    Imgproc.line(mDetectionResult, roadLeftLane.getPtLow(), roadLeftLane.getPtHigh(), ConfigConstants.RED_COLOR, ConfigConstants.THICKNESS_THICKER);
                }
            }else{
                //strait line
                printActionToScreen(Action.Forward);
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

    private void printActionToScreen(Action action){
        printActionToScreen(action, 1);
    }

    private void printActionToScreen(Action action, int row){
        Imgproc.putText(
                mDetectionResult,
                action.name(),
                new Point(40, 40 * row),
                ConfigConstants.DEFAULT_FONT,
                ConfigConstants.SCALE_SIZE_LARGE,
                ConfigConstants.GREEN_COLOR,
                ConfigConstants.THICKNESS_THICK);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }
}