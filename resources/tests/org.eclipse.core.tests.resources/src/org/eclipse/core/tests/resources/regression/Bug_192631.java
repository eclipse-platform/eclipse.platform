/*******************************************************************************
 * Copyright (c) 2012, 2026 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.regression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryTree;
import org.eclipse.core.tests.internal.filesystem.remote.RemoteFileSystem;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for bug 192631
 */
@ExtendWith(WorkspaceResetExtension.class)
public class Bug_192631 {

	private static final String USER_A = "userA";
	private static final String USER_B = "userB";
	private static final String HOST_A = "hostA.example.com";
	private static final String HOST_B = "hostB.example.com";
	private static final int PORT_A = 1111;
	private static final int PORT_B = 2222;

	private static final String COMMON = "/common";
	private static final String FOLDER_A = "/common/folderA";
	private static final String FOLDER_B = "/common/folderB";

	@BeforeEach
	public void setUp() {
		MemoryTree.TREE.deleteAll();
	}

	@AfterEach
	public void tearDown() {
		MemoryTree.TREE.deleteAll();
	}

	@Test
	public void testCompareHost() throws CoreException, URISyntaxException {
		URI commonA = new URI(RemoteFileSystem.SCHEME_REMOTE, null, HOST_A, -1, COMMON, null, null);
		URI commonB = new URI(RemoteFileSystem.SCHEME_REMOTE, null, HOST_B, -1, COMMON, null, null);
		URI folderA = new URI(RemoteFileSystem.SCHEME_REMOTE, null, HOST_A, -1, FOLDER_A, null, null);
		URI folderB = new URI(RemoteFileSystem.SCHEME_REMOTE, null, HOST_B, -1, FOLDER_B, null, null);

		final Set<URI> toVisit = new HashSet<>();
		final int toVisitCount[] = new int[] {0};
		IResourceVisitor visitor = resource -> {
			toVisit.remove(resource.getLocationURI());
			toVisitCount[0]--;
			return true;
		};

		EFS.getStore(folderA).mkdir(EFS.NONE, null);
		EFS.getStore(folderB).mkdir(EFS.NONE, null);

		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		IProject projectA = workspace.getRoot().getProject("projectA");
		createInWorkspace(projectA);
		IFolder linkA = projectA.getFolder("link_to_commonA");
		linkA.createLink(commonA, IResource.NONE, createTestMonitor());

		IProject projectB = workspace.getRoot().getProject("projectB");
		createInWorkspace(projectB);
		IFolder linkB = projectB.getFolder("link_to_commonB");
		linkB.createLink(commonB, IResource.NONE, createTestMonitor());

		toVisit.addAll(Arrays.asList(projectA.getLocationURI(), commonA, folderA, projectA.getFile(".project").getLocationURI()));
		toVisitCount[0] = 6;
		projectA.accept(visitor);
		assertThat(toVisit).isEmpty();
		assertEquals(0, toVisitCount[0]);

		toVisit.addAll(Arrays.asList(projectB.getLocationURI(), commonB, folderB, projectB.getFile(".project").getLocationURI()));
		toVisitCount[0] = 6;
		projectB.accept(visitor);
		assertThat(toVisit).isEmpty();
		assertEquals(0, toVisitCount[0]);

		projectA.delete(true, createTestMonitor());
		projectB.delete(true, createTestMonitor());
	}

