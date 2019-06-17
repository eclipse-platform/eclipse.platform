/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.ui.benchmark;

import java.io.File;

import junit.framework.Test;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.tests.ccvs.core.subscriber.SyncInfoSource;

public class WorkflowTests extends BenchmarkTest {
	private int FILE_SIZE_MEAN = 16384;
	private int FILE_SIZE_VARIANCE = 12288;
	private int PROB_BINARY = 5;
	private static final String SHARE_PROJECT = "Share";
	private static final String CHECKOUT_PROJECT = "Checkout";
	private static final String COMMIT1 = "Commit1";
	private static final String COMMIT2 = "Commit2";
	private static final String COMMIT3 = "Commit3";
	private static final String COMMIT4 = "Commit4";
	private static final String UPDATE1 = "Update1";
	private static final String UPDATE2 = "Update2";
	private static final String UPDATE3 = "Update3";
	private static final String UPDATE4 = "Update4";
	private static final String REPLACE1 = "Replace1";
	private static final String REPLACE2 = "Replace2";
	private static final String REPLACE3 = "Replace3";
	private static final String TAG1 = "Tag1";
	private static final String[] PERFORMANCE_GROUPS = new String[] {
		SHARE_PROJECT, CHECKOUT_PROJECT, COMMIT1, COMMIT2, COMMIT3, COMMIT4, 
		UPDATE1, UPDATE2, UPDATE3, UPDATE4, REPLACE1, REPLACE2, REPLACE3, TAG1
	};

	public WorkflowTests() {
		super();
	}

	public WorkflowTests(String name) {
		super(name);
	}

	public static Test suite() {
		return suite(WorkflowTests.class);
	}
	
	public void testBigWorkflowNoUI() throws Exception {
		runWorkflowTests("testBigWithNoUI", BenchmarkTestSetup.BIG_ZIP_FILE, "CVS Workflow No UI", BenchmarkTestSetup.LOOP_COUNT, false, new SyncInfoSource());
	}
	
	@Override
	protected void waitForBuild() {
		super.waitForBuild();
		// Ensure that we can obtrain the worksapce lock before continuing
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		try {
			Job.getJobManager().beginRule(root, null);
		} finally {
			Job.getJobManager().endRule(root);
		}
	}
	
