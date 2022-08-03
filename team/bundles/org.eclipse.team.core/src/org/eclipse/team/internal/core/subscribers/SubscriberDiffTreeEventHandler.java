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
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.TeamStatus;
import org.eclipse.team.core.diff.DiffFilter;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * A subscriber event handler whose output is a diff tree
 */
public class SubscriberDiffTreeEventHandler extends SubscriberEventHandler {

	// State constants for the event handler
	private static final int STATE_NEW = 0;
	public static final int STATE_STARTED = 1;
	private static final int STATE_OK_TO_INITIALIZE = 3;
	private static final int STATE_COLLECTING_CHANGES = 5;
	private static final int STATE_SHUTDOWN = 8;

	// state constants for exceptions
	private static final int EXCEPTION_NONE = 0;
	private static final int EXCEPTION_CANCELED = 1;
	private static final int EXCEPTION_ERROR = 2;

	private ResourceDiffTree tree;
	private SubscriberDiffCollector collector;
	private ISynchronizationScopeManager manager;
	private Object family;
	private DiffFilter filter;
	private int state = STATE_NEW;
	private int exceptionState = EXCEPTION_NONE;

	/*
	 * An event used to represent a change in a diff
	 */
	private class SubscriberDiffChangedEvent extends SubscriberEvent {
		private final IDiff node;

		public SubscriberDiffChangedEvent(IResource resource, int type, int depth, IDiff node) {
			super(resource, type, depth);
			this.node = node;
		}
		public IDiff getChangedNode() {
			return node;
		}
	}

	/*
	 * Collects resource and subscriber changes
	 */
	private class SubscriberDiffCollector extends SubscriberResourceCollector {

		public SubscriberDiffCollector(Subscriber subscriber) {
			super(subscriber);
		}

		@Override
		protected boolean hasMembers(IResource resource) {
			return tree.members(resource).length > 0;
		}

		@Override
		protected void remove(IResource resource) {
			SubscriberDiffTreeEventHandler.this.remove(resource);
		}

		@Override
		protected void change(IResource resource, int depth) {
			SubscriberDiffTreeEventHandler.this.change(resource, depth);
		}
	}

	/**
	 * Create the handler
	 * @param subscriber the subscriber for the handler
	 * @param manager the scope of the handler
	 * @param tree the tree to be populated by this handler
	 * @param filter a filter
	 */
	public SubscriberDiffTreeEventHandler(Subscriber subscriber, ISynchronizationScopeManager manager, ResourceDiffTree tree, DiffFilter filter) {
		super(subscriber, manager.getScope());
		this.manager = manager;
		this.tree = tree;
		this.collector = new SubscriberDiffCollector(subscriber);
		this.filter = filter;
	}

	@Override
	protected void reset(ResourceTraversal[] traversals, int type) {
		// Reset the exception state since we are reseting
		exceptionState = EXCEPTION_NONE;
		if (!manager.isInitialized() && state == STATE_OK_TO_INITIALIZE) {
			// This means the scope has not been initialized
			queueEvent(new RunnableEvent(monitor -> {
				// Only initialize the scope if we are in the STARTED state
				if (state == STATE_OK_TO_INITIALIZE) {
					try {
						prepareScope(monitor);
						state = STATE_COLLECTING_CHANGES;
					} finally {
						// If the initialization didn't complete,
						// return to the STARTED state.
						if (state != STATE_COLLECTING_CHANGES) {
							state = STATE_STARTED;
							if (exceptionState == EXCEPTION_NONE)
								exceptionState = EXCEPTION_CANCELED;
						}
					}
				}
			}, true), true);
		} else if (manager.isInitialized()) {
			state = STATE_COLLECTING_CHANGES;
			super.reset(traversals, type);
		}
	}

	public void reset(){
		reset(getScope().getTraversals(),
				SubscriberEventHandler.SubscriberEvent.INITIALIZE);
	}

	protected void prepareScope(IProgressMonitor monitor) {
		try {
			manager.initialize(monitor);
		} catch (CoreException e) {
			handleException(e);
		}
		ResourceTraversal[] traversals = manager.getScope().getTraversals();
		if (traversals.length > 0)
			reset(traversals, SubscriberEvent.INITIALIZE);
	}

	@Override
	protected void handleChange(IResource resource) throws CoreException {
		IDiff node = getSubscriber().getDiff(resource);
		if (node == null) {
			queueDispatchEvent(
				new SubscriberEvent(resource, SubscriberEvent.REMOVAL, IResource.DEPTH_ZERO));
		} else {
			if (isInScope(resource))
				queueDispatchEvent(
					new SubscriberDiffChangedEvent(resource, SubscriberEvent.CHANGE, IResource.DEPTH_ZERO, node));
		}
	}

	private boolean isInScope(IResource resource) {
		return manager.getScope().contains(resource);
	}

