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


import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.tests.core.AbstractAntTest;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;


public class ProjectTests extends AbstractAntTest {
	
	public ProjectTests(String name) {
		super(name);
	}
	
	/**
	 * Tests that the three properties that should always be set are correct
	 */
	public void testBasePropertiesSet() throws CoreException {
		String buildFileName="TestForEcho.xml"; 
		run(buildFileName);
		IFile buildFile= getBuildFile(buildFileName);
		String fullName= buildFile.getLocation().toFile().getAbsolutePath();
		assertEquals("eclipse.running should have been set as true", "true", AntTestChecker.getDefault().getUserProperty("eclipse.running"));
		assertEquals("ant.file should have been set as the build file name", fullName, AntTestChecker.getDefault().getUserProperty("ant.file"));
		assertNotNull("ant.java.version should have been set", AntTestChecker.getDefault().getUserProperty("ant.java.version"));
		assertNotNull("ant.version should have been set", AntTestChecker.getDefault().getUserProperty("ant.version"));
		assertNotNull("eclipse.home should have been set", AntTestChecker.getDefault().getUserProperty("eclipse.home"));
	}

	public void testValue() throws CoreException {
		String buildFileName="TestForEcho.xml"; 
		run(buildFileName);
		assertEquals("property.testing should have been set as true", "true", AntTestChecker.getDefault().getUserProperty("property.testing"));
	}

	public void testValueWithClass() throws CoreException {
		
		String buildFileName="TestForEcho.xml"; 
		run(buildFileName);
		assertEquals("property.testing2 should have been set as hey", "hey", AntTestChecker.getDefault().getUserProperty("property.testing2"));
	}

	public void testClass() throws CoreException {
		String buildFileName="TestForEcho.xml"; 
		run(buildFileName);
		assertEquals("property.testing3 should have been set as AntTestPropertyProvider", "AntTestPropertyValueProvider", AntTestChecker.getDefault().getUserProperty("property.testing3"));
	}
	
	public void testHeadless() throws CoreException {
		try {
			AntCorePlugin.getPlugin().setRunningHeadless(true);
			String buildFileName="TestForEcho.xml"; 
			run(buildFileName);
			assertNull("property.headless should not have been set as AntTestPropertyProvider", AntTestChecker.getDefault().getUserProperty("property.headless"));
		} finally {
			AntCorePlugin.getPlugin().setRunningHeadless(false);
		}
	}
	
	public void testNotHeadless() throws CoreException {
		String buildFileName="TestForEcho.xml"; 
		run(buildFileName);
		assertEquals("property.headless should have been set as AntTestPropertyProvider", "headless", AntTestChecker.getDefault().getUserProperty("property.headless"));
	}
}