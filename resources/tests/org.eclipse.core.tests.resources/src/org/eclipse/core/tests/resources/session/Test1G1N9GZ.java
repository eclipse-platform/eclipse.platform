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

import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.updateProjectDescription;

import java.io.ByteArrayInputStream;
import junit.framework.Test;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.internal.builders.SortBuilder;
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
		createInWorkspace(p1);
		updateProjectDescription(p1).addingCommand(SortBuilder.BUILDER_NAME).withTestBuilderId("P1Build1").apply();

		/* create P2 and set a builder */
		IProject p2 = workspace.getRoot().getProject("p2");
		createInWorkspace(p2);
		updateProjectDescription(p2).addingCommand(SortBuilder.BUILDER_NAME).withTestBuilderId("P2Build1").apply();

		/* PR test case */
		workspace.save(true, createTestMonitor());
	}

	public void test2() throws CoreException {
		workspace.save(true, createTestMonitor());
	}

	public void test3() throws Exception {
		/* get new handles */
		IProject p1 = workspace.getRoot().getProject("p1");
		IProject p2 = workspace.getRoot().getProject("p2");

		/* try to create other files */
		try (ByteArrayInputStream source = new ByteArrayInputStream("file's content".getBytes())) {
			p1.getFile("file2").create(source, true, null);
		}
		try (ByteArrayInputStream source = new ByteArrayInputStream("file's content".getBytes())) {
			p2.getFile("file2").create(source, true, null);
		}

	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, Test1G1N9GZ.class);
	}

}
