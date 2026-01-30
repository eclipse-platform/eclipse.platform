/*******************************************************************************
 *  Copyright (c) 2000, 2026 IBM Corporation and others.
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
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
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
 * Tests snapshoting, saving, snapshoting, then crash and recover.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestSnapSaveSnap {
	private static final String PROJECT = "Project";
	private static final String FOLDER = "Folder";
	private static final String FILE = "File";

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	@Test
	@Order(1)
	public void test1() throws Exception {
		/* create some resource handles */
		IProject project = getWorkspace().getRoot().getProject(PROJECT);
		IFolder folder = project.getFolder(FOLDER);
		IFile file = folder.getFile(FILE);
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// snapshot
		getWorkspace().save(false, createTestMonitor());

		/* do more stuff */
		folder.create(true, true, createTestMonitor());

		// full save
		getWorkspace().save(true, createTestMonitor());

		/* do even more stuff */
		byte[] bytes = "Test bytes".getBytes();
		try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
			file.create(in, true, createTestMonitor());
		}

		// snapshot
		getWorkspace().save(false, createTestMonitor());
		//exit without saving
	}

	@Test
	@Order(2)
	public void test2() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(PROJECT);
		IFolder folder = project.getFolder(FOLDER);
		IFile file = folder.getFile(FILE);

		/* see if the workspace contains the resources created earlier*/
		IResource[] children = getWorkspace().getRoot().members();
		assertThat(children).containsExactly(project);
		assertTrue(project.exists());
		assertTrue(project.isOpen());

		assertExistsInWorkspace(project, folder, file);
	}

}
