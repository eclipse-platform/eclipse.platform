/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package org.eclipse.team.tests.core.regression;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.actions.CompareRevisionAction;
import org.eclipse.team.tests.core.TeamTest;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;

/**
 * Some internal methods can not be removed since they are used in products.
 */
public class DoNotRemoveTest extends TeamTest {

	/**
	 * Tests
	 * {@link Utils#updateLabels(org.eclipse.team.core.synchronize.SyncInfo, org.eclipse.compare.CompareConfiguration)}
	 */
	public void test_Utils_updateLabels() {
		try {
			Method method = Utils.class.getMethod("updateLabels", SyncInfo.class, CompareConfiguration.class);
			assertEquals(Modifier.STATIC | Modifier.PUBLIC,
					method.getModifiers());
			assertEquals(Void.TYPE, method.getReturnType());
		} catch (SecurityException e) {
			fail("test_Utils_updateLabels", e);
		} catch (NoSuchMethodException e) {
			fail("test_Utils_updateLabels", e);
		}
	}

	/**
	 * Tests
	 * {@link CompareRevisionAction#findReusableCompareEditor(IWorkbenchPage)}
	 */
	public void testBug312217() {
		try {
			Method method = CompareRevisionAction.class.getMethod("findReusableCompareEditor", IWorkbenchPage.class);
			assertEquals(Modifier.STATIC | Modifier.PUBLIC,
					method.getModifiers());
			assertEquals(IEditorPart.class, method.getReturnType());
		} catch (SecurityException e) {
			fail("testBug312217", e);
		} catch (NoSuchMethodException e) {
			fail("testBug312217", e);
		}
	}

	public static Test suite() {
		return new TestSuite(DoNotRemoveTest.class);
	}

}
