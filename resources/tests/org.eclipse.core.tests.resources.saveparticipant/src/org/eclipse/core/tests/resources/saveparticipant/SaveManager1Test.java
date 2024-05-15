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
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.internal.builders.DeltaVerifierBuilder;
import org.eclipse.core.tests.resources.regression.SimpleBuilder;
import org.eclipse.core.tests.resources.saveparticipant1.SaveParticipant1Plugin;
import org.eclipse.core.tests.resources.saveparticipant2.SaveParticipant2Plugin;
import org.eclipse.core.tests.resources.saveparticipant3.SaveParticipant3Plugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * This class needs to be used with SaveManager2Test. Basically this
 * one builds up an environment in a platform session and the other,
 * running in another session, makes sure the environment is safelly
 * restored.
 *
 * @see SaveManager2Test
 * @see SaveManager3Test
 */
public class SaveManager1Test {

	/**
	 * Create some resources and save the workspace.
	 */
	public void testCreateMyProject() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(SaveManagerTest.PROJECT_1);
		project.create(null);
		project.open(null);
		assertTrue(project.exists());
		assertTrue(project.isOpen());

		project.close(null);
		assertTrue(project.exists());
		assertFalse(project.isOpen());

		// when closing and opening the project again, it should still exist
		project = getWorkspace().getRoot().getProject(SaveManagerTest.PROJECT_1);
		project.open(null);
		assertTrue(project.exists());
		assertTrue(project.isOpen());

		// create some children
		IResource[] resources = buildResources(project, SaveManagerTest.defineHierarchy(SaveManagerTest.PROJECT_1));
		createInWorkspace(resources);
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);

		project.close(null);
		project.open(null);
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);
	}

	/**
	 * Create another project and leave it closed for next session.
	 */
	public void testCreateProject2() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(SaveManagerTest.PROJECT_2);
		project.create(null);
		project.open(null);
		assertTrue(project.exists());
		assertTrue(project.isOpen());

		// create some children
		IResource[] resources = buildResources(project, SaveManagerTest.defineHierarchy(SaveManagerTest.PROJECT_2));
		createInWorkspace(resources);
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);

		// add a builder to this project
		IProjectDescription description = project.getDescription();
		ICommand command = description.newCommand();
		command.setBuilderName(SimpleBuilder.BUILDER_ID);
		description.setBuildSpec(new ICommand[] { command });
		project.setDescription(description, null);
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);

		project.close(null);
		assertTrue(project.exists());
		assertFalse(project.isOpen());
	}

	public void testAddSaveParticipant() throws Exception {
		// get plugin
		Bundle bundle = Platform.getBundle(SaveManagerTest.PI_SAVE_PARTICIPANT_1);
		assertNotNull(bundle);
		bundle.start();
		SaveParticipant1Plugin plugin1 = SaveParticipant1Plugin.getInstance();

		//	prepare plugin to the save operation
		plugin1.resetDeltaVerifier();
		IStatus status;
		status = plugin1.registerAsSaveParticipant();
		assertTrue(status.isOK(), "Registering save participant failed with message: " + status.getMessage());
		plugin1.setExpectedSaveKind(ISaveContext.FULL_SAVE);

		// SaveParticipant2Plugin
		bundle = Platform.getBundle(SaveManagerTest.PI_SAVE_PARTICIPANT_2);
		assertNotNull(bundle);
		bundle.start();
		SaveParticipant2Plugin plugin2 = SaveParticipant2Plugin.getInstance();

		//	prepare plugin to the save operation
		plugin2.getDeltaVerifier().reset();
		status = plugin2.registerAsSaveParticipant();
		assertTrue(status.isOK(), "Registering save participant failed with message: " + status.getMessage());
		plugin1.setExpectedSaveKind(ISaveContext.FULL_SAVE);

		// SaveParticipant3Plugin
		bundle = Platform.getBundle(SaveManagerTest.PI_SAVE_PARTICIPANT_3);
		assertNotNull(bundle);
		bundle.start();
		SaveParticipant3Plugin plugin3 = SaveParticipant3Plugin.getInstance();

		status = plugin3.registerAsSaveParticipant();
		assertTrue(status.isOK(), "Registering save participant failed with message: " + status.getMessage());
	}

	public void testBuilder() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(SaveManagerTest.PROJECT_1);
		assertTrue(project.isAccessible());

		setAutoBuilding(true);
		// Create and set a build spec for the project
		IProjectDescription description = project.getDescription();
		ICommand command = description.newCommand();
		command.setBuilderName(DeltaVerifierBuilder.BUILDER_NAME);
		description.setBuildSpec(new ICommand[] {command});
		project.setDescription(description, null);
		project.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		// close and open the project and see if the builder gets a good delta
		project.close(null);
		project.open(null);
		IFile added = project.getFile("added file");
		waitForBuild();
		DeltaVerifierBuilder verifier = DeltaVerifierBuilder.getInstance();
		verifier.reset();
		verifier.addExpectedChange(added, project, IResourceDelta.ADDED, 0);
		added.create(createRandomContentsStream(), true, null);
		waitForBuild();
		assertTrue(verifier.wasAutoBuild());
		assertTrue(verifier.isDeltaValid());
		// remove the file because we don't want it to affect any other delta in the test
		added.delete(true, false, null);
	}

	public void testPostSave() throws BundleException {
		// get plugin
		Bundle bundle = Platform.getBundle(SaveManagerTest.PI_SAVE_PARTICIPANT_1);
		assertNotNull(bundle);
		bundle.start();
		SaveParticipant1Plugin plugin = SaveParticipant1Plugin.getInstance();

		// look at the plugin save lifecycle
		IStatus status = plugin.getSaveLifecycleLog();
		assertTrue(status.isOK(), "Getting lifecycle log failed with message: " + status.getMessage());
	}

}
