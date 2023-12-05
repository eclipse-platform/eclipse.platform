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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.ant.core.Property;
import org.eclipse.ant.core.Task;
import org.eclipse.ant.internal.core.AntClasspathEntry;
import org.eclipse.ant.tests.core.AbstractAntTest;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.ant.tests.core.testplugin.ProjectHelper;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

public class TaskTests extends AbstractAntTest {

	/**
	 * Testing the old deprecated API
	 */
	@SuppressWarnings("deprecation")
	@Test
	public void testAddTaskSettingLibrary() throws CoreException {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		URL[] urls = prefs.getExtraClasspathURLs();
		Task newTask = new Task();
		newTask.setLibrary(urls[0]);
		newTask.setTaskName("AntTestTask"); //$NON-NLS-1$
		newTask.setClassName("org.eclipse.ant.tests.core.support.tasks.AntTestTask"); //$NON-NLS-1$
		prefs.setCustomTasks(new Task[] { newTask });

		prefs.updatePluginPreferences();

		run("CustomTask.xml", new String[0], false); //$NON-NLS-1$
		String msg = AntTestChecker.getDefault().getMessages().get(1);
		assertEquals("Message incorrect: " + msg, "Testing Ant in Eclipse with a custom task", msg); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
	}

	@Test
	public void testAddTaskSettingLibraryEntry() throws CoreException {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		URL[] urls = prefs.getExtraClasspathURLs();
		Task newTask = new Task();
		newTask.setLibraryEntry(new AntClasspathEntry(urls[0]));
		newTask.setTaskName("AntTestTask2"); //$NON-NLS-1$
		newTask.setClassName("org.eclipse.ant.tests.core.support.tasks.AntTestTask"); //$NON-NLS-1$
		prefs.setCustomTasks(new Task[] { newTask });

		prefs.updatePluginPreferences();

		run("CustomTask.xml", new String[] { "Custom Task from Entry" }, false); //$NON-NLS-1$ //$NON-NLS-2$
		String msg = AntTestChecker.getDefault().getMessages().get(1);
		assertEquals("Message incorrect: " + msg, "Testing Ant in Eclipse with a custom task", msg); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
	}

	@Test
	public void testRemoveTask() {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		prefs.setCustomTasks(new Task[] {});
		try {
			CoreException ce = assertThrows("Build should have failed as task no longer defined", CoreException.class, //$NON-NLS-1$
					() -> run("CustomTask.xml")); //$NON-NLS-1$
			assertTrue("Exception from undefined task is incorrect", ce.getMessage().trim() //$NON-NLS-1$
					.endsWith("Action: Check that any <presetdef>/<macrodef> declarations have taken place.")); //$NON-NLS-1$
		} finally {
			restorePreferenceDefaults();
		}
	}

	@Test
	public void testAddTaskFromFolder() throws CoreException {
		try {
			AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
			Task newTask = new Task();
			String path = getProject().getFolder(ProjectHelper.LIB_FOLDER).getFile("taskFolder").getLocation().toFile() //$NON-NLS-1$
					.getAbsolutePath();
			IAntClasspathEntry entry = new AntClasspathEntry(path + File.separatorChar);
			IAntClasspathEntry entries[] = prefs.getAdditionalClasspathEntries();
			IAntClasspathEntry newEntries[] = new IAntClasspathEntry[entries.length + 1];
			System.arraycopy(entries, 0, newEntries, 0, entries.length);
			newEntries[entries.length] = entry;
			prefs.setAdditionalClasspathEntries(newEntries);

			newTask.setLibraryEntry(entry);
			newTask.setTaskName("AntTestTask"); //$NON-NLS-1$
			newTask.setClassName("org.eclipse.ant.tests.core.support.tasks.AntTestTask2"); //$NON-NLS-1$
			prefs.setCustomTasks(new Task[] { newTask });

			prefs.updatePluginPreferences();

			run("CustomTask.xml", new String[0], false); //$NON-NLS-1$
			String msg = AntTestChecker.getDefault().getMessages().get(1);
			assertEquals("Message incorrect: " + msg, "Testing Ant in Eclipse with a custom task", msg); //$NON-NLS-1$ //$NON-NLS-2$
			assertSuccessful();
		} finally {
			restorePreferenceDefaults();
		}
	}

	@Test
	public void testTasksDefinedInPropertyFile() throws CoreException {
		try {
			AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
			Property newProp = new Property("ROOTDIR", "..//resources"); //$NON-NLS-1$ //$NON-NLS-2$
			prefs.setCustomProperties(new Property[] { newProp });
			run("Bug34663.xml"); //$NON-NLS-1$
		} finally {
			restorePreferenceDefaults();
		}
	}

	@Test
	public void testTaskDefinedInExtensionPoint() throws CoreException {
		run("ExtensionPointTask.xml"); //$NON-NLS-1$
		String msg = AntTestChecker.getDefault().getMessages().get(1);
		assertEquals("Message incorrect: " + msg, "Testing Ant in Eclipse with a custom task", msg); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
	}

	@Test
	public void testTaskDefinedInExtensionPointHeadless() {
		AntCorePlugin.getPlugin().setRunningHeadless(true);
		try {
			CoreException ce = assertThrows("Build should have failed as task was not defined to run in headless", //$NON-NLS-1$
					CoreException.class, () -> run("ExtensionPointTask.xml")); //$NON-NLS-1$
			assertTrue("Exception from undefined task is incorrect", ce.getMessage().trim() //$NON-NLS-1$
					.endsWith("Action: Check that any <presetdef>/<macrodef> declarations have taken place.")); //$NON-NLS-1$
		} finally {
			AntCorePlugin.getPlugin().setRunningHeadless(false);
		}
	}

	@Test
	public void testTaskDefinedInExtensionPointWithURI() throws CoreException {
		run("ExtensionPointTask.xml"); //$NON-NLS-1$
		String msg = AntTestChecker.getDefault().getMessages().get(2);
		assertEquals("Message incorrect: " + msg, "Testing Ant in Eclipse with a custom task", msg); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
	}
}