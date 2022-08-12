/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.BackgroundEventHandler;
import org.eclipse.team.internal.core.BackgroundEventHandler.Event;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIMessages;

/**
 * An abstract class that
 * listens to resource changes and synchronization context changes.
 * <p>
 * This class can be subclassed by clients.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class or interface has been added as
 * part of a work in progress. There is a guarantee neither that this API will
 * work nor that it will remain the same. Please do not use this API without
 * consulting with the Platform/Team team.
 * </p>
 * @since 3.3
 */
public abstract class CompareInputChangeNotifier implements
		IResourceChangeListener {

	private Map<ICompareInput, CompareInputConnecton> inputs = new HashMap<>();
	private InputChangeEventHandler eventHandler;

	private static class CompareInputConnecton {
		private int connections;
		public void increment() {
			connections++;
		}
		public void decrement() {
			if (connections > 0)
				connections--;

		}
		public boolean isDisconnected() {
			return connections == 0;
		}
	}

	private static final int COMPARE_INPUT_CHANGE = 1;

	private static class InputChangeEvent extends Event {
		private final ICompareInput[] inputs;
		public InputChangeEvent(ICompareInput[] inputs) {
			super(COMPARE_INPUT_CHANGE);
			this.inputs = inputs;

		}
		public ICompareInput[] getChangedInputs() {
			return inputs;
		}
	}

	private class InputChangeEventHandler extends BackgroundEventHandler {

		private final Set<ICompareInput> changedInputs = new HashSet<>();
		private final List<Event> pendingRunnables = new ArrayList<>();

		protected InputChangeEventHandler() {
			super(TeamUIMessages.CompareInputChangeNotifier_0, TeamUIMessages.CompareInputChangeNotifier_1);
		}

		@Override
		protected boolean doDispatchEvents(IProgressMonitor monitor)
				throws TeamException {
			ICompareInput[] toDispatch;
			RunnableEvent[] events;
			synchronized (pendingRunnables) {
				synchronized (changedInputs) {
					if (changedInputs.isEmpty() && pendingRunnables.isEmpty())
						return false;
					toDispatch = changedInputs.toArray(new ICompareInput[changedInputs.size()]);
					events = pendingRunnables.toArray(new RunnableEvent[pendingRunnables.size()]);
					changedInputs.clear();
					pendingRunnables.clear();
				}
			}
			dispatchChanges(toDispatch, monitor);
			for (RunnableEvent event : events) {
				executeRunnableNow(event, monitor);
			}
			return true;
		}

		@Override
		protected void processEvent(Event event, IProgressMonitor monitor)
				throws CoreException {
			int type = event.getType();
			switch (type) {
				case BackgroundEventHandler.RUNNABLE_EVENT :
					RunnableEvent runnableEvent = ((RunnableEvent)event);
					if (runnableEvent.isPreemtive())
						executeRunnableNow(event, monitor);
					else
						executeRunnableDuringDispatch(event);
					break;
				case COMPARE_INPUT_CHANGE :
					if (event instanceof InputChangeEvent) {
						InputChangeEvent changeEvent = (InputChangeEvent) event;
						ICompareInput[] inputs = changeEvent.getChangedInputs();
						synchronized (changedInputs) {
							Collections.addAll(changedInputs, inputs);
						}
					}
					break;
			}
		}

		private void executeRunnableDuringDispatch(Event event) {
			synchronized (pendingRunnables) {
				pendingRunnables.add(event);
			}
		}

		private void executeRunnableNow(Event event, IProgressMonitor monitor) {
			try {
				// Dispatch any queued results to clear pending output events
				dispatchEvents(Policy.subMonitorFor(monitor, 1));
			} catch (TeamException e) {
				handleException(e);
			}
			try {
				((RunnableEvent)event).run(Policy.subMonitorFor(monitor, 1));
			} catch (CoreException e) {
				handleException(e);
			}
		}

		protected synchronized void queueEvent(Event event) {
			super.queueEvent(event, false);
		}

		@Override
		protected long getShortDispatchDelay() {
			// Only wait 250 for additional changes to come in
			return 250;
		}

		@Override
		protected boolean belongsTo(Object family) {
			return CompareInputChangeNotifier.this.belongsTo(family);
		}
	}

	/**
	 * Create a change notifier for the given synchronization context.
	 */
	public CompareInputChangeNotifier() {
		super();
	}

	/**
	 * Initialize the change notifier. This method is called from the
	 * constructor and registers a listener with the workspace and the
	 * synchronization context. It also registers a listener with the context
	 * cache which will unregister the listeners when the context is disposed.
	 * Subclasses may extend this method.
	 */
	public void initialize() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		eventHandler = new InputChangeEventHandler();
	}

	/**
	 * Dispose of the change notifier. This method is invoked when the context
	 * to which the change notifier is associated is disposed.
	 * Subclasses may extend this method.
	 */
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		if (eventHandler != null)
			eventHandler.shutdown();
	}

	/**
	 * Connect the input to this change notifier. Once connected, the change notifier will issue
	 * change events for the given input. When change notification is no longer desired, the
	 * input should be disconnected. The number of calls to {@link #connect(ICompareInput)} needs to
	 * be matched by the same number of calls to {@link #disconnect(ICompareInput)}.
	 * @param input the compare input
	 */
	public void connect(ICompareInput input) {
		CompareInputConnecton con = inputs.get(input);
		if (con == null) {
			con = new CompareInputConnecton();
			inputs.put(input, con);
		}
		con.increment();
	}

	/**
	 * Disconnect the input from this change notifier.
	 * @param input the compare input
	 * @see #connect(ICompareInput)
	 */
	public void disconnect(ICompareInput input) {
		CompareInputConnecton con = inputs.get(input);
		if (con != null) {
			con.decrement();
			if (con.isDisconnected()) {
				inputs.remove(input);
			}
		}
	}

	/**
	 * Return the array of inputs that have connections.
	 * @return the array of inputs that have connections
	 */
	protected ICompareInput[] getConnectedInputs() {
		return inputs.keySet().toArray(new ICompareInput[inputs.size()]);
	}

	/**
	 * Send out notification that the given compare inputs have changed.
	 * @param inputs the changed inputs
	 */
	protected void inputsChanged(ICompareInput[] inputs) {
		InputChangeEvent event = new InputChangeEvent(inputs);
		eventHandler.queueEvent(event);
	}

	/**
	 * Dispatch the changes to the given inputs.
	 * @param inputs the changed compare inputs
	 * @param monitor a progress monitor
	 */
	protected void dispatchChanges(final ICompareInput[] inputs, IProgressMonitor monitor) {
		prepareInputs(inputs, monitor);
		Display.getDefault().syncExec(() -> fireChanges(inputs));
	}

	/**
	 * Prepare the inputs in the background before firing the compare input change event.
	 * This allows for the caching of contents etc. before the input change event is fired.
	 * @param inputs the changed inputs
	 * @param monitor a progress monitor
	 */
	protected void prepareInputs(ICompareInput[] inputs, IProgressMonitor monitor) {
		monitor.beginTask(null, inputs.length * 100);
		for (ICompareInput input : inputs) {
			prepareInput(input, Policy.subMonitorFor(monitor, 100));
		}
		monitor.done();
	}

	/**
	 * Prepare the input before firing the compare input change event.
	 * This allows for the caching of contents etc. before the input change event is fired.
	 * This method is called from {@link #prepareInputs(ICompareInput[], IProgressMonitor)}
	 * for each input. By default, nothing is done, subclasses may override.
	 * @param input the compare input
	 * @param monitor a progress monitor
	 */
	protected void prepareInput(ICompareInput input, IProgressMonitor monitor) {
		// Default is to do nothing
	}

	/**
	 * Update the compare inputs and fire the change events.
	 * This method is called from the UI thread after the inputs have
	 * been prepared in a background thread
	 * (see {@link #prepareInputs(ICompareInput[], IProgressMonitor)})
	 * @param inputs the changed inputs
	 */
	protected void fireChanges(ICompareInput[] inputs) {
		for (ICompareInput input : inputs) {
			fireChange(input);
		}
	}

	/**
	 * Run the given runnable in the background.
	 * @param runnable the runnable
	 */
	protected void runInBackground(IWorkspaceRunnable runnable) {
		eventHandler.queueEvent(new BackgroundEventHandler.RunnableEvent(runnable, false));
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		List<ICompareInput> changedInputs = new ArrayList<>();
		ICompareInput[] inputs = getConnectedInputs();
		for (ICompareInput input : inputs) {
			IResource[] resources = getResources(input);
			for (IResource resource : resources) {
				if (resource != null) {
					IResourceDelta delta = event.getDelta().findMember(resource.getFullPath());
					if (delta != null) {
						if ((delta.getKind() & (IResourceDelta.ADDED | IResourceDelta.REMOVED)) > 0
								|| (delta.getKind() & (IResourceDelta.CHANGED)) > 0
								&& (delta.getFlags() & (IResourceDelta.CONTENT | IResourceDelta.REPLACED)) > 0) {
							changedInputs.add(input);
							break;
						}
					}
				}
			}
		}
		if (!changedInputs.isEmpty())
			handleInputChanges(changedInputs.toArray(new ICompareInput[changedInputs.size()]), true);
	}

	/**
	 * Return the resources covered by the given compare input.
	 * This method is used by the {@link #resourceChanged(IResourceChangeEvent)}
	 * method to determine if a workspace change affects the compare input.
	 * @param input the compare input
	 * @return the resources covered by the given compare input
	 */
	protected abstract IResource[] getResources(ICompareInput input);

	/**
	 * Handle the input changes by notifying any listeners of the changed inputs.
	 * @param inputs the changed inputs
	 */
	protected void handleInputChanges(ICompareInput[] inputs, boolean force) {
		ICompareInput[] realChanges;
		if (force) {
			realChanges = inputs;
		} else {
			List<ICompareInput> result = new ArrayList<>();
			for (ICompareInput input : inputs) {
				if (isChanged(input)) {
					result.add(input);
				}
			}
			realChanges = result.toArray(new ICompareInput[result.size()]);
		}
		if (realChanges.length > 0)
			inputsChanged(realChanges);
	}

	/**
	 * Return whether the given compare input has changed and requires
	 * a compare input change event to be fired.
	 * @param input the compare input
	 * @return whether the given compare input has changed
	 */
	protected boolean isChanged(ICompareInput input) {
		if (input instanceof AbstractCompareInput) {
			AbstractCompareInput ci = (AbstractCompareInput) input;
			return ci.needsUpdate();
		}
		return false;
	}

	/**
	 * Update the compare input and fire the change event.
	 * This method is called from {@link #fireChanges(ICompareInput[])}
	 * for each changed input.
	 * @param input the changed compare input
	 */
	protected void fireChange(ICompareInput input) {
		if (input instanceof AbstractCompareInput) {
			AbstractCompareInput ci = (AbstractCompareInput) input;
			ci.update();
		}
	}

	/**
	 * Return whether the background handler for this notifier belongs to the
	 * given job family.
	 * @param family the job family
	 * @return whether the background handler belongs to the given job family.
	 * @see Job#belongsTo(Object)
	 */
	protected boolean belongsTo(Object family) {
		return false;
	}

}
