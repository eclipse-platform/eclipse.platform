/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.internal.core.commands.Request;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerInputRequestor;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerInputUpdate;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * Internal implementation of the {@link IViewerInputUpdate} interface.  Allows
 * implementors to translate the active debug context into an appropriate viewer
 * input.
 *
 * @since 3.4
 * @see IViewerInputUpdate
 */
public class ViewerInputUpdate extends Request implements IViewerInputUpdate {

	/**
	 * Presentation context
	 */
	private IPresentationContext fContext;

	/**
	 * New viewer input
	 */
	private Object fSource;

	/**
	 * Whether this update is done
	 */
	private boolean fDone;

	/**
	 * Viewer input to use
	 */
	private Object fInputElement;

	/**
	 * Viewer input at the time the request was made
	 */
	private Object fViewerInput;

	/**
	 * Client making request
	 */
	private IViewerInputRequestor fRequestor;

	/**
	 * When <code>done()</code> is called, the viewer must be informed that the update is complete in the UI thread.
	 */
	protected WorkbenchJob fViewerInputUpdateJob = new WorkbenchJob("Asynchronous viewer input update") { //$NON-NLS-1$
		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			if (ViewerInputUpdate.this.isCanceled()){
				return Status.CANCEL_STATUS;
			}
			fRequestor.viewerInputComplete(ViewerInputUpdate.this);
			return Status.OK_STATUS;
		}
	};

	/**
	 * Constructs a viewer input update request.
	 *
	 * @param context presentation context
	 * @param viewerInput viewer input at the time the request was made
	 * @param requestor client making the request
	 * @param source source from which to derive a viewer input
	 */
	public ViewerInputUpdate(IPresentationContext context, Object viewerInput, IViewerInputRequestor requestor, Object source){
		fContext = context;
		fSource = source;
		fRequestor = requestor;
		fViewerInputUpdateJob.setSystem(true);
		fViewerInput = viewerInput;
	}

	@Override
	public IPresentationContext getPresentationContext() {
		return fContext;
	}

	@Override
	public final void done() {
		synchronized (this) {
			if (isDone()) {
				return;
			}
			fDone = true;
		}
		fViewerInputUpdateJob.schedule();
	}

	/**
	 * Returns whether this request is done yet.
	 *
	 * @return whether this request is done yet
	 */
	protected synchronized boolean isDone() {
		return fDone;
	}

	@Override
	public Object getElement() {
		return fSource;
	}

	@Override
	public TreePath getElementPath() {
		return TreePath.EMPTY;
	}

	@Override
	public void setInputElement(Object element) {
		fInputElement = element;
	}

	@Override
	public Object getInputElement() {
		return fInputElement;
	}

	@Override
	public Object getViewerInput() {
		return fViewerInput;
	}



}
