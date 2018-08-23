/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.ui;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;
import org.eclipse.team.internal.ccvs.ui.actions.*;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.ui.IActionDelegate;

/**
 * Test the menu enablement code for the CVS menus
 */
public class MenuEnablementTest extends EnablementTest {
	
	private static final int MANAGED = 1;
	private static final int ADDED = 2;
	private static final int UNMANAGED = 4;
	private static final int IGNORED = 8;
	private static final int SINGLE_ONLY = 16;
	private static final int FOLDERS = 32;
	private static final int FILES = 64;
	private static final int UNMANAGED_PARENT = 128;
	
	/**
	 * Constructor for CVSProviderTest
	 */
	public MenuEnablementTest() {
		super();
	}

	/**
	 * Constructor for CVSProviderTest
	 */
	public MenuEnablementTest(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(MenuEnablementTest.class);
		return new CVSTestSetup(suite);
		//return new CVSTestSetup(new MenuEnablementTest("testReadOnly"));
	}
	
	
		
	/**
	 * Method assertEnablement.
	 * @param action
	 * @param project
	 * @param IGNORED
	 * @param b
	 */
	private void assertEnablement(IActionDelegate action, IProject project, int kind, boolean expectedEnablement) throws CoreException, TeamException {
		List resources = new ArrayList();
		boolean multiple = (kind & SINGLE_ONLY) == 0;
		boolean includeFolders = ((kind & FOLDERS) > 0) || ((kind & FILES) == 0);
		if ((kind & MANAGED) > 0) {
			resources.addAll(getManagedResources(project, includeFolders, multiple));
		}
		if ((kind & UNMANAGED) > 0) {
			resources.addAll(getUnmanagedResources(project));
		}
		if ((kind & IGNORED) > 0) {
			resources.addAll(getIgnoredResources(project));
		}
		if ((kind & ADDED) > 0) {
			resources.addAll(getAddedResources(project));
		}
		if ((kind & UNMANAGED_PARENT) > 0) {
			resources.addAll(getResourceWithUnmanagedParent(project));
		}
		ensureAllSyncInfoLoaded(project);
		assertEnablement(action, asSelection(resources), expectedEnablement);
	}
	
	/**
	 * Method ensureAllSyncInfoLoaded.
	 * @param project
	 */
	private void ensureAllSyncInfoLoaded(IProject project) throws CVSException {
		EclipseSynchronizer.getInstance().ensureSyncInfoLoaded(new IResource[] {project}, IResource.DEPTH_INFINITE);
	}

	/**
	 * Assert that the action is disabled for the reasons common to all menu
	 * actions.
	 * 
	 * @param action
	 * @param project
	 */
	public void assertDisabledForCommonReasons(IActionDelegate action, IProject project) throws CoreException {
		assertDisabledForNoSelection(action);
		assertDisabledForFolderFileOverlap(action, project);
		assertDisabledForClosedProject(action, project);
		assertDisabledForNonCVSProject(action);
	}
			
	private void assertDisabledForNoSelection(IActionDelegate actionDelegate) {
		assertEnablement(actionDelegate, StructuredSelection.EMPTY, false /* expected enablement */);
	}
	
	private void assertDisabledForFolderFileOverlap(IActionDelegate action, IProject project) {
		List resources = getOverlappingResources(project, true /* include files */);
		assertEnablement(action, asSelection(resources), false /* enabled */);
	}
	
	private void assertDisabledForClosedProject(IActionDelegate action, IProject project) throws CoreException {
		project.close(null);
		List resources = new ArrayList();
		resources.add(project);
		assertEnablement(action, asSelection(resources), false /* enabled */);
		project.open(null);
	}
	
