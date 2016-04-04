package ccbb.example.com.ccbb2.dataobjects;

/**
 * Created by gil on 25/03/2016.
 */
public class Line {
    private boolean isLineGroup;
    private double gradient;
    private PairOfPoints points;

    public Line() {
    }

    public Line(PairOfPoints points, double gradient) {
        this.points = points;
        this.gradient = gradient;
    }

    public double getGradient() {
        return gradient;
    }

    public boolean isLineGroup() {
        return isLineGroup;
    }

    public void setIsLineGroup(boolean isLineGroup) {
        this.isLineGroup = isLineGroup;
    }

    public void setGradient(double gradient) {
        this.gradient = gradient;
    }

    public PairOfPoints getPoints() {
        return points;
    }

    public void setPoints(PairOfPoints points) {
        this.points = points;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Line)) return false;

        Line line = (Line) o;

        if (Double.compare(line.getGradient(), getGradient()) != 0) return false;
        return getPoints().equals(line.getPoints());

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(getGradient());
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + getPoints().hashCode();
        return result;
    }
}
