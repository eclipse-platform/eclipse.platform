/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
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
 *     Patrick Chuong (Texas Instruments) - added support for checkbox (Bug 286310)
 *     Patrick Chuong (Texas Instruments) - bug fix 306768
 *******************************************************************************/
package org.eclipse.debug.internal.ui.model.elements;

import java.util.LinkedList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ICheckUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.progress.UIJob;

/**
 * Implementation of a context sensitive label provider, which provides
 * base functionality for subclasses such as label jobs and a basic label updater.
 *
 * @since 3.3.0.qualifier
 */
public abstract class ElementLabelProvider implements IElementLabelProvider {

	private Job fLabelJob = null;

	/**
	 * Describes a label job
	 */
	interface ILabelJob {
		/**
		 * Returns whether the updates were queued.
		 *
		 * @param updates updates
		 * @return whether the updates were queued
		 */
		boolean queue(ILabelUpdate[] updates);
	}

	/**
	 * A <code>Job</code> to update labels. This <code>Job</code> can run
	 * in a non-UI thread.
	 */
	class LabelJob extends Job implements ILabelJob {

		private LabelUpdater fUpdater = new LabelUpdater();

		public LabelJob() {
			super("Label Job"); //$NON-NLS-1$
			setSystem(true);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			fUpdater.run();
			return Status.OK_STATUS;
		}

		@Override
		public boolean queue(ILabelUpdate[] updates) {
			return fUpdater.queue(updates);
		}

		@Override
		public boolean shouldRun() {
			return fUpdater.shouldRun();
		}

	}

	/**
	 * A <code>Job</code> to update labels. This <code>Job</code> runs
	 * only in the UI thread.
	 */
	class UILabelJob extends UIJob implements ILabelJob {

		private LabelUpdater fUpdater = new LabelUpdater();

		public UILabelJob() {
			super("Label Job"); //$NON-NLS-1$
			setSystem(true);
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			fUpdater.run();
			return Status.OK_STATUS;
		}

		@Override
		public boolean queue(ILabelUpdate[] updates) {
			return fUpdater.queue(updates);
		}

		@Override
		public boolean shouldRun() {
			return fUpdater.shouldRun();
		}
	}

	/**
	 * Queue of label updates
	 */
	class LabelUpdater implements Runnable {
		LinkedList<ILabelUpdate> fQueue = new LinkedList<>();

		public synchronized boolean queue(ILabelUpdate[] updates) {
			if (fQueue == null) {
				return false;
			} else {
				for (ILabelUpdate update : updates) {
					fQueue.addLast(update);
				}
				return true;
			}
		}

		@Override
		public void run() {
			ILabelUpdate update = getNextUpdate();
			while (update != null) {
				ISchedulingRule rule = getRule(update);
				if (!update.isCanceled()) {
					try {
						if (rule != null) {
							Job.getJobManager().beginRule(rule, null);
						}
						retrieveLabel(update);
					} catch (CoreException e) {
						update.setStatus(e.getStatus());
					} finally {
						if (rule != null) {
							Job.getJobManager().endRule(rule);
						}
					}
				}
				update.done();
				update = getNextUpdate();
			}
		}

		/**
		 * Returns the next update to process, if there is one in the
		 * queue. If there are no queued items <code>null</code> is returned
		 * @return the next queued item or <code>null</code> if the queue is empty.
		 */
		public synchronized ILabelUpdate getNextUpdate() {
			if (fQueue == null) {
				return null;
			}
			if (fQueue.isEmpty()) {
				fQueue = null;
				return null;
			}
			return fQueue.removeFirst();
		}

		public boolean shouldRun() {
			return fQueue != null;
		}
	}

	/**
	 * Retrieves label attributes for the specified update.
	 *
	 * @param update
	 */
	protected void retrieveLabel(ILabelUpdate update) throws CoreException {
		String[] columnIds = update.getColumnIds();
		IPresentationContext presentationContext = update.getPresentationContext();
		TreePath elementPath = update.getElementPath();
		int numColumns = 1;
		if (columnIds != null) {
			numColumns = columnIds.length;
		}
		for (int i = 0; i < numColumns; i++) {
			String columnId = null;
			if (columnIds != null) {
				columnId = columnIds[i];
			}
			update.setLabel(getLabel(elementPath, presentationContext, columnId, i), i);
			update.setImageDescriptor(getImageDescriptor(elementPath, presentationContext, columnId, i), i);
			update.setBackground(getBackground(elementPath, presentationContext, columnId), i);
			update.setForeground(getForeground(elementPath, presentationContext, columnId), i);
			update.setFontData(getFontData(elementPath, presentationContext, columnId), i);
			if (update instanceof ICheckUpdate &&
				Boolean.TRUE.equals(presentationContext.getProperty(ICheckUpdate.PROP_CHECK)))
			{
				((ICheckUpdate) update).setChecked(
					getChecked(elementPath, presentationContext), getGrayed(elementPath, presentationContext));
			}
		}
	}

