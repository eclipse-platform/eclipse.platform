/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.unittest.internal.ui;

import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;

/**
 * A selection provider for view parts with more that one viewer. Tracks the
 * focus of the viewers to provide the correct selection.
 */
public class SelectionProviderMediator implements IPostSelectionProvider {

	private class InternalListener implements ISelectionChangedListener, FocusListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			doSelectionChanged(event);
		}

		@Override
		public void focusGained(FocusEvent e) {
			doFocusChanged(e.widget);
		}

		@Override
		public void focusLost(FocusEvent e) {
			// do not reset due to focus behavior on GTK
			// fViewerInFocus= null;
		}
	}

	private class InternalPostSelectionListener implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			doPostSelectionChanged(event);
		}

	}

	private StructuredViewer[] fViewers;

	private StructuredViewer fViewerInFocus;
	private ListenerList<ISelectionChangedListener> fSelectionChangedListeners;
	private ListenerList<ISelectionChangedListener> fPostSelectionChangedListeners;

	/**
	 * Constructs a selection provider mediator object
	 *
	 * @param viewers       All viewers that can provide a selection
	 * @param viewerInFocus the viewer currently in focus or <code>null</code>
	 */
	public SelectionProviderMediator(StructuredViewer[] viewers, StructuredViewer viewerInFocus) {
		Assert.isNotNull(viewers);
		fViewers = viewers;
		InternalListener listener = new InternalListener();
		fSelectionChangedListeners = new ListenerList<>();
		fPostSelectionChangedListeners = new ListenerList<>();
		fViewerInFocus = viewerInFocus;

		for (StructuredViewer viewer : fViewers) {
			viewer.addSelectionChangedListener(listener);
			viewer.addPostSelectionChangedListener(new InternalPostSelectionListener());
			Control control = viewer.getControl();
			control.addFocusListener(listener);
		}
	}

	private void doFocusChanged(Widget control) {
		for (StructuredViewer viewer : fViewers) {
			if (viewer.getControl() == control) {
				propagateFocusChanged(viewer);
				return;
			}
		}
	}

	final void doPostSelectionChanged(SelectionChangedEvent event) {
		ISelectionProvider provider = event.getSelectionProvider();
		if (provider == fViewerInFocus) {
			firePostSelectionChanged();
		}
	}

	final void doSelectionChanged(SelectionChangedEvent event) {
		ISelectionProvider provider = event.getSelectionProvider();
		if (provider == fViewerInFocus) {
			fireSelectionChanged();
		}
	}

	final void propagateFocusChanged(StructuredViewer viewer) {
		if (viewer != fViewerInFocus) { // OK to compare by identity
			fViewerInFocus = viewer;
			fireSelectionChanged();
			firePostSelectionChanged();
		}
	}

	private void fireSelectionChanged() {
		if (fSelectionChangedListeners != null) {
			SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());

			for (ISelectionChangedListener listener : fSelectionChangedListeners) {
				listener.selectionChanged(event);
			}
		}
	}

	private void firePostSelectionChanged() {
		if (fPostSelectionChangedListeners != null) {
			SelectionChangedEvent event = new SelectionChangedEvent(this, getSelection());

			for (ISelectionChangedListener listener : fPostSelectionChangedListeners) {
				listener.selectionChanged(event);
			}
		}
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		fSelectionChangedListeners.add(listener);
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		fSelectionChangedListeners.remove(listener);
	}

	@Override
	public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
		fPostSelectionChangedListeners.add(listener);
	}

	@Override
	public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
		fPostSelectionChangedListeners.remove(listener);
	}

	@Override
	public ISelection getSelection() {
		if (fViewerInFocus != null) {
			return fViewerInFocus.getSelection();
		}
		return StructuredSelection.EMPTY;
	}

	@Override
	public void setSelection(ISelection selection) {
		if (fViewerInFocus != null) {
			fViewerInFocus.setSelection(selection);
		}
	}

	/**
	 * Sets current selection
	 *
	 * @param selection a selection object
	 * @param reveal    a flag indicating if a reveal is needed
	 */
	public void setSelection(ISelection selection, boolean reveal) {
		if (fViewerInFocus != null) {
			fViewerInFocus.setSelection(selection, reveal);
		}
	}

	/**
	 * Returns the viewer in focus or null if no viewer has the focus
	 *
	 * @return returns the current viewer in focus
	 */
	public StructuredViewer getViewerInFocus() {
		return fViewerInFocus;
	}
}
