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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests closing a workspace without save.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestCloseNoSave {
	private static final String PROJECT = "Project";
	private static final String FOLDER = "Folder";
	private static final String FILE = "File";

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	@Test
	@Order(1)
	public void test1() throws CoreException {
		/* create some resource handles */
		IProject project = getWorkspace().getRoot().getProject(PROJECT);
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		IFolder folder = project.getFolder(FOLDER);
		folder.create(true, true, createTestMonitor());
		IFile file = folder.getFile(FILE);
		file.create(createRandomContentsStream(), true, createTestMonitor());
	}

	@Test
	@Order(2)
	public void test2() throws CoreException {
		// projects should exist immediately due to snapshot - files may or
		// may not exist due to snapshot timing. All resources should exist after refresh.
		IResource[] members = getWorkspace().getRoot().members();
		assertThat(members).hasSize(1).allSatisfy(member -> assertThat(member.getType()).isEqualTo(IResource.PROJECT));
		IProject project = (IProject) members[0];
		assertTrue(project.exists());
		IFolder folder = project.getFolder(FOLDER);
		IFile file = folder.getFile(FILE);

		//opening the project does an automatic local refresh
		if (!project.isOpen()) {
			project.open(null);
		}

		assertThat(project.members()).hasSize(3);
		assertTrue(folder.exists());
		assertTrue(file.exists());
	}

}
