/*******************************************************************************
 *  Copyright (c) 2004, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM - Initial API and implementation
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427, 483862
 *     Christoph Läubrich - Issue #84 - RefreshManager access ResourcesPlugin.getWorkspace in the init phase
 *******************************************************************************/
package org.eclipse.core.internal.refresh;

import java.util.*;
import org.eclipse.core.internal.localstore.PrefixPool;
import org.eclipse.core.internal.resources.InternalWorkspaceJob;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

/**
 * The <code>RefreshJob</code> class maintains a list of resources that
 * need to be refreshed, and periodically schedules itself to perform the
 * refreshes in the background.
 *
 * @since 3.0
 */
public class RefreshJob extends InternalWorkspaceJob {

	/**
	 * Max refresh recursion deep
	 */
	public static final int MAX_RECURSION = 2 << 29; // 1073741824

	/**
	 * Threshold (in milliseconds) at which the refresh operation is considered to
	 * be fast enough to increase refresh depth
	 */
	public static final int FAST_REFRESH_THRESHOLD = 1000;

	/**
	 * Threshold (in milliseconds) at which the refresh operation is considered to
	 * be slow enough to decrease refresh depth
	 */
	public static final int SLOW_REFRESH_THRESHOLD = 2000;

	/** Base depth used for refresh */
	public static final int BASE_REFRESH_DEPTH = 1000;

	/** Number of refresh rounds before updating refresh depth */
	public static final int DEPTH_INCREASE_STEP = 1000;

	/** Default refresh job delay (in milliseconds) */
	public static final int UPDATE_DELAY = 200;

	/**
	 * List of refresh requests. Requests are processed in order from
	 * the end of the list. Requests can be added to either the beginning
	 * or the end of the list depending on whether they are explicit user
	 * requests or background refresh requests.
	 */
	private final List<IResource> fRequests;

	/**
	 * The history of path prefixes visited during this refresh job invocation.
	 * This is used to prevent infinite refresh loops caused by symbolic links in the file system.
	 */
	private PrefixPool pathPrefixHistory, rootPathHistory;

	private final int fastRefreshThreshold;
	private final int slowRefreshThreshold;
	private final int baseRefreshDepth;
	private final int depthIncreaseStep;
	private final int updateDelay;
	private final int maxRecursionDeep;
	private final Workspace workspace;

	public RefreshJob(Workspace workspace) {
		this(FAST_REFRESH_THRESHOLD, SLOW_REFRESH_THRESHOLD, BASE_REFRESH_DEPTH, DEPTH_INCREASE_STEP, UPDATE_DELAY,
				MAX_RECURSION, workspace);
	}

	/**
	 * This method is protected for tests
	 */
	protected RefreshJob(int fastRefreshThreshold, int slowRefreshThreshold, int baseRefreshDepth,
			int depthIncreaseStep, int updateDelay, int maxRecursionDeep, Workspace workspace) {
		super(Messages.refresh_jobName, workspace);
		this.fRequests = new ArrayList<>(1);
		this.fastRefreshThreshold = fastRefreshThreshold;
		this.slowRefreshThreshold = slowRefreshThreshold;
		this.baseRefreshDepth = baseRefreshDepth;
		this.depthIncreaseStep = depthIncreaseStep;
		this.updateDelay = updateDelay;
		this.maxRecursionDeep = maxRecursionDeep;
		this.workspace = workspace;
	}

	/**
	 * Adds the given resource to the set of resources that need refreshing.
	 * Synchronized in order to protect the collection during add.
	 * @param resource
	 */
	private synchronized void addRequest(IResource resource) {
		IPath toAdd = resource.getFullPath();
		for (Iterator<IResource> it = fRequests.iterator(); it.hasNext();) {
			IPath request = it.next().getFullPath();
			//discard any existing requests the same or below the resource to be added
			if (toAdd.isPrefixOf(request))
				it.remove();
			//nothing to do if the resource to be added is a child of an existing request
			else if (request.isPrefixOf(toAdd))
				return;
		}
		//finally add the new request to the front of the queue
		fRequests.add(resource);
	}

	private synchronized void addRequests(List<IResource> list) {
		//add requests to the end of the queue
		if (!list.isEmpty()) {
			fRequests.addAll(0, list);
		}
	}

	@Override
	public boolean belongsTo(Object family) {
		return family == ResourcesPlugin.FAMILY_AUTO_REFRESH;
	}

	/**
	 * This method adds all members at the specified depth from the resource
	 * to the provided list.
	 */
	protected List<IResource> collectChildrenToDepth(IResource resource, ArrayList<IResource> children, int depth) {
		if (resource.getType() == IResource.FILE)
			return children;
		IResource[] members;
		try {
			members = ((IContainer) resource).members();
		} catch (CoreException e) {
			//resource is not accessible - just return what we have
			return children;
		}
		for (IResource member : members) {
			if (member.getType() == IResource.FILE)
				continue;
			if (depth <= 1)
				children.add(member);
			else
				collectChildrenToDepth(member, children, depth - 1);
		}
		return children;
	}

