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

import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.tests.resources.ResourceDeltaVerifier;
import org.eclipse.core.tests.resources.ResourceTest;

public class PR_1GEAB3C_Test extends ResourceTest {
	ResourceDeltaVerifier verifier;

	protected static final String VERIFIER_NAME = "TestListener";

	public void assertDelta() {
		assertTrue(verifier.getMessage(), verifier.isDeltaValid());
	}

	/**
	 * Runs code to handle a core exception
	 */
	protected void handleCoreException(CoreException e) {
		assertTrue("CoreException: " + e.getMessage(), false);
	}

	/**
	 * Sets up the fixture, for example, open a network connection.
	 * This method is called before a test is executed.
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
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
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		ensureDoesNotExistInWorkspace(getWorkspace().getRoot());
		getWorkspace().removeResourceChangeListener(verifier);
	}

	/*
	 * Ensure that we get ADDED and OPEN in the delta when we create and open
	 * a project in a workspace runnable.
	 */
	public void test_1GEAB3C() {
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
		try {
			getWorkspace().run(body, getMonitor());
		} catch (CoreException e) {
			fail("1.1", e);
		}
		assertDelta();
	}
}
