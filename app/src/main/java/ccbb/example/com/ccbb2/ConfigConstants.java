package ccbb.example.com.ccbb2;

import org.opencv.core.Scalar;

/**
 * Created by gil on 24/02/2016.
 */
public class ConfigConstants {
    public static final Scalar GREEN_COLOR = new Scalar(0, 255, 0);
    public static final Scalar RED_COLOR = new Scalar(0, 255, 0);
    public static final Scalar BLUE_COLOR = new Scalar(0, 0, 255);
    public static final Scalar WHITE_COLOR = new Scalar(255, 255, 255);
    public static final Scalar BLACK_COLOR = new Scalar(0, 0, 0);

    public static final int THICKNESS_THIN = 1;
    public static final int THICKNESS_THICK = 2;
    public static final int THICKNESS_THICKER = 3;
    public static final int THICKNESS_FILL_SHAPE = -1;

    public static final int DEFAULT_FONT = 1;

    public static final int SCALE_SIZE_SMALL = 1;
    public static final int SCALE_SIZE_MEDIUM = 2;
    public static final int SCALE_SIZE_LARGE = 3;

    public static final double SIGN_DETECTION_BY_SHAPE_THRESHOLD_C = -2;
    public static final int SIGN_DETECTION_BY_SHAPE_THRESHOLD_BLOCK_SIZE = 3;
    public static final int SIGN_DETECTION_BY_SHAPE_THRESHOLD_MAX_VALUE = 255;

    public static final double SIGN_DETECTION_BY_SHAPE_CANNY_THRESHOLD_1 = 0.66;
    public static final double SIGN_DETECTION_BY_SHAPE_CANNY_THRESHOLD_2 = 1.33;

    public static final double SIGN_DETECTION_BY_SHAPE_HOUGH_CIRCLE_DP = 1.2; // accumulator value
    public static final int SIGN_DETECTION_BY_SHAPE_HOUGH_CIRCLE_MIN_DISTANCE = 100; // minimum distance between the center coordinates of detected circles in pixels
    public static final int SIGN_DETECTION_BY_SHAPE_HOUGH_CIRCLE_PARAM_1 = 70; //gradient value used to handle edge detection
    public static final int SIGN_DETECTION_BY_SHAPE_HOUGH_CIRCLE_PARAM_2 = 50; //Accumulator threshold value for thecv2.CV_HOUGH_GRADIENT method. (smaller - more cicrles with false circles)
    public static final int SIGN_DETECTION_BY_SHAPE_HOUGH_CIRCLE_MIN_RADIUS = 40;
    public static final int SIGN_DETECTION_BY_SHAPE_HOUGH_CIRCLE_MAX_RADIUS = 100;

    public static final double LANE_DETECTION_BY_SHAPE_THRESHOLD_C = -2;
    public static final int LANE_DETECTION_BY_SHAPE_THRESHOLD_BLOCK_SIZE = 3;
    public static final int LANE_DETECTION_BY_SHAPE_THRESHOLD_MAX_VALUE = 255;

    public static final double LANE_DETECTION_BY_SHAPE_CANNY_THRESHOLD_1 = 0.66;
    public static final double LANE_DETECTION_BY_SHAPE_CANNY_THRESHOLD_2 = 1.33;

    public static final double LANE_DETECTION_BY_SHAPE_HOUGH_LINE_P_RHO = 1;
    public static final double LANE_DETECTION_BY_SHAPE_HOUGH_LINE_P_THETA = Math.PI / 180;
    public static final int LANE_DETECTION_BY_SHAPE_HOUGH_LINE_P_THRESHOLD = 50;
    public static final int LANE_DETECTION_BY_SHAPE_HOUGH_LINE_P_MIN_LINE_SIZE = 50;
    public static final int LANE_DETECTION_BY_SHAPE_HOUGH_LINE_P_LINE_GAP = 20;

    public static final int LANE_DETECTION_BY_SHAPE_CAR_LANE_DEVIATION_THRESHOLD =40;

    public static final int SIGN_DETECTION_BY_COLOR_CONTOUR_SIZE_THRESHOLD = 85;

    //todo: tmp param
    public static final int SEEK_BAR_MAX_VALUE = 256;


}
