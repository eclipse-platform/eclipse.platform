/*******************************************************************************
 *  Copyright (c) 2022 Joerg Kubitz
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Joerg Kubitz - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import junit.framework.*;

/** can be manual started for testing random fails **/
public class RepeatedTestSuite extends TestCase {
    public static Test suite() {
        TestSuite suite = new TestSuite(RepeatedTestSuite.class.getName());

		for (int i = 0; i < 1000; i++) {
			suite.addTestSuite(MarkerTest.class); // the test to repeat
        }
        return suite;
    }
}