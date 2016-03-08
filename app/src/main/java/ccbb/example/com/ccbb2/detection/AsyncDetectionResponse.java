package ccbb.example.com.ccbb2.detection;

import org.opencv.core.Mat;

/**
 * Created by gil on 04/03/2016.
 */
public interface AsyncDetectionResponse {
    void processFinish(Mat result);
}
