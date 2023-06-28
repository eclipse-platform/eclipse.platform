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
import org.eclipse.debug.internal.ui.views.launch.DebugElementHelper;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.progress.UIJob;

/**
 * Abstract implementation of an asynchronous label adapter
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 3.2
 */
public abstract class AsynchronousLabelAdapter implements IAsynchronousLabelAdapter {

	@Override
	public void retrieveLabel(final Object element, final IPresentationContext context, final ILabelRequestMonitor result) {
		Job job = null;
		if (requiresUIJob(element)) {
			job = new UIJob("Retrieving labels") { //$NON-NLS-1$
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					computeLabels(element, context, result);
					return Status.OK_STATUS;
				}
			};
		} else {
			job = new Job("Retrieving labels") { //$NON-NLS-1$
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					computeLabels(element, context, result);
					return Status.OK_STATUS;
				}
			};
		}
		job.setSystem(true);
		job.setRule(getLabelRule(element, context));
		job.schedule();
	}

	/**
	 * Returns the scheduling rule for label jobs.
	 *
	 * @param element the element context
	 * @param context the presentation context
	 * @return scheduling rule or <code>null</code>
	 */
	protected ISchedulingRule getLabelRule(Object element, IPresentationContext context) {
		return AsynchronousSchedulingRuleFactory.getDefault().newSerialPerObjectRule(context);
	}

	/**
	 * Returns whether this label adapter requires to be run in the UI thread.
	 * By default, label jobs are not run in the UI thread. Subclasses should
	 * override if required.
	 * @param object the object context
	 *
	 * @return whether this label adapter requires to be run in the UI thread.
	 */
	protected boolean requiresUIJob(Object object) {
		return !DebugElementHelper.requiresUIThread(object);
	}

	/**
	 * Computes label attributes for the given element in the specified context.
	 *
	 * @param element element to compute label for
	 * @param context presentation context
	 * @param monitor monitor to report results to
	 */
	protected void computeLabels(Object element, IPresentationContext context, ILabelRequestMonitor monitor) {
		if (!monitor.isCanceled()) {
			IStatus status = Status.OK_STATUS;
			try {
				monitor.setLabels(getLabels(element, context));
				if (!monitor.isCanceled()) {
					monitor.setImageDescriptors(getImageDescriptors(element, context));
				}
				if (!monitor.isCanceled()) {
					monitor.setFontDatas(getFontDatas(element, context));
				}
				if (!monitor.isCanceled()) {
					monitor.setBackgrounds(getBackgrounds(element, context));
				}
				if (!monitor.isCanceled()) {
					monitor.setForegrounds(getForegrounds(element, context));
				}
			} catch (CoreException e) {
				status = e.getStatus();
			}
			if (!monitor.isCanceled()) {
				monitor.setStatus(status);
				monitor.done();
			}
		}
	}

	/**
	 * Returns a label for the give element in the specified context.
	 *
	 * @param element element to compute label for
	 * @param context presentation context
	 * @return label
	 * @exception CoreException if an exception occurs computing label
	 */
	protected abstract String[] getLabels(Object element, IPresentationContext context) throws CoreException;

	/**
	 * Returns an image descriptor for the given element in the specified context
	 * or <code>null</code>.
	 *
	 * @param element element to compute image descriptor for
	 * @param context presentation context
	 * @return image descriptor or <code>null</code>
	 * @throws CoreException if an exception occurs computing image descriptor
	 */
	protected abstract ImageDescriptor[] getImageDescriptors(Object element, IPresentationContext context) throws CoreException;

	/**
	 * Returns font data for the given element in the specified context or <code>null</code>
	 * to use the default font.
	 *
	 * @param element element to compute font data for
	 * @param context presentation context
	 * @return font data or <code>null</code>
	 * @throws CoreException if an exception occurs computing font data
	 */
	protected abstract FontData[] getFontDatas(Object element, IPresentationContext context) throws CoreException;

	/**
	 * Returns a foreground color for the given element in the specified context or <code>null</code>
	 * to use the default color.
	 *
	 * @param element element to compute color for
	 * @param context presentation context
	 * @return color or <code>null</code>
	 * @throws CoreException if an exception occurs computing color
	 */
	protected abstract RGB[] getForegrounds(Object element, IPresentationContext context) throws CoreException;

	/**
	 * Returns a background color for the given element in the specified context or <code>null</code>
	 * to use the default color.
	 *
	 * @param element element to compute color for
	 * @param context presentation context
	 * @return color or <code>null</code>
	 * @throws CoreException if an exception occurs computing color
	 */
	protected abstract RGB[] getBackgrounds(Object element, IPresentationContext context) throws CoreException;
}
