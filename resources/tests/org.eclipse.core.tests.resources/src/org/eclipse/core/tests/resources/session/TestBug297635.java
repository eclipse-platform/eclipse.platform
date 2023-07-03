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
import org.eclipse.core.internal.resources.SaveManager;
import org.eclipse.core.internal.resources.SavedState;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.harness.BundleTestingHelper;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.ResourceTest;
import org.eclipse.core.tests.resources.content.ContentTypeTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * Tests regression of bug 297635
 */
public class TestBug297635 extends ResourceTest implements ISaveParticipant {

	private static final String BUNDLE01_ID = "org.eclipse.bundle01";
	private static final String FILE = "file1.txt";
	private static final String ANOTHER_FILE = "file2.txt";

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, TestBug297635.class);
	}

	public BundleContext getContext() {
		return Platform.getBundle(PI_RESOURCES_TESTS).getBundleContext();
	}

	public void testBug() throws Exception {
		installBundle();

		addSaveParticipant();

		IProject project = getProject("Project1");
		ensureExistsInWorkspace(project, true);
		createFileInProject(FILE, project);

		saveFull();

		reinstallBundle();

		project = getProject("Project1");
		createFileInProject(ANOTHER_FILE, project);

		Map<String, SavedState> savedStates = getSavedStatesFromSaveManager();

		addSaveParticipant();

		assertStateTreesIsNotNull(savedStates.get(BUNDLE01_ID));

		saveSnapshot();

		assertStateTreesIsNull(savedStates.get(BUNDLE01_ID));
	}

	private void assertStateTreesIsNotNull(SavedState savedState) throws Exception {
		assertStateTrees(savedState, false);
	}

	private void assertStateTreesIsNull(SavedState savedState) throws Exception {
		assertStateTrees(savedState, true);
	}

	private IFile createFileInProject(String fileName, IProject project) {
		IFile file = project.getFile(fileName);
		ensureExistsInWorkspace(file, getRandomContents());
		return file;
	}

	private IProject getProject(String projectName) {
		return getWorkspace().getRoot().getProject(projectName);
	}

	private void installBundle() throws BundleException, MalformedURLException, IOException {
		Bundle b = BundleTestingHelper.installBundle("1", getContext(),
				ContentTypeTest.TEST_FILES_ROOT + "content/bundle01");
		BundleTestingHelper.resolveBundles(getContext(), new Bundle[] { b });
		b.start(Bundle.START_TRANSIENT);
	}

	private void addSaveParticipant() throws CoreException {
		getWorkspace().addSaveParticipant(BUNDLE01_ID, TestBug297635.this);
	}

	private void saveFull() throws CoreException {
		getWorkspace().save(true, getMonitor());
	}

	private void reinstallBundle() throws BundleException, MalformedURLException, IOException {
		/*
		 * install the bundle again. We need to restart the org.eclipse.core.resources
		 * bundle to read the tree file again. We rely on the fact that the
		 * core.resources bundle doesn't save the tree when it is stopped
		 */
		Bundle coreResourcesBundle = Platform.getBundle(ResourcesPlugin.PI_RESOURCES);
		coreResourcesBundle.stop(Bundle.STOP_TRANSIENT);
		Bundle b = BundleTestingHelper.installBundle("1", getContext(),
				ContentTypeTest.TEST_FILES_ROOT + "content/bundle01");
		BundleTestingHelper.resolveBundles(getContext(), new Bundle[] { b });
		coreResourcesBundle.start(Bundle.START_TRANSIENT);
	}

	@SuppressWarnings("unchecked")
	private Map<String, SavedState> getSavedStatesFromSaveManager()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		// get access to SaveManager#savedStates to verify that tress are being kept
		// there
		Field field = SaveManager.class.getDeclaredField("savedStates");
		field.setAccessible(true);
		return (Map<String, SavedState>) field.get(((Workspace) getWorkspace()).getSaveManager());
	}

	private void saveSnapshot() throws CoreException {
		((Workspace) getWorkspace()).getSaveManager().save(ISaveContext.SNAPSHOT, true, null, getMonitor());
	}

	private void assertStateTrees(SavedState savedState, boolean isNull)
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Object oldTree = null;
		Object newTree = null;

		Field oldTreeField = SavedState.class.getDeclaredField("oldTree");
		oldTreeField.setAccessible(true);
		oldTree = oldTreeField.get(savedState);

		Field newTreeField = SavedState.class.getDeclaredField("newTree");
		newTreeField.setAccessible(true);
		newTree = newTreeField.get(savedState);

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
