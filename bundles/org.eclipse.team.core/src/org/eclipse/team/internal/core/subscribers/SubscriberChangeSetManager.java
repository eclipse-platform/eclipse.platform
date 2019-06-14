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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.core.BackgroundEventHandler;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.TeamPlugin;
import org.osgi.service.prefs.Preferences;

/**
 * This class manages the active change sets associated with a subscriber.
 */
public class SubscriberChangeSetManager extends ActiveChangeSetManager {

	private static final String PREF_CHANGE_SETS = "changeSets"; //$NON-NLS-1$

	private static final int RESOURCE_REMOVAL = 1;
	private static final int RESOURCE_CHANGE = 2;

	private EventHandler handler;
	private ResourceCollector collector;

	/*
	 * Background event handler for serializing and batching change set changes
	 */
	private class EventHandler extends BackgroundEventHandler {

		private List<Event> dispatchEvents = new ArrayList<>();

		protected EventHandler(String jobName, String errorTitle) {
			super(jobName, errorTitle);
		}

		@Override
		protected void processEvent(Event event, IProgressMonitor monitor) throws CoreException {
			// Handle everything in the dispatch
			if (isShutdown())
				throw new OperationCanceledException();
			dispatchEvents.add(event);
		}

		@Override
		protected boolean doDispatchEvents(IProgressMonitor monitor) throws TeamException {
			if (dispatchEvents.isEmpty()) {
				return false;
			}
			if (isShutdown())
				throw new OperationCanceledException();
			ResourceDiffTree[] locked = null;
			try {
				locked = beginDispath();
				for (Event event : dispatchEvents) {
					switch (event.getType()) {
					case RESOURCE_REMOVAL:
						handleRemove(event.getResource());
						break;
					case RESOURCE_CHANGE:
						handleChange(event.getResource(), ((ResourceEvent)event).getDepth());
						break;
					default:
						break;
					}
					if (isShutdown())
						throw new OperationCanceledException();
				}
			} catch (CoreException e) {
				throw TeamException.asTeamException(e);
			} finally {
				try {
					endDispatch(locked, monitor);
				} finally {
					dispatchEvents.clear();
				}
			}
			return true;
		}

		/*
		 * Begin input on all the sets and return the sync sets that were
		 * locked. If this method throws an exception then the client
		 * can assume that no sets were locked
		 */
		private ResourceDiffTree[] beginDispath() {
			ChangeSet[] sets = getSets();
			List<ResourceDiffTree> lockedSets = new ArrayList<>();
			try {
				for (ChangeSet s : sets) {
					ActiveChangeSet set = (ActiveChangeSet) s;
					ResourceDiffTree tree = set.internalGetDiffTree();
					lockedSets.add(tree);
					tree.beginInput();
				}
				return lockedSets.toArray(new ResourceDiffTree[lockedSets.size()]);
			} catch (RuntimeException e) {
				try {
					for (ResourceDiffTree tree : lockedSets) {
						try {
							tree.endInput(null);
						} catch (Throwable e1) {
							// Ignore so that original exception is not masked
						}
					}
				} catch (Throwable e1) {
					// Ignore so that original exception is not masked
				}
				throw e;
			}
		}

		private void endDispatch(ResourceDiffTree[] locked, IProgressMonitor monitor) {
			if (locked == null) {
				// The begin failed so there's nothing to unlock
				return;
			}
			monitor.beginTask(null, 100 * locked.length);
			for (ResourceDiffTree tree : locked) {
				try {
					tree.endInput(Policy.subMonitorFor(monitor, 100));
				} catch (RuntimeException e) {
					// Don't worry about ending every set if an error occurs.
					// Instead, log the error and suggest a restart.
					TeamPlugin.log(IStatus.ERROR, Messages.SubscriberChangeSetCollector_0, e);
					throw e;
				}
			}
			monitor.done();
		}

		@Override
		protected synchronized void queueEvent(Event event, boolean front) {
			// Override to allow access from enclosing class
			super.queueEvent(event, front);
		}

		/*
		 * Handle the removal
		 */
		private void handleRemove(IResource resource) {
			ChangeSet[] sets = getSets();
			for (ChangeSet set : sets) {
				// This will remove any descendants from the set and callback to
				// resourcesChanged which will batch changes
				if (!set.isEmpty()) {
					set.rootRemoved(resource, IResource.DEPTH_INFINITE);
					if (set.isEmpty()) {
						remove(set);
					}
				}
			}
		}

