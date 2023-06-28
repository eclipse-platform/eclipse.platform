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
package org.eclipse.debug.internal.ui.viewers.update;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;

/**
 * Handles debug events for an event update policy in a viewer.
 *
 * @since 3.2
 */
public abstract class DebugEventHandler {

	private AbstractModelProxy fModelProxy;

	/**
	 * Constructs an event handler for the given model proxy.
	 *
	 * @param proxy the backing proxy
	 */
	public DebugEventHandler(AbstractModelProxy proxy) {
		fModelProxy = proxy;
	}

	/**
	 * Disposes this event handler
	 */
	public synchronized void dispose() {
		fModelProxy = null;
	}

	/**
	 * Returns the model proxy this event handler working for,
	 * or <code>null</code> if disposed.
	 *
	 * @return the backing model proxy
	 */
	protected synchronized AbstractModelProxy getModelProxy() {
		return fModelProxy;
	}

	/**
	 * Returns whether this event handler handles the given event
	 *
	 * @param event event to handle
	 * @return whether this event handler handles the given event
	 */
	protected abstract boolean handlesEvent(DebugEvent event);

	/**
	 * Handles a create event.
	 *
	 * @param event the event to handle
	 */
	protected void handleCreate(DebugEvent event) {
		refreshRoot(event);
	}

	/**
	 * Handles a terminate event.
	 *
	 * @param event the event to handle
	 */
	protected void handleTerminate(DebugEvent event) {
		refreshRoot(event);
	}

	/**
	 * Handles a suspend event.
	 *
	 * @param event the event to handle
	 */
	protected void handleSuspend(DebugEvent event) {
		refreshRoot(event);
	}

	/**
	 * Handles a resume event for which a suspend is expected shortly (<500ms).
	 *
	 * @param event the event to handle
	 */
	protected void handleResumeExpectingSuspend(DebugEvent event) {
		// do nothing unless the suspend times out
	}

	/**
	 * Handles a resume event that is not expecting an immediate suspend event
	 *
	 * @param event the event to handle
	 */
	protected void handleResume(DebugEvent event) {
		refreshRoot(event);
	}

	/**
	 * Handles a change event.
	 *
	 * @param event the event to handle
	 */
	protected void handleChange(DebugEvent event) {
		refreshRoot(event);
	}

	/**
	 * Handles an unknown event.
	 *
	 * @param event the event to handle
	 */
	protected void handleOther(DebugEvent event) {
		refreshRoot(event);
	}

	/**
	 * Notification that a pending suspend event was not received for the given
	 * resume event within the timeout period.
	 *
	 * @param event the event to handle
	 */
	protected void handleSuspendTimeout(DebugEvent event) {
		refreshRoot(event);
	}

	/**
	 * Handles the given suspend event which caused a timeout. It is
	 * paired with its original resume event.
	 *
	 * @param suspend suspend event
	 * @param resume resume event
	 */
	protected void handleLateSuspend(DebugEvent suspend, DebugEvent resume) {
		refreshRoot(suspend);
	}

	/**
	 * Fires a model delta to indicate that the launch manager should be refreshed.
	 * Subclasses should override individual handle events to provide deltas that
	 * better reflect the actual change in the model.
	 *
	 * @param event the event that triggered the root refresh
	 */
	protected void refreshRoot(DebugEvent event) {
		ModelDelta delta = new ModelDelta(DebugPlugin.getDefault().getLaunchManager(), IModelDelta.CONTENT);
		fireDelta(delta);
	}

	/**
	 * Fires the given delta, unless this handler has been disposed.
	 *
	 * @param delta the delta to fire in the backing model proxy
	 */
	protected void fireDelta(IModelDelta delta) {
		AbstractModelProxy modelProxy = getModelProxy();
		if (modelProxy != null) {
			modelProxy.fireModelChanged(delta);
		}
	}

	/**
	 * Returns whether this handler has been disposed.
	 *
	 * @return whether this handler has been disposed
	 */
	protected synchronized boolean isDisposed() {
		return fModelProxy == null;
	}

	protected int indexOf(Object[] list, Object element) {
		for (int i = 0; i < list.length; i++) {
			if (element.equals(list[i])) {
				return i;
			}
		}
		return -1;
	}

}
