/*******************************************************************************
 * Copyright (c) 2003, 2015 IBM Corporation and others.
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
 *     Warren Paul (Nokia) - Fix for build scheduling bug 209236
 *******************************************************************************/
package org.eclipse.core.internal.events;

import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.framework.Bundle;

/**
 * The job for performing workspace auto-builds, and pre- and post- autobuild
 * notification.  This job is run whenever the workspace changes regardless
 * of whether autobuild is on or off.
 */
class AutoBuildJob extends Job implements IEclipsePreferences.IPreferenceChangeListener {
	private volatile boolean avoidBuild;
	private volatile boolean buildNeeded;
	private volatile boolean forceBuild;
	/**
	 * Indicates that another thread tried to modify the workspace during
	 * the autobuild.  The autobuild should be immediately rescheduled
	 * so that it will run as soon as the next workspace modification completes.
	 */
	private volatile boolean interrupted;
	private volatile boolean isAutoBuilding;
	private volatile long lastBuild = 0L;
	private final Bundle systemBundle = Platform.getBundle("org.eclipse.osgi"); //$NON-NLS-1$
	private Workspace workspace;
	final Job noBuildJob;

	AutoBuildJob(Workspace workspace) {
		super(Messages.events_building_0);
		setRule(workspace.getRoot());
		setPriority(BUILD);
		isAutoBuilding = workspace.isAutoBuilding();
		this.workspace = workspace;
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).addPreferenceChangeListener(this);
		noBuildJob = new AutoBuildOffJob();
	}

	/**
	 * Used to prevent auto-builds at the end of operations that contain
	 * explicit builds
	 */
	synchronized void avoidBuild() {
		avoidBuild = true;
	}

	/**
	 * Prevent auto-builds if the auto-build job was not interrupted.
	 */
	synchronized void avoidBuildIfNotInterrupted() {
		if (!interrupted) {
			avoidBuild();
		}
	}

	@Override
	public boolean belongsTo(Object family) {
		return family == ResourcesPlugin.FAMILY_AUTO_BUILD;
	}

	/**
	 * Instructs the build job that a build is required.  Ensure the build
	 * job is scheduled to run.
	 * @param needsBuild Whether a build is required, either due to
	 * workspace change or other factor that invalidates the built state.
	 */
	synchronized void build(boolean needsBuild) {
		buildNeeded |= needsBuild;
		long delay = computeScheduleDelay();
		int state = getState();
		if (Policy.DEBUG_BUILD_NEEDED) {
			Policy.debug("build requested, needsBuild: " + needsBuild + " state: " + state + ", delay: " + delay); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		//don't mess with the interrupt flag if the job is still running
		if (state != Job.RUNNING)
			setInterrupted(false);

		switch (state) {
			case Job.SLEEPING :
				if (Policy.DEBUG_BUILD_INVOKING) {
					traceMessageOrFullStack("wakeup, needsBuild: " + needsBuild + ", delay: " + delay); //$NON-NLS-1$ //$NON-NLS-2$
				}
				wakeUp(delay);
				break;
			case NONE :
				if (isAutoBuilding) {
					if (Policy.DEBUG_BUILD_INVOKING) {
						traceMessageOrFullStack("scheduled, needsBuild: " + needsBuild + ", delay: " + delay); //$NON-NLS-1$//$NON-NLS-2$
					}
					schedule(delay);
				} else {
					// The code below is required to maintain the ancient contract
					// in IResourceChangeEvent, stating that even if autobuild is
					// switched off, we still send PRE_BUILD/POST_BUILD events
					if (noBuildJob.getState() != Job.RUNNING) {
						noBuildJob.schedule(delay);
					}
				}
				break;
			case RUNNING:
				// In rare cases, we can end up in a situation where some other thread calls
				// WorkManager.setBuild(true) while the AutoBuildJob is already running.
				// In this case the AutoBuildJob should be rescheduled so we are certain that
				// the new build request is eventually processed and does not get lost.
				// Therefore, if a build is needed and the autobuild is enabled, then reschedule
				// the job
				if (isAutoBuilding && buildNeeded && !avoidBuild && Job.getJobManager().currentJob() != this) {
					if (Policy.DEBUG_BUILD_INVOKING) {
						traceMessageOrFullStack("scheduled from other thread with delay: " + delay); //$NON-NLS-1$
					}
					schedule(delay);
				}
				break;
		}
	}

	/**
	 * Should only be called after check for enabled trace flag
	 */
	private static void traceMessageOrFullStack(String message) {
		message = "AutoBuildJob: " + message; //$NON-NLS-1$
		if (Policy.DEBUG_BUILD_NEEDED_STACK) {
			Policy.debug(new RuntimeException(message));
		} else {
			Policy.debug(message);
		}
	}

	/**
	 * Computes the delay time that autobuild should be scheduled with.  The
	 * value will be in the range (MIN_BUILD_DELAY, MAX_BUILD_DELAY).
	 */
	private long computeScheduleDelay() {
		// don't assume that the last build time is always less than the current system time
		long maxDelay = Math.min(Policy.MAX_BUILD_DELAY, Policy.MAX_BUILD_DELAY + lastBuild - System.currentTimeMillis());
		return Math.max(Policy.MIN_BUILD_DELAY, maxDelay);
	}

	/**
	 * The autobuild job has been canceled.  There are two flavours of
	 * cancel, explicit user cancelation, and implicit interruption due to another
	 * thread trying to modify the workspace.  In the latter case, we must
	 * make sure the build is immediately rescheduled if it was interrupted
	 * by another thread, so that clients waiting to join autobuild will properly
	 * continue waiting
	 * @return a status with severity <code>CANCEL</code>
	 */
	private synchronized IStatus canceled() {
		//regardless of the form of cancelation, the build state is not happy
		buildNeeded = true;
		//schedule a rebuild immediately if build was implicitly canceled
		if (interrupted) {
			if (Policy.DEBUG_BUILD_INTERRUPT) {
				traceMessageOrFullStack("scheduling due to interruption"); //$NON-NLS-1$
			}
			setInterrupted(false);
			schedule(computeScheduleDelay());
		}
		return Status.CANCEL_STATUS;
	}

	private void doBuild(IProgressMonitor monitor) throws CoreException, OperationCanceledException {
		SubMonitor subMonitor = SubMonitor.convert(monitor, Policy.opWork + 1);
		final ISchedulingRule rule = workspace.getRuleFactory().buildRule();
		SubMonitor split = subMonitor.split(1); // will throw OperationCanceledException if autobuild canceled
		try {
			workspace.prepareOperation(rule, split);
			workspace.beginOperation(true);
			final int trigger = IncrementalProjectBuilder.AUTO_BUILD;
			workspace.broadcastBuildEvent(workspace, IResourceChangeEvent.PRE_BUILD, trigger);
			IStatus result = Status.OK_STATUS;
			try {
				// Note: shouldBuild() also resets the need/force/avoid build flags!
				if (shouldBuild())
					result = workspace.getBuildManager().build(workspace.getBuildOrder(), ICoreConstants.EMPTY_BUILD_CONFIG_ARRAY, trigger, subMonitor.split(Policy.opWork));
			} finally {
				//always send POST_BUILD if there has been a PRE_BUILD
				workspace.broadcastBuildEvent(workspace, IResourceChangeEvent.POST_BUILD, trigger);
			}
			if (!result.isOK()) {
				throw new ResourceException(result);
			}
		} finally {
			//building may close the tree, but we are still inside an
			// operation so open it
			if (workspace.getElementTree().isImmutable()) {
				workspace.newWorkingTree();
			}
			workspace.endOperation(rule, false);
		}
	}

	/**
	 * Forces an autobuild to occur, even if nothing has changed since the last
	 * build. This is used to force a build after a clean.
	 */
	public void forceBuild() {
		forceBuild = true;
	}

	/**
	 * Another thread is attempting to modify the workspace. Flag the auto-build
	 * as interrupted so that it will cancel and reschedule itself
	 */
	synchronized void interrupt() {
		//if already interrupted, do nothing
		if (interrupted)
			return;
		switch (getState()) {
			case NONE :
				return;
			case WAITING :
				//put the job to sleep if it is waiting to run
				setInterrupted(!sleep());
				break;
			case RUNNING :
				//make sure autobuild doesn't interrupt itself
				if (Job.getJobManager().currentJob() == this)
					return;
				setInterrupted(true);
				break;
		}
		//clear the autobuild avoidance flag if we were interrupted
		if (interrupted)
			avoidBuild = false;
	}

	synchronized boolean isInterrupted() {
		if (interrupted)
			return true;
		//check if another job is blocked by the build job
		if (isBlocking())
			setInterrupted(true);
		return interrupted;
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if (!ResourcesPlugin.PREF_AUTO_BUILDING.equals(event.getKey()))
			return;
		// get the new value of auto-build directly from the preferences
		boolean wasAutoBuilding = isAutoBuilding;
		isAutoBuilding = Platform.getPreferencesService().getBoolean(ResourcesPlugin.PI_RESOURCES,
				ResourcesPlugin.PREF_AUTO_BUILDING, false, null);
		if (wasAutoBuilding && !isAutoBuilding) {
			// stop the current autobuild when autobuild has been turned off
			interrupt();
		} else if (!wasAutoBuilding && isAutoBuilding) {
			// force a build when autobuild has been turned on
			noBuildJob.cancel();
			forceBuild = true;
			build(false);
		}
	}

	@Override
	public IStatus run(IProgressMonitor monitor) {
		SubMonitor subMonitor = SubMonitor.convert(monitor, 1);
		synchronized (this) {
			if (subMonitor.isCanceled() || isInterrupted()) {
				return canceled();
			}
		}
		//if the system is shutting down, don't build
		if (systemBundle.getState() == Bundle.STOPPING)
			return Status.OK_STATUS;
		try {
			doBuild(subMonitor.split(1));
			lastBuild = System.currentTimeMillis();
			//if the build was successful then it should not be recorded as interrupted
			setInterrupted(false);
			return Status.OK_STATUS;
		} catch (OperationCanceledException e) {
			return canceled();
		} catch (CoreException sig) {
			return sig.getStatus();
		}
	}

	/**
	 * Sets or clears the interrupted flag.
	 */
	private synchronized void setInterrupted(boolean value) {
		interrupted = value;
		// we do not "cancel" in case of interrupt but let the builder decide because
		// for example JDT builder can not resume from canceled autobuild but requires full build
		// cancel = explicit user request
		// interrupt = automatic conflict solving
		if (interrupted && Policy.DEBUG_BUILD_INTERRUPT) {
			traceMessageOrFullStack("was interrupted"); //$NON-NLS-1$
		}
	}

	/**
	 * Returns true if a build is actually needed, and false otherwise.
	 */
	private synchronized boolean shouldBuild() {
		try {
			//if auto-build is off then we never run
			if (!workspace.isAutoBuilding())
				return false;
			//build if the workspace requires a build (description changes)
			if (forceBuild)
				return true;
			if (avoidBuild)
				return false;
			//return whether there have been any changes to the workspace tree.
			return buildNeeded;
		} finally {
			//regardless of the result, clear the build flags for next time
			forceBuild = avoidBuild = buildNeeded = false;
		}
	}

	/**
	 * The class is required to maintain the ancient contract in
	 * IResourceChangeEvent, stating that even if autobuild is switched off, we
	 * still should send PRE_BUILD/POST_BUILD events. This job only send events, and
	 * never triggers a build.
	 */
	private final class AutoBuildOffJob extends Job {

		private AutoBuildOffJob() {
			super("Sending build events with disabled autobuild"); //$NON-NLS-1$
			setRule(workspace.getRoot());
			setSystem(true);
		}

		@Override
		public boolean belongsTo(Object family) {
			return family == ResourcesPlugin.FAMILY_AUTO_BUILD;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			// if the system is shutting down, don't build
			if (systemBundle.getState() == Bundle.STOPPING)
				return Status.OK_STATUS;
			final ISchedulingRule rule = workspace.getRuleFactory().buildRule();
			try {
				workspace.prepareOperation(rule, monitor);
				workspace.beginOperation(true);
				final int trigger = IncrementalProjectBuilder.AUTO_BUILD;
				workspace.broadcastBuildEvent(workspace, IResourceChangeEvent.PRE_BUILD, trigger);
				workspace.broadcastBuildEvent(workspace, IResourceChangeEvent.POST_BUILD, trigger);
			} catch (CoreException e) {
				return e.getStatus();
			} finally {
				try {
					workspace.endOperation(rule, false);
				} catch (CoreException e) {
					return e.getStatus();
				}
			}
			return Status.OK_STATUS;
		}
	}
}
