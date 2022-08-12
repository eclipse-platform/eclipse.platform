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
package org.eclipse.team.internal.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.team.core.TeamException;

/**
 * This class provides the infrastructure for processing/dispatching of events using a
 * background job. This is useful in the following situations.
 * <ul>
 * <li>an operation is potentially long running but a responsive UI is desired
 * while the operation is being performed. To do this incoming events are processed
 * and resulting outgoing events are queued and then dispatched at an appropriate time,
 * thus batching UI updates.</li>
 * <li>a change is a POST_CHANGE delta requires further modifications to the workspace
 * which cannot be performed in the delta handler because the workspace is locked.</li>
 * <li>a data structure is not thread safe and requires serialized operations.<li>
 * </ul>
 * <p>
 * The event handler has the following characteristics:
 * </p>
 * <ol>
 * <li>Incoming events are placed in an incoming queue.</li>
 * <li>Each event is processed by calling the <code>processEvent</code> method
 * which is implemented by the subclass. The implementation may choose to process events
 * directly or queue events on an outgoing event queue</li>
 * <li>The <code>doDispatchEvents</code> method of the subclass is called at certain intervals
 * to give the subclass a chance to dispatch the events in it's outgoing queue. The interval between
 * the first 3 dispatches will be the <code>shortDispatchDelay</code> and subsequent intervals will be
 * the <code>longDispatchDelay</code>. This is done to avoid constantly hammering the UI for long running
 * operations.<li>
 * <li>Errors that occur during event processing or dispatch can be accumulated by calling the <code>handle</code>
 * method. Accumulated errors are used to form the status that is returned when the job completes.<li>
 * </ol>
 *
 * @since 3.0
 */
public abstract class BackgroundEventHandler {

	/**
	 * Event type constant used to identify a runnable event
	 */
	public static final int RUNNABLE_EVENT = 1000;

	// Events that need to be processed
	private List<Event> awaitingProcessing = new ArrayList<>();

	// The job that runs when events need to be processed
	private Job eventHandlerJob;

	// Indicate if the event handler has been shutdown
	private boolean shutdown;

	// Accumulate exceptions that occur
	private ExceptionCollector errors;

	// time the last dispatch occurred
	private long timeOfLastDispatch = 0L;

	// the number of dispatches that have occurred since the job started
	private int dispatchCount;

	// time between event dispatches
	private static final long DISPATCH_DELAY = 1500;

	// time between dispatches if the dispatch threshold has been exceeded
	private static final long LONG_DISPATCH_DELAY = 10000;

	// the number of dispatches that can occur before using the long delay
	private static final int DISPATCH_THRESHOLD = 3;

	// time to wait for messages to be queued
	private static final long WAIT_DELAY = 100;

	private String jobName;

