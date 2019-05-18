/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.compatible;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTestsCompatibility extends TestSuite {
			
	public static Test suite() {	
		TestSuite suite = new TestSuite();
		suite.addTest(BasicTest.suite());
		suite.addTest(ConflictTest.suite());
		suite.addTest(ModuleTest.suite());
		return new CompatibleTestSetup(suite);
	}	
	
	public AllTestsCompatibility(String name) {
		super(name);
	}

	public AllTestsCompatibility() {
		super();
	}
}

