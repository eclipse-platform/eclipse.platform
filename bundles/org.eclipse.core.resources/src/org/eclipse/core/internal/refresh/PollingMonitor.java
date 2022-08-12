/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *     Christoph Läubrich - Issue #97 - RefreshManager.manageAutoRefresh calls ResourcesPlugin.getWorkspace before the Workspace is fully open
 *******************************************************************************/
package org.eclipse.core.internal.refresh;

import java.util.ArrayList;
import java.util.Arrays;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.refresh.IRefreshMonitor;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;

/**
 * The <code>PollingMonitor</code> is an <code>IRefreshMonitor</code> that
 * polls the file system rather than registering natively for call-backs.
 *
 * The polling monitor operates in iterations that span multiple invocations
 * of the job's run method.  At the beginning of an iteration, a set of
 * all resource roots is collected.  Each time the job runs, it removes items
 * from the set and searches for changes for a fixed period of time.
 * This ensures that the refresh job is broken into very small discrete
 * operations that do not interrupt the user's main-line activity.
 *
 * @since 3.0
 */
public class PollingMonitor extends Job implements IRefreshMonitor {
	/**
	 * The maximum duration of a single polling iteration
	 */
	private static final long MAX_DURATION = 250;
	/**
	 * The amount of time that a changed root should remain hot.
	 */
	private static final long HOT_ROOT_DECAY = 90000;
	/**
	 * The minimum delay between executions of the polling monitor
	 */
	private static final long MIN_FREQUENCY = 4000;
	/**
	 * The roots of resources which should be polled
	 */
	private final ArrayList<IResource> resourceRoots;
	/**
	 * The resources remaining to be refreshed in this iteration
	 */
	private final ArrayList<IResource> toRefresh;
	/**
	 * The root that has most recently been out of sync
	 */
	private IResource hotRoot;
	/**
	 * The time the hot root was last refreshed
	 */
	private long hotRootTime;

	private final RefreshManager refreshManager;
	/**
	 * True if this job has never been run. False otherwise.
	 */
	private boolean firstRun = true;

	/**
	 * Creates a new polling monitor.
	 */
	public PollingMonitor(RefreshManager manager) {
		super(Messages.refresh_pollJob);
		this.refreshManager = manager;
		setPriority(Job.DECORATE);
		setSystem(true);
		resourceRoots = new ArrayList<>();
		toRefresh = new ArrayList<>();
	}

	/**
	 * Add the given root to the list of roots that need to be polled.
	 */
	public synchronized void monitor(IResource root) {
		resourceRoots.add(root);
		schedule(MIN_FREQUENCY);
	}

	/**
	 * Polls the file system under the root containers for changes.
	 */
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		//sleep until resources plugin has finished starting
		if (firstRun) {
			firstRun = false;
			Bundle bundle = Platform.getBundle(ResourcesPlugin.PI_RESOURCES);
			long waitStart = System.currentTimeMillis();
			while (bundle.getState() == Bundle.STARTING) {
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					//ignore
				}
				//don't wait forever
				if ((System.currentTimeMillis() - waitStart) > 90000)
					break;
			}
		}
		long time = System.currentTimeMillis();
		//check to see if we need to start an iteration
		if (toRefresh.isEmpty()) {
			beginIteration();
			if (Policy.DEBUG_AUTO_REFRESH)
				Policy.debug(RefreshManager.DEBUG_PREFIX + "New polling iteration on " + toRefresh.size() + " roots"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		final int oldSize = toRefresh.size();
		if (Policy.DEBUG_AUTO_REFRESH)
			Policy.debug(RefreshManager.DEBUG_PREFIX + "started polling"); //$NON-NLS-1$
		//refresh the hot root if applicable
		if (time - hotRootTime > HOT_ROOT_DECAY)
			hotRoot = null;
		else if (hotRoot != null && !monitor.isCanceled())
			poll(hotRoot);
		//process roots that have not yet been refreshed this iteration
		final long loopStart = System.currentTimeMillis();
		while (!toRefresh.isEmpty()) {
			if (monitor.isCanceled())
				break;
			poll(toRefresh.remove(toRefresh.size() - 1));
			//stop the iteration if we have exceed maximum duration
			if (System.currentTimeMillis() - loopStart > MAX_DURATION)
				break;
		}
		time = System.currentTimeMillis() - time;
		if (Policy.DEBUG_AUTO_REFRESH)
			Policy.debug(RefreshManager.DEBUG_PREFIX + "polled " + (oldSize - toRefresh.size()) + " roots in " + time + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		//reschedule automatically - shouldRun will cancel if not needed
		//make sure it doesn't run more than 5% of the time
		long delay = Math.max(MIN_FREQUENCY, time * 20);
		//back off even more if there are other jobs running
		if (!getJobManager().isIdle())
			delay *= 2;
		if (Policy.DEBUG_AUTO_REFRESH)
			Policy.debug(RefreshManager.DEBUG_PREFIX + "rescheduling polling job in: " + delay / 1000 + " seconds"); //$NON-NLS-1$ //$NON-NLS-2$
		//don't reschedule the job if the resources plugin has been shut down
		Bundle bundle = Platform.getBundle(ResourcesPlugin.PI_RESOURCES);
		if (bundle != null && bundle.getState() == Bundle.ACTIVE)
			schedule(delay);
		return Status.OK_STATUS;
	}

	/**
	 * Instructs the polling job to do one complete iteration of all workspace roots, and
	 * then discard itself. This is used when
	 * the refresh manager is first turned on if there is a native monitor installed (which
	 * don't handle changes that occurred while the monitor was turned off).
	 */
	void runOnce() {
		synchronized (this) {
			//add all roots to the refresh list, but not to the real set of roots
			//this will cause the job to never run again once it has exhausted
			//the set of roots to refresh
			IProject[] projects = refreshManager.getWorkspace().getRoot().getProjects(IContainer.INCLUDE_HIDDEN);
			toRefresh.addAll(Arrays.asList(projects));
		}
		schedule(MIN_FREQUENCY);
	}

	private void poll(IResource resource) {
		if (resource.isSynchronized(IResource.DEPTH_INFINITE))
			return;
		//don't refresh links with no local content
		if (resource.isLinked() && !((Resource) resource).getStore().fetchInfo().exists())
			return;
		//submit refresh request
		refreshManager.refresh(resource);
		hotRoot = resource;
		hotRootTime = System.currentTimeMillis();
		if (Policy.DEBUG_AUTO_REFRESH)
			Policy.debug(RefreshManager.DEBUG_PREFIX + "new hot root: " + resource); //$NON-NLS-1$
	}

	@Override
	public boolean shouldRun() {
		//only run if there is something to refresh
		return !resourceRoots.isEmpty() || !toRefresh.isEmpty();
	}

	/**
	 * Copies the resources to be polled into the list of resources
	 * to refresh this iteration. This method is synchronized to
	 * guard against concurrent access to the resourceRoots field.
	 */
	private synchronized void beginIteration() {
		toRefresh.addAll(resourceRoots);
		if (hotRoot != null)
			toRefresh.remove(hotRoot);
	}

	/*
	 * @see org.eclipse.core.resources.refresh.IRefreshMonitor#unmonitor(IContainer)
	 */
	@Override
	public synchronized void unmonitor(IResource resource) {
		if (resource == null)
			resourceRoots.clear();
		else
			resourceRoots.remove(resource);
		if (resourceRoots.isEmpty())
			cancel();
	}
}
