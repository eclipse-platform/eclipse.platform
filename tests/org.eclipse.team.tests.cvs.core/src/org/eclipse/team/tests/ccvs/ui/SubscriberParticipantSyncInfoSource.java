/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.ccvs.core.CVSCompareSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSMergeSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.subscriber.CompareParticipant;
import org.eclipse.team.internal.ccvs.ui.subscriber.MergeSynchronizeParticipant;
import org.eclipse.team.internal.ccvs.ui.subscriber.WorkspaceSynchronizeParticipant;
import org.eclipse.team.internal.core.subscribers.SubscriberSyncInfoCollector;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.synchronize.AbstractSynchronizeModelProvider;
import org.eclipse.team.internal.ui.synchronize.SubscriberParticipantPage;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.team.internal.ui.synchronize.SynchronizeModelManager;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.internal.ui.synchronize.SynchronizeView;
import org.eclipse.team.internal.ui.synchronize.TreeViewerAdvisor;
import org.eclipse.team.tests.ccvs.core.EclipseTest;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantReference;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.team.ui.synchronize.SubscriberParticipant;
import org.eclipse.team.ui.synchronize.WorkspaceScope;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPage;

import junit.framework.AssertionFailedError;

/**
 * SyncInfoSource that obtains SyncInfo from the SynchronizeView's SyncSet.
 */
public class SubscriberParticipantSyncInfoSource extends ParticipantSyncInfoSource {

	public SubscriberParticipantSyncInfoSource() {
		super();
	}
	
	@Override
	public SyncInfo getSyncInfo(Subscriber subscriber, IResource resource) throws TeamException {
		// Wait for the collector
		getCollector(subscriber);
		// Obtain the sync info from the viewer to ensure that the 
		// entire chain has the proper state
		SyncInfo info = internalGetSyncInfo(subscriber, resource);
		// Do a sanity check on the collected sync info
		if (info == null) {
			info = subscriber.getSyncInfo(resource);
			if ((info != null && info.getKind() != SyncInfo.IN_SYNC)) {
				throw new AssertionFailedError(
						"Sync state for " 
						+ resource.getFullPath() 
						+ " was "
						+ SyncInfo.kindToString(info.getKind())
						+ " but resource was not collected");
			}
		} else {
			SyncInfo realInfo = subscriber.getSyncInfo(resource);
			if (info.getKind() != realInfo.getKind()) {
				throw new AssertionFailedError(
						"Collected sync state for " 
						+ resource.getFullPath() 
						+ " was "
						+ SyncInfo.kindToString(info.getKind())
						+ " but the real state was "
						+ SyncInfo.kindToString(realInfo.getKind()));
			}
		}
		return info;
	}
	
	@Override
	public IDiff getDiff(Subscriber subscriber, IResource resource) throws CoreException {
		SyncInfo info = getSyncInfo(subscriber, resource);
		if (info == null || info.getKind() == SyncInfo.IN_SYNC) {
			return null;
		}
		return getConverter(subscriber).getDeltaFor(info);
	}

	public static SubscriberParticipant getParticipant(Subscriber subscriber) {
		// show the sync view
		ISynchronizeParticipantReference[] participants = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
		for (ISynchronizeParticipantReference participant2 : participants) {
			ISynchronizeParticipant participant;
			try {
				participant = participant2.getParticipant();
			} catch (TeamException e) {
				return null;
			}
			if(participant instanceof SubscriberParticipant) {
				if(((SubscriberParticipant)participant).getSubscriber() == subscriber) {
					return (SubscriberParticipant)participant;
				}
			}
		}
		return null;
	}
	
	public static SubscriberSyncInfoCollector getCollector(Subscriber subscriber) {
		SubscriberParticipant participant = getParticipant(subscriber);
		if (participant == null) return null;
		SubscriberSyncInfoCollector syncInfoCollector = participant.getSubscriberSyncInfoCollector();
		EclipseTest.waitForSubscriberInputHandling(syncInfoCollector);
		SubscriberParticipantPage page = getPage(subscriber);
		SynchronizeModelManager manager = (SynchronizeModelManager)page.getConfiguration().getProperty(SynchronizePageConfiguration.P_MODEL_MANAGER);
		AbstractSynchronizeModelProvider provider = (AbstractSynchronizeModelProvider)manager.getActiveModelProvider();
		provider.waitUntilDone(new IProgressMonitor() {
			@Override
			public void beginTask(String name, int totalWork) {
			}
			@Override
			public void done() {
			}
			@Override
			public void internalWorked(double work) {
			}
			@Override
			public boolean isCanceled() {
				return false;
			}
			@Override
			public void setCanceled(boolean value) {
			}
			@Override
			public void setTaskName(String name) {
			}
			@Override
			public void subTask(String name) {
			}
			@Override
			public void worked(int work) {
				while (Display.getCurrent().readAndDispatch()) {}
			}
		});
		return syncInfoCollector;
	}

	@Override
	protected void assertProjectRemoved(Subscriber subscriber, IProject project) {		
		super.assertProjectRemoved(subscriber, project);
		SyncInfoTree set = getCollector(subscriber).getSyncInfoSet();
		if (set.hasMembers(project)) {
			throw new AssertionFailedError("The sync set still contains resources from the deleted project " + project.getName());	
		}
	}

