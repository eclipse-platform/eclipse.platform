/*******************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation and others.
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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;

/**
 * Runs all the SaveManager tests as a single session test.
 */
public class SaveManagerTest extends WorkspaceSessionTest {

	/** project names */
	static final String PROJECT_1 = "MyProject";
	static final String PROJECT_2 = "Project2";

	/** activities */
	static final String COMMENT_1 = "COMMENT ONE";
	static final String COMMENT_2 = "COMMENT TWO";

	/** plugin ids */
	static final String PI_SAVE_PARTICIPANT_1 = "org.eclipse.core.tests.resources.saveparticipant1";
	static final String PI_SAVE_PARTICIPANT_2 = "org.eclipse.core.tests.resources.saveparticipant2";
	static final String PI_SAVE_PARTICIPANT_3 = "org.eclipse.core.tests.resources.saveparticipant3";

	public SaveManagerTest() {
	}

	public SaveManagerTest(String name) {
		super(name);
	}

	protected String[] defineHierarchy(String project) {
		if (project.equals(PROJECT_1))
			return defineHierarchy1();
		if (project.equals(PROJECT_2))
			return defineHierarchy2();
		return new String[0];
	}

	protected String[] defineHierarchy1() {
		return new String[] {"/folder110/", "/folder110/folder120/", "/folder110/folder120/folder130/", "/folder110/folder120/folder130/folder140/", "/folder110/folder120/folder130/folder140/folder150/", "/folder110/folder120/folder130/folder140/folder150/file160", "/folder110/folder120/folder130/folder140/file150", "/folder110/folder121/", "/folder110/folder121/folder131/", "/folder110/folder120/folder130/folder141/"};
	}

	protected String[] defineHierarchy2() {
		return new String[] {"/file110", "/folder110/", "/folder110/file120", "/folder111/", "/folder111/folder120/", "/folder111/file121"};
	}

	public void saveWorkspace() {
		try {
			getWorkspace().save(true, null);
		} catch (CoreException e) {
			fail("2.0", e);
		}
	}

	/**
	 * Sets the workspace autobuilding to the desired value.
	 */
	protected void setAutoBuilding(boolean value) throws CoreException {
		IWorkspace workspace = getWorkspace();
		if (workspace.isAutoBuilding() == value)
			return;
		IWorkspaceDescription desc = workspace.getDescription();
		desc.setAutoBuilding(value);
		workspace.setDescription(desc);
	}

	public void test1() {
		SaveManager1Test test = new SaveManager1Test();
		test.saveWorkspace();
		test.testCreateMyProject();
		test.testCreateProject2();
		test.testAddSaveParticipant();
		test.testBuilder();
		test.saveWorkspace();
		test.testPostSave();

	}

	public void test2() {
		SaveManager2Test test = new SaveManager2Test();
		test.testVerifyRestoredWorkspace();
		test.testBuilder();
		test.testSaveParticipant();
		test.testVerifyProject2();
		test.saveWorkspace();
	}

	public void test3() {
		SaveManager3Test test = new SaveManager3Test();
		test.testSaveParticipant();
		test.testBuilder();
		test.cleanUp();
	}

	protected void touch(IProject project) throws CoreException {
		project.accept(resource -> {
			if (resource.getType() == IResource.FILE) {
				resource.touch(null);
				return false;
			}
			return true;
		});
	}
}
