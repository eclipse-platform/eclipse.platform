/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
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
package org.eclipse.team.tests.core;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.tests.resources.ResourceTest;
import org.eclipse.team.tests.core.mapping.ScopeTests;
import org.eclipse.team.tests.ui.SaveableCompareEditorInputTest;

public class AllTeamUITests extends ResourceTest {

	public AllTeamUITests() {
		super();
	}

	public AllTeamUITests(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTest(ScopeTests.suite());
		suite.addTest(SaveableCompareEditorInputTest.suite());
		return suite;
	}
}
