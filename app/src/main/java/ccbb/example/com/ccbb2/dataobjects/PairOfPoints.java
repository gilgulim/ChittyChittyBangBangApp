package ccbb.example.com.ccbb2.dataobjects;

import org.opencv.core.Point;

/**
 * Created by gil on 30/01/2016.
 */
public class PairOfPoints implements Comparable<PairOfPoints>{
    private Point p1;
    private Point p2;

    public PairOfPoints(Point p1, Point p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public Point getP1() {
        return p1;
    }

    public void setP1(Point p1) {
        this.p1 = p1;
    }

    public Point getP2() {
        return p2;
    }

    public void setP2(Point p2) {
        this.p2 = p2;
    }

    @Override
    public int compareTo(PairOfPoints pairOfPoints) {

        return (this.p1.y > pairOfPoints.p1.y ? -1 : 1);
    }
}
