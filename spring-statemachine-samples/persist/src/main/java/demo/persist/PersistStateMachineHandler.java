package demo.persist;

import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.recipes.persist.GenericPersistStateMachineHandler;

import demo.persist.Application.OrderEvent;
import demo.persist.Application.OrderState;

public class PersistStateMachineHandler extends GenericPersistStateMachineHandler<OrderState, OrderEvent> {

    public PersistStateMachineHandler(StateMachine<OrderState, OrderEvent> stateMachine) {
        super(stateMachine);
    }

    public interface PersistStateChangeListener extends GenericPersistStateChangeListener<OrderState, OrderEvent> {
    }

}
