/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.core;

import org.eclipse.team.tests.ccvs.core.cvsresources.AllTestsCVSResources;
import org.eclipse.team.tests.ccvs.core.jsch.AllJschTests;

import org.eclipse.jface.util.Util;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests that don't require the Team UI plugin to be loaded.
 */
public class AllCoreTests extends EclipseTest {

	public AllCoreTests() {
		super();
	}

	public AllCoreTests(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();

		// Bug 525817: Disable CVS tests on Mac
		if (Util.isMac())
			return suite;

		suite.addTest(AllTestsCVSResources.suite());
		suite.addTest(AllJschTests.suite());
		return new TestSetup(suite);
	}
}
