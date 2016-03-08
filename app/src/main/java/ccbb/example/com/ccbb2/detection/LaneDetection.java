package ccbb.example.com.ccbb2.detection;

import android.os.AsyncTask;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

/**
 * Created by gil on 04/03/2016.
 */
public class LaneDetection extends AsyncTask {
    private Mat mDetectionResult;
    public AsyncDetectionResponse response;

    public LaneDetection(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mDetectionResult = inputFrame.rgba();
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        response.processFinish(mDetectionResult);
    }
}
