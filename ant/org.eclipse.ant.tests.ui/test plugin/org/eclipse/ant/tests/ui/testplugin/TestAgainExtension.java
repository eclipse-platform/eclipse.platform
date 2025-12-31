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

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.ant.tests.ui.debug.TestAgainException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

/**
 * Executes the test again multiple times in case a {@link TestAgainException}
 * is thrown.
 */
class TestAgainExtension implements TestExecutionExceptionHandler {

	private static final int RETRY_COUNT = 5;

	private final AtomicInteger counter = new AtomicInteger(1);

	public TestAgainExtension() {
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		printError(context, throwable);
		context.getTestMethod().ifPresent(method -> {
			while (counter.incrementAndGet() <= RETRY_COUNT) {
				try {
					context.getExecutableInvoker().invoke(method, context.getRequiredTestInstance());
				} catch (TestAgainException t) {
					printError(context, throwable);
					if (counter.get() >= RETRY_COUNT) {
						throw t;
					}
				}
			}
		});
	}

	private void printError(ExtensionContext context, Throwable e) {
		String errorMessage = String.format("%s failed attempt %s. Re-testing.", //$NON-NLS-1$
				context.getDisplayName(), counter.get());
		System.out.println(errorMessage);
		e.printStackTrace();
	}

}
