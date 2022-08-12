/*******************************************************************************
 *  Copyright (c) 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Test for Bug 380386
 */
public class Bug_380386 extends ResourceTest {

	public void testBug() throws Exception {

		String path = "C:\\temp";
		java.net.URI value = new java.io.File(path).toURI();
		IPathVariableManager pathManager = ResourcesPlugin.getWorkspace().getPathVariableManager();
		String name = "somename";
		IStatus statusName = pathManager.validateName(name);
		IStatus statusValue = pathManager.validateValue(value);

		if (statusName == null || statusValue == null) {
			System.err.println("statusName is " + (statusName == null ? "null" : ("not null: '" + statusName + "'.")));
			System.err.println("statusValue is " + (statusValue == null ? "null" : ("not null: '" + statusValue + "'.")));

		} else if (statusName.isOK() && statusValue.isOK()) {
			pathManager.setURIValue(name, value);
			System.out.println("Everything is fine");
		} else {
			if (!statusName.isOK()) {
				System.err.println("statusName is not OK.");
			}
			if (!statusValue.isOK()) {
				System.err.println("statusValue is not OK.");
			}
		}

		assertNotNull("1.0", statusName);
		assertNotNull("2.0", statusValue);

		assertTrue("3.0", statusName.isOK());
		assertNotNull("4.0", statusValue.isOK());

		try {
			pathManager.setURIValue(name, value);
		} catch (CoreException e) {
			fail("5.0", e);
		}
	}
}
