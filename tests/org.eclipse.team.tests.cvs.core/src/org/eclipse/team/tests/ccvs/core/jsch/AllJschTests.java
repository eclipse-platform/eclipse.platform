/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.jsch;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.team.tests.ccvs.core.EclipseTest;

public class AllJschTests extends EclipseTest {

	public AllJschTests() {
		super();
	}

	public AllJschTests(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTest(PreferenceInitializerTest.suite());
		return suite;
	}
}
