/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.subscribers;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.diff.DiffFilter;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.MergeContext;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.core.mapping.GroupProgressMonitor;
import org.eclipse.team.internal.core.subscribers.SubscriberDiffTreeEventHandler;

/**
 * A merge context that uses a subscriber to populate the diff tree
 * used by the context. The population of the diff tree is performed
 * by a handler that runs in a background job.
 *
 * @see Subscriber
 * @see MergeContext
 *
 * @since 3.2
 */
public abstract class SubscriberMergeContext extends MergeContext {

	private Subscriber subscriber;
	private SubscriberDiffTreeEventHandler handler;
	private final ISynchronizationScopeManager manager;

	/**
	 * Create a merge context for the given subscriber
	 * @param subscriber the subscriber
	 * @param manager the scope manager
	 */
	protected SubscriberMergeContext(Subscriber subscriber, ISynchronizationScopeManager manager) {
		super(manager, getType(subscriber), new ResourceDiffTree());
		this.subscriber = subscriber;
		this.manager = manager;
	}

	private static int getType(Subscriber subscriber) {
		return subscriber.getResourceComparator().isThreeWay()
			? THREE_WAY : TWO_WAY;
	}

	/**
	 * Initialize the diff tree of this context. This method must
	 * be called before the context is given to clients.
	 */
	protected void initialize() {
		handler = new SubscriberDiffTreeEventHandler(subscriber, manager, (ResourceDiffTree)getDiffTree(), getDiffFilter());
		handler.setJobFamily(this);
		handler.start();
	}

	/**
	 * Return the diff filter used to filter the differences that the merge context will present to clients.
	 * @return the diff filter used to filter the differences that the merge context will present to clients
	 * @since 3.3
	 */
	protected DiffFilter getDiffFilter() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.ISynchronizationContext#refresh(org.eclipse.core.resources.mapping.ResourceTraversal[], int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void refresh(ResourceTraversal[] traversals, int flags,
			IProgressMonitor monitor) throws CoreException {
		GroupProgressMonitor group = getGroup(monitor);
		if (group != null)
			handler.setProgressGroupHint(group.getGroup(), group.getTicks());
		handler.initializeIfNeeded();
		subscriber.refresh(traversals, monitor);
	}

	private GroupProgressMonitor getGroup(IProgressMonitor monitor) {
		if (monitor instanceof GroupProgressMonitor) {
			return (GroupProgressMonitor) monitor;
		}
		if (monitor instanceof ProgressMonitorWrapper) {
			ProgressMonitorWrapper wrapper = (ProgressMonitorWrapper) monitor;
			return getGroup(wrapper.getWrappedProgressMonitor());
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.mapping.provider.SynchronizationContext#dispose()
	 */
	public void dispose() {
		handler.shutdown();
		super.dispose();
	}

	/**
	 * Return the sync info for the given resource.
	 * @param resource the resource
	 * @return the sync info for the resource obtained from the subscriber
	 * @throws CoreException
	 */
	protected SyncInfo getSyncInfo(IResource resource) throws CoreException {
		return handler.getSubscriber().getSyncInfo(resource);
	}

	/**
	 * Return the subscriber associated with this context.
	 * @return the subscriber associated with this context
	 */
	public Subscriber getSubscriber() {
		return subscriber;
	}

	/**
	 * Run the given runnable when the background handler
	 * for this context is idle. The given runnable should not lock
	 * the workspace.
	 * @param runnable the runnable
	 */
	protected void runInBackground(IWorkspaceRunnable runnable) {
		handler.run(runnable, false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.PlatformObject#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (adapter == SubscriberDiffTreeEventHandler.class)
			return handler;
		return super.getAdapter(adapter);
	}

}
