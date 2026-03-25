/*******************************************************************************
 * Copyright (c) 2026 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.localstore;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Test for disabling file history based on a preference. Sets the preference
 * {@code org.eclipse.core.resources/disable_history_property=disable_history},
 * adds the session property {@code disable_history} on some test file and
 * performs edits. After setting the preference and session property, no new
 * file history is expected.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class DisableHistoryTests {

	private static final String DISABLE_PREFERENCE_NAME = "disable_history_property";

	private static final QualifiedName SESSION_PROPERTY_QN = new QualifiedName(null, "disable_history");

	private IProject project;

	@BeforeEach
	public void createTestProject() throws Exception {
		project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);
	}

	@AfterEach
	public void cleanUp() throws Exception {
		setHistoryDisablePreference(null);
	}

	@Test
	public void testDisableHistoryBeforeEdit() throws Exception {
		IFile file = project.getFile("test.txt");
		file.create("initial".getBytes(), IResource.FORCE, null);
		setHistoryDisablePreference(SESSION_PROPERTY_QN.getLocalName());
		disableHistory(file);
		writeContents(file, "edit 1");
		writeContents(file, "edit 2");
		IFileState[] history = file.getHistory(null);
		assertEquals(0, history.length, "Unexpected history after disable: " + toString(history));
	}

	@Test
	public void testDisableHistoryAfterEdit() throws Exception {
		IFile file = project.getFile("test.txt");
		file.create("initial".getBytes(), IResource.FORCE, null);
		writeContents(file, "edit 1");
		writeContents(file, "edit 2");
		IFileState[] history = file.getHistory(null);
		assertEquals(2, history.length, "Unexpected history before disable: " + toString(history));
		setHistoryDisablePreference(SESSION_PROPERTY_QN.getLocalName());
		disableHistory(file);
		writeContents(file, "edit 3");
		history = file.getHistory(null);
		assertEquals(2, history.length, "Unexpected history after disable: " + toString(history));
	}

	@Test
	public void testDisableHistoryBeforeMove() throws Exception {
		IFile file = project.getFile("test.txt");
		file.create("initial".getBytes(), IResource.FORCE, null);
		writeContents(file, "edit 1");
		writeContents(file, "edit 2");
		IFileState[] history = file.getHistory(null);
		assertEquals(2, history.length, "Unexpected history before disable: " + toString(history));
		setHistoryDisablePreference(SESSION_PROPERTY_QN.getLocalName());
		disableHistory(file);
		IFile movedFile = project.getFile("test2.txt");
		file.move(movedFile.getFullPath(), IResource.FORCE, null);
		history = movedFile.getHistory(null);
		assertEquals(2, history.length, "Unexpected history after move: " + toString(history));
		writeContents(movedFile, "edit 3");
		history = movedFile.getHistory(null);
		assertEquals(2, history.length, "Unexpected history after edit: " + toString(history));
	}

	@Test
	public void testDisableHistoryBeforeMovingParentFolder() throws Exception {
		IFolder folder = project.getFolder("test_folder");
		folder.create(true, true, null);
		IFile file = folder.getFile("test.txt");
		file.create("initial".getBytes(), IResource.FORCE, null);
		writeContents(file, "edit 1");
		writeContents(file, "edit 2");
		IFileState[] history = file.getHistory(null);
		assertEquals(2, history.length, "Unexpected history before disable: " + toString(history));
		setHistoryDisablePreference(SESSION_PROPERTY_QN.getLocalName());
		disableHistory(file);
		IFolder movedFolder = project.getFolder("test_folder2");
		folder.move(movedFolder.getFullPath(), true, null);
		IFile movedFile = movedFolder.getFile("test.txt");
		history = movedFile.getHistory(null);
		assertEquals(2, history.length, "Unexpected history after move: " + toString(history));
		writeContents(movedFile, "edit 3");
		history = movedFile.getHistory(null);
		assertEquals(2, history.length, "Unexpected history after edit: " + toString(history));
	}

	@Test
	public void testDisableHistoryBeforeDelete() throws Exception {
		IFile file = project.getFile("test.txt");
		file.create("initial".getBytes(), IResource.FORCE, null);
		writeContents(file, "edit 1");
		writeContents(file, "edit 2");
		IFileState[] history = file.getHistory(null);
		assertEquals(2, history.length, "Unexpected history before disable: " + toString(history));
		setHistoryDisablePreference(SESSION_PROPERTY_QN.getLocalName());
		disableHistory(file);
		writeContents(file, "edit 3");
		file.delete(true, null);
		history = file.getHistory(null);
		assertEquals(2, history.length, "Unexpected history after delete: " + toString(history));
	}

	@Test
	public void testDisableHistoryBeforeDeletingParentFolder() throws Exception {
		IFolder folder = project.getFolder("test_folder");
		folder.create(true, true, null);
		IFile file = folder.getFile("test.txt");
		file.create("initial".getBytes(), IResource.FORCE, null);
		writeContents(file, "edit 1");
		writeContents(file, "edit 2");
		IFileState[] history = file.getHistory(null);
		assertEquals(2, history.length, "Unexpected history before disable: " + toString(history));
		setHistoryDisablePreference(SESSION_PROPERTY_QN.getLocalName());
		disableHistory(file);
		writeContents(file, "edit 3");
		folder.delete(true, null);
		history = file.getHistory(null);
		assertEquals(2, history.length, "Unexpected history after delete: " + toString(history));
	}

	private void writeContents(IFile file, String contents) throws CoreException {
		file.setContents(contents.getBytes(), IResource.KEEP_HISTORY, null);
	}

	private static String toString(IFileState... states) {
		return Arrays.toString(states);
	}

	private static void disableHistory(IFile file) throws CoreException {
		file.setSessionProperty(SESSION_PROPERTY_QN, "true");
	}

	private static void setHistoryDisablePreference(String value) throws BackingStoreException {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		if (value != null) {
			node.put(DISABLE_PREFERENCE_NAME, value);
		} else {
			node.remove(DISABLE_PREFERENCE_NAME);
		}
		node.flush();
	}
}
