/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.localstore;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

public abstract class LocalStoreTest extends ResourceTest {
	// test configuration attributes
	protected static int numberOfProjects = 3;
	protected static int numberOfProperties = 5;

	// projects names and projects
	protected String[] projectNames;
	protected IProject[] projects;

	public LocalStoreTest() {
		super();
	}

	public LocalStoreTest(String name) {
		super(name);
	}

	/**
	 * Create a file with random content. If a resource exists in the same path, the
	 * resource is deleted.
	 */
	protected void createFile(IFileStore target, String content) throws CoreException, IOException {
		target.delete(EFS.NONE, null);
		InputStream input = new ByteArrayInputStream(content.getBytes());
		try (OutputStream output = target.openOutputStream(EFS.NONE, null)) {
			input.transferTo(output);
		}
		IFileInfo info = target.fetchInfo();
		assertTrue(info.exists() && !info.isDirectory());
	}

	protected void createNode(IFileStore node) throws CoreException, IOException {
		char type = node.getName().charAt(0);
		if (type == 'd') {
			node.mkdir(EFS.NONE, null);
		} else {
			InputStream input = createRandomContentsStream();
			try (OutputStream output = node.openOutputStream(EFS.NONE, null)) {
				input.transferTo(output);
			}
		}
	}

	protected void createTree(IFileStore[] tree) throws CoreException, IOException {
		for (IFileStore element : tree) {
			createNode(element);
		}
	}

	protected IFileStore[] getTree(IFileStore root) {
		return getTree(root, getTreeElements());
	}

	protected IFileStore[] getTree(IFileStore root, String[] elements) {
		IFileStore[] tree = new IFileStore[elements.length];
		for (int i = 0; i < elements.length; i++) {
			tree[i] = root.getChild(elements[i]);
		}
		return tree;
	}

	protected String[] getTreeElements() {
		String[] tree = new String[10];
		tree[0] = "d-folder";
		tree[1] = tree[0] + File.separator + "d-subfolder";
		tree[2] = tree[0] + File.separator + "f-file";
		tree[3] = tree[1] + File.separator + "f-anotherFile";
		tree[4] = tree[1] + File.separator + "d-subfolder";
		tree[5] = "d-1";
		tree[6] = "d-2";
		tree[7] = "f-3";
		tree[8] = "f-4";
		tree[9] = "f-5";
		return tree;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		projectNames = new String[numberOfProjects];
		projects = new IProject[numberOfProjects];
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			for (int i = 0; i < projectNames.length; i++) {
				projectNames[i] = "Project" + i;
				projects[i] = getWorkspace().getRoot().getProject(projectNames[i]);
				projects[i].create(null);
				projects[i].open(null);
				deleteOnTearDown(projects[i].getLocation());
			}
		}, null);
	}

}
