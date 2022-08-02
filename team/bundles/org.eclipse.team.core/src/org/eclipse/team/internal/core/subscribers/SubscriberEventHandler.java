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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeChangeListener;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.core.BackgroundEventHandler;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.Policy;

/**
 * This handler collects changes and removals to resources and calculates their
 * synchronization state in a background job. The result is fed input the SyncSetInput.
 *
 * Exceptions that occur when the job is processing the events are collected and
 * returned as part of the Job's status.
 */
public abstract class SubscriberEventHandler extends BackgroundEventHandler {

	// Changes accumulated by the event handler
	private List<Event> resultCache = new ArrayList<>();

	private boolean started = false;
	private boolean initializing = true;

	private IProgressMonitor progressGroup;

	private int ticks;

	private final Subscriber subscriber;
	private ISynchronizationScope scope;

	private ISynchronizationScopeChangeListener scopeChangeListener;

	/**
	 * Internal resource synchronization event. Can contain a result.
	 */
	class SubscriberEvent extends ResourceEvent{
		static final int REMOVAL = 1;
		static final int CHANGE = 2;
		static final int INITIALIZE = 3;

		SubscriberEvent(IResource resource, int type, int depth) {
			super(resource, type, depth);
		}
		@Override
		protected String getTypeString() {
			switch (getType()) {
				case REMOVAL :
					return "REMOVAL"; //$NON-NLS-1$
				case CHANGE :
					return "CHANGE"; //$NON-NLS-1$
				case INITIALIZE :
					return "INITIALIZE"; //$NON-NLS-1$
				default :
					return "INVALID"; //$NON-NLS-1$
			}
		}
		public ResourceTraversal asTraversal() {
			return new ResourceTraversal(new IResource[] { getResource() }, getDepth(), IResource.NONE);
		}
	}

	/**
	 * Create a handler. This will initialize all resources for the subscriber associated with
	 * the set.
	 * @param subscriber the subscriber
	 * @param scope the scope
	 */
	public SubscriberEventHandler(Subscriber subscriber, ISynchronizationScope scope) {
		super(
			NLS.bind(Messages.SubscriberEventHandler_jobName, new String[] { subscriber.getName() }),
			NLS.bind(Messages.SubscriberEventHandler_errors, new String[] { subscriber.getName() }));
		this.subscriber = subscriber;
		this.scope = scope;
		scopeChangeListener = (scope1, newMappings, newTraversals) -> reset(new ResourceTraversal[0], scope1.getTraversals());
		this.scope.addScopeChangeListener(scopeChangeListener);
	}

	/**
	 * The traversals of the scope have changed
	 * @param oldTraversals the old traversals
	 * @param newTraversals the new traversals
	 */
	protected synchronized void reset(ResourceTraversal[] oldTraversals, ResourceTraversal[] newTraversals) {
		reset(newTraversals, SubscriberEvent.CHANGE);
	}

	/**
	 * Start the event handler by queuing events to prime the sync set input with the out-of-sync
	 * resources of the subscriber.
	 */
	public synchronized void start() {
		// Set the started flag to enable event queuing.
		// We are guaranteed to be the first since this method is synchronized.
		started = true;
		ResourceTraversal[] traversals = scope.getTraversals();
		reset(traversals, SubscriberEvent.INITIALIZE);
		initializing = false;
	}

	@Override
	protected synchronized void queueEvent(Event event, boolean front) {
		// Only post events if the handler is started
		if (started) {
			super.queueEvent(event, front);
		}
	}
	/**
	 * Schedule the job or process the events now.
	 */
	@Override
	public void schedule() {
		Job job = getEventHandlerJob();
		if (job.getState() == Job.NONE) {
			if(progressGroup != null) {
				job.setSystem(false);
				job.setProgressGroup(progressGroup, ticks);
			} else {
				job.setSystem(isSystemJob());
			}
		}
		getEventHandlerJob().schedule();
	}

	protected boolean isSystemJob() {
		return !initializing;
	}


	@Override
	protected void jobDone(IJobChangeEvent event) {
		super.jobDone(event);
		progressGroup = null;
	}

	/**
	 * Called by a client to indicate that a resource has changed and its synchronization state
	 * should be recalculated.
	 * @param resource the changed resource
	 * @param depth the depth of the change calculation
	 */
	public void change(IResource resource, int depth) {
		queueEvent(new SubscriberEvent(resource, SubscriberEvent.CHANGE, depth), false);
	}

