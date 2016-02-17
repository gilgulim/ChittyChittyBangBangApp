package ccbb.example.com.ccbb2.enums;

/**
 * Created by gil on 02/01/2016.
 */
public enum Action {
    Forward(1),
    TurnLeft(2),
    TurnRight(3),
    Stop(4),
    Wait(5),
    SpeedUp(6, "1"),
    SpeedDown(7, "-1"),
    None(8, "0");

    private int id;
    private String signal;

    private Action(int id){
        this(id, null);
    }

    private Action(int id, String signal){
        this.signal = signal;
    }

    public int getId() {
        return id;
    }

    public String getSignal(){
        return signal;
    }
}
