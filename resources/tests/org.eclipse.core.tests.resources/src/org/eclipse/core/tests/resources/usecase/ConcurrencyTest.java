/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

public class ConcurrencyTest extends ResourceTest {

	protected void assertIsNotRunning(ConcurrentOperation01 op, String label) {
		/* try more than once, "just in case" */
		for (int i = 0; i < 3; i++) {
			try {
				Thread.sleep(100 * i); // fancy sleep
			} catch (InterruptedException e) {
				// ignore
			}
			assertTrue(label, !op.isRunning());
		}
	}

	/**
	 * This test is used to find out if two operations can start concurrently. It assumes
	 * that they cannot.
	 */
	public void testConcurrentOperations() throws CoreException {

		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(null);
		project.open(null);

		buildResources(project, defineHierarchy());

		ConcurrentOperation01 op1 = new ConcurrentOperation01(getWorkspace());
		ConcurrentOperation01 op2 = new ConcurrentOperation01(getWorkspace());

		/* start first operation */
		new Thread(op1, "op1").start();
		assertTrue("0.0", op1.hasStarted());
		op1.returnWhenInSyncPoint();
		assertTrue("0.1", op1.isRunning());

		/* start second operation but it should not run until the first finishes */
		new Thread(op2, "op2").start();
		assertTrue("1.0", op2.hasStarted());
		assertIsNotRunning(op2, "1.1");

		/* free operations */
		op1.proceed();
		op2.returnWhenInSyncPoint();
		assertTrue("2.0", op2.isRunning());
		op2.proceed();
		assertTrue("2.1", op1.getStatus().isOK());
		assertTrue("2.2", op2.getStatus().isOK());

		/* remove trash */
		ensureDoesNotExistInWorkspace(getWorkspace().getRoot());
	}
}
