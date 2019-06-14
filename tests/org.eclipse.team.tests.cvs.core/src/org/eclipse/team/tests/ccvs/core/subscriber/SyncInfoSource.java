/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.subscriber;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.provider.Diff;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ccvs.core.CVSCompareSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSMergeSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.subscriber.CVSSubscriberOperation;
import org.eclipse.team.internal.ccvs.ui.subscriber.ConfirmMergedOperation;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.junit.Assert;

import junit.framework.AssertionFailedError;

/**
 * This class acts as the source for the sync info used by the subscriber tests.
 * The purpose is to allow the sync info to be obtained directly from the subscriber 
 * or through the sync set visible in the sync view.
 */
public class SyncInfoSource {

	protected static IProgressMonitor DEFAULT_MONITOR = new NullProgressMonitor();
	protected List<CVSMergeSubscriber> mergeSubscribers = new ArrayList<>();
	protected List<CVSCompareSubscriber> compareSubscribers = new ArrayList<>();
	
	public CVSMergeSubscriber createMergeSubscriber(IProject project, CVSTag root, CVSTag branch) {
		return createMergeSubscriber(project, root, branch, false /*default*/);
	}

	public CVSMergeSubscriber createMergeSubscriber(IProject project, CVSTag root, CVSTag branch, boolean isModelSync) {
		CVSMergeSubscriber subscriber = new CVSMergeSubscriber(new IResource[] { project }, root, branch, isModelSync);
		mergeSubscribers.add(subscriber);
		return subscriber;
	}
	
	public CVSCompareSubscriber createCompareSubscriber(IResource resource, CVSTag tag) {
		CVSCompareSubscriber subscriber = new CVSCompareSubscriber(new IResource[] { resource }, tag);
		compareSubscribers.add(subscriber);
		return subscriber;
	}
	
	public void disposeSubscriber(Subscriber subscriber) {
		// Nothing to do
	}
	
	/**
	 * @throws TeamException  
	 */
	public Subscriber createWorkspaceSubscriber() throws TeamException {
		return CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber();
	}
	
	/**
	 * Return the sync info for the given subscriber for the given resource.
	 */
	protected SyncInfo getSyncInfo(Subscriber subscriber, IResource resource) throws TeamException {
		return subscriber.getSyncInfo(resource);
	}
	
	/**
	 * Return the diff for the given subscriber for the given resource.
	 */
	protected IDiff getDiff(Subscriber subscriber, IResource resource) throws CoreException {
		return subscriber.getDiff(resource);
	}
	
	/**
	 * Refresh the subscriber for the given resource
	 */
	public void refresh(Subscriber subscriber, IResource resource) throws TeamException {
		refresh(subscriber, new IResource[] { resource});
	}
	
	/**
	 * Refresh the subscriber for the given resources
	 */
	public void refresh(Subscriber subscriber, IResource[] resources) throws TeamException {
		subscriber.refresh(resources, IResource.DEPTH_INFINITE, DEFAULT_MONITOR);
	}
	
	protected void assertProjectRemoved(Subscriber subscriber, IProject project) {
		IResource[] roots = subscriber.roots();
		for (IResource resource : roots) {
			if (resource.equals(project)) {
				throw new AssertionFailedError();
			}
		}
	}

	public void tearDown() {
		for (CVSMergeSubscriber s : mergeSubscribers) {
			s.cancel();
		}
	}
	
	public void assertSyncEquals(String message, Subscriber subscriber, IResource resource, int syncKind) throws CoreException {
		int conflictTypeMask = 0x0F; // ignore manual and auto merge sync types for now.
		SyncInfo info = getSyncInfo(subscriber, resource);
		int kind;
		int kindOther = syncKind & conflictTypeMask;
		if (info == null) {
			kind = SyncInfo.IN_SYNC;
		} else {
			kind = info.getKind() & conflictTypeMask;
		}
		// Special handling for folders
		if (kind != kindOther && resource.getType() == IResource.FOLDER) {
			// The only two states for folders are outgoing addition and in-sync.
			// Other additions will appear as in-sync
			if (info.getKind() == SyncInfo.IN_SYNC 
					&& (syncKind & SyncInfo.ADDITION) != 0) {
				return;
			}
		} else {
			// Only test if kinds are equal
			assertDiffKindEquals(message, subscriber, resource, SyncInfoToDiffConverter.asDiffFlags(syncKind));
		}
		Assert.assertTrue(message + ": improper sync state for " + resource + " expected " + 
					SyncInfo.kindToString(kindOther) + " but was " +
					SyncInfo.kindToString(kind), kind == kindOther);
		
	}
	
