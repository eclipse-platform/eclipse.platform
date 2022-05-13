/*******************************************************************************
 * Copyright (c) 2020, 2022 Paul Pazderski and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Paul Pazderski - initial API and implementation
 *     Hannes Wellmann - add tests regarding termination of descendants and timeout
 *******************************************************************************/
package org.eclipse.debug.tests.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;
import org.eclipse.debug.internal.core.DebugCoreMessages;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.debug.tests.TestUtil;
import org.eclipse.debug.tests.sourcelookup.TestLaunch;
import org.junit.Ignore;
import org.junit.Test;

public class RuntimeProcessTests extends AbstractDebugTest {

	/**
	 * Test behavior of {@link RuntimeProcess} if the wrapped process
	 * terminates.
	 */
	@Test
	public void testProcessTerminated() throws Exception {
		AtomicInteger processTerminateEvents = new AtomicInteger();
		DebugPlugin.getDefault().addDebugEventListener(events -> {
			for (DebugEvent event : events) {
				if (event.getKind() == DebugEvent.TERMINATE) {
					processTerminateEvents.incrementAndGet();
				}
			}
		});

		MockProcess mockProcess = new MockProcess(MockProcess.RUN_FOREVER);
		RuntimeProcess runtimeProcess = mockProcess.toRuntimeProcess();

		assertFalse("RuntimeProcess already terminated.", runtimeProcess.isTerminated());
		assertTrue(runtimeProcess.canTerminate());

		mockProcess.setExitValue(1);
		mockProcess.destroy();

		TestUtil.waitWhile(p -> !p.isTerminated(), runtimeProcess, 1000, p -> "RuntimePocess not terminated.");
		TestUtil.waitForJobs(name.getMethodName(), 25, 500);
		assertEquals("Wrong number of terminate events.", 1, processTerminateEvents.get());
		assertEquals("RuntimeProcess reported wrong exit code.", 1, runtimeProcess.getExitValue());
	}

	/** Test {@link RuntimeProcess} terminating the wrapped process. */
	@Test
	public void testTerminateProcess() throws Exception {
		AtomicInteger processTerminateEvents = new AtomicInteger();
		DebugPlugin.getDefault().addDebugEventListener(events -> {
			for (DebugEvent event : events) {
				if (event.getKind() == DebugEvent.TERMINATE) {
					processTerminateEvents.incrementAndGet();
				}
			}
		});

		MockProcess mockProcess = new MockProcess(MockProcess.RUN_FOREVER);
		RuntimeProcess runtimeProcess = mockProcess.toRuntimeProcess();

		assertFalse("RuntimeProcess already terminated.", runtimeProcess.isTerminated());
		assertTrue(runtimeProcess.canTerminate());

		mockProcess.setExitValue(1);
		runtimeProcess.terminate();
		assertFalse("RuntimeProcess failed to terminate wrapped process.", mockProcess.isAlive());

		TestUtil.waitWhile(p -> !p.isTerminated(), runtimeProcess, 1000, p -> "RuntimePocess not terminated.");
		TestUtil.waitForJobs(name.getMethodName(), 25, 500);
		assertEquals("Wrong number of terminate events.", 1, processTerminateEvents.get());
		assertEquals("RuntimeProcess reported wrong exit code.", 1, runtimeProcess.getExitValue());
	}

	/**
	 * Test {@link RuntimeProcess} terminating the wrapped process and its
	 * descendants.
	 */
	@Test
	public void testTerminateProcessWithSubProcesses() throws Exception {

		MockProcess grandChildProcess = new MockProcess(MockProcess.RUN_FOREVER);

		MockProcess childProcess1 = new MockProcess(MockProcess.RUN_FOREVER);
		childProcess1.setHandle(new MockProcessHandle(childProcess1, List.of(grandChildProcess)));

		MockProcess childProcess2 = new MockProcess(MockProcess.RUN_FOREVER);

		MockProcess mockProcess = new MockProcess(MockProcess.RUN_FOREVER);
		mockProcess.setHandle(new MockProcessHandle(childProcess1, List.of(childProcess1, childProcess2)));

		RuntimeProcess runtimeProcess = mockProcess.toRuntimeProcess();

		assertTrue("RuntimeProcess already terminated.", grandChildProcess.isAlive());
		assertTrue("RuntimeProcess already terminated.", childProcess1.isAlive());
		assertTrue("RuntimeProcess already terminated.", childProcess2.isAlive());
		assertFalse("RuntimeProcess already terminated.", runtimeProcess.isTerminated());

		runtimeProcess.terminate();

		assertFalse("RuntimeProcess failed to terminate wrapped process.", mockProcess.isAlive());
		assertFalse("RuntimeProcess failed to terminate child of wrapped process.", childProcess1.isAlive());
		assertFalse("RuntimeProcess failed to terminate child of wrapped process.", childProcess2.isAlive());
		assertFalse("RuntimeProcess failed to terminate descendant of wrapped process.", grandChildProcess.isAlive());

		TestUtil.waitWhile(p -> !p.isTerminated(), runtimeProcess, 1000, p -> "RuntimePocess not terminated.");
	}

