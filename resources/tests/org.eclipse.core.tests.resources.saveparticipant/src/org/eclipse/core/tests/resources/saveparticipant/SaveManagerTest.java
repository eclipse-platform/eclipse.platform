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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Runs all the SaveManager tests as a single session test.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SaveManagerTest {

	public static final String PI_RESOURCES_SAVEPARTICIPANT_TESTS = "org.eclipse.core.tests.resources.saveparticipant";

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension
			.forPlugin(PI_RESOURCES_SAVEPARTICIPANT_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	/** project names */
	static final String PROJECT_1 = "MyProject";
	static final String PROJECT_2 = "Project2";

	/** plugin ids */
	static final String PI_SAVE_PARTICIPANT_1 = "org.eclipse.core.tests.resources.saveparticipant1";
	static final String PI_SAVE_PARTICIPANT_2 = "org.eclipse.core.tests.resources.saveparticipant2";
	static final String PI_SAVE_PARTICIPANT_3 = "org.eclipse.core.tests.resources.saveparticipant3";

	static String[] defineHierarchy(String project) {
		if (project.equals(PROJECT_1))
			return defineHierarchy1();
		if (project.equals(PROJECT_2))
			return defineHierarchy2();
		return new String[0];
	}

	private static String[] defineHierarchy1() {
		return new String[] {"/folder110/", "/folder110/folder120/", "/folder110/folder120/folder130/", "/folder110/folder120/folder130/folder140/", "/folder110/folder120/folder130/folder140/folder150/", "/folder110/folder120/folder130/folder140/folder150/file160", "/folder110/folder120/folder130/folder140/file150", "/folder110/folder121/", "/folder110/folder121/folder131/", "/folder110/folder120/folder130/folder141/"};
	}

	private static String[] defineHierarchy2() {
		return new String[] {"/file110", "/folder110/", "/folder110/file120", "/folder111/", "/folder111/folder120/", "/folder111/file121"};
	}

	private void saveWorkspace() throws CoreException {
		getWorkspace().save(true, null);
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

	@Test
	@Order(1)
	public void test1() throws Exception {
		SaveManager1Test test = new SaveManager1Test();
		saveWorkspace();
		test.testCreateMyProject();
		test.testCreateProject2();
		test.testAddSaveParticipant();
		test.testBuilder();
		saveWorkspace();
		test.testPostSave();
	}

	@Test
	@Order(2)
	public void test2() throws Exception {
		SaveManager2Test test = new SaveManager2Test();
		test.testVerifyRestoredWorkspace();
		test.testBuilder();
		test.testSaveParticipant();
		test.testVerifyProject2();
		saveWorkspace();
	}

	@Test
	@Order(3)
	public void test3() throws Exception {
		SaveManager3Test test = new SaveManager3Test();
		test.testSaveParticipant();
		test.testBuilder();
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
