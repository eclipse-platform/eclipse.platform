/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.session;

import java.io.ByteArrayInputStream;
import junit.framework.Test;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.internal.builders.SortBuilder;
import org.eclipse.core.tests.internal.builders.TestBuilder;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Regression test for 1G1N9GZ: ITPCORE:WIN2000 - ElementTree corruption when linking trees
 */
public class Test1G1N9GZ extends WorkspaceSerializationTest {

	/**
	 * Initial setup and save
	 */
	public void test1() throws CoreException {
		/* create P1 and set a builder */
		IProject p1 = workspace.getRoot().getProject("p1");
		p1.create(null);
		p1.open(null);
		IProjectDescription desc = p1.getDescription();
		ICommand command = desc.newCommand();
		command.setBuilderName(SortBuilder.BUILDER_NAME);
		command.getArguments().put(TestBuilder.BUILD_ID, "P1Build1");
		desc.setBuildSpec(new ICommand[] {command});
		p1.setDescription(desc, getMonitor());

		/* create P2 and set a builder */
		IProject p2 = workspace.getRoot().getProject("p2");
		p2.create(null);
		p2.open(null);
		desc = p1.getDescription();
		command = desc.newCommand();
		command.setBuilderName(SortBuilder.BUILDER_NAME);
		command.getArguments().put(TestBuilder.BUILD_ID, "P2Build1");
		desc.setBuildSpec(new ICommand[] {command});
		p1.setDescription(desc, getMonitor());

		/* PR test case */
		workspace.save(true, getMonitor());
	}

	public void test2() throws CoreException {
		workspace.save(true, getMonitor());
	}

	public void test3() {
		/* get new handles */
		IProject p1 = workspace.getRoot().getProject("p1");
		IProject p2 = workspace.getRoot().getProject("p2");

		/* try to create other files */
		try {
			ByteArrayInputStream source = new ByteArrayInputStream("file's content".getBytes());
			p1.getFile("file2").create(source, true, null);
		} catch (Exception e) {
			fail("1.0", e);
		}
		try {
			ByteArrayInputStream source = new ByteArrayInputStream("file's content".getBytes());
			p2.getFile("file2").create(source, true, null);
		} catch (Exception e) {
			fail("1.1", e);
		}
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, Test1G1N9GZ.class);
	}

}
