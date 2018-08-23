/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.compare.examples.xml;

import junit.framework.*;

/**
 * TestSuite that runs all the XML Compare tests.
 */
public class AllXMLCompareTests {

	public static void main (String[] args) {
		junit.textui.TestRunner.run (suite());
	}
	
	public static Test suite ( ) {
		TestSuite suite= new TestSuite("All XML Compare Tests"); //$NON-NLS-1$
		suite.addTest(TestXMLStructureCreator.suite());
	    return suite;
	}
}