	/**
	 * Runs a series of incoming and outgoing workflow-related tests.
	 */
	protected void runWorkflowTests(String name, File initialContents, String globalName, int loopCount, boolean global, SyncInfoSource source) throws Exception {
		openEmptyPerspective();
		setupGroups(PERFORMANCE_GROUPS, globalName, global);
		for (int i = 0; i < loopCount; i++) {
			final SequenceGenerator gen = new SequenceGenerator();
			IProject outProject = createAndImportProject(name, initialContents);
			
			// test project sharing			
			startGroup(SHARE_PROJECT);
			shareProject(outProject);
			endGroup();
			
			// move the project out of the way
			String moduleName = outProject.getName();
			BenchmarkUtils.renameResource(outProject, moduleName + "out");
			outProject = BenchmarkUtils.getProject(moduleName + "out");
			waitForBuild();
			
			// test initial project checkout
			IProject inProject = BenchmarkUtils.getProject(moduleName);
			startGroup(CHECKOUT_PROJECT);
			checkoutProject(inProject, moduleName, null);
			endGroup();
			
			// Test incoming and outgoing change scenarios
			
			// Test 1: adding a new component - localized additions and some changes
			startGroup(COMMIT1);
			BenchmarkUtils.modifyRandomDeepFiles(gen, outProject, 5);
			BenchmarkUtils.touchRandomDeepFiles(gen, outProject, 2);
			IFolder componentRoot = BenchmarkUtils.createRandomDeepFolder(gen, outProject);
			BenchmarkUtils.createRandomDeepFiles(gen, componentRoot, 12, FILE_SIZE_MEAN, FILE_SIZE_VARIANCE, PROB_BINARY);
			syncCommitResources(source, new IResource[] { outProject }, "");	
			endGroup();
			// Test 1: catching up to a new component - localized additions and some changes
			startGroup(UPDATE1);
			syncUpdateResources(source, new IResource[] { inProject });
			endGroup();
	
			// Test 2: fixing a bug - localized changes
			startGroup(COMMIT2);
			BenchmarkUtils.modifyRandomDeepFiles(gen, componentRoot, 2);
			BenchmarkUtils.touchRandomDeepFiles(gen, componentRoot, 2);
			syncCommitResources(source, new IResource[] { outProject }, "");
			endGroup();
			// Test 2: catching up to a bug fix - localized changes
			startGroup(UPDATE2);
			syncUpdateResources(source, new IResource[] { inProject });
			endGroup();
			
			// Test 3: moving a package - scattered changes, files moved
			startGroup(COMMIT3);
			BenchmarkUtils.modifyRandomDeepFiles(gen, outProject, 5);        // a few scattered changes
			BenchmarkUtils.modifyRandomDeepFiles(gen, componentRoot, 12); // changes to "package" stmt
			BenchmarkUtils.renameResource(componentRoot, BenchmarkUtils.makeUniqueName(gen, "folder", null));
			syncCommitResources(source, new IResource[] { outProject }, "");
			endGroup();
			// Test 3: catching up to a moved package - scattered changes, files moved
			startGroup(UPDATE3);
			syncUpdateResources(source, new IResource[] { inProject });
			endGroup();
			
			// Test 4: big refactoring - scattered changes, files renamed and balanced additions/deletions
			startGroup(COMMIT4);		
			BenchmarkUtils.deleteRandomDeepFiles(gen, outProject, 4);  // some stuff deleted
			BenchmarkUtils.modifyRandomDeepFiles(gen, outProject, 20); // many scattered changes
			BenchmarkUtils.renameRandomDeepFiles(gen, outProject, 5);  // renamed some stuff
			BenchmarkUtils.createRandomDeepFiles(gen, outProject, 4, FILE_SIZE_MEAN, FILE_SIZE_VARIANCE, PROB_BINARY);  // some new stuff added
			syncCommitResources(source, new IResource[] { outProject }, "");
			endGroup();
			// Test 4: catching up to a big refactoring - scattered changes, files renamed and balanced additions/deletions
			startGroup(UPDATE4);		
			syncUpdateResources(source, new IResource[] { inProject });
			endGroup();
	
			// Test 5: test tagging a project
			startGroup(TAG1);
			tagProject(outProject, new CVSTag("v101", CVSTag.VERSION), false);
			endGroup();
	
			// replace with remote contents
			// Test 6: no local dirty files, no remote changes
			startGroup(REPLACE1);
			replace(new IResource[] { inProject }, null, true);
			endGroup();
	
			// Test 7: abandoning some local work, no remote changes
			startGroup(REPLACE2);
			BenchmarkUtils.deleteRandomDeepFiles(gen, inProject, 4); // some stuff locally deleted
			BenchmarkUtils.modifyRandomDeepFiles(gen, inProject, 6); // a few unimportant changes to forget
			BenchmarkUtils.createRandomDeepFiles(gen, inProject, 4, FILE_SIZE_MEAN, FILE_SIZE_VARIANCE, PROB_BINARY); // some new work to abandon
			replace(new IResource[] { inProject }, null, true);
			endGroup();
	
			// Test 8: no local dirty files, many remote changes
			// e.g. returning from a long vacation
			BenchmarkUtils.deleteRandomDeepFiles(gen, outProject, 10); // some components obsoleted
			BenchmarkUtils.modifyRandomDeepFiles(gen, outProject, 42); // many changes
			BenchmarkUtils.renameRandomDeepFiles(gen, outProject, 8);  // evidence of some refactoring
			BenchmarkUtils.createRandomDeepFiles(gen, outProject, 10, FILE_SIZE_MEAN, FILE_SIZE_VARIANCE, PROB_BINARY); // a few new components added
			syncCommitResources(source, new IResource[] { outProject }, "");
			startGroup(REPLACE3);
			replace(new IResource[] { inProject }, null, true);
			endGroup();
		}
		commitGroups(global);
	}
}
