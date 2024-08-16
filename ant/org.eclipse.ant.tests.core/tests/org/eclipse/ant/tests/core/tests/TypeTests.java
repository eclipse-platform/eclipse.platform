/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.Type;
import org.eclipse.ant.internal.core.AntClasspathEntry;
import org.eclipse.ant.tests.core.AbstractAntTest;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.core.runtime.CoreException;
import org.junit.jupiter.api.Test;

public class TypeTests extends AbstractAntTest {

	@Test
	public void testAddType() throws CoreException {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		URL[] urls = prefs.getExtraClasspathURLs();
		Type newType = new Type();
		newType.setLibraryEntry(new AntClasspathEntry(urls[0]));
		newType.setTypeName("anttestpath"); //$NON-NLS-1$
		newType.setClassName("org.eclipse.ant.tests.core.support.types.AntTestPath"); //$NON-NLS-1$
		prefs.setCustomTypes(new Type[] { newType });

		run("CustomType.xml"); //$NON-NLS-1$
		String msg = AntTestChecker.getDefault().getMessages().get(1);
		assertEquals("Test adding a custom type", msg); //$NON-NLS-1$
		assertSuccessful();
	}

	@Test
	public void testRemoveType() {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		prefs.setCustomTypes(new Type[] {});
		try {
			CoreException ce = assertThrows(CoreException.class, () -> run("CustomType.xml"), //$NON-NLS-1$
					"Build should have failed as type no longer defined"); //$NON-NLS-1$
			assertThat(ce.getMessage().trim()).as("exception from undefined type") //$NON-NLS-1$
					.endsWith("Action: Check that any <presetdef>/<macrodef> declarations have taken place."); //$NON-NLS-1$
		} finally {
			restorePreferenceDefaults();
		}

	}

	@Test
	public void testTypeDefinedInExtensionPoint() throws CoreException {
		run("ExtensionPointType.xml"); //$NON-NLS-1$
		String msg = AntTestChecker.getDefault().getMessages().get(1);
		assertEquals("Ensure that an extension point defined type is present", msg); //$NON-NLS-1$
		assertSuccessful();
	}

	@Test
	public void testTypeDefinedInExtensionPointHeadless() {
		AntCorePlugin.getPlugin().setRunningHeadless(true);
		try {
			CoreException ce = assertThrows(CoreException.class, () -> run("ExtensionPointType.xml"), //$NON-NLS-1$
					"Build should have failed as type was not defined to run in headless"); //$NON-NLS-1$
			assertThat(ce.getMessage().trim()).as("exception from undefined type") //$NON-NLS-1$
					.endsWith("Action: Check that any <presetdef>/<macrodef> declarations have taken place."); //$NON-NLS-1$
		} finally {
			AntCorePlugin.getPlugin().setRunningHeadless(false);
		}
	}
}