	@Test
	public void testCompareUserInfo() throws CoreException, URISyntaxException {
		URI commonA = new URI(RemoteFileSystem.SCHEME_REMOTE, USER_A, HOST_A, -1, COMMON, null, null);
		URI commonB = new URI(RemoteFileSystem.SCHEME_REMOTE, USER_B, HOST_A, -1, COMMON, null, null);
		URI folderA = new URI(RemoteFileSystem.SCHEME_REMOTE, USER_A, HOST_A, -1, FOLDER_A, null, null);
		URI folderB = new URI(RemoteFileSystem.SCHEME_REMOTE, USER_B, HOST_A, -1, FOLDER_B, null, null);

		final Set<URI> toVisit = new HashSet<>();
		final int toVisitCount[] = new int[] {0};
		IResourceVisitor visitor = resource -> {
			toVisit.remove(resource.getLocationURI());
			toVisitCount[0]--;
			return true;
		};

		EFS.getStore(folderA).mkdir(EFS.NONE, null);
		EFS.getStore(folderB).mkdir(EFS.NONE, null);

		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		IProject projectA = workspace.getRoot().getProject("projectA");
		createInWorkspace(projectA);
		IFolder linkA = projectA.getFolder("link_to_commonA");
		linkA.createLink(commonA, IResource.NONE, createTestMonitor());

		IProject projectB = workspace.getRoot().getProject("projectB");
		createInWorkspace(projectB);
		IFolder linkB = projectB.getFolder("link_to_commonB");
		linkB.createLink(commonB, IResource.NONE, createTestMonitor());

		toVisit.addAll(Arrays.asList(projectA.getLocationURI(), commonA, folderA, projectA.getFile(".project").getLocationURI()));
		toVisitCount[0] = 6;
		projectA.accept(visitor);
		assertThat(toVisit).isEmpty();
		assertEquals(0, toVisitCount[0]);

		toVisit.addAll(Arrays.asList(projectB.getLocationURI(), commonB, folderB, projectB.getFile(".project").getLocationURI()));
		toVisitCount[0] = 6;
		projectB.accept(visitor);
		assertThat(toVisit).isEmpty();
		assertEquals(0, toVisitCount[0]);

		projectA.delete(true, createTestMonitor());
		projectB.delete(true, createTestMonitor());
	}

	@Test
	public void testComparePort() throws CoreException, URISyntaxException {
		URI commonA = new URI(RemoteFileSystem.SCHEME_REMOTE, null, HOST_A, PORT_A, COMMON, null, null);
		URI commonB = new URI(RemoteFileSystem.SCHEME_REMOTE, null, HOST_A, PORT_B, COMMON, null, null);
		URI folderA = new URI(RemoteFileSystem.SCHEME_REMOTE, null, HOST_A, PORT_A, FOLDER_A, null, null);
		URI folderB = new URI(RemoteFileSystem.SCHEME_REMOTE, null, HOST_A, PORT_B, FOLDER_B, null, null);

		final Set<URI> toVisit = new HashSet<>();
		final int toVisitCount[] = new int[] {0};
		IResourceVisitor visitor = resource -> {
			toVisit.remove(resource.getLocationURI());
			toVisitCount[0]--;
			return true;
		};

		EFS.getStore(folderA).mkdir(EFS.NONE, null);
		EFS.getStore(folderB).mkdir(EFS.NONE, null);

		IWorkspace workspace = ResourcesPlugin.getWorkspace();

		IProject projectA = workspace.getRoot().getProject("projectA");
		createInWorkspace(projectA);
		IFolder linkA = projectA.getFolder("link_to_commonA");
		linkA.createLink(commonA, IResource.NONE, createTestMonitor());

		IProject projectB = workspace.getRoot().getProject("projectB");
		createInWorkspace(projectB);
		IFolder linkB = projectB.getFolder("link_to_commonB");
		linkB.createLink(commonB, IResource.NONE, createTestMonitor());

		toVisit.addAll(Arrays.asList(projectA.getLocationURI(), commonA, folderA, projectA.getFile(".project").getLocationURI()));
		toVisitCount[0] = 6;
		projectA.accept(visitor);
		assertThat(toVisit).isEmpty();
		assertEquals(0, toVisitCount[0]);

		toVisit.addAll(Arrays.asList(projectB.getLocationURI(), commonB, folderB, projectB.getFile(".project").getLocationURI()));
		toVisitCount[0] = 6;
		projectB.accept(visitor);
		assertThat(toVisit).isEmpty();
		assertEquals(0, toVisitCount[0]);

		projectA.delete(true, createTestMonitor());
		projectB.delete(true, createTestMonitor());
	}

}
