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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.eclipse.ant.core.TargetInfo;
import org.eclipse.ant.tests.core.AbstractAntTest;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;

public class TargetTests extends AbstractAntTest {

	/**
	 * Ensures that a default target is found
	 */
	@Test
	public void testDefaultTarget() throws CoreException {
		TargetInfo[] targets = getTargets("TestForEcho.xml"); //$NON-NLS-1$
		assertThat(targets).as("number of targets in TestForEcho.xml").hasSize(2); //$NON-NLS-1$
		assertTrue("Test for Echo should be the default target", targets[1].isDefault()); //$NON-NLS-1$
	}

	/**
	 * Ensures that targets are found in a build file with data types
	 */
	@Test
	public void testGetTargetsWithDataTypes() throws CoreException {
		TargetInfo[] targets = getTargets("Bug32551.xml"); //$NON-NLS-1$
		assertThat(targets).as("number of targets in Bug32551.xml").hasSize(1); //$NON-NLS-1$
	}

	/**
	 * Ensures that targets are found in a buildfile with a fileset based on
	 * ant_home (that ant_home is set at parse time) Bug 42926.
	 */
	@Test
	public void testGetTargetsWithAntHome() {
		System.getProperties().remove("ant.home"); //$NON-NLS-1$
		CoreException ce = assertThrows(CoreException.class, () -> getTargets("Bug42926.xml")); //$NON-NLS-1$
		// classpathref was successful but the task is not defined
		String message = ce.getMessage();
		assertTrue("Core exception message not as expected: " //$NON-NLS-1$
				+ message,
				message.endsWith(
						"Bug42926.xml:7: taskdef class com.foo.SomeTask cannot be found\n using the classloader AntClassLoader[]")); //$NON-NLS-1$
	}

	/**
	 * Ensures that target names are retrieved properly
	 */
	@Test
	public void testTargetNames() throws CoreException {
		String[] targetNames = getTargetNames("TestForEcho.xml"); //$NON-NLS-1$
		assertThat(targetNames).as("number of targets in TestForEcho.xml").hasSize(2); //$NON-NLS-1$
		assertEquals("First name should be init", "init", targetNames[0]); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Second name should be Test for Echo", "Test for Echo", targetNames[1]); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Ensures that target descriptions are retrieved properly
	 */
	@Test
	public void testTargetDescription() throws CoreException {
		String[] targetDescriptions = getTargetDescriptions("TestForEcho.xml"); //$NON-NLS-1$
		assertThat(targetDescriptions).as("number of targets in TestForEcho.xml").hasSize(2); //$NON-NLS-1$
		assertNull("First description should be null", targetDescriptions[0]); //$NON-NLS-1$
		assertEquals("Second description should be Calls other targets", "Calls other echos", targetDescriptions[1]); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Ensures that target projects are retrieved properly
	 */
	@Test
	public void testTargetProject() throws CoreException {
		String targetProject = getProjectName("TestForEcho.xml", "Test for Echo"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Project name should be Echo Test", "Echo Test", targetProject); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Ensures that target dependencies are retrieved properly
	 */
	@Test
	public void testTargetDependencies() throws CoreException {
		String[] dependencies = getDependencies("TestForEcho.xml", "Test for Echo"); //$NON-NLS-1$ //$NON-NLS-2$
		assertThat(dependencies).as("number of dependencies in Test for Echo").hasSize(1); //$NON-NLS-1$
		assertEquals("First dependency should be init", "init", dependencies[0]); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Runs an Ant build and ensure that the build file location is logged
	 */
	@Test
	public void testRunScript() throws CoreException {
		run("TestForEcho.xml"); //$NON-NLS-1$
		String message = AntTestChecker.getDefault().getMessages().get(0);
		assertTrue("Build file location should be logged as the first message", message != null //$NON-NLS-1$
				&& message.endsWith("AntTests" + File.separator + "buildfiles" + File.separator + "TestForEcho.xml")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
	}
}