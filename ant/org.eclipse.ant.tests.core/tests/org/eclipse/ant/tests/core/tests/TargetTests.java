/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.core.tests;


import java.io.File;

import org.eclipse.ant.core.TargetInfo;
import org.eclipse.ant.tests.core.AbstractAntTest;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.core.runtime.CoreException;


public class TargetTests extends AbstractAntTest {
	
	public TargetTests(String name) {
		super(name);
	}
	
	/**
	 * Ensures that a default target is found
	 */
	public void testDefaultTarget() throws CoreException {
		TargetInfo[] targets= getTargets("TestForEcho.xml");
		assertTrue("Should be two targets in TestForEcho.xml", targets.length == 2);
		assertTrue("Test for Echo should be the default target", targets[1].isDefault());
	}
	
	/**
	 * Ensures that targets are found in a build file with data types
	 */
	public void testGetTargetsWithDataTypes() throws CoreException {
		TargetInfo[] targets= getTargets("Bug32551.xml");
		assertTrue("Should be one targets in Bug32551.xml", targets.length == 1);
	}
	
	/**
	 * Ensures that target names are retrieved properly
	 */
	public void testTargetNames() throws CoreException {
		String[] targetNames= getTargetNames("TestForEcho.xml");
		assertTrue("Should be two targets in TestForEcho.xml", targetNames.length == 2);
		assertTrue("First name should be init", targetNames[0].equals("init"));
		assertTrue("Second name should be Test for Echo", targetNames[1].equals("Test for Echo"));
	}
	
	/**
	 * Ensures that target descriptions are retrieved properly
	 */
	public void testTargetDescription() throws CoreException {
		String[] targetDescriptions= getTargetDescriptions("TestForEcho.xml");
		assertTrue("Should be two targets in TestForEcho.xml", targetDescriptions.length == 2);
		assertNull("First description should be null", targetDescriptions[0]);
		assertTrue("Second description should be Calls other targets", targetDescriptions[1].equals("Calls other echos"));
	}
	
	/**
	 * Ensures that target projects are retrieved properly
	 */
	public void testTargetProject() throws CoreException {
		String targetProject= getProjectName("TestForEcho.xml", "Test for Echo");
		assertTrue("Project name should be Echo Test", "Echo Test".equals(targetProject));
	}
	
	/**
	 * Ensures that target dependencies are retrieved properly
	 */
	public void testTargetDependencies() throws CoreException {
		String[] dependencies= getDependencies("TestForEcho.xml", "Test for Echo");
		assertNotNull("Dependencies should not be null", dependencies);
		assertTrue("Should be one dependency in Test for Echo", dependencies.length == 1);
		assertTrue("First dependency should be init", "init".equals(dependencies[0]));
	}
	
	/**
	 * Runs an Ant build and ensure that the build file location is logged
	 */
	public void testRunScript() throws CoreException {
		run("TestForEcho.xml");
		String message= (String)AntTestChecker.getDefault().getMessages().get(0);
		assertTrue("Build file location should be logged as the first message", message != null && message.endsWith("AntTests" + File.separator + "buildfiles" + File.separator + "TestForEcho.xml"));
		assertSuccessful();
	}
}

