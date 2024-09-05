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
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForRefresh;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.tests.resources.ResourceDeltaVerifier;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorkspaceResetExtension.class)
public class PR_1GEAB3C_Test {

	ResourceDeltaVerifier verifier;

	protected static final String VERIFIER_NAME = "TestListener";

	/**
	 * Sets up the fixture, for example, open a network connection.
	 * This method is called before a test is executed.
	 */
	@BeforeEach
	public void setUp() {
		//ensure background work is done before adding verifier
		waitForBuild();
		waitForRefresh();
		verifier = new ResourceDeltaVerifier();
		getWorkspace().addResourceChangeListener(verifier);
	}

	/**
	 * Tears down the fixture, for example, close a network connection.
	 * This method is called after a test is executed.
	 */
	@AfterEach
	public void tearDown() {
		getWorkspace().removeResourceChangeListener(verifier);
	}

	/*
	 * Ensure that we get ADDED and OPEN in the delta when we create and open
	 * a project in a workspace runnable.
	 */
	@Test
	public void test_1GEAB3C() throws CoreException {
		verifier.reset();
		final IProject project = getWorkspace().getRoot().getProject("MyAddedAndOpenedProject");
		IFile prefs = project.getFolder(EclipsePreferences.DEFAULT_PREFERENCES_DIRNAME)
				.getFile(ResourcesPlugin.PI_RESOURCES + "." + EclipsePreferences.PREFS_FILE_EXTENSION);
		verifier.addExpectedChange(project, IResourceDelta.ADDED, IResourceDelta.OPEN);
		verifier.addExpectedChange(project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME), IResourceDelta.ADDED, 0);
		verifier.addExpectedChange(new IResource[] { prefs.getParent() }, IResourceDelta.ADDED, 0);
		verifier.addExpectedChange(new IResource[] { prefs }, IResourceDelta.ADDED, 0);
		IWorkspaceRunnable body = monitor -> {
			monitor.beginTask("Creating and deleting", 100);
			try {
				project.create(SubMonitor.convert(monitor, 50));
				project.open(SubMonitor.convert(monitor, 50));
			} finally {
				monitor.done();
			}
		};
		getWorkspace().run(body, createTestMonitor());
		assertTrue(verifier.getMessage(), verifier.isDeltaValid());
	}

}
