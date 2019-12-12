/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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
package org.eclipse.team.ui.mapping;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageSite;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This operation class can be used by model providers when performing
 * merge operations triggered from a synchronize participant page
 * associated with a synchronization or merge context.
 * <p>
 * This class may be subclasses by clients.
 *
 * @see ISynchronizationContext
 * @see IMergeContext
 *
 * @since 3.2
 */
public abstract class SynchronizationOperation extends TeamOperation {

	private final ISynchronizePageConfiguration configuration;
	private final Object[] elements;

	/*
	 * Helper method for extracting the part safely from a configuration
	 */
	private static IWorkbenchPart getPart(ISynchronizePageConfiguration configuration) {
		if (configuration != null) {
			ISynchronizePageSite site = configuration.getSite();
			if (site != null) {
				return site.getPart();
			}
		}
		return null;
	}

	/**
	 * Create a synchronize operation that operations on the given elements
	 * @param configuration the configuration for the page the operation is associated with
	 * @param elements the elements to be operated on
	 */
	protected SynchronizationOperation(ISynchronizePageConfiguration configuration, Object[] elements) {
		super(getPart(configuration), configuration.getRunnableContext());
		this.configuration = configuration;
		this.elements = elements;
	}

	/**
	 * Return the configuration for the page from which this
	 * operation was launched.
	 * @return the configuration for the page from which this
	 * operation was launched
	 */
	public ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Return the synchronization context associated with this action.
	 * @return the synchronization context associated with this action
	 */
	protected ISynchronizationContext getContext() {
		return (ISynchronizationContext)getConfiguration().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT);
	}

	/**
	 * Return the model elements that are the target of this operation.
	 * @return the model elements that are the target of this operation
	 */
	public Object[] getElements() {
		return elements;
	}

	/**
	 * Make <code>shouldRun</code> public so the result
	 * can be used to provide handler enablement
	 */
	@Override
	public boolean shouldRun() {
		return super.shouldRun();
	}

	/**
	 * Return the saveable that this operation will write its results
	 * to or <code>null</code> if the operation does not buffer
	 * its results. By default, <code>null</code> is returned but
	 * subclasses may override.
	 * @return the saveable that this operation will write its results
	 * to or <code>null</code>
	 */
	public SaveableComparison getSaveable() {
		return null;
	}

	@Override
	public final void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		try {
			monitor.beginTask(null, 100);
			setContextBusy(Policy.subMonitorFor(monitor, 5));
			execute(Policy.subMonitorFor(monitor, 90));
		} finally {
			clearContextBusy(Policy.subMonitorFor(monitor, 5));
			monitor.done();
		}
	}

	private void clearContextBusy(final IProgressMonitor monitor) {
		// Add a job change listener to the job manager that will clear the busy
		// when there are no more jobs related to the context running
		final IJobManager jobManager = Job.getJobManager();
		final IJobChangeListener listener = new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				Job[] jobs = jobManager.find(getContext());
				if (jobs.length == 0) {
					final IResourceDiffTree diffTree = getContext().getDiffTree();
					diffTree.clearBusy(null);
					jobManager.removeJobChangeListener(this);
				}
			}
		};
		jobManager.addJobChangeListener(listener);
	}

	private void setContextBusy(final IProgressMonitor monitor) {
		try {
			// TODO: This may miss setting some diffs (i.e. those that don't exist locally)
			ResourceTraversal[] traversals = Utils.getTraversals(getElements());
			final IResourceDiffTree diffTree = getContext().getDiffTree();
			IDiff[] diffs = diffTree.getDiffs(traversals);
			diffTree.setBusy(diffs, monitor);
		} catch (CoreException e) {
			TeamUIPlugin.log(e);
		}
	}

	/**
	 * Execute the operation. Subclasses should implement the operations behavior in the
	 * execute method. Clients should call either {@link #run()} or {@link #run(IProgressMonitor)}
	 * to invoke the operation.
	 * @param monitor a progress monitor
	 * @throws InvocationTargetException if an error occurs
	 * @throws InterruptedException if operation is interrupted
	 */
	protected abstract void execute(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException;

}
