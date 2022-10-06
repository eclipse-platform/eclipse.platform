/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.core.internal.expressions.tests;

import junit.framework.JUnit4TestAdapter;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllExpressionTests {

	public static Test suite() {
		TestSuite suite= new TestSuite("All Expression Language Tests"); //$NON-NLS-1$
		suite.addTest(PropertyTesterTests.suite());
		suite.addTest(new JUnit4TestAdapter(ExpressionTests.class));
		suite.addTest(ExpressionInfoTests.suite());
		suite.addTest(CountExpressionTest.suite());
		return suite;
	}
}
