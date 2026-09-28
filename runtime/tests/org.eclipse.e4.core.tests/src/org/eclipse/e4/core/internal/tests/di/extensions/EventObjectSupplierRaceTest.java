/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.e4.core.internal.tests.di.extensions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.e4.core.di.IInjector;
import org.eclipse.e4.core.di.InjectionException;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.di.internal.extensions.EventObjectSupplier;
import org.eclipse.e4.core.di.suppliers.IObjectDescriptor;
import org.eclipse.e4.core.di.suppliers.IRequestor;
import org.eclipse.e4.core.di.suppliers.PrimaryObjectSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;

/**
 * Verifies that {@link EventObjectSupplier} hands an event to the injector if and only if the
 * calling thread is the one currently delivering it.
 * <p>
 * The supplier is a singleton OSGi service shared by all contexts and all threads, and it has no
 * way to pass the event down into the injector other than parking it for the duration of
 * {@link IRequestor#resolveArguments(boolean)}. Because {@code EventAdmin.sendEvent(Event)}
 * delivers on the caller's thread, several threads can be inside that window at the same time, and
 * a handler may even re-enter it on the same thread while its arguments are being computed.
 * </p>
 * <p>
 * The deliveries below are driven through the handler the supplier creates for a requestor, so that
 * publishing, resolving and withdrawing happen in the same order as in production. The
 * interleavings are forced with latches instead of with concurrent load, so a regression fails
 * reliably rather than occasionally.
 * </p>
 */
@Timeout(value = 60, unit = TimeUnit.SECONDS)
public class EventObjectSupplierRaceTest {

	private static final String TOPIC = "e4/test/supplier/delivery";

	private static final String OTHER_TOPIC = "e4/test/supplier/other";

	private static final long AWAIT_SECONDS = 30;

	/**
	 * Template for the descriptors used below. The qualifiers are read from these parameters, so
	 * the supplier's own {@code getTopic(IObjectDescriptor)} is exercised instead of being stubbed.
	 */
	@SuppressWarnings("unused")
	private static void handlerParameters(
			@EventTopic(TOPIC) Event event, 
			@EventTopic(TOPIC) String payload,
			@EventTopic(OTHER_TOPIC) Event otherEvent, 
			Event unqualified) {
		// never called, only its parameters are looked at
	}

	private static final IObjectDescriptor EVENT_OF_TOPIC = parameterDescriptor(0);

	private static final IObjectDescriptor PAYLOAD_OF_TOPIC = parameterDescriptor(1);

	private static final IObjectDescriptor EVENT_OF_OTHER_TOPIC = parameterDescriptor(2);

	private static final IObjectDescriptor UNQUALIFIED_EVENT = parameterDescriptor(3);

	private TestableSupplier supplier;

	@BeforeEach
	public void createSupplier() {
		supplier = new TestableSupplier();
	}

	/**
	 * While a delivery is in progress the event belongs to the arguments being resolved, afterwards
	 * nothing is current anymore.
	 */
	@Test
	public void eventIsResolvedWhileItIsDeliveredAndNotAfterwards() {
		Event event = event(TOPIC, "payload");
		ResolvingRequestor requestor = new ResolvingRequestor(EVENT_OF_TOPIC);

		deliver(TOPIC, event, requestor);

		assertSame(event, requestor.resolved(), "the delivered event was not resolved");
		assertTrue(requestor.wasExecuted(), "the requestor was not executed after its arguments were resolved");
		assertSame(IInjector.NOT_A_VALUE, probe(EVENT_OF_TOPIC),
				"the event is still current although its delivery has finished");
	}

	/** A parameter that is not an {@code Event} receives the payload the event carries. */
	@Test
	public void payloadIsResolvedForParametersThatAreNotEvents() {
		ResolvingRequestor requestor = new ResolvingRequestor(PAYLOAD_OF_TOPIC);

		deliver(TOPIC, event(TOPIC, "payload"), requestor);

		assertEquals("payload", requestor.resolved(), "the payload of the delivered event was not resolved");
	}

	/** Deliveries of different topics must not interfere with each other. */
	@Test
	public void eventOfOneTopicIsNotResolvedForAnother() {
		ResolvingRequestor requestor = new ResolvingRequestor(EVENT_OF_OTHER_TOPIC);

		deliver(TOPIC, event(TOPIC, "payload"), requestor);

		assertSame(IInjector.NOT_A_VALUE, requestor.resolved(),
				"an event was resolved for a topic that is not being delivered");
	}

	/** A parameter without an {@code @EventTopic} qualifier has no topic and therefore no value. */
	@Test
	public void parameterWithoutTopicIsNotAValue() {
		assertSame(IInjector.NOT_A_VALUE, probe(UNQUALIFIED_EVENT),
				"a parameter without @EventTopic must not be resolved");
	}

