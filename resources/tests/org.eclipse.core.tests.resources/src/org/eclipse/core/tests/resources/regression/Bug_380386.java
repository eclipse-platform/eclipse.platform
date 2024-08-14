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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for Bug 380386
 */
@ExtendWith(WorkspaceResetExtension.class)
public class Bug_380386 {

	@Test
	@Disabled("This regression test has to be rewritten in a proper way")
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

		assertNotNull(statusName);
		assertNotNull(statusValue);

		assertTrue(statusName.isOK());
		assertNotNull(statusValue.isOK());

		pathManager.setURIValue(name, value);
	}

}
