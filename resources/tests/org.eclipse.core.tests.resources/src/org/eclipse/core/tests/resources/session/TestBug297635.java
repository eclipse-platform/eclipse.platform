/*******************************************************************************
 *  Copyright (c) 2010, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.session;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.Map;
import junit.framework.Test;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.resources.content.ContentTypeTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;
import org.osgi.framework.*;

/**
 * Tests regression of bug 297635
 */
public class TestBug297635 extends WorkspaceSessionTest implements ISaveParticipant {

	private static final String BUNDLE01_ID = "org.eclipse.bundle01";

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, TestBug297635.class);
	}

	public BundleContext getContext() {
		return Platform.getBundle(PI_RESOURCES_TESTS).getBundleContext();
	}

	public void test1() {
		// install a bundle
		try {
			Bundle b = BundleTestingHelper.installBundle("1", getContext(), ContentTypeTest.TEST_FILES_ROOT + "content/bundle01");
			BundleTestingHelper.resolveBundles(getContext(), new Bundle[] {b});
			b.start(Bundle.START_TRANSIENT);
		} catch (MalformedURLException e) {
			fail("1.0", e);
		} catch (BundleException e) {
			fail("1.1", e);
		} catch (IOException e) {
			fail("1.2", e);
		}

		// register a save participant for the bundle
		try {
			getWorkspace().addSaveParticipant(BUNDLE01_ID, TestBug297635.this);
		} catch (CoreException e) {
			fail("2.0", e);
		}

		// create a project with a file
		IProject project = getWorkspace().getRoot().getProject("Project1");
		IFile file = project.getFile("file1.txt");
		ensureExistsInWorkspace(project, true);
		ensureExistsInWorkspace(file, getRandomContents());

		// perform a full save
		try {
			getWorkspace().save(true, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}
	}

	@SuppressWarnings("unchecked")
	public void test2() {
		// install the bundle again
		// we need to restart the org.eclipse.core.resources bundle to read the tree file again
		// we rely on the fact that the core.resources bundle doesn't save the tree when it is stopped
		try {
			Bundle coreResourcesBundle = Platform.getBundle(ResourcesPlugin.PI_RESOURCES);
			coreResourcesBundle.stop(Bundle.STOP_TRANSIENT);
			Bundle b = BundleTestingHelper.installBundle("1", getContext(), ContentTypeTest.TEST_FILES_ROOT + "content/bundle01");
			BundleTestingHelper.resolveBundles(getContext(), new Bundle[] {b});
			coreResourcesBundle.start(Bundle.START_TRANSIENT);
		} catch (MalformedURLException e2) {
			fail("1.0", e2);
		} catch (BundleException e2) {
			fail("1.1", e2);
		} catch (IOException e2) {
			fail("1.2", e2);
		}

		// create yet another file in the existing project
		IProject project = getWorkspace().getRoot().getProject("Project1");
		IFile file = project.getFile("file2.txt");
		ensureExistsInWorkspace(file, getRandomContents());

		// get access to SaveManager#savedStates to verify that tress are being kept there
		Map<String, SavedState> savedStates = null;
		try {
			Field field = SaveManager.class.getDeclaredField("savedStates");
			field.setAccessible(true);
			savedStates = (Map<String, SavedState>) field.get(((Workspace) getWorkspace()).getSaveManager());
		} catch (IllegalArgumentException e) {
			fail("2.0", e);
		} catch (IllegalAccessException e) {
			fail("2.1", e);
		} catch (SecurityException e) {
			fail("2.2", e);
		} catch (NoSuchFieldException e) {
			fail("2.3", e);
		}

		// register a save participant for the bundle
		try {
			getWorkspace().addSaveParticipant(BUNDLE01_ID, TestBug297635.this);
		} catch (CoreException e) {
			fail("3.0", e);
		}

		// assert the saved state for Bundle01, trees should not be null
		assertStateTrees(savedStates.get(BUNDLE01_ID), false);

		try {
			((Workspace) getWorkspace()).getSaveManager().save(ISaveContext.SNAPSHOT, true, null, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}

		// assert the saved state for Bundle01, trees should be null after a snapshot save
		assertStateTrees(savedStates.get(BUNDLE01_ID), true);
	}

	private void assertStateTrees(SavedState savedState, boolean isNull) {
		Object oldTree = null;
		Object newTree = null;

		try {
			Field oldTreeField = SavedState.class.getDeclaredField("oldTree");
			oldTreeField.setAccessible(true);
			oldTree = oldTreeField.get(savedState);

			Field newTreeField = SavedState.class.getDeclaredField("newTree");
			newTreeField.setAccessible(true);
			newTree = newTreeField.get(savedState);
		} catch (SecurityException e) {
			fail("1.0", e);
		} catch (NoSuchFieldException e) {
			fail("2.0", e);
		} catch (IllegalArgumentException e) {
			fail("3.0", e);
		} catch (IllegalAccessException e) {
			fail("4.0", e);
		}

		if (isNull) {
			assertNull(oldTree);
			assertNull(newTree);
		} else {
			assertNotNull(oldTree);
			assertNotNull(newTree);
		}
	}

	// ISaveParticipant methods

	@Override
	public void doneSaving(ISaveContext context) {
		// nothing to do
	}

	@Override
	public void prepareToSave(ISaveContext context) {
		context.needDelta();
		context.needSaveNumber();
	}

	@Override
	public void rollback(ISaveContext context) {
		// nothing to do
	}

	@Override
	public void saving(ISaveContext context) {
		// nothing to do
	}
}
