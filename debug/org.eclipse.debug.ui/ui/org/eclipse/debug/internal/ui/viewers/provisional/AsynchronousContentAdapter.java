/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.viewers.provisional;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.internal.ui.viewers.AsynchronousSchedulingRuleFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;

/**
 * Abstract implementation of an asynchronous content adapter.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 3.2
 */
public abstract class AsynchronousContentAdapter implements IAsynchronousContentAdapter {

	protected static final Object[] EMPTY = new Object[0];

	@Override
	public void retrieveChildren(final Object parent, final IPresentationContext context, final IChildrenRequestMonitor result) {
		Job job = new Job("Retrieving Children") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (!monitor.isCanceled()) {
					computeChildren(parent, context, result);
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setRule(getRetrieveChildrenRule(parent, context));
		job.schedule();
	}

	/**
	 * Returns the scheduling rule for jobs retrieving children.
	 *
	 * @param parent the parent
	 * @param context the presentation context
	 * @return scheduling rule or <code>null</code>
	 */
	protected ISchedulingRule getRetrieveChildrenRule(Object parent, IPresentationContext context) {
		return AsynchronousSchedulingRuleFactory.getDefault().newSerialPerObjectRule(context);
	}


	@Override
	public void isContainer(final Object element, final IPresentationContext context, final IContainerRequestMonitor result) {
		Job job = new Job("Computing hasChildren") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (!monitor.isCanceled()) {
					computeIsContainer(element, context, result);
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setRule(getIsContainerRule(element, context));
		job.schedule();
	}

	/**
	 * Returns the scheduling rule for jobs determining if an element is a container.
	 *
	 * @param parent the parent
	 * @param context the presentation context
	 * @return scheduling rule or <code>null</code>
	 */
	protected ISchedulingRule getIsContainerRule(Object parent, IPresentationContext context) {
		return AsynchronousSchedulingRuleFactory.getDefault().newSerialPerObjectRule(context);
	}

	/**
	 * Computes the children for the given parent in the specified context.
	 *
	 * @param parent parent to retrieve children for
	 * @param context presentation context
	 * @param monitor result to report to
	 */
	protected void computeChildren(Object parent, IPresentationContext context, IChildrenRequestMonitor monitor) {
		if (!monitor.isCanceled()) {
			IStatus status = Status.OK_STATUS;
			try {
				if (supportsContext(context)) {
					monitor.addChildren(getChildren(parent, context));
				}
			} catch (CoreException e) {
				status = e.getStatus();
			}
			monitor.setStatus(status);
			monitor.done();
		}
	}

	/**
	 * Computes whether the given element is a container.
	 *
	 * @param parent potential parent
	 * @param context presentation context
	 * @param monitor result to report to
	 */
	protected void computeIsContainer(Object parent, IPresentationContext context, IContainerRequestMonitor monitor) {
		if (!monitor.isCanceled()) {
			IStatus status = Status.OK_STATUS;
			try {
				monitor.setIsContainer(hasChildren(parent, context));
			} catch (CoreException e) {
				status = e.getStatus();
			}
			monitor.setStatus(status);
			monitor.done();
		}
	}

	/**
	 * Returns the children for the given parent in the specified context.
	 *
	 * @param parent element to retrieve children for
	 * @param context context children will be presented in
	 * @return children
	 * @throws CoreException if an exception occurs retrieving children
	 */
	protected abstract Object[] getChildren(Object parent, IPresentationContext context) throws CoreException;

	/**
	 * Returns whether the given element has children in the specified context.
	 *
	 * @param element element that may have children
	 * @param context context element will be presented in
	 * @return whether the given element has children in the specified context
	 * @throws CoreException if an exception occurs determining whether the
	 *  element has children
	 */
	protected abstract boolean hasChildren(Object element, IPresentationContext context) throws CoreException;

	/**
	 * Returns whether this adapter supports the given context.
	 *
	 * @param context the presentation context
	 * @return whether this adapter supports the given context
	 */
	protected boolean supportsContext(IPresentationContext context) {
		return supportsPartId(context.getId());
	}

	/**
	 * Returns whether this adapter provides content in the specified part.
	 *
	 * @param id part id
	 * @return whether this adapter provides content in the specified part
	 */
	protected abstract boolean supportsPartId(String id);
}
