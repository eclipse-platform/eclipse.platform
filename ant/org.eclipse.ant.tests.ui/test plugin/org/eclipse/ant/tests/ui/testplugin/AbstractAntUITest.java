/*******************************************************************************
 *  Copyright (c) 2003, 2025 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.testplugin;

import static org.eclipse.ant.tests.ui.testplugin.AntUITestUtil.assertProject;

import org.junit.Before;
import org.junit.Rule;

/**
 * Abstract Ant UI test class
 */
public abstract class AbstractAntUITest {

	private final CloseWelcomeScreenExtension closeWelcomeScreenExtension = new CloseWelcomeScreenExtension();

	@Rule
	public TestAgainExceptionRule testAgainRule = new TestAgainExceptionRule(5);

	@Before
	public void setUp() throws Exception {
		assertProject();
		closeWelcomeScreenExtension.assertWelcomeScreenClosed();
	}

}