	/** An obsolete requestor is dropped instead of being resolved and executed. */
	@Test
	public void invalidRequestorIsNeitherResolvedNorExecuted() {
		ResolvingRequestor requestor = new ResolvingRequestor(EVENT_OF_TOPIC);
		requestor.invalidate();

		deliver(TOPIC, event(TOPIC, "payload"), requestor);

		assertFalse(requestor.wasResolved(), "an invalid requestor had its arguments resolved");
		assertFalse(requestor.wasExecuted(), "an invalid requestor was executed");
	}

	/**
	 * Two threads delivering the same topic at the same time must not see each other's event: the
	 * thread that is not delivering gets nothing, and the thread that is delivering gets its own
	 * event, no matter what the other one published or withdrew in the meantime.
	 */
	@Test
	public void concurrentDeliveriesOfTheSameTopicAreIndependent() throws InterruptedException {
		Event deliveredElsewhere = event(TOPIC, "from the other thread");
		Event deliveredHere = event(TOPIC, "from this thread");

		CountDownLatch otherThreadIsDelivering = new CountDownLatch(1);
		CountDownLatch thisThreadIsDone = new CountDownLatch(1);

		// resolves only after this thread has run a complete delivery of the same topic
		ResolvingRequestor otherRequestor = new ResolvingRequestor(EVENT_OF_TOPIC, () -> {
			otherThreadIsDelivering.countDown();
			await(thisThreadIsDone);
		});

		AtomicReference<Throwable> otherThreadFailure = new AtomicReference<>();
		Thread otherThread = new Thread(() -> deliver(TOPIC, deliveredElsewhere, otherRequestor), "other-delivery");
		otherThread.setUncaughtExceptionHandler((thread, thrown) -> otherThreadFailure.set(thrown));
		otherThread.start();
		try {
			await(otherThreadIsDelivering);

			assertSame(IInjector.NOT_A_VALUE, probe(EVENT_OF_TOPIC),
					"the event delivered by another thread was resolved on this one");

			ResolvingRequestor ownRequestor = new ResolvingRequestor(EVENT_OF_TOPIC);
			deliver(TOPIC, deliveredHere, ownRequestor);
			assertSame(deliveredHere, ownRequestor.resolved(),
					"the event delivered by this thread was not the one resolved here");
		} finally {
			thisThreadIsDone.countDown();
			otherThread.join();
		}

		rethrow(otherThreadFailure.get());
		assertSame(deliveredElsewhere, otherRequestor.resolved(),
				"the delivery on the other thread lost its event to this thread");
	}

	/**
	 * Resolving the arguments of a handler runs arbitrary code, which may send another event of the
	 * same topic before the outer delivery is finished. The inner delivery must leave the outer one
	 * untouched.
	 */
	@Test
	public void nestedDeliveryOfTheSameTopicKeepsTheOuterEventCurrent() {
		Event outerEvent = event(TOPIC, "outer");
		Event innerEvent = event(TOPIC, "inner");

		ResolvingRequestor innerRequestor = new ResolvingRequestor(EVENT_OF_TOPIC);
		// a complete delivery of the same topic, run while the outer arguments are being resolved
		ResolvingRequestor outerRequestor = new ResolvingRequestor(EVENT_OF_TOPIC,
				() -> deliver(TOPIC, innerEvent, innerRequestor));

		deliver(TOPIC, outerEvent, outerRequestor);

		assertSame(innerEvent, innerRequestor.resolved(), "the nested delivery did not resolve its own event");
		assertSame(outerEvent, outerRequestor.resolved(), "the nested delivery invalidated the outer event");
		assertSame(IInjector.NOT_A_VALUE, probe(EVENT_OF_TOPIC),
				"an event is still current although both deliveries have finished");
	}

	/** A delivery that fails while resolving must not leave its event behind for the next one. */
	@Test
	public void failedDeliveryDoesNotLeaveItsEventCurrent() {
		ResolvingRequestor brokenRequestor = new ResolvingRequestor(EVENT_OF_TOPIC, () -> {
			throw new InjectionException("cannot resolve the other arguments of this handler");
		});

		assertThrows(InjectionException.class, () -> deliver(TOPIC, event(TOPIC, "failing"), brokenRequestor));

		assertSame(IInjector.NOT_A_VALUE, probe(EVENT_OF_TOPIC),
				"the event of the failed delivery is still current");

		Event next = event(TOPIC, "next");
		ResolvingRequestor nextRequestor = new ResolvingRequestor(EVENT_OF_TOPIC);
		deliver(TOPIC, next, nextRequestor);
		assertSame(next, nextRequestor.resolved(), "a later delivery did not get its own event");
	}

