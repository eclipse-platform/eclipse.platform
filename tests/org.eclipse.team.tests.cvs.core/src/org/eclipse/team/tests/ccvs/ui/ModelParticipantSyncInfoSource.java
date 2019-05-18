/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.ui;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.internal.resources.mapping.ShallowContainer;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.diff.provider.Diff;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ccvs.core.CVSCompareSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSMergeSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.CVSWorkspaceSubscriber;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.mappings.CompareSubscriberContext;
import org.eclipse.team.internal.ccvs.ui.mappings.MergeSubscriberContext;
import org.eclipse.team.internal.ccvs.ui.mappings.ModelCompareParticipant;
import org.eclipse.team.internal.ccvs.ui.mappings.ModelMergeParticipant;
import org.eclipse.team.internal.ccvs.ui.mappings.WorkspaceModelParticipant;
import org.eclipse.team.internal.ccvs.ui.mappings.WorkspaceSubscriberContext;
import org.eclipse.team.internal.ccvs.ui.wizards.CommitWizard;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.ModelSynchronizePage;
import org.eclipse.team.internal.ui.mapping.ResourceMarkAsMergedHandler;
import org.eclipse.team.internal.ui.mapping.ResourceMergeHandler;
import org.eclipse.team.internal.ui.synchronize.IRefreshEvent;
import org.eclipse.team.internal.ui.synchronize.IRefreshSubscriberListener;
import org.eclipse.team.internal.ui.synchronize.RefreshModelParticipantJob;
import org.eclipse.team.internal.ui.synchronize.RefreshParticipantJob;
import org.eclipse.team.internal.ui.synchronize.SynchronizeView;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeManager;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipantReference;
import org.eclipse.team.ui.synchronize.ISynchronizeView;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.part.IPage;
import org.junit.Assert;

import junit.framework.AssertionFailedError;

public class ModelParticipantSyncInfoSource extends ParticipantSyncInfoSource {

	public class ZeroDepthContainer extends PlatformObject {
		private IContainer container;
		public ZeroDepthContainer(IContainer container) {
			this.container = container;
		}
		public IContainer getResource() {
			return container;
		}
		@Override
		public boolean equals(Object obj) {
			if (obj == this)
				return true;
			if (obj instanceof ShallowContainer) {
				ZeroDepthContainer other = (ZeroDepthContainer) obj;
				return other.getResource().equals(getResource());
			}
			return false;
		}
		@Override
		public int hashCode() {
			return getResource().hashCode();
		}
		@Override
		public <T> T getAdapter(Class<T> adapter) {
			if (adapter == IResource.class || adapter == IContainer.class)
				return adapter.cast(container);
			return super.getAdapter(adapter);
		}
	}
	
