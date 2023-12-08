/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromWorkspace;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import org.eclipse.core.internal.localstore.SafeChunkyInputStream;
import org.eclipse.core.internal.localstore.SafeChunkyOutputStream;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

public class LocalStoreRegressionTests {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	/**
	 * 1FU4PJA: ITPCORE:ALL - refreshLocal for new file with depth zero doesn't work
	 */
	@Test
	public void test_1FU4PJA() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("Test");
		createInWorkspace(project);

		/* */
		IFile file = project.getFile("file");
		createInFileSystem(file);
		assertTrue("1.0", !file.exists());
		file.refreshLocal(IResource.DEPTH_ZERO, null);
		assertTrue("1.1", file.exists());
	}

	/**
	 * From: 1FU4TW7: ITPCORE:ALL - Behaviour not specified for refreshLocal when parent doesn't exist
	 */
	@Test
	public void test_1FU4TW7() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("Test");
		createInWorkspace(project);

		IFolder folder = project.getFolder("folder");
		IFile file = folder.getFile("file");
		createInFileSystem(folder);
		createInFileSystem(file);
		file.refreshLocal(IResource.DEPTH_INFINITE, null);
		assertTrue("1.1", folder.exists());
		assertTrue("1.2", file.exists());
		removeFromWorkspace(folder);
		removeFromFileSystem(folder);
	}

	/**
	 * The PR reported a problem with longs, but we are testing more types here.
	 */
	@Test
	public void test_1G65KR1() throws IOException {
		/* evaluate test environment */
		IPath root = getWorkspace().getRoot().getLocation().append("" + new Date().getTime());
		workspaceRule.deleteOnTearDown(root);
		File temp = root.toFile();
		temp.mkdirs();

		File target = new File(temp, "target");
		Workspace.clear(target); // make sure there was nothing here before
		assertTrue("1.0", !target.exists());

		// write chunks
		try (SafeChunkyOutputStream output = new SafeChunkyOutputStream(target);
				DataOutputStream dos = new DataOutputStream(output)) {
			dos.writeLong(1234567890l);
			output.succeed();
		}

		// read chunks
		try (SafeChunkyInputStream input = new SafeChunkyInputStream(target);
				DataInputStream dis = new DataInputStream(input)) {
				assertEquals("3.0", dis.readLong(), 1234567890l);
		}
	}

}