	/** Runs one complete delivery, exactly as EventAdmin does when it calls the supplier's handler. */
	private void deliver(String topic, Event event, IRequestor requestor) {
		supplier.handlerFor(topic, requestor).handleEvent(event);
	}

	/** Asks for a value outside of any delivery, which is what an ordinary injection does. */
	private Object probe(IObjectDescriptor descriptor) {
		return supplier.resolve(descriptor, null);
	}

	private static Event event(String topic, String payload) {
		return new Event(topic, Map.of(EventObjectSupplier.DATA, payload));
	}

	private static void await(CountDownLatch latch) {
		try {
			assertTrue(latch.await(AWAIT_SECONDS, TimeUnit.SECONDS), "timed out waiting for the other thread");
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	private static void rethrow(Throwable thrown) {
		if (thrown != null) {
			throw new AssertionError("the delivery on the other thread failed", thrown);
		}
	}

	private static IObjectDescriptor parameterDescriptor(int index) {
		try {
			Parameter parameter = EventObjectSupplierRaceTest.class
					.getDeclaredMethod("handlerParameters", Event.class, String.class, Event.class, Event.class)
					.getParameters()[index];
			return new ParameterDescriptor(parameter);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Gives the test access to the members the injector and EventAdmin use. */
	private static final class TestableSupplier extends EventObjectSupplier {

		TestableSupplier() {
			// get() only verifies that an EventAdmin is around, it never talks to it
			setEventAdmin(new UnusableEventAdmin());
		}

		EventHandler handlerFor(String topic, IRequestor requestor) {
			return makeHandler(topic, requestor);
		}

		Object resolve(IObjectDescriptor descriptor, IRequestor requestor) {
			// no tracking: subscribing needs a running framework and is not what is tested here
			return get(descriptor, requestor, false, false);
		}
	}

	/**
	 * Stands for one injected method: it asks the supplier for its argument while its arguments are
	 * being resolved, which is what {@code InjectorImpl} does, and optionally runs other work first.
	 */
	private final class ResolvingRequestor implements IRequestor {

		private final IObjectDescriptor descriptor;

		private final Runnable whileResolving;

		private volatile Object resolved;

		private volatile boolean resolveCalled;

		private volatile boolean executeCalled;

		private volatile boolean valid = true;

		ResolvingRequestor(IObjectDescriptor descriptor) {
			this(descriptor, null);
		}

		ResolvingRequestor(IObjectDescriptor descriptor, Runnable whileResolving) {
			this.descriptor = descriptor;
			this.whileResolving = whileResolving;
		}

		@Override
		public void resolveArguments(boolean initial) {
			resolveCalled = true;
			if (whileResolving != null) {
				whileResolving.run();
			}
			resolved = supplier.resolve(descriptor, this);
		}

		@Override
		public Object execute() {
			executeCalled = true;
			return null;
		}

		Object resolved() {
			assertNotNull(resolved, "the arguments of this requestor were never resolved");
			return resolved;
		}

		boolean wasResolved() {
			return resolveCalled;
		}

		boolean wasExecuted() {
			return executeCalled;
		}

		void invalidate() {
			valid = false;
		}

		@Override
		public boolean isValid() {
			return valid;
		}

		@Override
		public Object getRequestingObject() {
			return null;
		}

		@Override
		public Class<?> getRequestingObjectClass() {
			return null;
		}

		@Override
		public void disposed(PrimaryObjectSupplier objectSupplier) {
			// nothing to dispose
		}

		@Override
		public boolean uninject(Object object, PrimaryObjectSupplier objectSupplier) {
			return false;
		}
	}

	/** Describes one parameter of {@link EventObjectSupplierRaceTest#handlerParameters}. */
	private static final class ParameterDescriptor implements IObjectDescriptor {

		private final Parameter parameter;

		ParameterDescriptor(Parameter parameter) {
			this.parameter = parameter;
		}

		@Override
		public Type getDesiredType() {
			return parameter.getParameterizedType();
		}

		@Override
		public boolean hasQualifier(Class<? extends Annotation> clazz) {
			return parameter.getAnnotation(clazz) != null;
		}

		@Override
		public <T extends Annotation> T getQualifier(Class<T> clazz) {
			return parameter.getAnnotation(clazz);
		}

		@Override
		public Annotation[] getQualifiers() {
			return parameter.getAnnotations();
		}
	}

	/** The supplier must not use the EventAdmin while a value is resolved. */
	private static final class UnusableEventAdmin implements EventAdmin {

		@Override
		public void postEvent(Event event) {
			throw new AssertionError("the supplier posted an event: " + event);
		}

		@Override
		public void sendEvent(Event event) {
			throw new AssertionError("the supplier sent an event: " + event);
		}
	}
}