	public class ZeroDepthResourceMapping extends ResourceMapping {
		private final ZeroDepthContainer container;
		public ZeroDepthResourceMapping(ZeroDepthContainer container) {
			this.container = container;
		}
		@Override
		public Object getModelObject() {
			return container;
		}
		@Override
		public String getModelProviderId() {
			return ModelProvider.RESOURCE_MODEL_PROVIDER_ID;
		}
		@Override
		public IProject[] getProjects() {
			return new IProject[] { container.getResource().getProject() };
		}
		@Override
		public ResourceTraversal[] getTraversals(ResourceMappingContext context, IProgressMonitor monitor) {
			return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { container.getResource() }, IResource.DEPTH_ZERO, IResource.NONE)};
		}
		@Override
		public boolean contains(ResourceMapping mapping) {
			if (mapping.getModelProviderId().equals(this.getModelProviderId())) {
				Object object = mapping.getModelObject();
				IResource resource = container.getResource();
				// A shallow mapping only contains direct file children or equal shallow containers
				if (object instanceof ShallowContainer) {
					ZeroDepthContainer sc = (ZeroDepthContainer) object;
					return sc.getResource().equals(resource);
				}
				if (object instanceof IResource) {
					IResource other = (IResource) object;
					return other.equals(resource);
				}
			}
			return false;
		}
	}
	
	public static ModelSynchronizeParticipant getParticipant(Subscriber subscriber) {
		// show the sync view
		ISynchronizeParticipantReference[] participants = TeamUI.getSynchronizeManager().getSynchronizeParticipants();
		for (ISynchronizeParticipantReference participant2 : participants) {
			ISynchronizeParticipant participant;
			try {
				participant = participant2.getParticipant();
			} catch (TeamException e) {
				return null;
			}
			if (participant instanceof ModelSynchronizeParticipant) {
				ModelSynchronizeParticipant msp = (ModelSynchronizeParticipant) participant;
				ISynchronizationContext context = msp.getContext();
				if (context instanceof SubscriberMergeContext) {
					SubscriberMergeContext smc = (SubscriberMergeContext) context;
					if (smc.getSubscriber().equals(subscriber))
							return msp;
				}
			}
		}
		return null;
	}
	
	@Override
	public void assertSyncEquals(String message, Subscriber subscriber, IResource resource, int syncKind) throws CoreException {
		assertDiffKindEquals(message, subscriber, resource, SyncInfoToDiffConverter.asDiffFlags(syncKind));	
	}
	
	@Override
	protected IDiff getDiff(Subscriber subscriber, IResource resource) throws CoreException {
		waitForCollectionToFinish(subscriber);
		IDiff subscriberDiff =  subscriber.getDiff(resource);
		IDiff contextDiff = getContextDiff(subscriber, resource);
		assertDiffsEqual(subscriber, subscriberDiff, contextDiff);
		return contextDiff;
	}
	
	@Override
	public void refresh(Subscriber subscriber, IResource[] resources) throws TeamException {
		RefreshParticipantJob job = new RefreshModelParticipantJob(getParticipant(subscriber), "Refresh", "Refresh", Utils.getResourceMappings(resources), new IRefreshSubscriberListener() {
			@Override
			public void refreshStarted(IRefreshEvent event) {
				// Do nothing
			}
		
			@Override
			public IWorkbenchAction refreshDone(IRefreshEvent event) {
				// Do nothing
				return null;
			}
		});
		job.schedule();
		waitForCollectionToFinish(subscriber);
		assertViewMatchesModel(subscriber);
	}
	
	public void waitForCollectionToFinish(Subscriber subscriber) {
		ModelSynchronizeParticipant family = getParticipant(subscriber);
		if (family == null) {
			while (waitUntilFamilyDone(subscriber)) {
				// just keep looping until we no longer wait for any jobs
			}
		} else {
			while (waitUntilFamilyDone(subscriber) 
					|| waitUntilFamilyDone(family)
					|| waitUntilFamilyDone(family.getContext())
					|| waitUntilFamilyDone(family.getContext().getScope())
					|| waitUntilFamilyDone(((SynchronizationContext)family.getContext()).getScopeManager())) {
				// just keep looping until we no longer wait for any jobs
			}
		}
	}

	private boolean waitUntilFamilyDone(Object family) {
		if (family == null)
			return false;
		Job[] jobs = Job.getJobManager().find(family);
		boolean waited = false;
		for (Job job : jobs) {
			while (job.getState() != Job.NONE) {
				waited = true;
				while (Display.getCurrent().readAndDispatch()) {}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// Ignore
				}
			}
		}
		return waited;
	}

	private void assertDiffsEqual(Subscriber subscriber, IDiff subscriberDiff, IDiff contextDiff) throws CoreException {
		if (subscriberDiff == null && contextDiff == null)
			return;
		if (subscriberDiff == null && contextDiff != null) {
			Assert.fail("Subscriber contains no change for "
					+ ResourceDiffTree.getResourceFor(contextDiff).getFullPath().toString()
					+ " but the context contains: "
					+ contextDiff.toDiffString());
		}
		if (subscriberDiff != null && contextDiff == null) {
			if (subscriber instanceof CVSCompareSubscriber) {
				// The compare context filters out nodes whose contents are equal
				if (localContentsMatchRemote(subscriberDiff))
					return;
			}
			Assert.fail("Subscriber contains change: "
					+ subscriberDiff.toDiffString()
					+ " for "
					+ ResourceDiffTree.getResourceFor(subscriberDiff).getFullPath().toString()
					+ " but the context has no change");
		}
		int subscriberStatus = ((Diff)subscriberDiff).getStatus();
		int contextStatus = ((Diff)subscriberDiff).getStatus();
		if (subscriberStatus != contextStatus) {
			Assert.fail("Subscriber contains change: "
					+ subscriberDiff.toDiffString()
					+ " for "
					+ ResourceDiffTree.getResourceFor(contextDiff).getFullPath().toString()
					+ " but the context contains: "
					+ contextDiff.toDiffString());
		}
	}

	private boolean localContentsMatchRemote(IDiff subscriberDiff) throws CoreException {
		IResource resource = ResourceDiffTree.getResourceFor(subscriberDiff);
		if (resource instanceof IFile) {
			IFile file = (IFile) resource;
			IFileRevision remote = SyncInfoToDiffConverter.getRemote(subscriberDiff);
			return compareContent(file.getContents(), remote.getStorage(DEFAULT_MONITOR).getContents());
		}
		return false;
	}

	/**
	 * Returns a boolean value indicating whether or not the contents
	 * of the given streams are considered to be equal. Closes both input streams.
	 */
	public boolean compareContent(InputStream a, InputStream b) {
		int c, d;
		if (a == null && b == null)
			return true;
		try {
			if (a == null || b == null)
				return false;
			while ((c = a.read()) == (d = b.read()) && (c != -1 && d != -1)) {
				//body not needed
			}
			return (c == -1 && d == -1);
		} catch (IOException e) {
			return false;
		} finally {
			try {
				if (a != null)
					a.close();
			} catch (IOException e) {
				// ignore
			}
			try {
				if (b != null)
					b.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}
	
	private IDiff getContextDiff(Subscriber subscriber, IResource resource) {
		ModelSynchronizeParticipant p = getParticipant(subscriber);
		return p.getContext().getDiffTree().getDiff(resource);
	}

	@Override
	protected SyncInfo getSyncInfo(Subscriber subscriber, IResource resource) throws TeamException {
		try {
			IDiff diff = getDiff(subscriber, resource);
			return getConverter(subscriber).asSyncInfo(diff, subscriber.getResourceComparator());
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}
	
	@Override
	protected void assertProjectRemoved(Subscriber subscriber, IProject project) {
		super.assertProjectRemoved(subscriber, project);
		waitForCollectionToFinish(subscriber);
		ModelSynchronizeParticipant participant = getParticipant(subscriber);
		IResourceDiffTree tree = participant.getContext().getDiffTree();
		if (tree.members(project).length > 0) {
			throw new AssertionFailedError("The diff tree still contains resources from the deleted project " + project.getName());	
		}
	}
	
	private ISynchronizationScopeManager createScopeManager(IResource resource, Subscriber subscriber) {
		return new SubscriberScopeManager(subscriber.getName(), 
				new ResourceMapping[] { Utils.getResourceMapping(resource) }, subscriber, true);
	}
	
	private ISynchronizationScopeManager createWorkspaceScopeManager() {
		CVSWorkspaceSubscriber workspaceSubscriber = CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber();
		try {
			ModelProvider workspaceModel = ModelProvider.getModelProviderDescriptor(ModelProvider.RESOURCE_MODEL_PROVIDER_ID).getModelProvider();
			return new SubscriberScopeManager(workspaceSubscriber.getName(), 
					new ResourceMapping[] { Utils.getResourceMapping(workspaceModel) }, workspaceSubscriber, true);
		} catch (CoreException e) {
			Assert.fail(e.getMessage());
		}
		return null;
	}
	
	@Override
	public CVSMergeSubscriber createMergeSubscriber(IProject project, CVSTag root, CVSTag branch) {
		CVSMergeSubscriber mergeSubscriber = super.createMergeSubscriber(project, root, branch);
		ModelSynchronizeParticipant participant = new ModelMergeParticipant(MergeSubscriberContext.createContext(createScopeManager(project, mergeSubscriber), mergeSubscriber));
		showParticipant(participant);
		return mergeSubscriber;
	}

	@Override
	public Subscriber createWorkspaceSubscriber() throws TeamException {
		ISynchronizeManager synchronizeManager = TeamUI.getSynchronizeManager();
		ISynchronizeParticipantReference[] participants = synchronizeManager.get(WorkspaceModelParticipant.ID);
		if (participants.length > 0) {
			Subscriber subscriber = ((SubscriberMergeContext)((WorkspaceModelParticipant)participants[0].getParticipant()).getContext()).getSubscriber();
			waitForCollectionToFinish(subscriber);
			return subscriber;
		}
		WorkspaceModelParticipant participant = new WorkspaceModelParticipant(WorkspaceSubscriberContext.createContext(createWorkspaceScopeManager(), ISynchronizationContext.THREE_WAY));
		showParticipant(participant);
		Subscriber subscriber = super.createWorkspaceSubscriber();
		refresh(subscriber, subscriber.roots());
		return subscriber;
	}

	@Override
	public CVSCompareSubscriber createCompareSubscriber(IResource resource, CVSTag tag) {
		CVSCompareSubscriber s = super.createCompareSubscriber(resource, tag);
		ModelSynchronizeParticipant participant = new ModelCompareParticipant(CompareSubscriberContext.createContext(createScopeManager(resource, s), s));
		showParticipant(participant);
		return s;
	}
	
	@Override
	public void disposeSubscriber(Subscriber subscriber) {
		ISynchronizeParticipant participant = getParticipant(subscriber);
		ISynchronizeManager synchronizeManager = TeamUI.getSynchronizeManager();
		synchronizeManager.removeSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
	}
	
	@Override
	public void mergeResources(Subscriber subscriber, IResource[] resources, boolean allowOverwrite) throws TeamException {
		// Try a merge first
		internalMergeResources(subscriber, resources, false);
		if (allowOverwrite) {
			internalMergeResources(subscriber, resources, true);
			try {
				assertInSync(subscriber, resources);
			} catch (CoreException e) {
				throw TeamException.asTeamException(e);
			}
		}
	}

	private void assertInSync(Subscriber subscriber, IResource[] resources) throws CoreException {
		waitForCollectionToFinish(subscriber);
		for (IResource resource : resources) {
			assertSyncEquals("merge failed", subscriber, resource, SyncInfo.IN_SYNC);
		}
		
	}

	private void internalMergeResources(Subscriber subscriber, IResource[] resources, final boolean allowOverwrite) throws TeamException {
		ResourceMergeHandler handler = new ResourceMergeHandler(getConfiguration(subscriber), allowOverwrite) {
			@Override
			protected boolean promptToConfirm() {
				return true;
			}
			@Override
			protected void promptForNoChanges() {
				// Do nothing
			}
		};
		handler.updateEnablement(new StructuredSelection(asResourceMappings(resources)));
		try {
			handler.execute(new ExecutionEvent(null, Collections.EMPTY_MAP, null, null));
		} catch (ExecutionException e) {
			throw new TeamException("Error running merge", e);
		}
		waitForCollectionToFinish(subscriber);
	}

	@Override
	public void markAsMerged(Subscriber subscriber, IResource[] resources) throws InvocationTargetException, InterruptedException, TeamException {
		ResourceMarkAsMergedHandler handler = new ResourceMarkAsMergedHandler(getConfiguration(subscriber));
		handler.updateEnablement(new StructuredSelection(resources));
		try {
			handler.execute(new ExecutionEvent(null, Collections.EMPTY_MAP, null, null));
		} catch (ExecutionException e) {
			throw new TeamException("Error running markAsMerged", e);
		}
		waitForCollectionToFinish(subscriber);
	}
	
	@Override
	public void updateResources(Subscriber subscriber, IResource[] resources) throws CoreException {
		mergeResources(subscriber, resources, false);
	}
	
	@Override
	public void overrideAndUpdateResources(Subscriber subscriber, boolean shouldPrompt, IResource[] resources) throws CoreException {
		mergeResources(subscriber, resources, true);
	}
	
	@Override
	public void commitResources(Subscriber subscriber, IResource[] resources) throws CoreException {
		try {
			new CommitWizard.AddAndCommitOperation(null, 
					getChangedFiles(subscriber, resources),
					getNewResources(subscriber, resources), "").run(DEFAULT_MONITOR);
		} catch (InvocationTargetException e) {
			throw CVSException.wrapException(e);
		} catch (InterruptedException e) {
			Assert.fail();
		}
	}

	private IResource[] getChangedFiles(Subscriber subscriber, IResource[] resources) throws CoreException {
		List<IResource> result = new ArrayList<>();
		for (IResource resource : resources) {
			if (resource.getType() == IResource.FILE) {
				IDiff diff = subscriber.getDiff(resource);
				if (diff != null)
					result.add(resource);
			}
		}
		return result.toArray(new IResource[result.size()]);
	}
	
	private IResource[] getNewResources(Subscriber subscriber, IResource[] resources) throws CoreException {
		List<IResource> result = new ArrayList<>();
		for (IResource resource : resources) {
			IDiff diff = subscriber.getDiff(resource);
			if (diff instanceof IThreeWayDiff) {
				IThreeWayDiff twd = (IThreeWayDiff) diff;
				if (twd.getKind() == IDiff.ADD && twd.getDirection() == IThreeWayDiff.OUTGOING) {
					if (!CVSWorkspaceRoot.getCVSResourceFor(resource).isManaged()) {
						result.add(resource);
					}
				}
			}
		}
		return result.toArray(new IResource[result.size()]);
	}

	private ResourceMapping[] asResourceMappings(IResource[] resources) {
		List<ResourceMapping> result = new ArrayList<>();
		for (IResource resource : resources) {
			if (resource.getType() == IResource.FILE) {
				result.add(Utils.getResourceMapping(resource));
			} else {
				result.add(new ZeroDepthResourceMapping(new ZeroDepthContainer((IContainer)resource)));
			}
		}
		return result.toArray(new ResourceMapping[result.size()]);
	}

	@Override
	public void overrideAndCommitResources(Subscriber subscriber, IResource[] resources) throws CoreException {
		try {
			markAsMerged(subscriber, resources);
		} catch (InvocationTargetException e) {
			throw CVSException.wrapException(e);
		} catch (InterruptedException e) {
			Assert.fail("unexpected interrupt");
		}
		commitResources(subscriber, resources);
	}
	
	public ISynchronizePageConfiguration getConfiguration(Subscriber subscriber) {
		ModelSynchronizePage page = getPage(subscriber);
		return page.getConfiguration();
	}

	private ModelSynchronizePage getPage(Subscriber subscriber) {
		try {
			ModelSynchronizeParticipant participant = getParticipant(subscriber);
			if (participant == null)
				throw new AssertionFailedError("The participant for " + subscriber.getName() + " could not be retrieved");
			IWorkbenchPage activePage = TeamUIPlugin.getActivePage();
			ISynchronizeView view = (ISynchronizeView)activePage.showView(ISynchronizeView.VIEW_ID);
			IPage page = ((SynchronizeView)view).getPage(participant);
			if (page instanceof ModelSynchronizePage) {
				ModelSynchronizePage subscriberPage = (ModelSynchronizePage)page;
				return subscriberPage;
			}
		} catch (PartInitException e) {
			throw new AssertionFailedError("Cannot show sync view in active page");
		}
		throw new AssertionFailedError("The page for " + subscriber.getName() + " could not be retrieved");
	}
	
	@Override
	public void assertViewMatchesModel(Subscriber subscriber) {
		waitForCollectionToFinish(subscriber);
		TreeItem[] rootItems = getTreeItems(subscriber);
		ModelSynchronizeParticipant p = getParticipant(subscriber);
		ResourceDiffTree tree = (ResourceDiffTree)p.getContext().getDiffTree();
		ResourceDiffTree copy = new ResourceDiffTree();
		IDiff[] diffs = tree.getDiffs();
		for (IDiff diff : diffs) {
			copy.add(diff);
		}
		assertTreeMatchesDiffs(rootItems, copy);
	}
	
	private void assertTreeMatchesDiffs(TreeItem[] rootItems, ResourceDiffTree copy) {
		assertItemsInDiffTree(rootItems, copy);
		if (!copy.isEmpty()) {
			new AssertionFailedError("Viewer is not showing all diffs");
		}
	}

	private void assertItemsInDiffTree(TreeItem[] items, ResourceDiffTree copy) {
		if (items == null || items.length == 0) {
			return;
		}
		for (TreeItem item : items) {
			assertItemInTree(item, copy);
		}
		
	}

	private void assertItemInTree(TreeItem item, ResourceDiffTree copy) {
		Object element = item.getData();
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			if (copy.getDiff(resource) != null) {
				copy.remove(resource);
			} else if (copy.getChildren(resource.getFullPath()).length == 0) {
				// When running in the suites, we want to avoid intermittent failures so we only flag errors that would result in lost changes
				if (CVSTestSetup.FAIL_ON_BAD_DIFF) {
					throw new AssertionFailedError("Resource" + resource.getFullPath() + " is in the view but not in the diff tree");
				} else {
					System.out.println("Resource" + resource.getFullPath() + " is in the view but not in the diff tree");
					new Exception().printStackTrace();
					return;
				}
				
			}
			assertItemsInDiffTree(item.getItems(), copy);
		}
	}

	private TreeItem[] getTreeItems(Subscriber subscriber) {
		ModelSynchronizePage page = getPage(subscriber);
		Viewer v = page.getViewer();
		if (v instanceof TreeViewer) {
			TreeViewer treeViewer = (TreeViewer)v;
			treeViewer.expandAll();
			Tree t = (treeViewer).getTree();
			return t.getItems();
		}
		throw new AssertionFailedError("The tree for " + subscriber.getName() + " could not be retrieved");
	}
	
}