	protected void assertDiffKindEquals(String message, Subscriber subscriber, IResource resource, int expectedFlags) throws CoreException {
		int actualFlags = getActualDiffFlags(subscriber, resource);
		boolean result = compareFlags(resource, actualFlags, expectedFlags);
		int count = 0;
		while (!result && count < 40) {
			// The discrepancy may be due to a timing issue. 
			// Let's wait a few seconds and get the flags again.
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// Ignore
			}
			actualFlags = getActualDiffFlags(subscriber, resource);
			result = compareFlags(resource, actualFlags, expectedFlags);
			if (result) {
				System.out.println("A timing issue has been detected in the CVS test");
				new Exception().printStackTrace();
			} else {
				count++;
			}
		}
		String errorString = message + ": improper diff for " + resource
				+ " expected "
				+ SyncInfoToDiffConverter.diffStatusToString(expectedFlags)
				+ " but was "
				+ SyncInfoToDiffConverter.diffStatusToString(actualFlags);
		if (CVSTestSetup.FAIL_ON_BAD_DIFF 
				|| (expectedFlags != IDiff.NO_CHANGE && actualFlags == IDiff.NO_CHANGE)) {
			// When running in the suites, we want to avoid intermittent failures.
			// However, still fail if we expected a change but we get no change since that can
			// cause work to be lost
			Assert.assertTrue(errorString, result);
		} else if (!result) {
			System.out.println(errorString);
			new Exception().printStackTrace();
		}
	}
	
	private boolean compareFlags(IResource resource, int actualFlags, int expectedFlags) {
		// Special handling for folders
		if (actualFlags != expectedFlags && resource.getType() == IResource.FOLDER) {
			// The only two states for folders are outgoing addition and in-sync.
			// Other additions will appear as in-sync
			int expectedKind = expectedFlags & Diff.KIND_MASK;
			int actualKind = actualFlags & Diff.KIND_MASK;
			if (actualKind == IDiff.NO_CHANGE 
					&& expectedKind == IDiff.ADD) {
				return true;
			}
		}
		return actualFlags == expectedFlags;
	}

	private int getActualDiffFlags(Subscriber subscriber, IResource resource)
			throws CoreException {
		IDiff node = getDiff(subscriber, resource);
		int actualFlags;
		if (node == null) {
			actualFlags = IDiff.NO_CHANGE;
		} else {
			actualFlags = ((Diff)node).getStatus();
		}
		return actualFlags;
	}
	
	public void mergeResources(Subscriber subscriber, IResource[] resources, boolean allowOverwrite) throws TeamException, InvocationTargetException, InterruptedException {
		SyncInfo[] infos = createSyncInfos(subscriber, resources);
		mergeResources(subscriber, infos, allowOverwrite);
	}
	
	private void mergeResources(Subscriber subscriber, SyncInfo[] infos, boolean allowOverwrite) throws InvocationTargetException, InterruptedException {
		TestMergeUpdateOperation action = new TestMergeUpdateOperation(getElements(infos), allowOverwrite);
		action.run(DEFAULT_MONITOR);
	}
	
	protected SyncInfo[] createSyncInfos(Subscriber subscriber, IResource[] resources) throws TeamException {
		SyncInfo[] result = new SyncInfo[resources.length];
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			result[i] = getSyncInfo(subscriber, resource);
		}
		return result;
	}
	
	public void markAsMerged(Subscriber subscriber, IResource[] resources) throws InvocationTargetException, InterruptedException, TeamException {
		SyncInfo[] infos = createSyncInfos(subscriber, resources);
		new ConfirmMergedOperation(null, getElements(infos)).run(DEFAULT_MONITOR);
	}
	

	protected IDiffElement[] getElements(SyncInfo[] infos) {
		SyncInfoModelElement[] elements = new SyncInfoModelElement[infos.length];
		for (int i = 0; i < elements.length; i++) {
			elements[i] = new SyncInfoModelElement(null, infos[i]);
		}
		return elements;
	}
	
	public void updateResources(Subscriber subscriber, IResource[] resources) throws CoreException {
		runSubscriberOperation(new TestUpdateOperation(getElements(createSyncInfos(subscriber, resources))));
	}
	
	public void commitResources(Subscriber subscriber, IResource[] resources) throws CoreException {
		runSubscriberOperation(new TestCommitOperation(getElements(createSyncInfos(subscriber, resources)), false /* override */));
	}
	
	public void overrideAndUpdateResources(Subscriber subscriber, boolean shouldPrompt, IResource[] resources) throws CoreException {
		TestOverrideAndUpdateOperation action = new TestOverrideAndUpdateOperation(getElements(createSyncInfos(subscriber, resources)));
		runSubscriberOperation(action);
		Assert.assertTrue(shouldPrompt == action.isPrompted());
	}
	
	public void overrideAndCommitResources(Subscriber subscriber, IResource[] resources) throws CoreException {
		TestCommitOperation action = new TestCommitOperation(getElements(createSyncInfos(subscriber, resources)), true /* override */);
		runSubscriberOperation(action);
	}
	
	private void runSubscriberOperation(CVSSubscriberOperation op) throws CoreException {
		try {
			op.run();
		} catch (InvocationTargetException e) {
			throw CVSException.wrapException(e);
		} catch (InterruptedException e) {
			Assert.fail("Operation was interrupted");
		}
	}

}
