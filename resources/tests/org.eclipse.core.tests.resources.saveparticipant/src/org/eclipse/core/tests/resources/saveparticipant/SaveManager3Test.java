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
import org.eclipse.core.tests.resources.saveparticipant1.SaveParticipant1Plugin;
import org.eclipse.core.tests.resources.saveparticipant2.SaveParticipant2Plugin;
import org.eclipse.core.tests.resources.saveparticipant3.SaveParticipant3Plugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

/**
 * @see SaveManager1Test
 * @see SaveManager2Test
 */
public class SaveManager3Test extends SaveManagerTest {
	/**
	 * Need a zero argument constructor to satisfy the test harness.
	 * This constructor should not do any real work nor should it be
	 * called by user code.
	 */
	public SaveManager3Test() {
	}

	public SaveManager3Test(String name) {
		super(name);
	}

	public static Test suite() {
		// we do not add the whole class because the order is important
		TestSuite suite = new TestSuite();
		suite.addTest(new SaveManager3Test("testSaveParticipant"));
		suite.addTest(new SaveManager3Test("testBuilder"));
		suite.addTest(new SaveManager3Test("cleanUp"));
		return suite;
	}

	public void testBuilder() {
		IProject project = getWorkspace().getRoot().getProject(PROJECT_1);
		assertTrue("0.0", project.isAccessible());

		try {
			setAutoBuilding(false);
			touch(project);
			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
			setAutoBuilding(true);
		} catch (CoreException e) {
			fail("1.0", e);
		}
		waitForBuild();
		DeltaVerifierBuilder verifier = DeltaVerifierBuilder.getInstance();
		assertTrue("1.1", verifier.wasIncrementalBuild());

		IFile added = project.getFile("added file");
		verifier.addExpectedChange(added, project, IResourceDelta.ADDED, 0);
		try {
			added.create(getRandomContents(), true, null);
		} catch (CoreException e) {
			fail("3.1", e);
		}
		waitForBuild();
		assertTrue("3.2", verifier.wasAutoBuild());
		assertTrue("3.3", verifier.isDeltaValid());
		// remove the file because we don't want it to affect any other delta in the test
		try {
			added.delete(true, false, null);
		} catch (CoreException e) {
			fail("3.4", e);
		}
	}

	public void testSaveParticipant() {
		// SaveParticipant1Plugin
		Bundle bundle = Platform.getBundle(PI_SAVE_PARTICIPANT_1);
		assertTrue("0.1", bundle != null);
		try {
			bundle.start();
		} catch (BundleException e) {
			fail("0.0", e);
		}
		SaveParticipant1Plugin plugin1 = SaveParticipant1Plugin.getInstance();

		// check saved state and delta
		plugin1.resetDeltaVerifier();
		IStatus status;
		try {
			status = plugin1.registerAsSaveParticipant();
			if (!status.isOK()) {
				System.out.println(status.getMessage());
				assertTrue("1.0", false);
			}
		} catch (CoreException e) {
			fail("1.1", e);
		}

		// SaveParticipant2Plugin
		bundle = Platform.getBundle(PI_SAVE_PARTICIPANT_2);
		assertTrue("5.1", bundle != null);
		try {
			bundle.start();
		} catch (BundleException e) {
			fail("5.0", e);
		}
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
		IResource[] resources = buildResources(project, defineHierarchy(PROJECT_2));
		plugin2.addExpectedChange(resources, IResourceDelta.ADDED, 0);
		//
		try {
			status = plugin2.registerAsSaveParticipant();
			if (!status.isOK()) {
				System.out.println(status.getMessage());
				fail("6.0");
			}
		} catch (CoreException e) {
			fail("6.1", e);
		}

		// SaveParticipant3Plugin
		bundle = Platform.getBundle(PI_SAVE_PARTICIPANT_3);
		assertTrue("7.1", bundle != null);
		try {
			bundle.start();
		} catch (BundleException e) {
			fail("7.0", e);
		}
		SaveParticipant3Plugin plugin3 = SaveParticipant3Plugin.getInstance();

		try {
			status = plugin3.registerAsSaveParticipant();
			if (!status.isOK()) {
				System.out.println(status.getMessage());
				fail("7.2");
			}
		} catch (CoreException e) {
			fail("7.3", e);
		}
	}

	public void cleanUp() {
		try {
			ensureDoesNotExistInWorkspace(getWorkspace().getRoot());
			getWorkspace().save(true, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}
	}
}