	/**
	 * Called by a client to indicate that a resource has been removed and should be removed. The
	 * removal will propagate to the set.
	 * @param resource the resource that was removed
	 */
	public void remove(IResource resource) {
		queueEvent(
			new SubscriberEvent(resource, SubscriberEvent.REMOVAL, IResource.DEPTH_INFINITE), false);
	}

	/**
	 * Collect the calculated synchronization information for the given resource at the given depth. The
	 * results are added to the provided list.
	 */
	private void collect(
		IResource resource,
		int depth,
		IProgressMonitor monitor) {

		Policy.checkCanceled(monitor);

		// handle any preemptive events before continuing
		handlePreemptiveEvents(monitor);

		if (resource.getType() != IResource.FILE
			&& depth != IResource.DEPTH_ZERO) {
			try {
				IResource[] members =
					getSubscriber().members(resource);
				for (IResource member : members) {
					collect(member, depth == IResource.DEPTH_INFINITE
							? IResource.DEPTH_INFINITE
							: IResource.DEPTH_ZERO, monitor);
				}
			} catch (TeamException e) {
				// We only handle the exception if the resource's project is accessible.
				// The project close delta will clean up.
				if (resource.getProject().isAccessible())
					handleException(e, resource, ITeamStatus.SYNC_INFO_SET_ERROR, NLS.bind(Messages.SubscriberEventHandler_8, new String[] { resource.getFullPath().toString(), e.getMessage() }));
			}
		}

		monitor.subTask(NLS.bind(Messages.SubscriberEventHandler_2, new String[] { resource.getFullPath().toString() }));
		try {
			handleChange(resource);
			handlePendingDispatch(monitor);
		} catch (CoreException e) {
			handleException(e, resource, ITeamStatus.RESOURCE_SYNC_INFO_ERROR, NLS.bind(Messages.SubscriberEventHandler_9, new String[] { resource.getFullPath().toString(), e.getMessage() }));
		}
		monitor.worked(1);
	}

	/**
	 * Return the subscriber associated with this event handler
	 * @return the subscriber associated with this event handler
	 */
	protected Subscriber getSubscriber() {
		return subscriber;
	}

	/**
	 * The given resource has changed. Subclasses should handle
	 * this in an appropriate fashion
	 * @param resource the resource whose state has changed
	 */
	protected abstract void handleChange(IResource resource) throws CoreException;

	protected void handlePendingDispatch(IProgressMonitor monitor) {
		if (isReadyForDispatch(false /*don't wait if queue is empty*/)) {
			try {
				dispatchEvents(Policy.subMonitorFor(monitor, 5));
			} catch (TeamException e) {
				handleException(e, null, ITeamStatus.SYNC_INFO_SET_ERROR, e.getMessage());
			}
		}
	}

	/**
	 * Handle the exception by returning it as a status from the job but also by
	 * dispatching it to the sync set input so any down stream views can react
	 * accordingly.
	 * The resource passed may be null.
	 */
	protected void handleException(CoreException e, IResource resource, int code, String message) {
		handleException(e);
	}

	/**
	 * Called to initialize to calculate the synchronization information using the optimized subscriber method. For
	 * subscribers that don't support the optimization, all resources in the subscriber are manually re-calculated.
	 * @param resource the resources to check
	 * @param depth the depth
	 * @param monitor
	 */
	protected abstract void collectAll(
		IResource resource,
		int depth,
		IProgressMonitor monitor);

	/**
	 * Feed the given events to the set. The appropriate method on the set is called
	 * for each event type.
	 * @param events
	 */
	protected abstract void dispatchEvents(SubscriberEvent[] events, IProgressMonitor monitor);

	/**
	 * Initialize all resources for the subscriber associated with the set. This will basically recalculate
	 * all synchronization information for the subscriber.
	 * @param type can be Event.CHANGE to recalculate all states or Event.INITIALIZE to perform the
	 *   optimized recalculation if supported by the subscriber.
	 */
	protected void reset(ResourceTraversal[] traversals, int type) {
		for (ResourceTraversal traversal : traversals) {
			IResource[] resources = traversal.getResources();
			for (IResource resource : resources) {
				queueEvent(new SubscriberEvent(resource, type, traversal.getDepth()), false);
			}
		}
	}