		/*
		 * Handle the change
		 */
		private void handleChange(IResource resource, int depth) throws CoreException {
			IDiff diff = getDiff(resource);
			if (isModified(diff)) {
				ActiveChangeSet[] containingSets = getContainingSets(resource);
				if (containingSets.length == 0) {
					// Consider for inclusion in the default set
					// if the resource is not already a member of another set
					if (getDefaultSet() != null) {
						getDefaultSet().add(diff);
					}
				} else {
					for (ActiveChangeSet set : containingSets) {
						// Update the sync info in the set
						set.add(diff);
					}
				}
			} else {
				removeFromAllSets(resource);
			}
			if (depth != IResource.DEPTH_ZERO) {
				IResource[] members = getSubscriber().members(resource);
				for (IResource member : members) {
					handleChange(member, depth == IResource.DEPTH_ONE ? IResource.DEPTH_ZERO : IResource.DEPTH_INFINITE);
				}
			}
		}

		private void removeFromAllSets(IResource resource) {
			List<ChangeSet> toRemove = new ArrayList<>();
			ChangeSet[] sets = getSets();
			for (ChangeSet set : sets) {
				if (set.contains(resource)) {
					set.remove(resource);
					if (set.isEmpty()) {
						toRemove.add(set);
					}
				}
			}
			for (Object element : toRemove) {
				ActiveChangeSet set = (ActiveChangeSet) element;
				remove(set);
			}
		}

		private ActiveChangeSet[] getContainingSets(IResource resource) {
			Set<ActiveChangeSet> result = new HashSet<>();
			ChangeSet[] sets = getSets();
			for (ChangeSet set : sets) {
				if (set.contains(resource)) {
					result.add((ActiveChangeSet) set);
				}
			}
			return result.toArray(new ActiveChangeSet[result.size()]);
		}
	}

	private class ResourceCollector extends SubscriberResourceCollector {

		public ResourceCollector(Subscriber subscriber) {
			super(subscriber);
		}

		@Override
		protected void remove(IResource resource) {
			if (handler != null)
				handler.queueEvent(new BackgroundEventHandler.ResourceEvent(resource, RESOURCE_REMOVAL, IResource.DEPTH_INFINITE), false);
		}

		@Override
		protected void change(IResource resource, int depth) {
			if (handler != null)
				handler.queueEvent(new BackgroundEventHandler.ResourceEvent(resource, RESOURCE_CHANGE, depth), false);
		}

		@Override
		protected boolean hasMembers(IResource resource) {
			return SubscriberChangeSetManager.this.hasMembers(resource);
		}
	}

	public SubscriberChangeSetManager(Subscriber subscriber) {
		collector = new ResourceCollector(subscriber);
		handler = new EventHandler(NLS.bind(Messages.SubscriberChangeSetCollector_1, new String[] { subscriber.getName() }), NLS.bind(Messages.SubscriberChangeSetCollector_2, new String[] { subscriber.getName() })); //
	}

	@Override
	protected void initializeSets() {
		load(getPreferences());
	}

	public boolean hasMembers(IResource resource) {
		ChangeSet[] sets = getSets();
		for (ChangeSet s : sets) {
			ActiveChangeSet set = (ActiveChangeSet) s;
			if (set.getDiffTree().getChildren(resource.getFullPath()).length > 0)
				return true;
		}
		if (getDefaultSet() != null)
			return (getDefaultSet().getDiffTree().getChildren(resource.getFullPath()).length > 0);
		return false;
	}

	/**
	 * Return the sync info for the given resource obtained
	 * from the subscriber.
	 * @param resource the resource
	 * @return the sync info for the resource
	 * @throws CoreException
	 */
	@Override
	public IDiff getDiff(IResource resource) throws CoreException {
		Subscriber subscriber = getSubscriber();
		return subscriber.getDiff(resource);
	}

	/**
	 * Return the subscriber associated with this collector.
	 * @return the subscriber associated with this collector
	 */
	public Subscriber getSubscriber() {
		return collector.getSubscriber();
	}

	@Override
	public void dispose() {
		handler.shutdown();
		collector.dispose();
		super.dispose();
		save(getPreferences());
	}

	private Preferences getPreferences() {
		return getParentPreferences().node(getSubscriberIdentifier());
	}

	private static Preferences getParentPreferences() {
		return getTeamPreferences().node(PREF_CHANGE_SETS);
	}

	private static Preferences getTeamPreferences() {
		return InstanceScope.INSTANCE.getNode(TeamPlugin.getPlugin().getBundle().getSymbolicName());
	}

	/**
	 * Return the id that will uniquely identify the subscriber across
	 * restarts.
	 * @return the id that will uniquely identify the subscriber across
	 */
	protected String getSubscriberIdentifier() {
		return getSubscriber().getName();
	}

	/**
	 * Wait until the collector is done processing any events.
	 * This method is for testing purposes only.
	 * @param monitor
	 */
	public void waitUntilDone(IProgressMonitor monitor) {
		monitor.worked(1);
		// wait for the event handler to process changes.
		while(handler.getEventHandlerJob().getState() != Job.NONE) {
			monitor.worked(1);
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
			Policy.checkCanceled(monitor);
		}
		monitor.worked(1);
	}

	@Override
	protected String getName() {
		return getSubscriber().getName();
	}
}
