/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.views.memory.renderings;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;

/**
 * @since 3.0
 *
 */
public abstract class BasicDebugViewContentProvider implements IStructuredContentProvider, IDebugEventSetListener {

	protected StructuredViewer fViewer;
	protected boolean fDisposed= false;

	@Override
	public void dispose() {
		fDisposed= true;
	}

	/**
	 * Returns whether this content provider has already
	 * been disposed.
	 * @return if the provider is disposed
	 */
	protected boolean isDisposed() {
		return fDisposed;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		fViewer= (StructuredViewer) viewer;
	}

	protected void asyncExec(Runnable r) {
		if (fViewer != null) {
			Control ctrl= fViewer.getControl();
			if (ctrl != null && !ctrl.isDisposed()) {
				ctrl.getDisplay().asyncExec(r);
			}
		}
	}

	protected void syncExec(Runnable r) {
		if (fViewer != null) {
			Control ctrl= fViewer.getControl();
			if (ctrl != null && !ctrl.isDisposed()) {
				ctrl.getDisplay().syncExec(r);
			}
		}
	}

	/**
	 * Refreshes the viewer - must be called in UI thread.
	 */
	protected void refresh() {
		if (fViewer != null) {
			fViewer.refresh();
		}
	}

	/**
	 * Refresh the given element in the viewer - must be called in UI thread.
	 * @param element the element to refresh in the viewer
	 */
	protected void refresh(Object element) {
		if (fViewer != null) {
			 fViewer.refresh(element);
		}
	}

	/**
	 * Handle debug events on the main thread.
	 * @param event the debug event
	 */
	public void handleDebugEvent(final DebugEvent event) {
		if (fViewer == null) {
			return;
		}
		Object element= event.getSource();
		if (element == null) {
			return;
		}
		Runnable r = () -> {
			if (!isDisposed()) {
				doHandleDebugEvent(event);
			}
		};

		asyncExec(r);
	}

	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		for (DebugEvent event : events) {
			handleDebugEvent(event);
		}
	}

	/**
	 * Performs an update based on the event
	 * @param event the debug event
	 */
	protected abstract void doHandleDebugEvent(DebugEvent event);
}
