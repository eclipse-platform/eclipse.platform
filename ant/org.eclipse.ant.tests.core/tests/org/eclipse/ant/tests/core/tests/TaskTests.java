/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.core.tests;

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

public class TaskTests extends AbstractAntTest {

	
	public TaskTests(String name) {
		super(name);
	}
	
	/**
	 * Testing the old deprecated API
	 * @throws CoreException
	 */
	public void testAddTaskSettingLibrary() throws CoreException {
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		URL[] urls= prefs.getExtraClasspathURLs();
		Task newTask= new Task();
		newTask.setLibrary(urls[0]);
		newTask.setTaskName("AntTestTask");
		newTask.setClassName("org.eclipse.ant.tests.core.support.tasks.AntTestTask");
		prefs.setCustomTasks(new Task[]{newTask});
		
		prefs.updatePluginPreferences();
		
		run("CustomTask.xml", new String[0], false);
		String msg= (String)AntTestChecker.getDefault().getMessages().get(1);
		assertEquals("Message incorrect: " + msg, "Testing Ant in Eclipse with a custom task", msg);
		assertSuccessful();
	}
	
	public void testAddTaskSettingLibraryEntry() throws CoreException {
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		URL[] urls= prefs.getExtraClasspathURLs();
		Task newTask= new Task();
		newTask.setLibraryEntry(new AntClasspathEntry(urls[0]));
		newTask.setTaskName("AntTestTask2");
		newTask.setClassName("org.eclipse.ant.tests.core.support.tasks.AntTestTask");
		prefs.setCustomTasks(new Task[]{newTask});
		
		prefs.updatePluginPreferences();
		
		run("CustomTask.xml", new String[] {"Custom Task from Entry"}, false);
		String msg= (String)AntTestChecker.getDefault().getMessages().get(1);
		assertEquals("Message incorrect: " + msg, "Testing Ant in Eclipse with a custom task", msg);
		assertSuccessful();
	}
	
	public void testRemoveTask() {
		AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
		prefs.setCustomTasks(new Task[]{});
		try {
			run("CustomTask.xml");
		} catch (CoreException ce) {
			assertTrue("Exception from undefined task is incorrect", ce.getMessage().trim().endsWith("Action: Check that any <presetdef>/<macrodef> declarations have taken place."));
			return;
		} finally {
			restorePreferenceDefaults();	
		}
		assertTrue("Build should have failed as task no longer defined", false);
	}
	
	public void testAddTaskFromFolder() throws CoreException {
		try {
			AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
			Task newTask= new Task();
			String path= getProject().getFolder(ProjectHelper.LIB_FOLDER).getFile("taskFolder").getLocation().toFile().getAbsolutePath();
			IAntClasspathEntry entry= new AntClasspathEntry(path + File.separatorChar);
			IAntClasspathEntry entries[] = prefs.getAdditionalClasspathEntries();
			IAntClasspathEntry newEntries[] = new IAntClasspathEntry[entries.length + 1];
			System.arraycopy(entries, 0, newEntries, 0, entries.length);
			newEntries[entries.length] = entry;
			prefs.setAdditionalClasspathEntries(newEntries);
		
			newTask.setLibraryEntry(entry);
			newTask.setTaskName("AntTestTask");
			newTask.setClassName("org.eclipse.ant.tests.core.support.tasks.AntTestTask2");
			prefs.setCustomTasks(new Task[]{newTask});
		
			prefs.updatePluginPreferences();
		
			run("CustomTask.xml", new String[0], false);
			String msg= (String)AntTestChecker.getDefault().getMessages().get(1);
			assertEquals("Message incorrect: " + msg, "Testing Ant in Eclipse with a custom task", msg);
			assertSuccessful();
		} finally {
			restorePreferenceDefaults();
		}
	}
		
	public void testTasksDefinedInPropertyFile() throws CoreException {
		try {
			AntCorePreferences prefs =AntCorePlugin.getPlugin().getPreferences();
			Property newProp= new Property("ROOTDIR", "..//resources");
			prefs.setCustomProperties(new Property[]{newProp});
			run("Bug34663.xml");
		} finally {
			restorePreferenceDefaults();
		}
	}
	
	public void testTaskDefinedInExtensionPoint() throws CoreException {
		run("ExtensionPointTask.xml");
		String msg= (String)AntTestChecker.getDefault().getMessages().get(1);
		assertEquals("Message incorrect: " + msg, "Testing Ant in Eclipse with a custom task", msg);
		assertSuccessful();
	}
		
	public void testTaskDefinedInExtensionPointHeadless() {
		AntCorePlugin.getPlugin().setRunningHeadless(true);
		try {
			run("ExtensionPointTask.xml");
		} catch (CoreException ce) {
			assertTrue("Exception from undefined task is incorrect", ce.getMessage().trim().endsWith("Action: Check that any <presetdef>/<macrodef> declarations have taken place."));
			return;
		} finally {
			AntCorePlugin.getPlugin().setRunningHeadless(false);
		}
		assertTrue("Build should have failed as task was not defined to run in headless", false);
	}
	
	public void testTaskDefinedInExtensionPointWithURI() throws CoreException {
		run("ExtensionPointTask.xml");
		String msg= (String)AntTestChecker.getDefault().getMessages().get(2);
		assertEquals("Message incorrect: " + msg, "Testing Ant in Eclipse with a custom task", msg);
		assertSuccessful();
	}
}