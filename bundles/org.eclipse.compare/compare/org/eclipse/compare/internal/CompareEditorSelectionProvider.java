/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.compare.internal;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Widget;


/**
 * A selection provider for view parts with more that one viewer. Tracks the
 * focus of the viewers to provide the correct selection.
 *
 * This is a modified version of
 * org.eclipse.jdt.internal.ui.viewsupport.SelectionProviderMediator
 */
public class CompareEditorSelectionProvider implements IPostSelectionProvider {

	private class InternalListener implements ISelectionChangedListener, FocusListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			doSelectionChanged(event);
		}

		@Override
		public void focusGained(FocusEvent e) {
			// expecting a StyledText widget here
			doFocusChanged(e.widget);
		}

		@Override
		public void focusLost(FocusEvent e) {
			// do not reset due to focus behavior on GTK
			//fViewerInFocus= null;
		}
	}

	private class InternalPostSelectionListener implements ISelectionChangedListener {
		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			doPostSelectionChanged(event);
		}

	}

	private TextViewer[] fViewers;

	private TextViewer fViewerInFocus;
	private ListenerList<ISelectionChangedListener> fSelectionChangedListeners;
	private ListenerList<ISelectionChangedListener> fPostSelectionChangedListeners;

	public CompareEditorSelectionProvider() {
		fSelectionChangedListeners = new ListenerList<>();
		fPostSelectionChangedListeners = new ListenerList<>();
		// nothing more to do here, Compare Editor is initializing
	}

	/**
	 * @param viewers All viewers that can provide a selection
	 * @param viewerInFocus the viewer currently in focus or <code>null</code>
	 */
	public void setViewers(TextViewer[] viewers, TextViewer viewerInFocus) {
		Assert.isNotNull(viewers);
		fViewers= viewers;
		InternalListener listener= new InternalListener();
		fViewerInFocus= viewerInFocus;

		for (TextViewer viewer : fViewers) {
			viewer.addSelectionChangedListener(listener);
			viewer.addPostSelectionChangedListener(new InternalPostSelectionListener());
			StyledText textWidget = viewer.getTextWidget();
			textWidget.addFocusListener(listener);
		}
	}

	private void doFocusChanged(Widget control) {
		for (TextViewer viewer : fViewers) {
			if (viewer.getTextWidget() == control) {
				propagateFocusChanged(viewer);
				return;
			}
		}
	}

	final void doPostSelectionChanged(SelectionChangedEvent event) {
		ISelectionProvider provider= event.getSelectionProvider();
		if (provider == fViewerInFocus) {
			firePostSelectionChanged();
		}
	}

	final void doSelectionChanged(SelectionChangedEvent event) {
		ISelectionProvider provider= event.getSelectionProvider();
		if (provider == fViewerInFocus) {
			fireSelectionChanged();
		}
	}

	final void propagateFocusChanged(TextViewer viewer) {
		if (viewer != fViewerInFocus) { // OK to compare by identity
			fViewerInFocus= viewer;
			fireSelectionChanged();
			firePostSelectionChanged();
		}
	}

	private void fireSelectionChanged() {
		if (fSelectionChangedListeners != null) {
			SelectionChangedEvent event= new SelectionChangedEvent(this, getSelection());

			for (ISelectionChangedListener listener : fSelectionChangedListeners) {
				listener.selectionChanged(event);
			}
		}
	}

	private void firePostSelectionChanged() {
		if (fPostSelectionChangedListeners != null) {
			SelectionChangedEvent event= new SelectionChangedEvent(this, getSelection());

			for (ISelectionChangedListener listener: fPostSelectionChangedListeners) {
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
		return TextSelection.emptySelection();
	}

	@Override
	public void setSelection(ISelection selection) {
		setSelection(selection, true);
	}

	public void setSelection(ISelection selection, boolean reveal) {
		if (fViewerInFocus != null) {
			if (reveal && !isSelectionInsideVisibleRegion(fViewerInFocus, selection))
				resetVisibleRegion();
			fViewerInFocus.setSelection(selection, reveal);
		}
	}

	/**
	 * Resets the visible region for all text viewers of this selection provider.
	 *
	 * @since 3.6
	 */
	private void resetVisibleRegion() {
		if (fViewers == null)
			return;

		for (TextViewer viewer : fViewers) {
			viewer.setVisibleRegion(0, viewer.getDocument().getLength());
		}
	}

	/**
	 * Tells whether the given selection is inside the text viewer's visible region.
	 *
	 * @param textViewer the text viewer
	 * @param selection the selection
	 * @return <code>true</code> if the selection is inside the text viewer's visible region
	 * @since 3.6
	 */
	private static boolean isSelectionInsideVisibleRegion(TextViewer textViewer, ISelection selection) {
		if (!(selection instanceof ITextSelection))
			return false;

		ITextSelection textSelection= (ITextSelection)selection;
		IRegion visibleRegion= textViewer.getVisibleRegion();

		return textSelection.getOffset() >= visibleRegion.getOffset() && textSelection.getOffset() + textSelection.getLength() <= visibleRegion.getOffset() + visibleRegion.getLength();
	}

}
