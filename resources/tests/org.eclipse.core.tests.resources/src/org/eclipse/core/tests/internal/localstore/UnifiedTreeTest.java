/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.internal.localstore;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Hashtable;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.localstore.IUnifiedTreeVisitor;
import org.eclipse.core.internal.localstore.UnifiedTree;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

public class UnifiedTreeTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private static final int LIMIT = 10;

	protected void createFiles(IFileStore folder, Hashtable<String, String> set) throws Exception {
		for (int i = 0; i < LIMIT; i++) {
			IFileStore child = folder.getChild("fsFile" + i);
			try (OutputStream out = child.openOutputStream(EFS.NONE, null)) {
				out.write("contents".getBytes());
			}
			set.put(child.toString(), "");
		}
	}

	protected void createFiles(final IContainer target, final Hashtable<String, String> set) throws CoreException {
		final Workspace workspace = (Workspace) getWorkspace();
		IWorkspaceRunnable operation = monitor -> {
			for (int i = 0; i < LIMIT; i++) {
				IFile child = target.getFile(IPath.fromOSString("wbFile" + i));
				workspace.createResource(child, false);
				String location = child.getLocation().toOSString();
				set.put(location, "");
			}
		};
		workspace.run(operation, null);
	}

	protected void createResourcesInFileSystem(IFileStore folder, Hashtable<String, String> set) throws Exception {
		createFiles(folder, set);
		for (int i = 0; i < LIMIT; i++) {
			IFileStore child = folder.getChild("fsFolder" + i);
			child.mkdir(EFS.NONE, null);
			set.put(child.toString(), "");
			if (i < (LIMIT / 2)) {
				createFiles(child, set);
			}
		}
	}

	protected void createResourcesInWorkspace(IContainer target, Hashtable<String, String> set) throws CoreException {
		createFiles(target, set);
		for (int i = 0; i < LIMIT; i++) {
			IFolder child = target.getFolder(IPath.fromOSString("wbFolder" + i));
			child.create(true, true, null);
			String location = child.getLocation().toOSString();
			set.put(location, "");
			if (i < (LIMIT / 2)) {
				createFiles(child, set);
			}
		}
	}

	/**
	 * Creates some resources in the file system and some in the workspace. After that,
	 * makes sure the visitor is going to walk through all of them.
	 */
	@Test
	public void testTraverseMechanismInFolder() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);
		IFolder folder = project.getFolder("root");
		folder.create(true, true, null);

		/* Create a hash table to hold all resources the tree should visit.
		 The resources are going to be removed from the hash table as
		 the visitor visits it. */
		final Hashtable<String, String> set = new Hashtable<>();

		/* create some workspace structure */
		createResourcesInWorkspace(folder, set);

		/* create some file system structure */
		createResourcesInFileSystem(((Resource) folder).getStore(), set);

		/* create a visitor */
		IUnifiedTreeVisitor visitor = node -> {
			/* test the node.getLocalName() method */
			final IResource resource = node.getResource();
			final IFileStore store = ((Resource) resource).getStore();
			if (node.existsInFileSystem()) {
				assertEquals("1.0", store.fetchInfo().getName(), node.getLocalName());
			}
			assertEquals("1.1", store, node.getStore());

			/* remove from the hash table the resource we're visiting */
			set.remove(resource.getLocation().toOSString());
			return true;
		};

		/* instantiate a unified tree and use the visitor */
		UnifiedTree tree = new UnifiedTree(folder);
		tree.accept(visitor);

		/* if the hash table is empty, we walked through all resources */
		assertTrue("2.0", set.isEmpty());
	}

	/**
	 * Creates some resources in the file system and some in the workspace. After that,
	 * makes sure the visitor is going to walk through some of them.
	 */
	@Test
	public void testTraverseMechanismInFolderSkippingSomeChildren() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);
		IFolder folder = project.getFolder("root");
		folder.create(true, true, null);

		/* Create a hash table to hold all resources the tree should visit.
		 The resources are going to be removed from the hash table as
		 the visitor visits it. */
		final Hashtable<String, String> set = new Hashtable<>();

		/* create some workspace structure */
		createResourcesInWorkspace(folder, set);

		/* create some file system structure */
		createResourcesInFileSystem(((Resource) folder).getStore(), set);

		/* create a visitor */
		IUnifiedTreeVisitor visitor = node -> {

			/* test the node.getLocalName() method */
			final IResource resource = node.getResource();
			IFileStore store = ((Resource) resource).getStore();
			String key = store.fetchInfo().getName();
			if (node.existsInFileSystem()) {
				assertEquals(key, node.getLocalName());
			}
			assertEquals(store, node.getStore());

			/* force children to be added to the queue */
			node.getChildren();

			/* skip some resources */
			if (resource.getName().startsWith("fsFolder")) {
				return false;
			}

			/* remove from the hash table the resource we're visiting */
			set.remove(resource.getLocation().toOSString());
			return true;
		};

		/**/
		int initialSize = set.size();

		/* instantiate a unified tree and use the visitor */
		UnifiedTree tree = new UnifiedTree(folder);
		tree.accept(visitor);

		/* if the hash table is empty, we walked through all resources */
		assertFalse(set.isEmpty());
		assertTrue(set.size() != initialSize);
	}

	/**
	 * Creates some resources in the file system and some in the workspace. After that,
	 * makes sure the visitor is going to walk through all of them.
	 */
	@Test
	public void testTraverseMechanismInProject() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);

		/* Create a hash table to hold all resources the tree should visit.
		 The resources are going to be removed from the hash table as
		 the visitor visits it. */
		final Hashtable<String, String> set = new Hashtable<>();

		/* create some workspace structure */
		createResourcesInWorkspace(project, set);

		/* create some file system structure */
		createResourcesInFileSystem(((Resource) project).getStore(), set);

		/* create a visitor */
		IUnifiedTreeVisitor visitor = node -> {
			/* test the node.getLocalName() method */
			final IResource resource = node.getResource();
			IFileStore store = ((Resource) resource).getStore();
			if (node.existsInFileSystem()) {
				assertEquals(store.fetchInfo().getName(), node.getLocalName());
			}
			assertEquals(store, node.getStore());
			/* remove from the hash table the resource we're visiting */
			set.remove(resource.getLocation().toOSString());
			return true;
		};

		/* instantiate a unified tree and use the visitor */
		UnifiedTree tree = new UnifiedTree(project);
		tree.accept(visitor);

		/* if the hash table is empty, we walked through all resources */
		assertTrue(set.isEmpty());
	}

	/**
	 * Regression test for 342968 - Resource layers asks IFileTree for info of linked resources
	 */
	@Test
	public void test342968() throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("test");
		createInWorkspace(project);
		project.open(createTestMonitor());

		IProjectDescription description = project.getDescription();
		URI projectLocation = Test342968FileSystem.getTestUriFor(EFS.getLocalFileSystem().fromLocalFile(new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(), "test")).toURI());
		description.setLocationURI(projectLocation);

		project.delete(false, false, null);

		project.create(description, IResource.NONE, null);
		project.open(createTestMonitor());

		assertTrue(project.getLocationURI().equals(projectLocation));

		IFolder link = project.getFolder("link");

		File file = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(), "link");
		file.mkdir();

		IFileStore fileStore = EFS.getLocalFileSystem().fromLocalFile(file);
		link.createLink(fileStore.toURI(), IResource.NONE, null);
		workspaceRule.deleteOnTearDown(fileStore);

		IFile rf = link.getFile("fileTest342968.txt");
		rf.create(new ByteArrayInputStream("test342968".getBytes()), false, null);
		assertTrue("1.0", rf.exists());

		project.refreshLocal(IResource.DEPTH_INFINITE, null);

		assertTrue("2.0", rf.exists());
	}

	@Test
	public void test368376() throws CoreException, IOException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(createUniqueString());
		createInWorkspace(project);

		String filePath = "a/b/c/file.txt";
		File javaFile = new File(project.getLocation().toFile(), filePath);
		assertTrue(javaFile.getParentFile().mkdirs());
		assertTrue(javaFile.createNewFile());

		IFolder folder = project.getFolder("a");
		IFile file = project.getFile(filePath);
		assertFalse(folder.exists());
		assertFalse(file.exists());

		file.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		assertTrue(folder.exists());
		assertTrue(file.exists());
		assertTrue(folder.isSynchronized(IResource.DEPTH_INFINITE));

		project.delete(true, createTestMonitor());
	}
}
