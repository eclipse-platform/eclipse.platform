/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core;

import java.util.Stack;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.internal.core.Messages;

/**
 * Install progress monitor
 * Delegating wrapper for IProgressMonitor used for installation handling.
 * 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class InstallMonitor implements IProgressMonitor {

	protected IProgressMonitor monitor;
	protected Stack tasks;

	protected String taskString;
	protected String subTaskString;
	protected boolean showDetails;
	protected long totalCopyCount;
	protected	long currentCount = 0;

	protected class MonitorState {

		private String taskString;
		private String subTaskString;
		private boolean showDetails;
		private long totalCopyCount;
        private long currentCount;

		private MonitorState(
			String taskString,
			String subTaskString,
			boolean showDetails,
            long currentCount,
			long totalCopyCount) {
			this.taskString = taskString;
			this.subTaskString = subTaskString;
			this.showDetails = showDetails;
            this.currentCount = currentCount;
			this.totalCopyCount = totalCopyCount;
		}

		private String getTaskString() {
			return this.taskString;
		}

		private String getSubTaskString() {
			return this.subTaskString;
		}

		private boolean getShowDetails() {
			return this.showDetails;
		}

        private long getCurrentCount() {
            return this.currentCount;
        }
        
		private long getTotalCopyCount() {
			return this.totalCopyCount;
		}
	}
	
	protected InstallMonitor() {
	}

	/**
	 * Install monitor constructor
	 * 
	 * @param monitor base install monitor
	 * @since 2.0
	 */
	public InstallMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
		this.tasks = new Stack();
		this.taskString = ""; //$NON-NLS-1$
		this.subTaskString = ""; //$NON-NLS-1$
		this.showDetails = false;
		this.totalCopyCount = 0;
	}

	/**
	 * Begin new monitor task.
	 * 
	 * @see IProgressMonitor#beginTask(String, int)
	 * @since 2.0
	 */
	public void beginTask(String name, int totalWork) {
		taskString = name;
		monitor.beginTask(name, totalWork);
	}

	/**
	 * Indicate completion of monitor activity.
	 * 
	 * @see IProgressMonitor#done()
	 * @since 2.0
	 */
	public void done() {
		monitor.done();
	}

	/**
	 * Indicate monitor progress.
	 * 
	 * @see IProgressMonitor#internalWorked(double)
	 * @since 2.0
	 */
	public void internalWorked(double work) {
		monitor.internalWorked(work);
	}

	/**
	 * Check is use indicated that the operation be cancelled.
	 * 
	 * @see IProgressMonitor#isCanceled()
	 * @since 2.0
	 */
	public boolean isCanceled() {
		return monitor.isCanceled();
	}

	/**
	 * Set the cancellation state.
	 * 
	 * @see IProgressMonitor#setCanceled(boolean)
	 * @since 2.0
	 */
	public void setCanceled(boolean value) {
		monitor.setCanceled(value);
	}

	/**
	 * Set task name.
	 * 
	 * @see IProgressMonitor#setTaskName(String)
	 * @since 2.0
	 */
	public void setTaskName(String name) {
		this.taskString = name;
		this.subTaskString = ""; //$NON-NLS-1$
		this.showDetails = false;
		this.totalCopyCount = 0;
		monitor.subTask(""); //$NON-NLS-1$
		monitor.setTaskName(name);
	}

	/**
	 * Set subtask name.
	 * 
	 * @see IProgressMonitor#subTask(String)
	 * @since 2.0
	 */
	public void subTask(String name) {
		this.subTaskString = name;
		this.showDetails = false;
		this.totalCopyCount = 0;
		monitor.subTask(name);
	}

	/**
	 * Indicate monitor progress.
	 * 
	 * @see IProgressMonitor#worked(int)
	 * @since 2.0
	 */
	public void worked(int work) {
		monitor.worked(work);
	}

	/**
	 * Save the current monitor state.
	 * The states are saved on a push-down stack. Prior states
	 * can be restored by calling restorState()
	 * 
	 * @see #restoreState()
	 * @since 2.0
	 */
	public void saveState() {
		tasks.push(
			new MonitorState(taskString, subTaskString, showDetails, currentCount, totalCopyCount));
	}

	/**
	 * Restore the monitor state.
	 * 
	 * @see #saveState()
	 * @since 2.0
	 */
	public void restoreState() {
		if (tasks.size() > 0) {
			MonitorState state = (MonitorState) tasks.pop();
			setTaskName(state.getTaskString());
			subTask(state.getSubTaskString());
			this.showDetails = state.getShowDetails();
            this.currentCount = state.getCurrentCount();
			this.totalCopyCount = state.getTotalCopyCount();
		}
	}

	/**
	 * Indicate whether the monitor subtask message should include
	 * copy progress counts.
	 * 
	 * @see #setCopyCount(long)
	 * @see #setTotalCount(long)
	 * @param setting <code>true</code> to show the copy count,
	 * <code>false</code> otherwise
	 * @since 2.0
	 */
	public void showCopyDetails(boolean setting) {
		this.showDetails = setting;
	}

	/**
	 * Sets the total number of bytes to copy.
	 * 
	 * @see #showCopyDetails(boolean)
	 * @see #setCopyCount(long)
	 * @param count total number of bytes to copy.
	 * @since 2.0
	 */
	public void setTotalCount(long count) {
		this.totalCopyCount = count;
	}

	/**
	 * Sets the number of bytes already copied.
	 * 
	 * @see #showCopyDetails(boolean)
	 * @see #setTotalCount(long)
	 * @param count number of bytes already copied.
	 * @since 2.0
	 */
	public void setCopyCount(long count) {
		if (showDetails && count > 0) {
			currentCount = count;
			long countK = count / 1024;
			long totalK = totalCopyCount / 1024;
			String msg =
				(totalK <= 0)
					? NLS.bind(Messages.InstallMonitor_DownloadSize, (new String[] { Long.toString(countK) }))
					: NLS.bind(Messages.InstallMonitor_DownloadSizeLong, (new String[] { Long.toString(countK), Long.toString(totalK) }));
			monitor.subTask(subTaskString + msg);
		}
	}
	
	/**
	 * Increments the number of bytes copied.
	 * 
	 * @param increment number of new bytes  copied.
	 * @since 3.0
	 */
	public void incrementCount(long increment) {
		setCopyCount(currentCount + increment);
	}
}
