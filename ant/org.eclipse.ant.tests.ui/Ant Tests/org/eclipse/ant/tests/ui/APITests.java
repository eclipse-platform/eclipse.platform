/*******************************************************************************
 * Copyright (c) 2015, 2018 IBM Corporation and others.
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
package org.eclipse.ant.tests.ui;

import static org.junit.Assert.assertEquals;

import org.eclipse.jdt.core.JavaCore;
import org.junit.Test;

public class APITests {

	@Test
	public void testCompareJavaVersions() {
		String vmver = "1.6"; //$NON-NLS-1$
		int comparison = JavaCore.compareJavaVersions(vmver, JavaCore.VERSION_1_7);
		assertEquals("VM less than 1.7 version: ", -1, comparison); //$NON-NLS-1$

		vmver = "1.7"; //$NON-NLS-1$
		comparison = JavaCore.compareJavaVersions(vmver, JavaCore.VERSION_1_7);
		assertEquals("VM equal to 1.7: ", 0, comparison); //$NON-NLS-1$

		vmver = "1.8"; //$NON-NLS-1$
		comparison = JavaCore.compareJavaVersions(vmver, JavaCore.VERSION_1_7);
		assertEquals("VM more than 1.7: ", 1, comparison); //$NON-NLS-1$

	}

}