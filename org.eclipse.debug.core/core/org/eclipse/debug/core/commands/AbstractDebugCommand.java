/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.debug.core.commands;

import java.util.Collections;
import java.util.LinkedHashSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IRequest;
import org.eclipse.debug.internal.core.DebugOptions;

/**
 * Abstract implementation of a debug command handler. Handles {@link IDebugCommandRequest}
 * and {@link IEnabledStateRequest} updates asynchronously using jobs.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 3.6
 */
public abstract class AbstractDebugCommand implements IDebugCommandHandler {

	/**
	 * Job to update enabled state of action.
	 */
	private class UpdateJob extends Job implements IJobChangeListener {

		/**
		 * The request to update
		 */
		private IEnabledStateRequest request;

		/**
		 * Whether this job has been run
		 */
		private boolean run = false;

		/**
		 * Creates a new job to update the specified request
		 *
		 * @param stateRequest the {@link IEnabledStateRequest}
		 */
		UpdateJob(IEnabledStateRequest stateRequest) {
			super(getEnabledStateTaskName());
			request = stateRequest;
			setSystem(true);
			setRule(getEnabledStateSchedulingRule(request));
			getJobManager().addJobChangeListener(this);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			run = true;
			if (DebugOptions.DEBUG_COMMANDS) {
				DebugOptions.trace("can execute command: " + AbstractDebugCommand.this); //$NON-NLS-1$
			}
			if (monitor.isCanceled()) {
				if (DebugOptions.DEBUG_COMMANDS) {
					DebugOptions.trace(" >> *CANCELED* <<"); //$NON-NLS-1$
				}
				request.cancel();
			}
			Object[] elements = request.getElements();
			Object[] targets = new Object[elements.length];
			if (!request.isCanceled()) {
				for (int i = 0; i < elements.length; i++) {
					targets[i] = getTarget(elements[i]);
					if (targets[i] == null) {
						request.setEnabled(false);
						request.cancel();
						if (DebugOptions.DEBUG_COMMANDS) {
							DebugOptions.trace(" >> false (no adapter)"); //$NON-NLS-1$
						}
					}
				}
				if (monitor.isCanceled()) {
					request.cancel();
				}
			}
			if (!request.isCanceled()) {
				targets = coalesce(targets);
				monitor.beginTask(getEnabledStateTaskName(), targets.length);
				try {
					boolean executable = isExecutable(targets, monitor, request);
					if (DebugOptions.DEBUG_COMMANDS) {
						DebugOptions.trace(" >> " + executable); //$NON-NLS-1$
					}
					request.setEnabled(executable);
				} catch (CoreException e) {
					request.setStatus(e.getStatus());
					request.setEnabled(false);
					if (DebugOptions.DEBUG_COMMANDS) {
						DebugOptions.trace(" >> ABORTED"); //$NON-NLS-1$
						DebugOptions.trace("\t" + e.getStatus().getMessage()); //$NON-NLS-1$
					}
				}
			}
			monitor.setCanceled(request.isCanceled());
			request.done();
			monitor.done();
			return Status.OK_STATUS;
		}

		@Override
		public boolean belongsTo(Object family) {
			Object myFamily = getEnabledStateJobFamily(request);
			if (myFamily != null) {
				return myFamily.equals(family);
			}
			return false;
		}

		@Override
		public void aboutToRun(IJobChangeEvent event) {
		}

		@Override
		public void awake(IJobChangeEvent event) {
		}

		@Override
		public void done(IJobChangeEvent event) {
			if (event.getJob() == this) {
				if (!run) {
					request.cancel();
					request.done();
					if (DebugOptions.DEBUG_COMMANDS) {
						DebugOptions.trace(" >> *CANCELED* <<" + AbstractDebugCommand.this); //$NON-NLS-1$
					}
				}
				getJobManager().removeJobChangeListener(this);
			}
		}

		@Override
		public void running(IJobChangeEvent event) {
		}

		@Override
		public void scheduled(IJobChangeEvent event) {
		}

