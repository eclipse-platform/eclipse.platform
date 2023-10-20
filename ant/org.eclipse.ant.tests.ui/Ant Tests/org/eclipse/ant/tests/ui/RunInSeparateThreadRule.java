/*******************************************************************************
 *  Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.ant.tests.ui;

import java.util.concurrent.CompletableFuture;

import org.eclipse.swt.widgets.Display;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Runs the test in a separate thread and spins readAndDisplay in the UI thread.
 * Terminates with the exception of the separate thread in case one occurred.
 */
class RunInSeparateThreadRule implements TestRule {

	@Override
	public Statement apply(final Statement base, final Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				final Display display = Display.getCurrent();
				CompletableFuture<Result> future = evaluateAsync(base);
				future.thenRun(display::wake);
				while (!future.isDone()) {
					doReadAndDispatch(display);
				}
				Throwable thrownException = future.get().throwable;
				if (thrownException != null) {
					throw thrownException;
				}
			}
		};
	}

	private static CompletableFuture<Result> evaluateAsync(final Statement statement) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				statement.evaluate();
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