	@Override
	protected void processEvent(Event event, IProgressMonitor monitor) {
		try {
			// Cancellation is dangerous because this will leave the sync info in a bad state.
			// Purposely not checking -
			int type = event.getType();
			switch (type) {
				case BackgroundEventHandler.RUNNABLE_EVENT :
					executeRunnable(event, monitor);
					break;
				case SubscriberEvent.REMOVAL :
					queueDispatchEvent(event);
					break;
				case SubscriberEvent.CHANGE :
					collect(
						event.getResource(),
						((ResourceEvent)event).getDepth(),
						monitor);
					break;
				case SubscriberEvent.INITIALIZE :
					monitor.subTask(NLS.bind(Messages.SubscriberEventHandler_2, new String[] { event.getResource().getFullPath().toString() }));
					collectAll(
							event.getResource(),
							((ResourceEvent)event).getDepth(),
							Policy.subMonitorFor(monitor, 64));
					break;
			}
		} catch (OperationCanceledException e) {
			// the job has been canceled.
			// Clear the queue and propagate the cancellation through the sets.
			handleCancel(e);
		} catch (RuntimeException e) {
			// handle the exception and keep processing
			if (event.getType() == BackgroundEventHandler.RUNNABLE_EVENT ) {
				handleException(new TeamException(Messages.SubscriberEventHandler_10, e));
			} else {
				handleException(new TeamException(Messages.SubscriberEventHandler_10, e), event.getResource(), ITeamStatus.SYNC_INFO_SET_ERROR, NLS.bind(Messages.SubscriberEventHandler_11, new String[] { event.getResource().getFullPath().toString(), e.getMessage() }));
			}
		}
	}

	/**
	 * Queue the event to be handle during the dispatch phase.
	 * @param event the event
	 */
	protected void queueDispatchEvent(Event event) {
		resultCache.add(event);
	}

	/**
	 * Handle the cancel exception
	 * @param e the cancel exception
	 */
	protected void handleCancel(OperationCanceledException e) {
		resultCache.clear();
	}

	/*
	 * Execute the RunnableEvent
	 */
	private void executeRunnable(Event event, IProgressMonitor monitor) {
		try {
			// Dispatch any queued results to clear pending output events
			dispatchEvents(Policy.subMonitorFor(monitor, 1));
		} catch (TeamException e) {
			handleException(e, null, ITeamStatus.SYNC_INFO_SET_ERROR, e.getMessage());
		}
		try {
			((RunnableEvent)event).run(Policy.subMonitorFor(monitor, 1));
		} catch (CoreException e) {
			handleException(e, null, ITeamStatus.SYNC_INFO_SET_ERROR, e.getMessage());
		}
	}

	@Override
	protected boolean  doDispatchEvents(IProgressMonitor monitor) {
		if (!resultCache.isEmpty()) {
			dispatchEvents(resultCache.toArray(new SubscriberEvent[resultCache.size()]), monitor);
			resultCache.clear();
			return true;
		}
		return false;
	}

	/**
	 * Queue up the given runnable in an event to be processed by this job
	 *
	 * @param runnable
	 *            the runnable to be run by the handler
	 * @param frontOnQueue
	 *            the frontOnQueue flag is used to indicate that the runnable
	 *            should be placed on the front of the queue and be processed as
	 *            soon as possible
	 */
	public void run(IWorkspaceRunnable runnable, boolean frontOnQueue) {
		queueEvent(new RunnableEvent(runnable, frontOnQueue), frontOnQueue);
	}

	public void setProgressGroupHint(IProgressMonitor progressGroup, int ticks) {
		this.progressGroup = progressGroup;
		this.ticks = ticks;
	}

	protected void handlePreemptiveEvents(IProgressMonitor monitor) {
		Event event = peek();
		if (event instanceof RunnableEvent && ((RunnableEvent)event).isPreemtive()) {
			executeRunnable(nextElement(), monitor);
		}
	}

	/**
	 * Return the scope of this event handler. The scope is
	 * used to determine the resources that are processed by the handler
	 * @return the scope of this event handler
	 */
	protected ISynchronizationScope getScope() {
		return scope;
	}

	@Override
	public void shutdown() {
		super.shutdown();
		scope.removeScopeChangeListener(scopeChangeListener);
	}
}
