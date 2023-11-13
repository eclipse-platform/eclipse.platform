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
package org.eclipse.ant.tests.ui.testplugin;

import org.eclipse.ant.tests.ui.debug.TestAgainException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Executes the test again for a defined number of retries after in case an
 * {@link TestAgainException} is thrown.
 */
class TestAgainExceptionRule implements TestRule {

	private final int retryCount;

	public TestAgainExceptionRule(int retryCount) {
		this.retryCount = retryCount;
	}

	@Override
	public Statement apply(final Statement base, final Description description) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				for (int attempt = 0; attempt < retryCount; attempt++) {
					try {
						base.evaluate();
						return;
					} catch (TestAgainException e) {
						String errorMessage = String.format("%s failed attempt %s. Re-testing.", //$NON-NLS-1$
								description.getDisplayName(), attempt);
						System.out.println(errorMessage);
						e.printStackTrace();
					}
				}
			}
		};
	}

}
