package ccbb.example.com.ccbb2.fsm;

import android.util.Log;

import org.squirrelframework.foundation.fsm.annotation.StateMachineParameters;
import org.squirrelframework.foundation.fsm.impl.AbstractUntypedStateMachine;

import ccbb.example.com.ccbb2.DetectionActivity;
import ccbb.example.com.ccbb2.enums.Action;

/**
 * Created by gil on 16/02/2016.
 */
public class FsmManager {
    private static final String TAG = "FsmMgr:";

    public enum FSMEvent {
        ToA("o={1, 2, 4};l={0}", "a"),   //o={1, 2, 4};l={0} Forward
        ToB("o={1, 2, 4};l={1}", "b"),   //o={1, 2, 4};l={1} Left
        ToC("o={1, 2, 4};l={2}", "c"),   //o={1, 2, 4};l={2} Right
        ToD("o={3};l={*}", "d"),         //o={3};l={*}       Stop
        ToE("o={0};l={*}", "e");         //o={0};l={*}       Wait

        private final String stateInfo;
        private final String signal;

        FSMEvent(String stateInfo, String signal) {
            this.stateInfo = stateInfo;
            this.signal = signal;
        }

        public String getStateInfo() {
            return stateInfo;
        }

        public String getSignal() {
            return signal;
        }
    }

    @StateMachineParameters(stateType = Action.class, eventType = FSMEvent.class, contextType = Action.class)
    static class StateMachine extends AbstractUntypedStateMachine {

        public StateMachine() {}

        protected void fromAToB(Action from, Action to, FSMEvent event, Action speedAction) {
            if(event != null){
                DetectionActivity.mCommandService.write(event.getSignal().getBytes());
                Log.i(TAG, "Transition from '" + from + "' to '" + to + "' on event '" + event + " " + event.getStateInfo());
            }
            if(speedAction != null){
                DetectionActivity.mCommandService.write(speedAction.getSignal().getBytes());
                Log.i(TAG, "Transition from '" + from + "' to '" + to + "' with context '" + speedAction + " " + speedAction.getSignal() + "'.");
            }
        }
    }
}