	/**
	 * Returns the path prefixes visited by this job so far.
	 */
	public PrefixPool getPathPrefixHistory() {
		if (pathPrefixHistory == null)
			pathPrefixHistory = new PrefixPool(20);
		return pathPrefixHistory;
	}

	/**
	 * Returns the root paths visited by this job so far.
	 */
	public PrefixPool getRootPathHistory() {
		if (rootPathHistory == null)
			rootPathHistory = new PrefixPool(20);
		return rootPathHistory;
	}

	/**
	 * Returns the next item to refresh, or <code>null</code> if there are no requests
	 */
	private synchronized IResource nextRequest() {
		// synchronized: in order to atomically obtain and clear requests
		int len = fRequests.size();
		if (len == 0)
			return null;
		return fRequests.remove(len - 1);
	}

	/**
	 * @see org.eclipse.core.resources.refresh.IRefreshResult#refresh
	 */
	public void refresh(IResource resource) {
		if (resource == null)
			return;
		addRequest(resource);
		schedule(updateDelay);
	}

	@Override
	public IStatus runInWorkspace(IProgressMonitor monitor) {
		long start = System.currentTimeMillis();
		String msg = Messages.refresh_refreshErr;
		MultiStatus errors = new MultiStatus(ResourcesPlugin.PI_RESOURCES, 1, msg, null);
		long longestRefresh = 0;
		SubMonitor subMonitor = SubMonitor.convert(monitor);
		try {
			if (Policy.DEBUG_AUTO_REFRESH)
				Policy.debug(RefreshManager.DEBUG_PREFIX + " starting refresh job"); //$NON-NLS-1$
			int refreshCount = 0;
			int depth = 2;

			IResourceRuleFactory ruleFactory = workspace.getRuleFactory();
			IResource toRefresh;
			while ((toRefresh = nextRequest()) != null) {
				ISchedulingRule refreshRule = ruleFactory.refreshRule(toRefresh);
				try {
					subMonitor.setWorkRemaining(Math.max(fRequests.size(), 100));
					Job.getJobManager().beginRule(refreshRule, subMonitor);
					refreshCount++;
					long refreshTime = -System.currentTimeMillis();
					toRefresh.refreshLocal(baseRefreshDepth + depth, subMonitor.split(1));
					refreshTime += System.currentTimeMillis();
					if (refreshTime > longestRefresh)
						longestRefresh = refreshTime;
					//show occasional progress
					if (refreshCount % depthIncreaseStep == 0) {
						//be polite to other threads (no effect on some platforms)
						Thread.yield();
						//throttle depth if it takes too long
						if (longestRefresh > slowRefreshThreshold && depth > 1) {
							depth = 1;
							if (Policy.DEBUG_AUTO_REFRESH) {
								Policy.debug(RefreshManager.DEBUG_PREFIX + " decreased refresh depth to: " + depth); //$NON-NLS-1$
							}
						}
						if (longestRefresh < fastRefreshThreshold) {
							depth *= 2;
							if (depth <= 0 || depth > maxRecursionDeep) {
								// avoid integer overflow
								depth = maxRecursionDeep;
							}
							if (Policy.DEBUG_AUTO_REFRESH) {
								Policy.debug(RefreshManager.DEBUG_PREFIX + " increased refresh depth to: " + depth); //$NON-NLS-1$
							}
						}
						longestRefresh = 0;
					}
					addRequests(collectChildrenToDepth(toRefresh, new ArrayList<>(), depth));
				} catch (CoreException e) {
					errors.merge(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, 1, errors.getMessage(), e));
				} finally {
					Job.getJobManager().endRule(refreshRule);
				}
			}
		} finally {
			pathPrefixHistory = null;
			rootPathHistory = null;
			if (Policy.DEBUG_AUTO_REFRESH)
				Policy.debug(RefreshManager.DEBUG_PREFIX + " finished refresh job in: " + (System.currentTimeMillis() - start) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (!errors.isOK())
			return errors;
		return Status.OK_STATUS;
	}

	@Override
	public synchronized boolean shouldRun() {
		return !fRequests.isEmpty();
	}

	/**
	 * Starts the refresh job
	 */
	public void start() {
		if (Policy.DEBUG_AUTO_REFRESH)
			Policy.debug(RefreshManager.DEBUG_PREFIX + " enabling auto-refresh"); //$NON-NLS-1$
	}

	/**
	 * Stops the refresh job
	 */
	public void stop() {
		if (Policy.DEBUG_AUTO_REFRESH)
			Policy.debug(RefreshManager.DEBUG_PREFIX + " disabling auto-refresh"); //$NON-NLS-1$
		cancel();
	}
}