	/**
	 * Test {@link RuntimeProcess} terminating the wrapped process while not
	 * terminating its descendants.
	 */
	@Test
	public void testTerminateProcessWithoutTerminatingDescendents() throws Exception {

		MockProcess childProcess = new MockProcess(MockProcess.RUN_FOREVER);

		MockProcess mockProcess = new MockProcess(MockProcess.RUN_FOREVER);
		mockProcess.setHandle(new MockProcessHandle(mockProcess, List.of(childProcess)));

		RuntimeProcess runtimeProcess = mockProcess.toRuntimeProcess("MockProcess", Map.of(DebugPlugin.ATTR_TERMINATE_DESCENDANTS, false));

		assertTrue("RuntimeProcess already terminated.", childProcess.isAlive());
		assertFalse("RuntimeProcess already terminated.", runtimeProcess.isTerminated());

		runtimeProcess.terminate();

		assertFalse("RuntimeProcess failed to terminate wrapped process.", mockProcess.isAlive());
		assertTrue("RuntimeProcess terminated child of wrapped process, unlike configured.", childProcess.isAlive());

		TestUtil.waitWhile(p -> !p.isTerminated(), runtimeProcess, 1000, p -> "RuntimePocess not terminated.");
	}

	/**
	 * Test {@link RuntimeProcess} terminating the wrapped process which does
	 * not support {@link Process#toHandle()}.
	 */
	@Test
	public void testTerminateProcessNotSupportingProcessToHandle() throws Exception {

		MockProcess mockProcess = new MockProcess(MockProcess.RUN_FOREVER);
		// set handle to null, so the standard java.lang.Process.toHandle()
		// implementation is called which throws an
		// UnsupportedOperationException
		mockProcess.setHandle(null);
		assertThrows(UnsupportedOperationException.class, mockProcess::toHandle);
		RuntimeProcess runtimeProcess = mockProcess.toRuntimeProcess();
		runtimeProcess.terminate(); // must not throw, even toHandle() does

		TestUtil.waitWhile(p -> !p.isTerminated(), runtimeProcess, 1000, p -> "RuntimePocess not terminated.");
	}

	/**
	 * Test {@link RuntimeProcess} terminating the wrapped process which does
	 * only terminate with a delay.
	 */
	@Test
	public void testTerminateProcessWithTimeoutExeedingTermination() {

		MockProcess mockProcess = new MockProcess(MockProcess.RUN_FOREVER);
		mockProcess.setTerminationDelay(6000);

		RuntimeProcess runtimeProcess = mockProcess.toRuntimeProcess();

		DebugException timeoutException = assertThrows(DebugException.class, runtimeProcess::terminate);
		assertEquals(DebugCoreMessages.RuntimeProcess_terminate_failed, timeoutException.getMessage());
	}

	/**
	 * Test {@link RuntimeProcess} terminating the wrapped process which does
	 * only terminate with a delay.
	 */
	@Test
	public void testTerminateProcessWithDescendentExceedingTimeoutForTermination() {

		MockProcess childProcess = new MockProcess(MockProcess.RUN_FOREVER);
		childProcess.setTerminationDelay(6000);

		MockProcess mockProcess = new MockProcess(MockProcess.RUN_FOREVER);
		mockProcess.setHandle(new MockProcessHandle(mockProcess, List.of(childProcess)));

		RuntimeProcess runtimeProcess = mockProcess.toRuntimeProcess();

		DebugException timeoutException = assertThrows(DebugException.class, runtimeProcess::terminate);
		assertEquals(DebugCoreMessages.RuntimeProcess_terminate_failed, timeoutException.getMessage());
	}

	/**
	 * See comment in MockProcess.MockProcess().new InputStream() {...}.read()
	 * for why this test fails
	 */
	@Test
	@Ignore("See https://bugs.eclipse.org/bugs/show_bug.cgi?id=577189")
	public void testOutputAfterDestroy() throws Exception {
		MockProcess proc = new MockProcess();

		IProcess iProc = new RuntimeProcess(new TestLaunch(), proc, "foo", Collections.emptyMap());
		iProc.terminate();

		String str = iProc.getStreamsProxy().getOutputStreamMonitor().getContents();
		TestUtil.log(IStatus.INFO, name.getMethodName(), "Stream result: ");
		for (int i = 0; i < str.length(); i += 100) {
			TestUtil.log(IStatus.INFO, name.getMethodName(), str.substring(i, Math.min(i + 100, str.length())));
		}
		TestUtil.log(IStatus.INFO, name.getMethodName(), "Stream done.");
		// Make sure that the inputstream (process's stdout) has been fully read
		// and is at EOF
		@SuppressWarnings("resource")
		InputStream inputStream = proc.getInputStream();
		assertEquals(-1, inputStream.read());
		// Make sure that the last character in the stream makes it through to
		// the monitor
		assertTrue(str.endsWith(String.valueOf((char) MockProcess.ProcessState.LASTREAD.getCode())));
	}


}
