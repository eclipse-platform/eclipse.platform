/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.net;

import junit.framework.*;

public class AllNetTests extends TestCase {
	
	public AllNetTests() {
		super();
	}

	public AllNetTests(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTest(NetTest.suite());
		suite.addTest(PreferenceModifyListenerTest.suite());
		return suite;
	}
}
