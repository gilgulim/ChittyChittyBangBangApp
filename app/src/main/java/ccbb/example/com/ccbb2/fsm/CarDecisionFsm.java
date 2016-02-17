package ccbb.example.com.ccbb2.fsm;

import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.UntypedStateMachineBuilder;

import ccbb.example.com.ccbb2.enums.Action;

/**
 * Created by gil on 16/02/2016.
 */
public class CarDecisionFsm {

    private UntypedStateMachineBuilder builder;

    public CarDecisionFsm() {
        builder = StateMachineBuilderFactory.create(FsmManager.StateMachine.class);
        builder.externalTransition().from(Action.Forward).to(Action.Forward).on(FsmManager.FSMEvent.ToA).callMethod("fromAToB");
        builder.externalTransition().from(Action.Forward).to(Action.TurnLeft).on(FsmManager.FSMEvent.ToB).callMethod("fromAToB");
        builder.externalTransition().from(Action.Forward).to(Action.TurnRight).on(FsmManager.FSMEvent.ToC).callMethod("fromAToB");
        builder.externalTransition().from(Action.Forward).to(Action.Stop).on(FsmManager.FSMEvent.ToD).callMethod("fromAToB");
        builder.externalTransition().from(Action.Forward).to(Action.Stop).on(FsmManager.FSMEvent.ToE).callMethod("fromAToB");

        builder.externalTransition().from(Action.TurnRight).to(Action.Forward).on(FsmManager.FSMEvent.ToA).callMethod("fromAToB");
        builder.externalTransition().from(Action.TurnRight).to(Action.Forward).on(FsmManager.FSMEvent.ToB).callMethod("fromAToB");
        builder.externalTransition().from(Action.TurnRight).to(Action.TurnRight).on(FsmManager.FSMEvent.ToC).callMethod("fromAToB");
        builder.externalTransition().from(Action.TurnRight).to(Action.Stop).on(FsmManager.FSMEvent.ToD).callMethod("fromAToB");
        builder.externalTransition().from(Action.TurnRight).to(Action.Stop).on(FsmManager.FSMEvent.ToE).callMethod("fromAToB");

        builder.externalTransition().from(Action.TurnLeft).to(Action.Forward).on(FsmManager.FSMEvent.ToA).callMethod("fromAToB");
        builder.externalTransition().from(Action.TurnLeft).to(Action.TurnLeft).on(FsmManager.FSMEvent.ToB).callMethod("fromAToB");
        builder.externalTransition().from(Action.TurnLeft).to(Action.Forward).on(FsmManager.FSMEvent.ToC).callMethod("fromAToB");
        builder.externalTransition().from(Action.TurnLeft).to(Action.Stop).on(FsmManager.FSMEvent.ToD).callMethod("fromAToB");
        builder.externalTransition().from(Action.TurnLeft).to(Action.Stop).on(FsmManager.FSMEvent.ToE).callMethod("fromAToB");

        builder.externalTransition().from(Action.Stop).to(Action.Forward).on(FsmManager.FSMEvent.ToA).callMethod("fromAToB");
        builder.externalTransition().from(Action.Stop).to(Action.Forward).on(FsmManager.FSMEvent.ToB).callMethod("fromAToB");
        builder.externalTransition().from(Action.Stop).to(Action.Forward).on(FsmManager.FSMEvent.ToC).callMethod("fromAToB");
        builder.externalTransition().from(Action.Stop).to(Action.Stop).on(FsmManager.FSMEvent.ToD).callMethod("fromAToB");
        builder.externalTransition().from(Action.Stop).to(Action.Wait).on(FsmManager.FSMEvent.ToE).callMethod("fromAToB");

        builder.externalTransition().from(Action.Wait).to(Action.Forward).on(FsmManager.FSMEvent.ToA).callMethod("fromAToB");
        builder.externalTransition().from(Action.Wait).to(Action.Forward).on(FsmManager.FSMEvent.ToB).callMethod("fromAToB");
        builder.externalTransition().from(Action.Wait).to(Action.Forward).on(FsmManager.FSMEvent.ToC).callMethod("fromAToB");
        builder.externalTransition().from(Action.Wait).to(Action.Forward).on(FsmManager.FSMEvent.ToD).callMethod("fromAToB");
        builder.externalTransition().from(Action.Wait).to(Action.Forward).on(FsmManager.FSMEvent.ToE).callMethod("fromAToB");

    }

    public UntypedStateMachineBuilder getBuilder() {
        return builder;
    }
}