	private void assertDisabledForNonCVSProject(IActionDelegate action) throws CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject("Non-CVS");
		if (!project.exists())
			project.create(null);
		List resources = new ArrayList();
		resources.add(project);
		assertEnablement(action, asSelection(resources), false /* enabled */);
	}
	
	private void assertEnabledForFolderOnlyOverlap(IActionDelegate action, IProject project) {
		List resources = getOverlappingResources(project, false /* include files */);
		assertEnablement(action, asSelection(resources), true /* enabled */);
	}
	
	public void testAddAction() throws CoreException, TeamException {
		IActionDelegate action = new AddAction();
		IProject project = createTestProject(action);
		assertDisabledForCommonReasons(action, project);
		assertEnablement(action, project, MANAGED, false /* expected enablement */);
		assertEnablement(action, project, ADDED, false /* expected enablement */);
		assertEnablement(action, project, IGNORED, true /* expected enablement */);
		assertEnablement(action, project, UNMANAGED, true /* expected enablement */);
		assertEnablement(action, project, UNMANAGED | IGNORED, true /* expected enablement */);		
		assertEnablement(action, project, UNMANAGED | SINGLE_ONLY, true /* expected enablement */);
		assertEnablement(action, project, UNMANAGED | MANAGED | IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED_PARENT, true /* expected enablement */);
	}

	public void testBranchAction() throws CoreException, TeamException {
		IActionDelegate action = new BranchAction();
		IProject project = createTestProject(action);
		assertDisabledForCommonReasons(action, project);
		assertEnabledForFolderOnlyOverlap(action, project);
		assertEnablement(action, project, MANAGED, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED, true /* expected enablement */);
		assertEnablement(action, project, IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED | MANAGED | IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED_PARENT, false /* expected enablement */);
	}
	
	public void testCommitAction() throws CoreException, TeamException {
		IActionDelegate action = new CommitAction();
		IProject project = createTestProject(action);
		assertDisabledForCommonReasons(action, project);
		assertEnabledForFolderOnlyOverlap(action, project);
		assertEnablement(action, project, MANAGED, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED | UNMANAGED, true /* expected enablement */);
		assertEnablement(action, project, IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED, true /* expected enablement */);
		assertEnablement(action, project, UNMANAGED | MANAGED | IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED_PARENT, true /* expected enablement */);
	}
	
	public void testCompareWithRevison() throws CoreException, TeamException {
		IActionDelegate action = new CompareWithRevisionAction();
		IProject project = createTestProject(action);
		assertDisabledForCommonReasons(action, project);
		assertEnablement(action, project, MANAGED | FILES | SINGLE_ONLY, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | FOLDERS | FILES, false /* expected enablement */);
		assertEnablement(action, project, MANAGED | FOLDERS | SINGLE_ONLY, false /* expected enablement */);
		assertEnablement(action, project, MANAGED | FILES, false /* expected enablement */);
		assertEnablement(action, project, ADDED | SINGLE_ONLY, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED | SINGLE_ONLY, false /* expected enablement */);
		assertEnablement(action, project, IGNORED | SINGLE_ONLY, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED_PARENT, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED | MANAGED | IGNORED, false /* expected enablement */);
	}
	
	public void testCompareWithTagAction() throws CoreException, TeamException {
		IActionDelegate action = new CompareWithTagAction();
		IProject project = createTestProject(action);
		assertDisabledForCommonReasons(action, project);
		assertEnabledForFolderOnlyOverlap(action, project);
		assertEnablement(action, project, MANAGED, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED | UNMANAGED, false /* expected enablement */);
		assertEnablement(action, project, IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED_PARENT, false /* expected enablement */);
		// true is expected for ignored resources whose parent is not ignored
		assertEnablement(action, project, UNMANAGED | MANAGED | IGNORED, false /* expected enablement */);
	}
	
	public void testGenerateDiffAction() throws CoreException, TeamException {
		IActionDelegate action = new GenerateDiffFileAction();
		IProject project = createTestProject(action);
		assertDisabledForCommonReasons(action, project);
		assertEnablement(action, project, MANAGED | FILES | SINGLE_ONLY, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | FOLDERS | FILES, false /* expected enablement */);
		assertEnablement(action, project, MANAGED | FOLDERS | SINGLE_ONLY, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | FILES, false /* expected enablement */);
		assertEnablement(action, project, ADDED | SINGLE_ONLY, true /* expected enablement */);
		assertEnablement(action, project, UNMANAGED | SINGLE_ONLY, true /* expected enablement */);
		assertEnablement(action, project, IGNORED | SINGLE_ONLY, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED_PARENT, true /* expected enablement */);
		assertEnablement(action, project, UNMANAGED | MANAGED | IGNORED, false /* expected enablement */);
	}
	
	/*
	 * Should be the same as testAdd
	 */
	public void testIgnoreAction() throws CoreException, TeamException {
		IActionDelegate action = new IgnoreAction();
		IProject project = createTestProject(action);
		assertDisabledForCommonReasons(action, project);
		assertEnablement(action, project, MANAGED, false /* expected enablement */);
		assertEnablement(action, project, ADDED, false /* expected enablement */);
		assertEnablement(action, project, IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED, true /* expected enablement */);
		assertEnablement(action, project, UNMANAGED | SINGLE_ONLY, true /* expected enablement */);
		assertEnablement(action, project, UNMANAGED | MANAGED | IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED_PARENT, true /* expected enablement */);
	}
	
	public void testMergeAction() throws CoreException, TeamException {
		IActionDelegate action = new MergeAction();
		IProject project = createTestProject(action);
		assertDisabledForCommonReasons(action, project);
		assertEnabledForFolderOnlyOverlap(action, project);
		assertEnablement(action, project, MANAGED, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED | UNMANAGED, false /* expected enablement */);
		assertEnablement(action, project, IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED | MANAGED | IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED_PARENT, false /* expected enablement */);
	}
	
	public void testReplaceWithRemoteAction() throws CoreException, TeamException {
		IActionDelegate action = new ReplaceWithRemoteAction();
		IProject project = createTestProject(action);
		assertDisabledForCommonReasons(action, project);
		assertEnabledForFolderOnlyOverlap(action, project);
		assertEnablement(action, project, MANAGED, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED, false /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED | UNMANAGED, false /* expected enablement */);
		// true is expected for ignored resources whose parent is not ignored
		assertEnablement(action, project, IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED_PARENT, false /* expected enablement */);
		// true is expected for ignored resources whose parent is not ignored
		assertEnablement(action, project, UNMANAGED | MANAGED | IGNORED, false /* expected enablement */);
	}
	
	/*
	 * Should be the same as testCompareWithTagAction
	 */
	public void testReplaceWithTagAction() throws CoreException, TeamException {
		IActionDelegate action = new ReplaceWithSelectableTagAction();
		IProject project = createTestProject(action);
		assertDisabledForCommonReasons(action, project);
		assertEnabledForFolderOnlyOverlap(action, project);
		assertEnablement(action, project, MANAGED, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED | UNMANAGED, false /* expected enablement */);
		assertEnablement(action, project, IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED_PARENT, false /* expected enablement */);
		// true is expected for ignored resources whose parent is not ignored
		assertEnablement(action, project, UNMANAGED | MANAGED | IGNORED, false /* expected enablement */);
	}

	public void testKeywordSubstitutionAction() throws CoreException, TeamException {
		IActionDelegate action = new SetKeywordSubstitutionAction();
		IProject project = createTestProject(action);
		assertDisabledForCommonReasons(action, project);
		assertEnabledForFolderOnlyOverlap(action, project);
		assertEnablement(action, project, MANAGED, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED | UNMANAGED, false /* expected enablement */);
		assertEnablement(action, project, IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED | MANAGED | IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED_PARENT, false /* expected enablement */);
	}
	
	public void testShowInHistory() throws CoreException, TeamException {
		IActionDelegate action = new ShowResourceInHistoryAction();
		IProject project = createTestProject(action);
		assertDisabledForCommonReasons(action, project);
		assertEnablement(action, project, MANAGED | FILES | SINGLE_ONLY, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | FOLDERS | FILES, false /* expected enablement */);
		assertEnablement(action, project, MANAGED | FOLDERS | SINGLE_ONLY, false /* expected enablement */);
		assertEnablement(action, project, MANAGED | FILES, false /* expected enablement */);
		assertEnablement(action, project, ADDED | SINGLE_ONLY, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED | SINGLE_ONLY, false /* expected enablement */);
		assertEnablement(action, project, IGNORED | SINGLE_ONLY, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED_PARENT, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED | MANAGED | IGNORED, false /* expected enablement */);
	}
	
	public void testSyncAction() throws CoreException, TeamException {
		IActionDelegate action = new SyncAction();
		IProject project = createTestProject(action);
		assertDisabledForCommonReasons(action, project);
		assertEnabledForFolderOnlyOverlap(action, project);
		assertEnablement(action, project, MANAGED, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED | UNMANAGED, true /* expected enablement */);
		// true is expected for ignored resources whose parent is not ignored
		assertEnablement(action, project, IGNORED, true /* expected enablement */);
		assertEnablement(action, project, UNMANAGED, true /* expected enablement */);
		assertEnablement(action, project, UNMANAGED_PARENT, false /* expected enablement */);
		// true is expected for ignored resources whose parent is not ignored
		assertEnablement(action, project, UNMANAGED | MANAGED | IGNORED, true /* expected enablement */);
	}
	
	public void testTagAction() throws CoreException, TeamException {
		IActionDelegate action = new TagLocalAction();
		IProject project = createTestProject(action);
		assertDisabledForCommonReasons(action, project);
		assertEnabledForFolderOnlyOverlap(action, project);
		assertEnablement(action, project, MANAGED, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED, false /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED | UNMANAGED, false /* expected enablement */);
		assertEnablement(action, project, IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED | MANAGED | IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED_PARENT, false /* expected enablement */);
	}
	
	public void testUnmanageAction() throws CoreException, TeamException {
		IActionDelegate action = new UnmanageAction();
		IProject project = createTestProject(action);
		assertDisabledForCommonReasons(action, project);
		List resources = new ArrayList();
		resources.add(project);
		ensureAllSyncInfoLoaded(project);
		assertEnablement(action, asSelection(resources), true);
	}
	
	public void testUpdateAction() throws CoreException, TeamException {
		IActionDelegate action = new UpdateAction();
		IProject project = createTestProject(action);
		assertDisabledForCommonReasons(action, project);
		assertEnabledForFolderOnlyOverlap(action, project);
		assertEnablement(action, project, MANAGED, true /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED, false /* expected enablement */);
		assertEnablement(action, project, MANAGED | ADDED | UNMANAGED, false /* expected enablement */);
		assertEnablement(action, project, IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED | MANAGED | IGNORED, false /* expected enablement */);
		assertEnablement(action, project, UNMANAGED_PARENT, false /* expected enablement */);
	}
	
}
