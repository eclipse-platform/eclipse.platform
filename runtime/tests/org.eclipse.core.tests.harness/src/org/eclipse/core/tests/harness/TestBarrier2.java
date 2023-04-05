/*******************************************************************************
 * Copyright (c) 2003, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.harness;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.Collectors;
import org.junit.Assert;

/**
 * This class acts as an implementation of a barrier that is appropriate for
 * concurrency test cases that want to fail if a thread fails to achieve a
 * particular state in a reasonable amount of time. This prevents test suites
 * from hanging indefinitely if a concurrency bug is found that would normally
 * result in an indefinite hang.
 */
public class TestBarrier2 {

	/**
	 * Convenience status constant that can be interpreted differently by each
	 * test.
	 */
	public static final int STATUS_BLOCKED = 6;
	/**
	 * Convenience status constant that can be interpreted differently by each
	 * test.
	 */
	public static final int STATUS_DONE = 5;
	/**
	 * Convenience status constant that can be interpreted differently by each
	 * test.
	 */
	public static final int STATUS_RUNNING = 3;
	/**
	 * Convenience status constant that can be interpreted differently by each
	 * test.
	 */
	public static final int STATUS_START = 1;
	/**
	 * Convenience status constant that can be interpreted differently by each
	 * test.
	 */
	public static final int STATUS_WAIT_FOR_DONE = 4;
	/**
	 * Convenience status constant that can be interpreted differently by each
	 * test.
	 */
	public static final int STATUS_WAIT_FOR_RUN = 2;
	/**
	 * Convenience status constant that can be interpreted differently by each
	 * test.
	 */
	public static final int STATUS_WAIT_FOR_START = 0;
	private final int myIndex;
	/**
	 * The status array and index for this barrier object
	 */
	private final AtomicIntegerArray myStatus;

	/**
	 * Blocks the calling thread until the status integer at the given index
	 * is set to the given value. Fails if the status change does not occur in
	 * a reasonable amount of time.
	 * @param statuses the array of statuses that represent the states of
	 * an array of jobs or threads
	 * @param index the index into the statuses array that the calling
	 * thread is waiting for
	 * @param status the status that the calling thread should wait for
	 */
	private static void doWaitForStatus(AtomicIntegerArray statuses, int index, int status, int timeout) {
		long start = System.nanoTime();
		while (statuses.get(index) != status) {
			Thread.yield();
			//sanity test to avoid hanging tests
			long elapsed = (System.nanoTime() - start) / 1_000_000;
			boolean condition = elapsed < timeout;
			if (!condition) {
				String dump = getThreadDump();
				if (statuses.get(index) > status) {
					Assert.fail("Timeout after " + elapsed + "ms - Status already in state "
							+ getStatus(statuses.get(index)) + " - waiting for " + getStatus(status) + "\n" + dump);
				} else {
					Assert.fail("Timeout after " + elapsed + "ms waiting for status to change from "
						+ getStatus(statuses.get(index)) + " to " + getStatus(status) + "\n" + dump);
				}
			}
		}
	}

	public static String getThreadDump() {
		StringBuilder out = new StringBuilder();
		out.append(" [ThreadDump taken from thread '" + Thread.currentThread().getName() + "' at "
				+ new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date(System.currentTimeMillis())) + ":\n");
		Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
		Comparator<Entry<Thread, StackTraceElement[]>> byId = Comparator.comparing(e -> e.getKey().getId());
		for (Entry<Thread, StackTraceElement[]> entry : stackTraces.entrySet().stream().sorted(byId)
				.collect(Collectors.toList())) {
			Thread thread = entry.getKey();
			String name = thread.getName();
			out.append("   Thread \"" + name + "\" #" + thread.getId() + " prio=" + thread.getPriority() + " "
					+ thread.getState() + "\n");
			StackTraceElement[] stack = entry.getValue();
			for (StackTraceElement se : stack) {
				out.append("     at " + se + "\n");
			}
		}
		out.append(" ] // ThreadDump end\n");
		return out.toString();
	}

	private static String getStatus(int status) {
		switch (status) {
			case STATUS_WAIT_FOR_START :
				return "WAIT_FOR_START";
			case STATUS_START :
				return "START";
			case STATUS_WAIT_FOR_RUN :
				return "WAIT_FOR_RUN";
			case STATUS_RUNNING :
				return "RUNNING";
			case STATUS_WAIT_FOR_DONE :
				return "WAIT_FOR_DONE";
			case STATUS_DONE :
				return "DONE";
			case STATUS_BLOCKED :
				return "BLOCKED";
			default :
				return "UNKNOWN_STATUS";
		}
	}

	public static void waitForStatus(AtomicIntegerArray location, int status) {
		doWaitForStatus(location, 0, status, 1000);
	}

	/**
	 * Blocks the current thread until the given variable is set to the given
	 * value Times out after a predefined period to avoid hanging tests
	 */
	public static void waitForStatus(AtomicIntegerArray location, int index, int status) {
		doWaitForStatus(location, index, status, 10000);
	}

	/**
	 * Creates a new test barrier suitable for a single thread
	 */
	public TestBarrier2() {
		this(new AtomicIntegerArray(new int[1]), 0);
	}

	/**
	 * Creates a new test barrier suitable for a single thread, with the given initial status.
	 */
	public TestBarrier2(int initalStatus) {
		this(new AtomicIntegerArray(new int[] { initalStatus }), 0);
	}

	/**
	 * Creates a new test barrier on the provided status array, suitable for
	 * acting as a barrier for multiple threads.
	 */
	public TestBarrier2(AtomicIntegerArray location, int index) {
		this.myStatus = location;
		this.myIndex = index;
	}

	/**
	 * Sets this barrier object's status.
	 */
	public void setStatus(int status) {
		myStatus.set(myIndex, status);
	}

	/**
	 * Blocks the current thread until the receiver's status is set to the given
	 * value. Times out after a predefined period to avoid hanging tests
	 */
	public void waitForStatus(int status) {
		waitForStatus(myStatus, myIndex, status);
	}

	/**
	 * The same as other barrier methods, except it will not fail if the job
	 * does not start in a "reasonable" time. This is only appropriate for tests
	 * that are explicitly very long running.
	 */
	public void waitForStatusNoFail(int status) {
		doWaitForStatus(myStatus, myIndex, status, 100_000);
	}

	public void upgradeTo(int newValue) {
		int actual = myStatus.get(myIndex);
		if (actual >= newValue) {
			String s = "wrong state " + myStatus.get(myIndex) + " should be < " + newValue;
			System.out.println(s);
			throw new IllegalStateException(s);
		}
		if (!myStatus.compareAndSet(myIndex, actual, newValue)) {
			String s = "wrong state " + myStatus.get(myIndex) + " should be " + actual + " upgrade to " + newValue;
			System.out.println(s);
			throw new IllegalStateException(s);
		}
	}
}
