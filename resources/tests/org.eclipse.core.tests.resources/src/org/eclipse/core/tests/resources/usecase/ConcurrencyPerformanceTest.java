/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.usecase;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.resources.ResourceTest;

public class ConcurrencyPerformanceTest extends ResourceTest {

	public void testSimpleCalls() {
		final IWorkspaceRunnable job = monitor -> {
			// do nothing
		};
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				try {
					getWorkspace().run(job, null);
				} catch (CoreException e) {
					fail("1.0", e);
				}
			}
		}.run(this, 10, 50);
	}
}
