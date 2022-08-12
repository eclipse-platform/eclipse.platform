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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * TestSuite that runs all the XML Compare tests.
 */
@RunWith(Suite.class)				
@Suite.SuiteClasses({	
	TestXMLStructureCreator.class
})
public class AllXMLCompareTests {
	//test suite
}

