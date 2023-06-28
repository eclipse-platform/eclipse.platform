/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
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
 *     Pawel Piech (Wind River) - added support for a virtual tree model viewer (Bug 242489)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model;

import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.debug.internal.core.commands.Request;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.viewers.AsynchronousSchedulingRuleFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementContentProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ITreeModelViewer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerUpdate;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.widgets.Display;

/**
 * @since 3.3
 */
public abstract class ViewerUpdateMonitor extends Request implements IViewerUpdate {

	private TreeModelContentProvider fContentProvider;

	/**
	 * Element's tree path
	 */
	private TreePath fElementPath;

	/**
	 * Element
	 */
	private Object fElement;

	/**
	 * Element content provider
	 */
	private IElementContentProvider fElementContentProvider;

	/**
	 * Whether this request's 'done' method has been called.
	 */
	private boolean fDone = false;

	/**
	 * Whether this request has been started
	 */
	private boolean fStarted = false;

	/**
	 * Viewer input at the time the request was made
	 */
	private Object fViewerInput = null;

	/**
	 * Whether this update has been delegated to another content provider
	 * @since 3.4
	 */
	private boolean fIsDelegated = false;

	/**
	 * Presentation context
	 */
	private IPresentationContext fContext;

	/**
	 * Constructs an update for the given content provider
	 *
	 * @param contentProvider content provider
	 * @param viewerInput Viewer input for update
	 * @param elementPath path to associated model element - empty for root element
	 * @param element associated model element
	 * @param elementContentProvider Content provider for this update.
	 * @param context Presentation contest for this update
	 */
	public ViewerUpdateMonitor(TreeModelContentProvider contentProvider, Object viewerInput, TreePath elementPath, Object element, IElementContentProvider elementContentProvider, IPresentationContext context) {
		fContext = context;
		// Bug 380288: Catch and log a race condition where the viewer input is null.
		if (viewerInput == null) {
			DebugUIPlugin.log(new NullPointerException("Input to viewer update should not be null")); //$NON-NLS-1$
		}
		fViewerInput = viewerInput;
		fElementContentProvider = elementContentProvider;
		fContentProvider = contentProvider;
		fElement = element;
		fElementPath = elementPath;
	}

	/**
	 * Returns the scheduling rule for viewer update job.
	 *
	 * @return rule or <code>null</code>
	 */
	protected ISchedulingRule getUpdateSchedulingRule() {
		return AsynchronousSchedulingRuleFactory.getDefault().newSerialPerObjectRule(getContentProvider());
	}

	/**
	 * Returns the model content provider this update is being performed for.
	 *
	 * @return the model content provider this update is being performed for
	 */
	protected TreeModelContentProvider getContentProvider() {
		return fContentProvider;
	}

	/**
	 * Returns the element content provider to use for this request
	 *
	 * @return element content provider
	 */
	protected IElementContentProvider getElementContentProvider() {
		return fElementContentProvider;
	}

	@Override
	public final void done() {
		synchronized (this) {
			if (isDone()) {
				return;
			}
			fDone = true;
		}
		scheduleViewerUpdate();
	}

	/**
	 * Returns whether this request is done yet.
	 *
	 * @return True if this update is done.
	 */
	protected synchronized boolean isDone() {
		return fDone;
	}

	protected void scheduleViewerUpdate() {
		getContentProvider().scheduleViewerUpdate(this);
	}

	/**
	 * Notification this update has been completed and should now be applied to
	 * this update's viewer. This method is called in the UI thread.
	 */
	protected abstract void performUpdate();

	@Override
	public IPresentationContext getPresentationContext() {
		return fContext;
	}

	@Override
	public Object getElement() {
		return fElement;
	}

	@Override
	public TreePath getElementPath() {
		return fElementPath;
	}

	/**
	 * Returns whether this request can coalesce the given request, and performs the
	 * coalesce if it can.
	 *
	 * @param update request to coalesce with this request
	 * @return whether it worked
	 */
	abstract boolean coalesce(ViewerUpdateMonitor update);

	/**
	 * Returns whether this update or any coalesced updates is for an
	 * element at the given path.
	 * @param path Element path to check.
	 * @return True if this update contains the given update path.
	 *
	 * @since 3.6
	 */
	abstract boolean containsUpdate(TreePath path);

	/**
	 * Starts this request. Subclasses must override startRequest().
	 */
	protected void start() {
		synchronized (this) {
			if (fStarted) {
				return;
			}
			fStarted = true;
		}
		getContentProvider().updateStarted(this);
		if (!isCanceled()) {
			startRequest();
		} else {
			done();
		}
	}

	@Override
	public Object getViewerInput() {
		return fViewerInput;
	}

	/**
	 * Subclasses must override to initiate specific request types.
	 */
	abstract void startRequest();

	/**
	 * Returns the priority of this request. Subclasses must override. The
	 * highest priority is 1. Priorities indicate the order that waiting
	 * requests should be started in (for example, 'hasChildren' before 'update child count').
	 *
	 * @return priority
	 */
	abstract int getPriority();

	/**
	 * Returns a path used to schedule this request - i.e. based on this path, this
	 * request will be scheduled to run when no requests are running against the
	 * same element or a parent of the element denoted by the path.
	 *
	 * @return path used to schedule request
	 */
	abstract TreePath getSchedulingPath();

	/**
	 * Sets whether this update has been delegated to another content provider
	 * @param delegated whether the update has been delegated
	 * @since 3.4
	 */
	public void setDelegated(boolean delegated) {
		fIsDelegated = delegated;
	}

	/**
	 * @return whether this update has been delegated to another content provider
	 * @since 3.4
	 */
	public boolean isDelegated() {
		return fIsDelegated;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ViewerUpdateMonitor) {
			return doEquals((ViewerUpdateMonitor)obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return doHashCode();
	}

	/**
	 * Checks whether the given update is equal as this.  The update is equal if it's
	 * the same type of update and its updating the same elements.
	 * @param update Update to compare to.
	 * @return True if the given update is equals
	 * @since 3.8
	 */
	abstract protected boolean doEquals(ViewerUpdateMonitor update);

	/**
	 * Calculates the hash code of the given update using the same parameters as doEquals().
	 * @return Update's hash code.
	 * @since 3.8
	 */
	abstract protected int doHashCode();

	/**
	 * Executes the given runnable in the UI thread.  If method is called in
	 * UI thread, then runnable is executed immediately, otherwise it's executed
	 * using <code>Display.asyncExec()</code>.  Runnable is not executed if update is
	 * canceled or content provider is disposed.
	 * @since 3.8
	 */
	protected void execInDisplayThread(Runnable runnable) {
		ITreeModelViewer viewer = getContentProvider().getViewer();
		if (viewer != null  && !isCanceled()) {
			Display display = viewer.getDisplay();
			if (Thread.currentThread() == display.getThread()) {
				runnable.run();
			} else {
				display.asyncExec(runnable);
			}
		}
	}

}
