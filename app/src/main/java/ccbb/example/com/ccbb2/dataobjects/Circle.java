package ccbb.example.com.ccbb2.dataobjects;

import org.opencv.core.Point;

/**
 * Created by gil on 24/02/2016.
 */
public class Circle implements Comparable<Circle>{
    private int radius;
    private Point center;

    public Circle(int radius, Point center) {
        this.radius = radius;
        this.center = center;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public Point getCenter() {
        return center;
    }

    public void setCenter(Point center) {
        this.center = center;
    }

    @Override
    public int compareTo(Circle circle) {
        if(this.radius == circle.getRadius())
            return 0;
        else
            return this.radius > circle.radius ? -1 : 1;
    }
}
