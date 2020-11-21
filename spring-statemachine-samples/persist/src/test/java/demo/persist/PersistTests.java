/*
 * Copyright 2015-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package demo.persist;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.statemachine.TestUtils.doStartAndAssert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import demo.CommonConfiguration;
import demo.persist.Application.OrderEvent;
import demo.persist.Application.OrderState;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@SpringBootTest(classes = { CommonConfiguration.class, Application.class, StateMachineCommands.class })
public class PersistTests {

	@Autowired
	private StateMachineCommands commands;

	@Autowired
	private StateMachine<OrderState, OrderEvent> machine;

	@Autowired
	private Persist persist;

	@Test
	public void testNotStarted() throws Exception {
		assertThat(commands.state(), is("No state"));
	}

	@Test
	public void testInitialState() throws Exception {
		TestListener listener = new TestListener();
		machine.addStateListener(listener);
		doStartAndAssert(machine);
		assertThat(listener.stateChangedLatch.await(3, TimeUnit.SECONDS), is(true));
		assertThat(listener.stateEnteredLatch.await(3, TimeUnit.SECONDS), is(true));
		assertThat(machine.getState().getIds(), contains("PLACED"));
		assertThat(listener.statesEntered.size(), is(1));
		assertThat(listener.statesEntered.get(0).getId(), is("PLACED"));
		assertThat(listener.statesExited.size(), is(0));
	}

	@Test
	public void testInitialDbList() {
		// dataOrder [id=1, state=PLACED]Order [id=2, state=PROCESSING]Order [id=3,
		// state=SENT]Order [id=4, state=DELIVERED]
		assertThat(persist.listDbEntries(), containsString("PLACED"));
	}

	@Test
	public void testUpdate1() {
		persist.change(1, OrderEvent.process);
		assertThat(persist.listDbEntries(), containsString("id=1, state=PROCESSING"));
	}

	@Test
	public void testUpdate2() {
		persist.change(2, OrderEvent.send);
		assertThat(persist.listDbEntries(), containsString("id=2, state=SENT"));
	}

	private static class TestListener extends StateMachineListenerAdapter<OrderState, OrderEvent> {

		volatile CountDownLatch stateChangedLatch = new CountDownLatch(1);
		volatile CountDownLatch stateEnteredLatch = new CountDownLatch(1);
		volatile CountDownLatch stateExitedLatch = new CountDownLatch(0);
		volatile CountDownLatch transitionLatch = new CountDownLatch(0);
		volatile List<Transition<OrderState, OrderEvent>> transitions = new ArrayList<Transition<OrderState, OrderEvent>>();
		List<State<OrderState, OrderEvent>> statesEntered = new ArrayList<State<OrderState, OrderEvent>>();
		List<State<OrderState, OrderEvent>> statesExited = new ArrayList<State<OrderState, OrderEvent>>();

		@Override
		public void stateChanged(State<OrderState, OrderEvent> from, State<OrderState, OrderEvent> to) {
			stateChangedLatch.countDown();
		}

		@Override
		public void stateEntered(State<OrderState, OrderEvent> state) {
			statesEntered.add(state);
			stateEnteredLatch.countDown();
		}

		@Override
		public void stateExited(State<OrderState, OrderEvent> state) {
			statesExited.add(state);
			stateExitedLatch.countDown();
		}

		@Override
		public void transition(Transition<OrderState, OrderEvent> transition) {
			transitions.add(transition);
			transitionLatch.countDown();
		}

	}

}
