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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.internal.builders.DeltaVerifierBuilder;
import org.eclipse.core.tests.resources.regression.SimpleBuilder;
import org.eclipse.core.tests.resources.saveparticipant1.SaveParticipant1Plugin;
import org.eclipse.core.tests.resources.saveparticipant2.SaveParticipant2Plugin;
import org.eclipse.core.tests.resources.saveparticipant3.SaveParticipant3Plugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;

/**
 * This class needs to be used with SaveManager2Test. Basically this
 * one builds up an environment in a platform session and the other,
 * running in another session, makes sure the environment is safelly
 * restored.
 *
 * @see SaveManager2Test
 * @see SaveManager3Test
 */
public class SaveManager1Test extends SaveManagerTest {
	/**
	 * Need a zero argument constructor to satisfy the test harness.
	 * This constructor should not do any real work nor should it be
	 * called by user code.
	 */
	public SaveManager1Test() {
	}

	public SaveManager1Test(String name) {
		super(name);
	}

	// copy and paste into the scrapbook
	public static void doIt() throws Exception {
		String[] testIds = {"saveparticipant.SaveManager1Test", "saveparticipant.SaveManager2Test", "saveparticipant.SaveManager3Test"};
		for (int i = 0; i < testIds.length; i++) {
			Process p = Runtime.getRuntime().exec(new String[] {"java", "org.eclipse.core.tests.harness.launcher.Main", "-test", testIds[i], "-data", "c:/temp/save_manager", (i < (testIds.length - 1) ? "-nocleanup" : "")});
			p.waitFor();
			java.io.InputStream input = p.getInputStream();
			int c;
			while ((c = input.read()) != -1)
				System.out.print((char) c);
			input.close();
			input = p.getErrorStream();
			while ((c = input.read()) != -1)
				System.out.print((char) c);
			input.close();
		}
		System.exit(-1);
	}

	public static Test suite() {
		// we do not add the whole class because the order is important
		TestSuite suite = new TestSuite();
		suite.addTest(new SaveManager1Test("saveWorkspace"));
		suite.addTest(new SaveManager1Test("testCreateMyProject"));
		suite.addTest(new SaveManager1Test("testCreateProject2"));
		suite.addTest(new SaveManager1Test("testAddSaveParticipant"));
		suite.addTest(new SaveManager1Test("testBuilder"));
		suite.addTest(new SaveManager1Test("saveWorkspace"));
		suite.addTest(new SaveManager1Test("testPostSave"));
		return suite;
	}

	public void testAddSaveParticipant() throws Exception {
		// get plugin
		Bundle bundle = Platform.getBundle(PI_SAVE_PARTICIPANT_1);
		assertTrue("0.1", bundle != null);
		bundle.start();
		SaveParticipant1Plugin plugin1 = SaveParticipant1Plugin.getInstance();

		//	prepare plugin to the save operation
		plugin1.resetDeltaVerifier();
		IStatus status;
		status = plugin1.registerAsSaveParticipant();
		assertTrue("Registering save participant failed with message: " + status.getMessage(), status.isOK());
		plugin1.setExpectedSaveKind(ISaveContext.FULL_SAVE);

		// SaveParticipant2Plugin
		bundle = Platform.getBundle(PI_SAVE_PARTICIPANT_2);
		assertTrue("5.1", bundle != null);
		bundle.start();
		SaveParticipant2Plugin plugin2 = SaveParticipant2Plugin.getInstance();

		//	prepare plugin to the save operation
		plugin2.getDeltaVerifier().reset();
		status = plugin2.registerAsSaveParticipant();
		assertTrue("Registering save participant failed with message: " + status.getMessage(), status.isOK());
		plugin1.setExpectedSaveKind(ISaveContext.FULL_SAVE);

		// SaveParticipant3Plugin
		bundle = Platform.getBundle(PI_SAVE_PARTICIPANT_3);
		assertTrue("7.1", bundle != null);
		bundle.start();
		SaveParticipant3Plugin plugin3 = SaveParticipant3Plugin.getInstance();

		status = plugin3.registerAsSaveParticipant();
		assertTrue("Registering save participant failed with message: " + status.getMessage(), status.isOK());
	}

	/**
	 * Create another project and leave it closed for next session.
	 */
	public void testAnotherProject() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		project.create(null);
		project.open(null);
		assertTrue("0.1", project.exists());
		assertTrue("0.2", project.isOpen());

		project.close(null);
		assertTrue("1.1", project.exists());
		assertTrue("1.2", !project.isOpen());

		// when closing and opening the project again, it should still exist
		project = getWorkspace().getRoot().getProject(PROJECT_1);
		project.open(null);
		assertTrue("2.1", project.exists());
		assertTrue("2.2", project.isOpen());

		// create some children
		IResource[] resources = buildResources(project, defineHierarchy(PROJECT_1));
		ensureExistsInWorkspace(resources);
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);

		project.close(null);
		project.open(null);
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);

		getWorkspace().save(true, null);
	}

	public void testBuilder() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		assertTrue("0.0", project.isAccessible());

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
		added.create(getRandomContents(), true, null);
		waitForBuild();
		assertTrue("3.2", verifier.wasAutoBuild());
		assertTrue("3.3", verifier.isDeltaValid());
		// remove the file because we don't want it to affect any other delta in the test
		added.delete(true, false, null);
	}

	/**
	 * Create some resources and save the workspace.
	 */
	public void testCreateMyProject() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		project.create(null);
		project.open(null);
		assertTrue("0.1", project.exists());
		assertTrue("0.2", project.isOpen());

		project.close(null);
		assertTrue("1.1", project.exists());
		assertTrue("1.2", !project.isOpen());

		// when closing and opening the project again, it should still exist
		project = getWorkspace().getRoot().getProject(PROJECT_1);
		project.open(null);
		assertTrue("2.1", project.exists());
		assertTrue("2.2", project.isOpen());

		// create some children
		IResource[] resources = buildResources(project, defineHierarchy(PROJECT_1));
		ensureExistsInWorkspace(resources);
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
		IProject project = getWorkspace().getRoot().getProject(PROJECT_2);
		project.create(null);
		project.open(null);
		assertTrue("0.1", project.exists());
		assertTrue("0.2", project.isOpen());

		// create some children
		IResource[] resources = buildResources(project, defineHierarchy(PROJECT_2));
		ensureExistsInWorkspace(resources);
		assertExistsInFileSystem(resources);
		assertExistsInWorkspace(resources);

		// add a builder to this project
		IProjectDescription description = project.getDescription();
		ICommand command = description.newCommand();
		command.setBuilderName(SimpleBuilder.BUILDER_ID);
		description.setBuildSpec(new ICommand[] {command});
		project.setDescription(description, null);
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);

		project.close(null);
		assertTrue("5.1", project.exists());
		assertTrue("5.2", !project.isOpen());
	}

	public void testPostSave() throws BundleException {
		// get plugin
		Bundle bundle = Platform.getBundle(PI_SAVE_PARTICIPANT_1);
		assertTrue("0.1", bundle != null);
		bundle.start();
		SaveParticipant1Plugin plugin = SaveParticipant1Plugin.getInstance();

		// look at the plugin save lifecycle
		IStatus status = plugin.getSaveLifecycleLog();
		assertTrue("Getting lifecycle log failed with message: " + status.getMessage(), status.isOK());
	}
}
