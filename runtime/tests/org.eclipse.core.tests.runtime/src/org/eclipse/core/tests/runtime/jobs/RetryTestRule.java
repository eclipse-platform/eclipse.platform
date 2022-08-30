/*******************************************************************************
 * Copyright (c) 2022 Joerg Kubitz and others.
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
package org.eclipse.core.tests.runtime.jobs;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/** runs the test class multiple times **/
public class RetryTestRule implements TestRule {

	private final int retryCount;

	public RetryTestRule(int retryCount) {
		this.retryCount = retryCount;
	}

	@Override
	public Statement apply(final Statement base, final Description description) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				Throwable caughtThrowable = null;
				int failuresCount = 0;
				for (int i = 0; i < retryCount; i++) {
					try {
						base.evaluate();
					} catch (Throwable t) {
						caughtThrowable = t;
						System.err.println(description.getDisplayName() + ": run " + (i + 1) + " failed:");
						t.printStackTrace();
						++failuresCount;
					}
				}
				if (caughtThrowable == null) {
					return;
				}
				throw new AssertionError(description.getDisplayName() + ": failures " + failuresCount + " out of "
						+ retryCount + " tries. Last cause:" + caughtThrowable, caughtThrowable);
			}
		};
	}
}