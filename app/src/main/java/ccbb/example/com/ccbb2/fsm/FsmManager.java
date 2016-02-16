package ccbb.example.com.ccbb2.fsm;

import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.UntypedStateMachine;
import org.squirrelframework.foundation.fsm.UntypedStateMachineBuilder;
import org.squirrelframework.foundation.fsm.annotation.StateMachineParameters;
import org.squirrelframework.foundation.fsm.impl.AbstractUntypedStateMachine;

import ccbb.example.com.ccbb2.enums.Action;

/**
 * Created by gil on 16/02/2016.
 */
public class FsmManager {

    // 1. Define State Machine Event
    enum FSMEvent {
        ToA, //o={1, 2, 4};l={0}
        ToB, //o={1, 2, 4};l={1}
        ToC, //o={1, 2, 4};l={2}
        ToD, //o={3};l={*}
        ToE; //o={0};l={*}
    }

    // 2. Define State Machine Class
    @StateMachineParameters(stateType=Action.class, eventType=FSMEvent.class, contextType=Integer.class)
    static class StateMachine extends AbstractUntypedStateMachine {
        protected void fromAToB(Action from, Action to, FSMEvent event, Integer context) {
            System.out.println("Transition from '"+from+"' to '"+to+"' on event '"+event+
                    "' with context '"+context+"'.");
        }

        protected void ontoB(Action from, Action to, FSMEvent event, Integer context) {
            System.out.println("Entry State \'"+to+"\'.");
        }
    }

//    public static void main(String[] args) {
//        // 3. Build State Transitions
//        UntypedStateMachineBuilder builder = StateMachineBuilderFactory.create(StateMachine.class);
//        //builder.externalTransition().from("A").to("B").on(FSMEvent.ToB).callMethod("fromAToB");
//        //builder.onEntry("B").callMethod("ontoB");
//        builder.externalTransition().from(Action.Forward).to(Action.Forward).on(FSMEvent.ToA).callMethod("fromAToB");
//        builder.externalTransition().from(Action.Forward).to(Action.TurnLeft).on(FSMEvent.ToB).callMethod("fromAToB");
//        builder.externalTransition().from(Action.Forward).to(Action.TurnRight).on(FSMEvent.ToC).callMethod("fromAToB");
//        builder.externalTransition().from(Action.Forward).to(Action.Stop).on(FSMEvent.ToD).callMethod("fromAToB");
//        builder.externalTransition().from(Action.Forward).to(Action.Stop).on(FSMEvent.ToE).callMethod("fromAToB");
//
//        builder.externalTransition().from(Action.TurnRight).to(Action.Forward).on(FSMEvent.ToA).callMethod("fromAToB");
//        builder.externalTransition().from(Action.TurnRight).to(Action.Forward).on(FSMEvent.ToB).callMethod("fromAToB");
//        builder.externalTransition().from(Action.TurnRight).to(Action.TurnRight).on(FSMEvent.ToC).callMethod("fromAToB");
//        builder.externalTransition().from(Action.TurnRight).to(Action.Stop).on(FSMEvent.ToD).callMethod("fromAToB");
//        builder.externalTransition().from(Action.TurnRight).to(Action.Stop).on(FSMEvent.ToE).callMethod("fromAToB");
//
//        builder.externalTransition().from(Action.TurnLeft).to(Action.Forward).on(FSMEvent.ToA).callMethod("fromAToB");
//        builder.externalTransition().from(Action.TurnLeft).to(Action.TurnLeft).on(FSMEvent.ToB).callMethod("fromAToB");
//        builder.externalTransition().from(Action.TurnLeft).to(Action.Forward).on(FSMEvent.ToC).callMethod("fromAToB");
//        builder.externalTransition().from(Action.TurnLeft).to(Action.Stop).on(FSMEvent.ToD).callMethod("fromAToB");
//        builder.externalTransition().from(Action.TurnLeft).to(Action.Stop).on(FSMEvent.ToE).callMethod("fromAToB");
//
//        builder.externalTransition().from(Action.Stop).to(Action.Forward).on(FSMEvent.ToA).callMethod("fromAToB");
//        builder.externalTransition().from(Action.Stop).to(Action.Forward).on(FSMEvent.ToB).callMethod("fromAToB");
//        builder.externalTransition().from(Action.Stop).to(Action.Forward).on(FSMEvent.ToC).callMethod("fromAToB");
//        builder.externalTransition().from(Action.Stop).to(Action.Stop).on(FSMEvent.ToD).callMethod("fromAToB");
//        builder.externalTransition().from(Action.Stop).to(Action.Wait).on(FSMEvent.ToE).callMethod("fromAToB");
//
//        builder.externalTransition().from(Action.Wait).to(Action.Forward).on(FSMEvent.ToA).callMethod("fromAToB");
//        builder.externalTransition().from(Action.Wait).to(Action.Forward).on(FSMEvent.ToB).callMethod("fromAToB");
//        builder.externalTransition().from(Action.Wait).to(Action.Forward).on(FSMEvent.ToC).callMethod("fromAToB");
//        builder.externalTransition().from(Action.Wait).to(Action.Forward).on(FSMEvent.ToD).callMethod("fromAToB");
//        builder.externalTransition().from(Action.Wait).to(Action.Forward).on(FSMEvent.ToE).callMethod("fromAToB");
//
//
//        // 4. Use State Machine
//        UntypedStateMachine fsm = builder.newStateMachine(Action.Forward);
//        fsm.fire(FSMEvent.ToB, 10);
//
//        fsm.fire(FSMEvent.ToA, 10);
//        fsm.fire(FSMEvent.ToC, 10);
//        fsm.fire(FSMEvent.ToD, 10);
//        fsm.fire(FSMEvent.ToE, 10);
//        System.out.println("Current state is "+fsm.getCurrentState());
//    }
}
