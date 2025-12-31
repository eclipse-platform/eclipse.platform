/*******************************************************************************
 *  Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ant.tests.ui.testplugin;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * Runs the test in a separate thread and spins readAndDisplay in the UI thread.
 * Terminates with the exception of the separate thread in case one occurred.
 */
class RunInSeparateThreadExtension implements InvocationInterceptor {

	@Override
	public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
			ExtensionContext extensionContext) throws Throwable {
		final Display display = Display.getCurrent();
		CompletableFuture<Result> future = evaluateAsync(invocation);
		future.thenRun(display::wake);
		while (!future.isDone()) {
			doReadAndDispatch(display);
		}
		Throwable thrownException = future.get().throwable;
		if (thrownException != null) {
			throw thrownException;
		}
	}

	private static CompletableFuture<Result> evaluateAsync(final Invocation<Void> statement) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				statement.proceed();
				return new Result(null);
			} catch (Throwable exception) {
				return new Result(exception);
			}
		});
	}

	private static void doReadAndDispatch(final Display display) {
		try {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private record Result(Throwable throwable) {
	}

}
