package ccbb.example.com.ccbb2.enums;

/**
 * Created by gil on 02/01/2016.
 */
public enum Action {
    None(0),
    Forward(1),
    TurnLeft(2),
    TurnRight(3),
    Stop(4),
    Wait(5),
    SpeedUp(6),
    SpeedDown(7);

    private int id;
    private Action(int id){
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
