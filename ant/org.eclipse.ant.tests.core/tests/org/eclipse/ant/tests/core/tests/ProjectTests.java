/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.ant.tests.core.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.tests.core.AbstractAntTest;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.junit.jupiter.api.Test;

public class ProjectTests extends AbstractAntTest {

	/**
	 * Tests that the three properties that should always be set are correct
	 */
	@Test
	public void testBasePropertiesSet() throws CoreException {
		String buildFileName = "TestForEcho.xml"; //$NON-NLS-1$
		run(buildFileName);
		IFile buildFile = getBuildFile(buildFileName);
		String fullName = buildFile.getLocation().toFile().getAbsolutePath();
		assertEquals("true", AntTestChecker.getDefault().getUserProperty("eclipse.running"), //$NON-NLS-1$ //$NON-NLS-2$
				"eclipse.running should have been set as true"); //$NON-NLS-1$
		assertEquals(fullName, AntTestChecker.getDefault().getUserProperty("ant.file"), //$NON-NLS-1$
				"ant.file should have been set as the build file name"); //$NON-NLS-1$
		assertNotNull(AntTestChecker.getDefault().getUserProperty("ant.java.version"), //$NON-NLS-1$
				"ant.java.version should have been set"); //$NON-NLS-1$
		assertNotNull(AntTestChecker.getDefault().getUserProperty("ant.version"), "ant.version should have been set"); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull(AntTestChecker.getDefault().getUserProperty("eclipse.home"), "eclipse.home should have been set"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testValue() throws CoreException {
		String buildFileName = "TestForEcho.xml"; //$NON-NLS-1$
		run(buildFileName);
		assertEquals("true", AntTestChecker.getDefault().getUserProperty("property.testing"), //$NON-NLS-1$ //$NON-NLS-2$
				"property.testing should have been set as true"); //$NON-NLS-1$
	}

	@Test
	public void testValueWithClass() throws CoreException {

		String buildFileName = "TestForEcho.xml"; //$NON-NLS-1$
		run(buildFileName);
		assertEquals("hey", AntTestChecker.getDefault().getUserProperty("property.testing2"), //$NON-NLS-1$ //$NON-NLS-2$
				"property.testing2 should have been set as hey"); //$NON-NLS-1$
	}

	@Test
	public void testClass() throws CoreException {
		String buildFileName = "TestForEcho.xml"; //$NON-NLS-1$
		run(buildFileName);
		assertEquals("AntTestPropertyValueProvider", AntTestChecker.getDefault().getUserProperty("property.testing3"), //$NON-NLS-1$ //$NON-NLS-2$
				"property.testing3 should have been set as AntTestPropertyProvider"); //$NON-NLS-1$
	}

	@Test
	public void testHeadless() throws CoreException {
		try {
			AntCorePlugin.getPlugin().setRunningHeadless(true);
			String buildFileName = "TestForEcho.xml"; //$NON-NLS-1$
			run(buildFileName);
			assertNull(AntTestChecker.getDefault().getUserProperty("property.headless"), //$NON-NLS-1$
					"property.headless should not have been set as AntTestPropertyProvider"); //$NON-NLS-1$
		}
		finally {
			AntCorePlugin.getPlugin().setRunningHeadless(false);
		}
	}

	@Test
	public void testNotHeadless() throws CoreException {
		String buildFileName = "TestForEcho.xml"; //$NON-NLS-1$
		run(buildFileName);
		assertEquals("headless", AntTestChecker.getDefault().getUserProperty("property.headless"), //$NON-NLS-1$ //$NON-NLS-2$
				"property.headless should have been set as AntTestPropertyProvider"); //$NON-NLS-1$
	}
}