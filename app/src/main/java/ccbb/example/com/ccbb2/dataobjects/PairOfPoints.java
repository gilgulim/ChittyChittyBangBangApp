package ccbb.example.com.ccbb2.dataobjects;

import org.opencv.core.Point;

/**
 * Created by gil on 30/01/2016.
 */
public class PairOfPoints implements Comparable<PairOfPoints>{
    private Point ptLow;
    private Point ptHigh;

    public PairOfPoints(Point ptLow, Point ptHigh) {
        this.ptLow = ptLow;
        this.ptHigh = ptHigh;
    }

    public Point getPtLow() {
        return ptLow;
    }

    public void setPtLow(Point ptLow) {
        this.ptLow = ptLow;
    }

    public Point getPtHigh() {
        return ptHigh;
    }

    public void setPtHigh(Point ptHigh) {
        this.ptHigh = ptHigh;
    }

    @Override
    public int compareTo(PairOfPoints pairOfPoints) {

        return (this.ptLow.y > pairOfPoints.ptLow.y ? -1 : 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PairOfPoints)) return false;

        PairOfPoints that = (PairOfPoints) o;

        if (!getPtLow().equals(that.getPtLow())) return false;
        return getPtHigh().equals(that.getPtHigh());

    }

    @Override
    public int hashCode() {
        int result = getPtLow().hashCode();
        result = 31 * result + getPtHigh().hashCode();
        return result;
    }
}
