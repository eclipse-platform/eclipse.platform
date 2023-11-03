/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.saveparticipant;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.internal.builders.DeltaVerifierBuilder;
import org.eclipse.core.tests.resources.regression.SimpleBuilder;
import org.eclipse.core.tests.resources.saveparticipant1.SaveParticipant1Plugin;
import org.eclipse.core.tests.resources.saveparticipant3.SaveParticipant3Plugin;
import org.osgi.framework.Bundle;

import junit.framework.Test;
import junit.framework.TestSuite;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInFileSystem;

/**
 * @see SaveManager1Test
 * @see SaveManager3Test
 */
public class SaveManager2Test extends SaveManagerTest {
	/**
	 * Need a zero argument constructor to satisfy the test harness.
	 * This constructor should not do any real work nor should it be
	 * called by user code.
	 */
	public SaveManager2Test() {
	}

	public SaveManager2Test(String name) {
		super(name);
	}

	public static Test suite() {
		// we do not add the whole class because the order is important
		TestSuite suite = new TestSuite();
		suite.addTest(new SaveManager2Test("testVerifyRestoredWorkspace"));
		suite.addTest(new SaveManager2Test("testBuilder"));
		suite.addTest(new SaveManager2Test("testSaveParticipant"));
		suite.addTest(new SaveManager2Test("testVerifyProject2"));
		suite.addTest(new SaveManager1Test("testSaveWorkspace"));
		return suite;
	}

	public void testBuilder() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		assertTrue("0.0", project.isAccessible());

		IFile added = project.getFile("added file");
		waitForBuild();
		DeltaVerifierBuilder verifier = DeltaVerifierBuilder.getInstance();
		verifier.reset();
		verifier.addExpectedChange(added, project, IResourceDelta.ADDED, 0);
		added.create(getRandomContents(), true, null);
		waitForBuild();
		assertTrue("3.2", verifier.wasAutoBuild());
		assertTrue("3.3", verifier.isDeltaValid());
		// remove the file because we don't want it to affect any other delta in the test
		added.delete(true, false, null);
	}

	public void testSaveParticipant() throws Exception {
		// get plugin
		Bundle bundle = Platform.getBundle(PI_SAVE_PARTICIPANT_1);
		assertTrue("0.1", bundle != null);
		bundle.start();
		SaveParticipant1Plugin plugin1 = SaveParticipant1Plugin.getInstance();

		// prepare plugin to the save operation
		plugin1.resetDeltaVerifier();
		IResource added1 = getWorkspace().getRoot().getFile(IPath.fromOSString(PROJECT_1).append("addedFile"));
		plugin1.addExpectedChange(added1, IResourceDelta.ADDED, 0);
		IStatus status;
		status = plugin1.registerAsSaveParticipant();
		assertTrue("Registering save participant failed with message: " + status.getMessage(), status.isOK());

		// SaveParticipant3Plugin
		bundle = Platform.getBundle(PI_SAVE_PARTICIPANT_3);
		assertTrue("7.1", bundle != null);
		bundle.start();
		SaveParticipant3Plugin plugin3 = SaveParticipant3Plugin.getInstance();

		status = plugin3.registerAsSaveParticipant();
		assertTrue("Registering save participant failed with message: " + status.getMessage(), status.isOK());
	}

	public void testVerifyProject2() throws CoreException {
		// project2 should be closed
		IProject project = getWorkspace().getRoot().getProject(PROJECT_2);
		assertTrue("0.0", project.exists());
		assertTrue("0.1", !project.isOpen());

		// verify its children
		IResource[] resources = buildResources(project, defineHierarchy(PROJECT_2));
		assertExistsInFileSystem(resources);
		assertDoesNotExistInWorkspace(resources);

		project.open(null);
		assertTrue("2.1", project.exists());
		assertTrue("2.2", project.isOpen());
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);

		// verify builder -- cause an incremental build
		touch(project);
		waitForBuild();
		SimpleBuilder builder = SimpleBuilder.getInstance();
		assertTrue("2.6", builder.wasAutoBuild());

		// add a file to test save participant delta
		IFile file = project.getFile("addedFile");
		file.create(getRandomContents(), true, null);
	}

	public void testVerifyRestoredWorkspace() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		assertTrue("0.0", project.exists());
		assertTrue("0.1", project.isOpen());

		// verify children still exist
		IResource[] resources = buildResources(project, defineHierarchy(PROJECT_1));
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);

		// add a file to test save participant delta
		IFile file = project.getFile("addedFile");
		file.create(getRandomContents(), true, null);
	}
}
