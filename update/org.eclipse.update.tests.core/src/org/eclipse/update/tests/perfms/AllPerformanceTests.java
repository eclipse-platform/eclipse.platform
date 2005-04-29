/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.tests.perfms;

import org.eclipse.update.tests.UpdateManagerTestCase;
import junit.framework.*;


public class AllPerformanceTests extends UpdateManagerTestCase {
public AllPerformanceTests(String name) {
	super(name);
}
public static Test suite() {
	TestSuite suite = new TestSuite();
	suite.setName("Update Performance Tests");
	
	// the following will take all the test methods in the class that start with 'test'
	//suite.addTest(new TestSuite(TestStartup.class));
	return suite;
}
}
