/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.internal.builders.DeltaVerifierBuilder;
import org.eclipse.core.tests.resources.saveparticipant1.SaveParticipant1Plugin;
import org.eclipse.core.tests.resources.saveparticipant2.SaveParticipant2Plugin;
import org.eclipse.core.tests.resources.saveparticipant3.SaveParticipant3Plugin;
import org.osgi.framework.Bundle;

/**
 * @see SaveManager1Test
 * @see SaveManager2Test
 */
public class SaveManager3Test extends SaveManagerTest {

	public void testBuilder() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		assertTrue(project.isAccessible());

		setAutoBuilding(false);
		touch(project);
		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
		setAutoBuilding(true);
		waitForBuild();
		DeltaVerifierBuilder verifier = DeltaVerifierBuilder.getInstance();
		assertTrue(verifier.wasIncrementalBuild());

		IFile added = project.getFile("added file");
		verifier.addExpectedChange(added, project, IResourceDelta.ADDED, 0);
		added.create(createRandomContentsStream(), true, null);
		waitForBuild();
		assertTrue(verifier.wasAutoBuild());
		assertTrue(verifier.isDeltaValid());
		// remove the file because we don't want it to affect any other delta in the test
		added.delete(true, false, null);
	}

	public void testSaveParticipant() throws Exception {
		// SaveParticipant1Plugin
		Bundle bundle = Platform.getBundle(PI_SAVE_PARTICIPANT_1);
		assertNotNull(bundle);
		bundle.start();
		SaveParticipant1Plugin plugin1 = SaveParticipant1Plugin.getInstance();

		// check saved state and delta
		plugin1.resetDeltaVerifier();
		IStatus status;
		status = plugin1.registerAsSaveParticipant();
		assertTrue(status.isOK(), "Registering save participant failed with message: " + status.getMessage());

		// SaveParticipant2Plugin
		bundle = Platform.getBundle(PI_SAVE_PARTICIPANT_2);
		assertNotNull(bundle);
		bundle.start();
		SaveParticipant2Plugin plugin2 = SaveParticipant2Plugin.getInstance();

		// check saved state and delta
		plugin2.resetDeltaVerifier();
		// MyProject changes
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		IResource file = project.getFile("addedFile");
		plugin2.addExpectedChange(file, IResourceDelta.ADDED, 0);

		// Project2 changes
		project = getWorkspace().getRoot().getProject(PROJECT_2);
		plugin2.addExpectedChange(project, IResourceDelta.CHANGED, IResourceDelta.OPEN);
		file = project.getFile("addedFile");
		plugin2.addExpectedChange(file, project, IResourceDelta.ADDED, 0);
		IResource prjFile = project.getFile(".project");
		plugin2.addExpectedChange(prjFile, IResourceDelta.ADDED, 0);
		plugin2.addExpectedChange(project.getFolder(".settings"), IResourceDelta.ADDED, 0);
		plugin2.addExpectedChange(project.getFolder(".settings").getFile("org.eclipse.core.resources.prefs"), IResourceDelta.ADDED, 0);
		IResource[] resources = buildResources(project, defineHierarchy(PROJECT_2));
		plugin2.addExpectedChange(resources, IResourceDelta.ADDED, 0);
		//
		status = plugin2.registerAsSaveParticipant();
		assertTrue(status.isOK(), "Status is not okay with message: " + status.getMessage());

		// SaveParticipant3Plugin
		bundle = Platform.getBundle(PI_SAVE_PARTICIPANT_3);
		assertNotNull(bundle);
		bundle.start();
		SaveParticipant3Plugin plugin3 = SaveParticipant3Plugin.getInstance();

		status = plugin3.registerAsSaveParticipant();
		assertTrue(status.isOK(), "Registering save participant failed with message: " + status.getMessage());
	}

}
