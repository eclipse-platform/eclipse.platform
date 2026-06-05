/*******************************************************************************
 *  Copyright (c) 2026 IBM Corporation.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.IRequest;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.internal.core.commands.ResumeOthersCommand;
import org.junit.jupiter.api.Test;

public class ResumeOthersCommandTests {

	@Test
	public void testResumeOthers() throws Exception {

		AtomicReference<IThread[]> threadsRef = new AtomicReference<>();
		IDebugTarget target = createTargetProxy(threadsRef::get);

		IThread threadA = createThreadProxy(() -> target);
		IThread threadB = createThreadProxy(() -> target);
		IThread threadC = createThreadProxy(() -> target);

		threadsRef.set(new IThread[] { threadA, threadB, threadC });

		assertTrue(threadA.isSuspended());
		assertTrue(threadB.isSuspended());
		assertTrue(threadC.isSuspended());

		TestResumeOthersCommand command = new TestResumeOthersCommand();

		command.run(new Object[] { threadA });

		assertTrue(threadA.isSuspended());
		assertFalse(threadB.isSuspended());
		assertFalse(threadC.isSuspended());
	}

	private static class TestResumeOthersCommand extends ResumeOthersCommand {

		void run(Object[] targets) throws Exception {
			doExecute(targets, new NullProgressMonitor(), (IRequest) null);
		}
	}

	private static IDebugTarget createTargetProxy(Supplier<IThread[]> threadsSupplier) {
		return (IDebugTarget) Proxy.newProxyInstance(ResumeOthersCommandTests.class.getClassLoader(), new Class<?>[] {
				IDebugTarget.class }, (proxy, method, args) -> (switch (method.getName()) {
					case "getThreads" -> threadsSupplier.get();
					case "hasThreads" -> {
						IThread[] threads = threadsSupplier.get();
						yield threads != null && threads.length > 0;
					}
					case "getDebugTarget" -> proxy;
					default -> defaultValue(method.getReturnType());
				}));
	}

	private static IThread createThreadProxy(Supplier<IDebugTarget> targetSupplier) {
		AtomicBoolean suspended = new AtomicBoolean(true);
		return (IThread) Proxy.newProxyInstance(ResumeOthersCommandTests.class.getClassLoader(), new Class<?>[] {
				IThread.class }, (proxy, method, args) -> (switch (method.getName()) {
					case "canResume" -> suspended.get();
					case "resume" -> {
						suspended.set(false);
						yield null;
					}
					case "isSuspended" -> suspended.get();
					case "getDebugTarget" -> targetSupplier.get();
					case "canSuspend" -> !suspended.get();
					case "suspend" -> {
						suspended.set(true);
						yield null;
					}
					default -> defaultValue(method.getReturnType());
				}));
	}

	private static Object defaultValue(Class<?> returnType) {
		if (returnType == boolean.class) {
			return false;
		}
		if (returnType == int.class) {
			return 0;
		}
		return null;
	}
}