	@Override
	public CVSMergeSubscriber createMergeSubscriber(IProject project, CVSTag root, CVSTag branch, boolean isModelSync) {
		CVSMergeSubscriber mergeSubscriber = super.createMergeSubscriber(project, root, branch, isModelSync);
		SubscriberParticipant participant = new MergeSynchronizeParticipant(mergeSubscriber);
		showParticipant(participant);
		return mergeSubscriber;
	}
	
	@Override
	public Subscriber createWorkspaceSubscriber() throws TeamException {
		ISynchronizeManager synchronizeManager = TeamUI.getSynchronizeManager();
		ISynchronizeParticipantReference[] participants = synchronizeManager.get(WorkspaceSynchronizeParticipant.ID);
		if (participants.length > 0) {
			return ((SubscriberParticipant)participants[0].getParticipant()).getSubscriber();
		}
		SubscriberParticipant participant = new WorkspaceSynchronizeParticipant(new WorkspaceScope());
		showParticipant(participant);
		return participant.getSubscriber();
	}
	
	
	@Override
	public CVSCompareSubscriber createCompareSubscriber(IResource resource, CVSTag tag) {
		CVSCompareSubscriber s = super.createCompareSubscriber(resource, tag);
		SubscriberParticipant participant = new CompareParticipant(s);
		showParticipant(participant);
		return s;
	}
	
	private SyncInfo internalGetSyncInfo(Subscriber subscriber, IResource resource) {
		ISynchronizeModelElement root = getModelRoot(subscriber);
		return findSyncInfo(root, resource);
	}
	
	private SyncInfo findSyncInfo(ISynchronizeModelElement node, IResource resource) {
		if (node instanceof SyncInfoModelElement) {
			SyncInfoModelElement element = (SyncInfoModelElement)node;
			if (element.getResource().equals(resource)) {
				return element.getSyncInfo();
			}
		}
		IDiffElement[] children = node.getChildren();
		for (IDiffElement element : children) {
			ISynchronizeModelElement child = (ISynchronizeModelElement)element;
			SyncInfo info = findSyncInfo(child, resource);
			if (info != null)
				return info;
		}
		return null;
	}
	
	@Override
	public void assertViewMatchesModel(Subscriber subscriber) {
		// Getting the collector waits for the subscriber input handlers
		getCollector(subscriber);
		ISynchronizeModelElement root = getModelRoot(subscriber);
		TreeItem[] rootItems = getTreeItems(subscriber);
		assertMatchingTrees(root, rootItems, root.getChildren());
	}

	private ISynchronizeModelElement getModelRoot(Subscriber subscriber) {
		SubscriberParticipantPage page = getPage(subscriber);
		return ((TreeViewerAdvisor)page.getViewerAdvisor()).getModelManager().getModelRoot();
	}
	
	private TreeItem[] getTreeItems(Subscriber subscriber) {
		SubscriberParticipantPage page = getPage(subscriber);
		Viewer v = page.getViewer();
		if (v instanceof TreeViewer) {
			TreeViewer treeViewer = (TreeViewer)v;
			treeViewer.expandAll();
			Tree t = (treeViewer).getTree();
			return t.getItems();
		}
		throw new AssertionFailedError("The tree for " + subscriber.getName() + " could not be retrieved");
	}

	private static SubscriberParticipantPage getPage(Subscriber subscriber) {
		try {
			SubscriberParticipant participant = getParticipant(subscriber);
			if (participant == null)
				throw new AssertionFailedError("The participant for " + subscriber.getName() + " could not be retrieved");
			IWorkbenchPage activePage = TeamUIPlugin.getActivePage();
			ISynchronizeView view = (ISynchronizeView)activePage.showView(ISynchronizeView.VIEW_ID);
			IPage page = ((SynchronizeView)view).getPage(participant);
			if (page instanceof SubscriberParticipantPage) {
				SubscriberParticipantPage subscriberPage = (SubscriberParticipantPage)page;
				return subscriberPage;
			}
		} catch (PartInitException e) {
			throw new AssertionFailedError("Cannot show sync view in active page");
		}
		throw new AssertionFailedError("The page for " + subscriber.getName() + " could not be retrieved");
	}
	
	private void assertMatchingTrees(IDiffElement parent, TreeItem[] items, IDiffElement[] children) {
		if ((items == null || items.length == 0) && (children == null || children.length == 0)) {
			// No children in either case so just return
			return;
		}
		if (items == null || children == null || items.length != children.length) {
			throw new AssertionFailedError("The number of children of " + parent.getName() + " is " + 
					(children == null ? 0: children.length) + " but the view has " + 
					(items == null ? 0 : items.length));
		}
		for (IDiffElement element : children) {
			TreeItem foundItem = null;
			for (TreeItem item : items) {
				if (item.getData() == element) {
					foundItem = item;
					break;
				}
			}
			if (foundItem == null) {
				throw new AssertionFailedError("Element" + element.getName() + " is in the model but not in the view");
			} else {
				assertMatchingTrees(element, foundItem.getItems(), ((IDiffContainer)element).getChildren());
			}
		}
		
	}
}
