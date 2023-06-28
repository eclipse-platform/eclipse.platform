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
package org.eclipse.debug.internal.ui.viewers;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.debug.internal.ui.commands.actions.AbstractRequestMonitor;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Base implementation of an asynchronous request monitor.
 * <p>
 * Not intended to be sub-classed or instantiated by clients. For internal use
 * with the <code>AsynchronousViewer</code> implementation.
 * </p>
 * @since 3.2
 */
public abstract class AsynchronousRequestMonitor extends AbstractRequestMonitor {

	/**
	 * Model node the update is rooted at
	 */
	private ModelNode fNode;

	/**
	 * Model the update is being performed for
	 */
	private AsynchronousModel fModel;

	/**
	 * Whether this request's 'done' method has been called.
	 */
	private boolean fDone = false;

	protected WorkbenchJob fViewerUpdateJob = new WorkbenchJob("Asynchronous viewer update") { //$NON-NLS-1$
		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			// necessary to check if widget is disposed. The item may
			// have been removed from the tree when another children update
			// occurred.
			getModel().viewerUpdateScheduled(AsynchronousRequestMonitor.this);
			if (fDone) {
				getModel().requestComplete(AsynchronousRequestMonitor.this);
			}
			if (!isCanceled() && !getNode().isDisposed()) {
				IStatus status = getStatus();
				if (status != null && !status.isOK()) {
					getModel().getViewer().handlePresentationFailure(AsynchronousRequestMonitor.this, status);
				} else {
					performUpdate();
				}
			}
			getModel().viewerUpdateComplete(AsynchronousRequestMonitor.this);
			return Status.OK_STATUS;
		}
	};

	/**
	 * Constructs an update rooted at the given item.
	 *
	 * @param node model node
	 * @param model model the node is in
	 */
	public AsynchronousRequestMonitor(ModelNode node, AsynchronousModel model) {
		fNode = node;
		fModel = model;
		// serialize updates per viewer
		fViewerUpdateJob.setRule(getUpdateSchedulingRule());
		fViewerUpdateJob.setSystem(true);
	}

	/**
	 * Returns the scheduling rule for viewer update job.
	 *
	 * @return rule or <code>null</code>
	 */
	protected ISchedulingRule getUpdateSchedulingRule() {
		return AsynchronousSchedulingRuleFactory.getDefault().newSerialPerObjectRule(getModel().getViewer());
	}

	/**
	 * Returns the model this update is being performed for
	 *
	 * @return the model this update is being performed for
	 */
	protected AsynchronousModel getModel() {
		return fModel;
	}

	/**
	 * Returns the model node this update is rooted at
	 *
	 * @return the model node this update is rooted at
	 */
	protected ModelNode getNode() {
		return fNode;
	}

	/**
	 * Returns whether this update contains the given node.
	 * That is, whether this update is for the same node or a child of
	 * the given node.
	 *
	 * @param node node to test containment on
	 * @return whether this update contains the given node
	 */
	protected boolean contains(ModelNode node) {
		if (node == getNode()) {
			return true;
		}
		ModelNode parentNode = getNode().getParentNode();
		while (parentNode != null) {
			if (parentNode.equals(getNode())) {
				return true;
			}
			parentNode = parentNode.getParentNode();
		}
		return false;
	}

	@Override
	public void setCanceled(boolean value) {
		super.setCanceled(value);
		if (value) {
			getModel().requestCanceled(this);
		}
	}

	@Override
	public final void done() {
		synchronized (this) {
			fDone = true;
		}
		scheduleViewerUpdate(0L);
	}

	/**
	 * Returns whether this request is done yet.
	 *
	 * @return if the request is done
	 */
	protected synchronized boolean isDone() {
		return fDone;
	}

	protected void scheduleViewerUpdate(long ms) {
		if(!isCanceled()) {
			fViewerUpdateJob.schedule(ms);
		}
	}

	/**
	 * Notification this update has been completed and should now be applied to
	 * this update's viewer. This method is called in the UI thread.
	 */
	protected abstract void performUpdate();

	/**
	 * Returns whether this update effectively contains the given update.
	 * That is, whether this update will also perform the given update.
	 *
	 * @param update update to compare to
	 * @return whether this update will also perform the given update
	 */
	protected abstract boolean contains(AsynchronousRequestMonitor update);

}
