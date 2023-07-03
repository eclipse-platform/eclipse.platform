/*******************************************************************************
 * Copyright (c) 2016, 2018 Andrey Loskutov.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrey Loskutov <loskutov@gmx.de> - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.launching;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ConcurrentModificationException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.Launch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IDisconnect;
import org.eclipse.debug.core.model.IProcess;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the {@link Launch} class
 *
 * @since 3.10
 */
public class LaunchTests extends AbstractLaunchTest {

	private InvocationHandler handler;
	private Runnable readIsTerminatedTask;
	private Runnable readIsDisconnectedTask;
	private Runnable writeProcessesTask;
	private Runnable writeDebugTargetsTask;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		final Launch launch = new Launch(null, ILaunchManager.RUN_MODE, null);

		handler = (proxy, method, args) -> {
			String name = method.getName();
			if (name.equals("equals")) { //$NON-NLS-1$
				return args.length == 1 && proxy == args[0];
			}
			return Boolean.TRUE;
		};

		readIsTerminatedTask = () -> launch.isTerminated();

		readIsDisconnectedTask = () -> launch.isDisconnected();

		writeProcessesTask = () -> {
			IProcess process = createProcessProxy();
			launch.addProcess(process);
			launch.removeProcess(process);
			try {
				Thread.sleep(0, 1);
			} catch (InterruptedException e) {
				//
			}
			launch.addProcess(process);
			launch.removeProcess(process);
		};

		writeDebugTargetsTask = () -> {
			IDebugTarget target2 = createDebugTargetProxy();
			launch.addDebugTarget(target2);
			launch.removeDebugTarget(target2);
			try {
				Thread.sleep(0, 1);
			} catch (InterruptedException e) {
				//
			}
			launch.addDebugTarget(target2);
			launch.removeDebugTarget(target2);
		};
	}

	/**
	 * Modifies debug targets and checks if this causes
	 * {@link ConcurrentModificationException} in the another thread
	 */
	@Test
	public void testTerminatedAndWriteTargets() throws Exception {
		assertTrue(testExecution(readIsTerminatedTask, writeDebugTargetsTask));
	}

	@Test
	public void testDisconnectedAndWriteTargets() throws Exception {
		assertTrue(testExecution(readIsDisconnectedTask, writeDebugTargetsTask));
	}

	/**
	 * Modifies processes and checks if this causes
	 * {@link ConcurrentModificationException} in the another thread
	 */
	@Test
	public void testTerminatedAndWriteProcesses() throws Exception {
		assertTrue(testExecution(readIsTerminatedTask, writeProcessesTask));
	}

	/**
	 * Modifies processes and checks if this causes
	 * {@link ConcurrentModificationException} in the another thread
	 */
	@Test
	public void testDisconnectedAndWriteProcesses() throws Exception {
		assertTrue(testExecution(readIsDisconnectedTask, writeProcessesTask));
	}

	private boolean testExecution(final Runnable readTask, final Runnable writeTask) {
		/*
		 * Normally 10 times trial is sufficient to reproduce concurrent
		 * modification error, but 2000 is chosen for better stability of test.
		 * (the test execution time is less than 2 sec)
		 */
		final int maxTrialCount = 2000;

		final Semaphore semaphore = new Semaphore(0);
		final AtomicInteger runs = new AtomicInteger();

		Job job = new Job("modify debug target") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					semaphore.acquire();
					for (int i = 0; i < maxTrialCount; i++) {
						if (monitor.isCanceled()) {
							return Status.CANCEL_STATUS;
						}
						// try to modify launch data
						writeTask.run();
					}
				} catch (Exception e1) {
					// we don't care
					return Status.CANCEL_STATUS;
				} finally {
					runs.set(maxTrialCount);
				}
				return Status.OK_STATUS;
			}
		};

		job.schedule();
		semaphore.release();

		try {
			while (runs.get() < maxTrialCount) {
				// try to read launch data
				readTask.run();

				// avoid endless loop if job already finished
				if (job.getResult() != null) {
					break;
				}
			}
		} finally {
			System.out.println(name.getMethodName() + " runs: " + runs); //$NON-NLS-1$
			job.cancel();
		}

		assertEquals(maxTrialCount, runs.get());
		return true;
	}

	private IDebugTarget createDebugTargetProxy() {
		IDebugTarget debugTarget = (IDebugTarget) Proxy.newProxyInstance(LaunchTests.class.getClassLoader(), new Class[] {
				IDebugTarget.class }, handler);
		return debugTarget;
	}

	private IProcess createProcessProxy() {
		IProcess process = (IProcess) Proxy.newProxyInstance(LaunchTests.class.getClassLoader(), new Class[] {
				IProcess.class, IDisconnect.class }, handler);
		return process;
	}

}