	@Override
	protected void collectAll(IResource resource, int depth,
			final IProgressMonitor monitor) {
		Policy.checkCanceled(monitor);
		monitor.beginTask(null, IProgressMonitor.UNKNOWN);
		ResourceTraversal[] traversals = new ResourceTraversal[] { new ResourceTraversal(new IResource[] { resource }, depth, IResource.NONE) };
		try {
			getSubscriber().accept(traversals, diff -> {
				Policy.checkCanceled(monitor);
				monitor.subTask(NLS.bind(Messages.SubscriberDiffTreeEventHandler_0, tree.getResource(diff).getFullPath().toString()));
				// Queue up any found diffs for inclusion into the output tree
				queueDispatchEvent(
						new SubscriberDiffChangedEvent(tree.getResource(diff), SubscriberEvent.CHANGE, IResource.DEPTH_ZERO, diff));
				// Handle any pending dispatches
				handlePreemptiveEvents(monitor);
				handlePendingDispatch(monitor);
				return true;
			});
		} catch (CoreException e) {
			if (resource.getProject().isAccessible())
				handleException(e, resource, ITeamStatus.SYNC_INFO_SET_ERROR, e.getMessage());
		} finally {
			monitor.done();
		}
	}

	@Override
	protected void dispatchEvents(SubscriberEvent[] events,
			IProgressMonitor monitor) {
		try {
			tree.beginInput();
			for (SubscriberEvent event : events) {
				switch (event.getType()) {
					case SubscriberEvent.CHANGE :
						if (event instanceof SubscriberDiffChangedEvent) {
							SubscriberDiffChangedEvent se = (SubscriberDiffChangedEvent) event;
							IDiff changedNode = se.getChangedNode();
							if (changedNode.getKind() == IDiff.NO_CHANGE) {
								tree.remove(changedNode.getPath());
							} else {
								addDiff(changedNode, monitor);
							}
						}
						break;
					case SubscriberEvent.REMOVAL:
						IDiff[] nodesToRemove = tree.getDiffs(new ResourceTraversal[] { event.asTraversal() });
						for (IDiff node : nodesToRemove) {
							tree.remove(node.getPath());
						}
						break;
				}
			}
		} finally {
			tree.endInput(monitor);
		}
	}

	private void addDiff(IDiff diff, IProgressMonitor monitor) {
		if (filter == null || filter.select(diff, monitor)) {
			tree.add(diff);
		} else {
			tree.remove(diff.getPath());
		}
	}

	/**
	 * Return the resource diff tree that contains the out-of-sync diffs for the
	 * subscriber.
	 * @return the resource diff tree
	 */
	public IResourceDiffTree getTree() {
		return tree;
	}

	@Override
	public Subscriber getSubscriber() {
		return super.getSubscriber();
	}

	@Override
	public void shutdown() {
		state = STATE_SHUTDOWN;
		collector.dispose();
		super.shutdown();
	}

	@Override
	protected Object getJobFamiliy() {
		return family;
	}

	/**
	 * Set the family of this handler to the given object
	 * @param family the family of the handler's job
	 */
	public void setJobFamily(Object family) {
		this.family = family;
	}

	@Override
	protected void handleException(CoreException e, IResource resource, int code, String message) {
		super.handleException(e, resource, code, message);
		tree.reportError(new TeamStatus(IStatus.ERROR, TeamPlugin.ID, code, message, e, resource));
		exceptionState = EXCEPTION_ERROR;
	}

	@Override
	protected void handleCancel(OperationCanceledException e) {
		super.handleCancel(e);
		tree.reportError(new TeamStatus(IStatus.ERROR, TeamPlugin.ID, ITeamStatus.SYNC_INFO_SET_CANCELLATION, Messages.SubscriberEventHandler_12, e, ResourcesPlugin.getWorkspace().getRoot()));
		if (exceptionState == EXCEPTION_NONE)
			exceptionState = EXCEPTION_CANCELED;
	}

	public DiffFilter getFilter() {
		return filter;
	}

	public void setFilter(DiffFilter filter) {
		this.filter = filter;
	}

	/**
	 * If the handler is not initialized or not in the process
	 * of initializing, start the initialization process.
	 */
	public synchronized void initializeIfNeeded() {
		if (state == STATE_STARTED) {
			state = STATE_OK_TO_INITIALIZE;
			reset(getScope().getTraversals(), SubscriberEvent.INITIALIZE);
		} else if (exceptionState != EXCEPTION_NONE) {
			reset(getScope().getTraversals(), SubscriberEvent.INITIALIZE);
		}
	}

	@Override
	public synchronized void start() {
		super.start();
		if (state == STATE_NEW)
			state = STATE_STARTED;
	}

	public int getState() {
		return state;
	}

	@Override
	protected boolean isSystemJob() {
		if (manager != null && !manager.isInitialized())
			return false;
		return super.isSystemJob();
	}

	@Override
	public synchronized void remove(IResource resource) {
		// Don't queue changes if we haven't been initialized
		if (state == STATE_STARTED)
			return;
		super.remove(resource);
	}

	@Override
	public void change(IResource resource, int depth) {
		// Don't queue changes if we haven't been initialized
		if (state == STATE_STARTED)
			return;
		super.change(resource, depth);
	}
}
