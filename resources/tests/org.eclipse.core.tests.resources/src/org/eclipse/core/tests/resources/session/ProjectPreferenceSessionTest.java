/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.session;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForRefresh;

import java.util.concurrent.atomic.AtomicReference;
import junit.framework.Test;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class ProjectPreferenceSessionTest extends WorkspaceSessionTest {
	private static final String DIR_NAME = ".settings";
	private static final String FILE_EXTENSION = "prefs";

	public static Test suite() {
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, ProjectPreferenceSessionTest.class);
		//						return new ProjectPreferenceSessionTest("testDeleteFileBeforeLoad2");
	}

	private void saveWorkspace() throws Exception {
		getWorkspace().save(true, createTestMonitor());
	}

	/*
	 * See bug 91244
	 * - set some project settings
	 * - save them
	 * - exit the session
	 * - startup
	 * - delete the .prefs file from disk
	 */
	public void testDeleteFileBeforeLoad1() throws Exception {
		IProject project = getProject("testDeleteFileBeforeLoad");
		String qualifier = "test.delete.file.before.load";
		ensureExistsInWorkspace(project, true);
		IScopeContext context = new ProjectScope(project);
		Preferences node = context.getNode(qualifier);
		node.put("key", "value");
		node.flush();
		waitForRefresh();
		IFile file = project.getFile(IPath.fromOSString(DIR_NAME).append(qualifier).addFileExtension(FILE_EXTENSION));
		assertTrue("2.0", file.exists());
		assertTrue("2.1", file.getLocation().toFile().exists());
		saveWorkspace();
	}

	public void testDeleteFileBeforeLoad2() throws Exception {
		IProject project = getProject("testDeleteFileBeforeLoad");
		Platform.getPreferencesService().getRootNode().node(ProjectScope.SCOPE).node(project.getName());
		AtomicReference<BackingStoreException> exceptionInListener = new AtomicReference<>();
		ILogListener listener = (status, plugin) -> {
			if (!Platform.PI_RUNTIME.equals(plugin)) {
				return;
			}
			Throwable t = status.getException();
			if (t instanceof BackingStoreException backingStoreException) {
				exceptionInListener.set(backingStoreException);
			}
		};
		try {
			Platform.addLogListener(listener);
			project.delete(IResource.NONE, createTestMonitor());
		} finally {
			Platform.removeLogListener(listener);
		}
		if (exceptionInListener.get() != null) {
			throw exceptionInListener.get();
		}
		saveWorkspace();
	}

	/*
	 * Test saving a key/value pair in one session and then ensure that they exist
	 * in the next session.
	 */
	public void testSaveLoad1() throws Exception {
		IProject project = getProject("testSaveLoad");
		ensureExistsInWorkspace(project, true);
		IScopeContext context = new ProjectScope(project);
		Preferences node = context.getNode("test.save.load");
		node.put("key", "value");
		node.flush();
		saveWorkspace();
	}

	public void testSaveLoad2() throws Exception {
		IProject project = getProject("testSaveLoad");
		IScopeContext context = new ProjectScope(project);
		Preferences node = context.getNode("test.save.load");
		assertEquals("1.0", "value", node.get("key", null));
		saveWorkspace();
	}

	private static IProject getProject(String name) {
		return getWorkspace().getRoot().getProject(name);
	}
}