	/**
	 * Returns the <code>FontData</code> for the path in the given column with the current presentation
	 * @param elementPath
	 * @param presentationContext
	 * @param columnId
	 * @return font information or <code>null</code>
	 * @throws CoreException
	 */
	protected FontData getFontData(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException {
		return null;
	}

	/**
	 * Returns the <code>RGB</code> foreground colour for the path in the given column with the current presentation
	 * @param elementPath
	 * @param presentationContext
	 * @param columnId
	 * @return color or <code>null</code>
	 * @throws CoreException
	 */
	protected RGB getForeground(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException {
		return null;
	}

	/**
	 * Returns the <code>RGB</code> background colour for the path in the given column with the current presentation
	 * @param elementPath
	 * @param presentationContext
	 * @param columnId
	 * @return color or <code>null</code>
	 * @throws CoreException
	 */
	protected RGB getBackground(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException {
		return null;
	}

	/**
	 * Returns the <code>ImageDescriptor</code> for the path in the given column with the current presentation
	 * @param elementPath
	 * @param presentationContext
	 * @param columnId
	 * @return image descriptor or <code>null</code>
	 * @throws CoreException
	 */
	protected ImageDescriptor getImageDescriptor(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException {
		return null;
	}

	/**
	 * Returns the <code>ImageDescriptor</code> for the path in the given column with the current presentation
	 * @param elementPath
	 * @param presentationContext
	 * @param columnId
	 * @param columnIndex
	 * @return image descriptor or <code>null</code>
	 * @throws CoreException
	 *
	 * @since 3.6
	 */
	protected ImageDescriptor getImageDescriptor(TreePath elementPath, IPresentationContext presentationContext, String columnId, int columnIndex) throws CoreException {
		return getImageDescriptor(elementPath, presentationContext, columnId);
	}

	/**
	 * Returns the label for the path in the given column with the current presentation
	 * @param element
	 * @param presentationContext
	 * @param columnId
	 * @return label
	 */
	protected abstract String getLabel(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException;

	/**
	 * Returns the label for the path in the given column with the current presentation.
	 * @param elementPath
	 * @param presentationContext
	 * @param columnId
	 * @param columnIndex
	 * @return label
	 *
	 * @since 3.6
	 */
	protected String getLabel(TreePath elementPath, IPresentationContext presentationContext, String columnId, int columnIndex) throws CoreException {
		return getLabel(elementPath, presentationContext, columnId);
	}

	/**
	 * Returns the checked state for the given path.
	 *
	 * @param path Path of the element to retrieve the grayed state for.
	 * @param presentationContext Presentation context where the element is
	 * displayed.
	 * @return <code>true<code> if the element check box should be checked
	 * @throws CoreException
	 *
	 * @since 3.6
	 */
	protected boolean getChecked(TreePath path, IPresentationContext presentationContext) throws CoreException {
		return false;
	}

	/**
	 * Returns the grayed state for the given path.
	 *
	 * @param path Path of the element to retrieve the grayed state for.
	 * @param presentationContext Presentation context where the element is
	 * displayed.
	 * @return <code>true<code> if the element check box should be grayed
	 * @throws CoreException
	 *
	 * @since 3.6
	 */
	protected boolean getGrayed(TreePath path, IPresentationContext presentationContext) throws CoreException {
		return false;
	}

	@Override
	public synchronized void update(ILabelUpdate[] updates) {
		if (fLabelJob == null) {
			fLabelJob = newLabelJob(updates);
		}
		if (!((ILabelJob)fLabelJob).queue(updates)) {
			fLabelJob = newLabelJob(updates);
			((ILabelJob)fLabelJob).queue(updates);
		}
		// TODO: rule
		fLabelJob.schedule();
	}

	/**
	 * Returns a new <code>Job</code> to update the specified labels. This method
	 * is used to determine if a UI job is needed or not, in the event the request for an update
	 * job has come from a non-UI thread.
	 * @param updates an array of pending label updates
	 * @return a new <code>Job</code> to update labels with.
	 */
	private Job newLabelJob(ILabelUpdate[] updates) {
		if (requiresUIJob(updates)) {
			return new UILabelJob();
		} else {
			return new LabelJob();
		}
	}

	/**
	 * Returns whether a UI job should be used for updates versus a non-UI job.
	 * @param updates
	 * @return true if the array of updates requires a UI job to update the labels, false otherwise
	 */
	protected boolean requiresUIJob(ILabelUpdate[] updates) {
		return false;
	}

	/**
	 * Returns the scheduling rule for the given update or <code>null</code>
	 * it none.
	 *
	 * @param update label update
	 * @return associated scheduling rule, or <code>null</code>
	 */
	protected ISchedulingRule getRule(ILabelUpdate update) {
		return null;
	}

}
