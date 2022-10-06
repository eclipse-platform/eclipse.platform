/*******************************************************************************
 * Copyright (c) 2004, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.session;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ SampleSessionTest.class, TestBug93473.class, TestSave.class, Test1G1N9GZ.class,
		TestCloseNoSave.class, TestMultiSnap.class, TestSaveCreateProject.class, TestSaveSnap.class,
		TestSaveWithClosedProject.class, TestSnapSaveSnap.class, TestBug6995.class,
		TestInterestingProjectPersistence.class, TestBuilderDeltaSerialization.class, Test1GALH44.class,
		TestMissingBuilder.class, TestClosedProjectLocation.class, FindDeletedMembersTest.class, TestBug20127.class,
		TestBug12575.class, WorkspaceDescriptionTest.class, TestBug30015.class,
		TestMasterTableCleanup.class,
		ProjectPreferenceSessionTest.class, TestBug113943.class, TestCreateLinkedResourceInHiddenProject.class,
		Bug_266907.class, TestBug297635.class, TestBug323833.class,
		org.eclipse.core.tests.resources.regression.TestMultipleBuildersOfSameType.class,
		org.eclipse.core.tests.resources.usecase.SnapshotTest.class, ProjectDescriptionDynamicTest.class,
		TestBug202384.class, TestBug369177.class, TestBug316182.class, TestBug294854.class, TestBug426263.class,
		TestWorkspaceEncodingExistingWorkspace.class, TestWorkspaceEncodingNewWorkspace.class,
		TestWorkspaceEncodingWithJvmArgs.class, TestWorkspaceEncodingWithPluginCustomization.class, })
public class AllSessionTests {
}
