/*******************************************************************************
 * Copyright (c) 2022 Joerg Kubitz and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Joerg Kubitz - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.runtime.jobs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.eclipse.core.tests.harness.TestBarrier2;

/**
 *
 */
public class OrderAsserter {
	public class Event {
		final String name;
		final Integer eventNumber;

		Event(String name, Integer eventNumber) {
			this.eventNumber = eventNumber;
			this.name = name;
		}

		@Override
		public String toString() {
			return "Event " + eventNumber + " " + name;
		}
	}

	private final AtomicInteger preparation = new AtomicInteger();
	private final AtomicInteger progress = new AtomicInteger();
	private final Collection<Throwable> errors = new ConcurrentLinkedQueue<>();
	private final Map<Integer, Event> eventsByEventNumber = new ConcurrentHashMap<>();
	private final ReentrantLock lock = new ReentrantLock();
	private final long defaultWaitMs;

	public OrderAsserter() {
		this(1);
	}

	public OrderAsserter(long defaultWaitMs) {
		this.defaultWaitMs = defaultWaitMs;
	}

	/**
	 * verifies this event is executed at the right time.
	 *
	 * @param event - specifies the event that should only happen at the right time
	 *              and not concurrently with any other event;
	 **/
	public void expect(Event event) {
		expect(event, defaultWaitMs);
	}

	volatile Exception potentialDeadlock;

	public void expect(Event event, long waitMs) {
		System.out.println(event + " happend in Thread '" + Thread.currentThread().getName() + "'");
		if (!errors.isEmpty()) {
			return;
		}
		try {
			int currentProgress = progress.incrementAndGet();
			assertNotNull("Should not happen but was event number " + getEventString(currentProgress), event);

			// two locks at the same time => race condition happend
			if (!lock.tryLock()) {
				throw new AssertionError("Race condition at " + event, potentialDeadlock);
			}
			try {
				// let's wait some time to see if events in other threads cause a race condition
				potentialDeadlock = new IllegalStateException(
						"Race condition with " + event + " in Thread '" + Thread.currentThread().getName() + "'");
				Thread.sleep(waitMs);
			} finally {
				potentialDeadlock = null;
				lock.unlock();
			}

			lock.lock();
			try {

			} finally {
				lock.unlock();
			}

			assertFalse("Too late. Expected to happen as " + event + " but was " + getEventString(currentProgress),
					currentProgress > event.eventNumber);
			assertFalse("Too early. Expected to happen as " + event + " but was " + getEventString(currentProgress),
					currentProgress < event.eventNumber);
		} catch (Throwable e) {
			addError(e);
		}
	}

	private String getEventString(Integer eventNumber) {
		Event event = eventsByEventNumber.get(eventNumber);
		if (event != null) {
			return event.toString();
		}
		return "eventNumber=" + eventNumber;
	}

	public void runCollectingError(Runnable runnable) {
		if (!errors.isEmpty()) {
			return;
		}
		try {
			runnable.run();
		} catch (Throwable e) {
			addError(e);
		}
	}

	public void addError(Throwable e) {
		synchronized (errors) {
			// synchronized to only report "first" error
			e = new AssertionError(e.getLocalizedMessage() + " in Thread '" + Thread.currentThread().getName() + "'",
					e);
			if (!errors.isEmpty()) {
				return;
			}
			String threadDump = TestBarrier2.getThreadDump();
			errors.add(e);
			e.printStackTrace();
			System.err.println(threadDump);
		}

	}

	/** to be called in the testing thread **/
	public void assertNoErrors() {
		errors.forEach(e -> {
			throw new AssertionError(e);
		});
	}

	public Event never(String name) {
		return new Event(name, null);
	}

	public Event getNext(String name) {
		int eventNumber = preparation.incrementAndGet();
		Event event = new Event(name, eventNumber);
		eventsByEventNumber.put(eventNumber, event);
		return event;
	}
}