	/**
	 * General event class. The type is specific to subclasses.
	 */
	public static class Event {
		private int type;
		public Event(int type) {
			this.type = type;
		}
		public int getType() {
			return type;
		}
		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			buffer.append("Background Event: "); //$NON-NLS-1$
			buffer.append(getTypeString());
			return buffer.toString();
		}
		public IResource getResource() {
			return null;
		}
		protected String getTypeString() {
			return String.valueOf(type);
		}
	}

	/**
	 * Resource event class. The type is specific to subclasses.
	 */
	public static class ResourceEvent extends Event {
		private IResource resource;
		private int depth;
		public ResourceEvent(IResource resource, int type, int depth) {
			super(type);
			this.resource = resource;
			this.depth = depth;
		}
		public int getDepth() {
			return depth;
		}
		@Override
		public IResource getResource() {
			return resource;
		}
		@Override
		public String toString() {
			StringBuilder buffer = new StringBuilder();
			buffer.append("resource: "); //$NON-NLS-1$
			buffer.append(resource.getFullPath());
			buffer.append(" type: "); //$NON-NLS-1$
			buffer.append(getTypeString());
			buffer.append(" depth: "); //$NON-NLS-1$
			buffer.append(getDepthString());
			return buffer.toString();
		}
		protected String getDepthString() {
			switch (depth) {
				case IResource.DEPTH_ZERO :
					return "DEPTH_ZERO"; //$NON-NLS-1$
				case IResource.DEPTH_ONE :
					return "DEPTH_ONE"; //$NON-NLS-1$
				case IResource.DEPTH_INFINITE :
					return "DEPTH_INFINITE"; //$NON-NLS-1$
				default :
					return "INVALID"; //$NON-NLS-1$
			}
		}
	}

	/**
	 * This is a special event used to run some work in the background.
	 * The preemptive flag is used to indicate that the runnable should take
	 * the highest priority and thus be placed on the front of the queue
	 * and be processed as soon as possible, preempting any event that is currently
	 * being processed. The current event will continue processing once the
	 * high priority event has been processed
	 */
	public static class RunnableEvent extends Event {
		private IWorkspaceRunnable runnable;
		private boolean preemtive;
		public RunnableEvent(IWorkspaceRunnable runnable, boolean preemtive) {
			super(RUNNABLE_EVENT);
			this.runnable = runnable;
			this.preemtive = preemtive;
		}
		public void run(IProgressMonitor monitor) throws CoreException {
			runnable.run(monitor);
		}
		public boolean isPreemtive() {
			return preemtive;
		}
	}

	protected BackgroundEventHandler(String jobName, String errorTitle) {
		this.jobName = jobName;
		errors =
			new ExceptionCollector(
				errorTitle,
				TeamPlugin.ID,
				IStatus.ERROR,
				null /* don't log */
		);
		createEventHandlingJob();
		schedule();
	}

	/**
	 * Create the job used for processing the events in the queue. The job stops working when
	 * the queue is empty.
	 */
	protected void createEventHandlingJob() {
		eventHandlerJob = new Job(getName()) {
			@Override
			public IStatus run(IProgressMonitor monitor) {
				return processEvents(monitor);
			}
			@Override
			public boolean shouldRun() {
				return ! isQueueEmpty();
			}
			@Override
			public boolean shouldSchedule() {
				return ! isQueueEmpty();
			}
			@Override
			public boolean belongsTo(Object family) {
				return BackgroundEventHandler.this.belongsTo(family);
			}
		};
		eventHandlerJob.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				jobDone(event);
			}
		});
		eventHandlerJob.setSystem(true);
		eventHandlerJob.setPriority(Job.SHORT);
	}

	/**
	 * Return whether this background handler belongs to the given job family.
	 * @param family the job family
	 * @return whether this background handler belongs to the given job family.
	 * @see Job#belongsTo(Object)
	 */
	protected boolean belongsTo(Object family) {
		return getJobFamiliy() == family;
	}

	/**
	 * Return the family that the background job for this
	 * event handler belongs to.
	 * @return the family that the background job for this
	 * event handler belongs to
	 */
	protected Object getJobFamiliy() {
		return null;
	}

	/**
	 * This method is invoked when the processing job completes. The
	 * default behavior of the handler is to restart the job if the queue
	 * is no longer empty and to clear the queue if the handler was shut down.
	 */
	protected void jobDone(IJobChangeEvent event) {
		if (isShutdown()) {
			// The handler has been shutdown. Clean up the queue.
			synchronized(this) {
				awaitingProcessing.clear();
			}
		} else if (! isQueueEmpty()) {
			// An event squeaked in as the job was finishing. Reschedule the job.
			schedule();
		}
	}

	/**
	 * Schedule the job to process the events now.
	 */
	protected void schedule() {
		eventHandlerJob.schedule();
	}

	/**
	 * Shutdown the event handler. Any events on the queue will be removed from the queue
	 * and will not be processed.
	 */
	public void shutdown() {
		shutdown = true;
		eventHandlerJob.cancel();
	}

	/**
	 * Returns whether the handle has been shutdown.
	 * @return Returns whether the handle has been shutdown.
	 */
	public boolean isShutdown() {
		return shutdown;
	}

	/**
	 * Queue the event and start the job if it's not already doing work. If the job is
	 * already running then notify in case it was waiting.
	 * @param event the event to be queued
	 */
	protected synchronized void queueEvent(Event event, boolean front) {
		if (Policy.DEBUG_BACKGROUND_EVENTS) {
			System.out.println("Event queued on " + getName() + ":" + event.toString()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (front) {
			awaitingProcessing.add(0, event);
		} else {
			awaitingProcessing.add(event);
		}
		if (!isShutdown() && eventHandlerJob != null) {
			if(eventHandlerJob.getState() == Job.NONE) {
				schedule();
			} else {
				notify();
			}
		}
	}

	/**
	 * Return the name that is to be associated with the background job.
	 * @return the job name
	 */
	protected String getName() {
		return jobName;
	}

	/*
	 * Return the next event that has been queued, removing it from the queue.
	 * @return the next event in the queue
	 */
	protected synchronized Event nextElement() {
		if (isShutdown() || isQueueEmpty()) {
			return null;
		}
		return awaitingProcessing.remove(0);
	}

	protected synchronized Event peek() {
		if (isShutdown() || isQueueEmpty()) {
			return null;
		}
		return awaitingProcessing.get(0);
	}

	/**
	 * Return whether there are unprocessed events on the event queue.
	 * @return whether there are unprocessed events on the queue
	 */
	protected synchronized boolean isQueueEmpty() {
		return awaitingProcessing.isEmpty();
	}

	/**
	 * Process events from the events queue and dispatch results. This method does not
	 * directly check for or handle cancelation of the provided monitor. However,
	 * it does invoke <code>processEvent(Event)</code> which may check for and handle
	 * cancelation by shutting down the receiver.
	 * <p>
	 * The <code>isReadyForDispatch()</code> method is used in conjunction
	 * with the <code>dispatchEvents(IProgressMonitor)</code> to allow
	 * the output of the event handler to be batched in order to avoid
	 * fine grained UI updating.
	 * @param monitor a progress monitor
	 */
	protected IStatus processEvents(IProgressMonitor monitor) {
		errors.clear();
		try {
			// It's hard to know how much work is going to happen
			// since the queue can grow. Use the current queue size as a hint to
			// an infinite progress monitor
			monitor.beginTask(null, IProgressMonitor.UNKNOWN);
			IProgressMonitor subMonitor = Policy.infiniteSubMonitorFor(monitor, 90);
			subMonitor.beginTask(null, 1024);

			Event event;
			timeOfLastDispatch = System.currentTimeMillis();
			dispatchCount = 1;
			while ((event = nextElement()) != null && ! isShutdown()) {
				try {
					processEvent(event, subMonitor);
					if (Policy.DEBUG_BACKGROUND_EVENTS) {
						System.out.println("Event processed on " + getName() + ":" + event.toString()); //$NON-NLS-1$ //$NON-NLS-2$
					}
					if(isReadyForDispatch(true /*wait if queue is empty*/)) {
						dispatchEvents(Policy.subMonitorFor(subMonitor, 1));
					}
				} catch (CoreException e) {
					// handle exception but keep going
					handleException(e);
				}
			}
		} finally {
			monitor.done();
		}
		return errors.getStatus();
	}

	/**
	 * Dispatch any accumulated events by invoking <code>doDispatchEvents</code>
	 * and then rest the dispatch counters.
	 * @param monitor a progress monitor
	 * @throws TeamException
	 */
	protected final void dispatchEvents(IProgressMonitor monitor) throws TeamException {
		if (doDispatchEvents(monitor)) {
			// something was dispatched so adjust dispatch count.
			dispatchCount++;
		}
		timeOfLastDispatch = System.currentTimeMillis();
	}

	/**
	 * Notify clients of processed events. Return <code>true</code> if there
	 * was something to dispatch and false otherwise. This is used to help
	 * control the frequency of dispatches (e.g. if there is a lot of dispatching
	 * going on, the frequency of dispatches may be reduced.
	 * @param monitor a progress monitor
	 */
	protected abstract boolean doDispatchEvents(IProgressMonitor monitor) throws TeamException;

	/**
	 * Returns <code>true</code> if processed events should be dispatched and
	 * <code>false</code> otherwise. Events are dispatched at regular intervals
	 * to avoid fine grain events causing the UI to be too jumpy. Also, if the
	 * events queue is empty we will wait a small amount of time to allow
	 * pending events to be queued. The queueEvent notifies when events are
	 * queued.
	 * @return <code>true</code> if processed events should be dispatched and
	 * <code>false</code> otherwise
	 */
	protected boolean isReadyForDispatch(boolean wait) {
		// Check if the time since the last dispatch is greater than the delay.
		if (isDispatchDelayExceeded())
			return true;

		synchronized(this) {
			// If we have incoming events, process them before dispatching
			if(! isQueueEmpty() || ! wait) {
				return false;
			}
			// There are no incoming events but we want to wait a little before
			// dispatching in case more events come in.
			try {
				wait(getDispatchWaitDelay());
			} catch (InterruptedException e) {
				// just continue
			}
		}
		return isQueueEmpty() || isDispatchDelayExceeded();
	}

	private boolean isDispatchDelayExceeded() {
		long duration = System.currentTimeMillis() - timeOfLastDispatch;
		return ((dispatchCount < DISPATCH_THRESHOLD && duration >= getShortDispatchDelay()) ||
				duration >= getLongDispatchDelay());
	}

	/**
	 * Return the amount of time to wait for more events before dispatching.
	 * @return the amount of time to wait for more events before dispatching.
	 */
	protected long getDispatchWaitDelay() {
		return WAIT_DELAY;
	}

	/**
	 * Return the value that is used to determine how often
	 * the events are dispatched (i.e. how often the UI is
	 * updated) for the first 3 cycles. The default value is 1.5 seconds.
	 * After the first 3 cycles, a longer delay is used
	 * @return the dispatch delay used for the first 3 cycles.
	 */
	protected long getShortDispatchDelay() {
		return DISPATCH_DELAY;
	}

	/**
	 * Return the value that is used to determine how often
	 * the events are dispatched (i.e. how often the UI is
	 * updated) after the first 3 cycles. The default value is 10 seconds.
	 * @return the dispatch delay used after the first 3 cycles.
	 */
	protected long getLongDispatchDelay() {
		return LONG_DISPATCH_DELAY;
	}

	/**
	 * Handle the exception by recording it in the errors list.
	 * @param e
	 */
	protected void handleException(CoreException e) {
		errors.handleException(e);
	}

	/**
	 * Process the event in the context of a running background job. Subclasses may
	 * (but are not required to) check the provided monitor for cancelation and shut down the
	 * receiver by invoking the <code>shutdown()</code> method.
	 * <p>
	 * In many cases, a background event handler will translate incoming events into outgoing
	 * events. If this is the case, the handler should accumulate these events in the
	 * <code>proceessEvent</code> method and propagate them from the <code>dispatchEvent</code>
	 * method which is invoked periodically in order to batch outgoing events and avoid
	 * the UI becoming too jumpy.
	 *
	 * @param event the <code>Event</code> to be processed
	 * @param monitor a progress monitor
	 */
	protected abstract void processEvent(Event event, IProgressMonitor monitor) throws CoreException;

	/**
	 * Return the job from which the <code>processedEvent</code> method is invoked.
	 * @return Returns the background event handling job.
	 */
	public Job getEventHandlerJob() {
		return eventHandlerJob;
	}
}
