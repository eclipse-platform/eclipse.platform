/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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

import junit.framework.Test;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.team.internal.ccvs.core.CVSMergeSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.tests.ccvs.core.subscriber.SyncInfoSource;
import org.eclipse.team.tests.ccvs.ui.SubscriberParticipantSyncInfoSource;

/**
 * The test performed by this class is used to compare the performance of the
 * merge with and without the fix introduced in bug 315694.
 * <p>
 * When {@link CVSMergeSubscriber} is created with <code>isModelSync</code> flag
 * set to <code>true</code>, which takes place when <code>MODEL_MERGE</code>
 * group is started, the subscriber will *not* ignore outgoing changes during a
 * merge. For more details see bug 315694.
 */
public class MergeTests extends BenchmarkTest {

	private static final String NON_MODEL_MERGE = "NonModelMerge";
	private static final String MODEL_MERGE = "ModelMerge";

	private static final String[] PERFORMANCE_GROUPS = new String[] { NON_MODEL_MERGE, MODEL_MERGE };

	private static final int FILE_SIZE_MEAN = 16384;
	private static final int FILE_SIZE_VARIANCE = 0;
	private static final int PROB_BINARY = 0;

	public MergeTests() {
		super();
	}

	public MergeTests(String name) {
		super(name);
	}

	public static Test suite() {
		return suite(MergeTests.class);
	}

	protected IProject setupProject() throws Exception {
		IProject project = createUniqueProject(BenchmarkTestSetup.SMALL_ZIP_FILE);
		shareProject(project);
		return project;
	}

	public void testCompareMerges() throws Exception {
		openEmptyPerspective();
		setupGroups(PERFORMANCE_GROUPS, "Merge Tests", false);
		System.out.println("Loop: " + BenchmarkTestSetup.LOOP_COUNT);
		for (int i = 0; i < BenchmarkTestSetup.LOOP_COUNT; i++) {
			final SequenceGenerator gen = new SequenceGenerator();

			IProject headProject = setupProject();
			CVSTag root = new CVSTag("Root_branch", CVSTag.BRANCH);
			CVSTag branch = new CVSTag("branch", CVSTag.BRANCH);
			makeBranch(new IResource[] {headProject}, root, branch, false);
			IProject branchProject = checkoutCopy(headProject, branch);

			SyncInfoSource source = new SubscriberParticipantSyncInfoSource();
			int size = 50;
			BenchmarkUtils.deleteRandomDeepFiles(gen, branchProject, size);
			BenchmarkUtils.modifyRandomDeepFiles(gen, branchProject, size);
			IResource[] newResources = BenchmarkUtils.createRandomDeepFiles(gen, branchProject, size, FILE_SIZE_MEAN, FILE_SIZE_VARIANCE, PROB_BINARY);
			addResources(newResources);
			syncCommitResources(source, new IResource[] { branchProject }, "");

			BenchmarkUtils.deleteRandomDeepFiles(gen, headProject, size);
			BenchmarkUtils.modifyRandomDeepFiles(gen, headProject, size);
			BenchmarkUtils.createRandomDeepFiles(gen, headProject, size, FILE_SIZE_MEAN, FILE_SIZE_VARIANCE, PROB_BINARY);

			startGroup(NON_MODEL_MERGE);
			CVSMergeSubscriber subscriber = source.createMergeSubscriber(headProject, CVSTag.DEFAULT, branch, false);
			source.refresh(subscriber, headProject);
			endGroup();

			startGroup(MODEL_MERGE);
			subscriber = source.createMergeSubscriber(headProject, CVSTag.DEFAULT, branch, true);
			source.refresh(subscriber, headProject);
			endGroup();

			System.out.println(i + 1);
		}
		commitGroups(false);
	}
}
