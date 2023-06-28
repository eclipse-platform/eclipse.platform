/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.model.elements;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerInputProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerInputUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;

/**
 * Common code for viewer input providers. Creates a job to process request asynchronously.

 * @since 3.4
 */
public abstract class ViewerInputProvider implements IViewerInputProvider {

	protected static final Object[] EMPTY = new Object[0];

	@Override
	public void update(final IViewerInputUpdate update) {
		Job job = new Job("viewer input resolution") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (!update.isCanceled()) {
					retrieveInput(update);
				}
				update.done();
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.setRule(getRule(update));
		job.schedule();
	}

	/**
	 * Computes the viewer input for the specified context.
	 *
	 * @param update update request
	 */
	protected void retrieveInput(IViewerInputUpdate update) {
		if (!update.isCanceled()) {
			IStatus status = Status.OK_STATUS;
			try {
				IPresentationContext context = update.getPresentationContext();
				if (supportsContext(context)) {
					update.setInputElement(getViewerInput(update.getElement(), context, update));
				} else {
					update.setInputElement(update.getElement());
				}
			} catch (CoreException e) {
				status = e.getStatus();
			}
			update.setStatus(status);
		}
	}


	/**
	 * Returns the viewer input derived from the given source object in the specified
	 * context, possibly <code>null</code>.
	 *
	 * @param source element to derive a viewer input from
	 * @param context context for which an input is requested
	 * @param update viewer update request
	 * @throws CoreException if an exception occurs retrieving child
	 */
	protected abstract Object getViewerInput(Object source, IPresentationContext context, IViewerUpdate update) throws CoreException;


	/**
	 * Returns whether this adapter supports the given context.
	 *
	 * @param context
	 * @return whether this adapter supports the given context
	 */
	protected boolean supportsContext(IPresentationContext context) {
		return supportsContextId(context.getId());
	}

	/**
	 * Returns whether this adapter provides content in the specified context id.
	 *
	 * @param id part id
	 * @return whether this adapter provides content in the specified context id
	 */
	protected abstract boolean supportsContextId(String id);

	/**
	 * Returns a scheduling rule to use when performing the given updates or
	 * <code>null</code> if none.
	 *
	 * @param update
	 * @return scheduling rule or <code>null</code> if none
	 */
	protected ISchedulingRule getRule(IViewerInputUpdate update) {
		return null;
	}

}
