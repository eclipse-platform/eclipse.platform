/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.session;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test for bug 426263
 */
public class TestBug426263 {

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	@Test
	public void testBug() {
		IPathVariableManager manager = getWorkspace().getPathVariableManager();
		assertFalse(manager.isUserDefined("ECLIPSE_HOME"));
	}

}