		@Override
		public void sleeping(IJobChangeEvent event) {
		}

		@Override
		public String toString() {
			return getName() + " on " + request; //$NON-NLS-1$
		}

	}

	/**
	 * Scheduling rule to serialize commands on an object
	 */
	private static class SerialPerObjectRule implements ISchedulingRule {

		private Object fObject = null;

		public SerialPerObjectRule(Object lock) {
			fObject = lock;
		}

		@Override
		public boolean contains(ISchedulingRule rule) {
			return rule == this;
		}

		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			if (rule instanceof SerialPerObjectRule) {
				SerialPerObjectRule vup = (SerialPerObjectRule) rule;
				return fObject == vup.fObject;
			}
			return false;
		}

	}

	@Override
	public boolean execute(final IDebugCommandRequest request) {
		Job job = new Job(getExecuteTaskName()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (DebugOptions.DEBUG_COMMANDS) {
					DebugOptions.trace("execute: " + AbstractDebugCommand.this); //$NON-NLS-1$
				}
				Object[] elements = request.getElements();
				Object[] targets = new Object[elements.length];
				for (int i = 0; i < elements.length; i++) {
					targets[i]= getTarget(elements[i]);
				}
				targets = coalesce(targets);
				monitor.beginTask(getExecuteTaskName(), targets.length);
				try {
					doExecute(targets, monitor, request);
				} catch (CoreException e) {
					request.setStatus(e.getStatus());
					if (DebugOptions.DEBUG_COMMANDS) {
						DebugOptions.trace("\t" + e.getStatus().getMessage()); //$NON-NLS-1$
					}
				}
				request.done();
				monitor.setCanceled(request.isCanceled());
				monitor.done();
				return Status.OK_STATUS;
			}
			@Override
			public boolean belongsTo(Object family) {
				Object jobFamily = getExecuteJobFamily(request);
				if (jobFamily != null) {
					return jobFamily.equals(family);
				}
				return false;
			}
		};
		job.setSystem(true);
		job.setRule(getExecuteSchedulingRule(request));
		job.schedule();
		return isRemainEnabled(request);
	}

	/**
	 * Returns whether this command should remain enabled after starting execution of the specified request.
	 *
	 * @param request the request being executed
	 * @return whether to remain enabled while executing the request
	 */
	protected boolean isRemainEnabled(IDebugCommandRequest request) {
		return false;
	}

	@Override
	public void canExecute(final IEnabledStateRequest request) {
		Job job = new UpdateJob(request);
		job.schedule();
	}

	/**
	 * Returns the name to use for a job and progress monitor task names when performing
	 * an {@link IEnabledStateRequest}.
	 *
	 * @return task name
	 */
	protected String getEnabledStateTaskName() {
		// this is a system job name and does not need to be NLS'd
		return "Check Debug Command"; //$NON-NLS-1$
	}

	/**
	 * Returns the name to use for jobs and progress monitor task names when executing
	 * an {@link IDebugCommandRequest}.
	 *
	 * @return task name
	 */
	protected String getExecuteTaskName() {
		// this is a system job name and does not need to be NLS'd
		return "Execute Debug Command"; //$NON-NLS-1$
	}

	/**
	 * Executes this command synchronously on the specified targets, reporting progress. This method
	 * is called by a job. If an exception is thrown, the calling job will set the associated status
	 * on the request object. The calling job also calls #done() on the request object after this method
	 * is called, and sets a cancel status on the progress monitor if the request is canceled.
	 * <p>
	 * Handlers must override this method.
	 * </p>
	 * @param targets objects to perform this command on
	 * @param monitor progress monitor
	 * @param request can be used to cancel this command
	 * @exception CoreException if this handler fails to perform the request
	 */
	protected abstract void doExecute(Object[] targets, IProgressMonitor monitor, IRequest request) throws CoreException;

	/**
	 * Returns whether this command is executable on the specified targets, reporting progress. This method
	 * is called by a job. If an exception is thrown, the calling job will set the associated status
	 * on the request object and report that this command is not enabled. The calling job also calls #done()
	 * on the request object after this method is called, and sets a cancel status on the progress monitor if
	 * the request is canceled. Enabled state is set to <code>false</code> if the request is canceled.
	 * <p>
	 * Handlers must override this method.
	 * </p>
	 * @param targets objects to check command enabled state for
	 * @param monitor progress monitor
	 * @param request can be used to cancel this update request
	 * @return whether this command can be executed for the given targets
	 * @throws CoreException if a problem is encountered
	 */
	protected abstract boolean isExecutable(Object[] targets, IProgressMonitor monitor, IEnabledStateRequest request) throws CoreException;

	/**
	 * Returns the appropriate target for this command handler for the given object.
	 * This method is called to map each element in a command request to the target
	 * object that is used in {@link #doExecute(Object[], IProgressMonitor, IRequest)}
	 * and {@link #isExecutable(Object[], IProgressMonitor, IEnabledStateRequest)}.
	 * The target may be the element itself, or some other object. Allows for redirection.
	 * <p>
	 * Clients must override this method.
	 * </p>
	 * @param element element from a {@link IDebugCommandRequest}
	 * @return associated target object for execution or enabled state update. Cannot return <code>null</code>.
	 */
	protected abstract Object getTarget(Object element);

	/**
	 * Convenience method to return an adapter of the specified type for the given object or <code>null</code>
	 * if none.
	 *
	 * @param element element to retrieve adapter for
	 * @param type adapter type
	 * @return adapter or <code>null</code>
	 */
	protected Object getAdapter(Object element, Class<?> type) {
		return DebugPlugin.getAdapter(element, type);
	}

	/**
	 * Returns a scheduling rule for this command's {@link IEnabledStateRequest} update job
	 * or <code>null</code> if none. By default a rule is created to serialize
	 * jobs on the first element in the request.
	 * <p>
	 * Clients may override this method as required.
	 * </p>
	 * @param request request that a scheduling rule is required for
	 * @return scheduling rule or <code>null</code>
	 */
	protected ISchedulingRule getEnabledStateSchedulingRule(IDebugCommandRequest request) {
		return new SerialPerObjectRule(request.getElements()[0]);
	}

	/**
	 * Returns a scheduling rule for this command's {@link IDebugCommandRequest} execute job
	 * or <code>null</code> if none. By default, execution jobs have no scheduling rule.
	 * <p>
	 * Clients may override this method as required.
	 * </p>
	 * @param request request that a scheduling rule is required for
	 * @return scheduling rule or <code>null</code>
	 */
	protected ISchedulingRule getExecuteSchedulingRule(IDebugCommandRequest request) {
		return null;
	}

	/**
	 * Returns the job family for the this command's {@link IEnabledStateRequest} update job
	 * or <code>null</code> if none. The default implementation returns <code>null</code>.
	 * <p>
	 * Clients may override this method as required.
	 * </p>
	 * @param request request the job family is required for
	 * @return job family object or <code>null</code> if none
	 */
	protected Object getEnabledStateJobFamily(IDebugCommandRequest request) {
		return null;
	}

	/**
	 * Returns the job family for the this command's {@link IDebugCommandRequest} execute job
	 * or <code>null</code> if none. The default implementation returns <code>null</code>.
	 * <p>
	 * Clients may override this method as required.
	 * </p>
	 * @param request request the job family is required for
	 * @return job family object or <code>null</code> if none
	 */
	protected Object getExecuteJobFamily(IDebugCommandRequest request) {
		return null;
	}

	/**
	 * Returns an array of objects with duplicates removed, if any.
	 *
	 * @param objects array of objects
	 * @return array of object in same order with duplicates removed, if any.
	 */
	private Object[] coalesce(Object[] objects) {
		if (objects.length == 1) {
			return objects;
		} else {
			LinkedHashSet<Object> set = new LinkedHashSet<>(objects.length);
			Collections.addAll(set, objects);
			return set.toArray();
		}
	}

}
