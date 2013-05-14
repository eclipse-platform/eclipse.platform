/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Widget;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextViewer;


/**
 * A selection provider for view parts with more that one viewer. Tracks the
 * focus of the viewers to provide the correct selection.
 * 
 * This is a modified version of
 * org.eclipse.jdt.internal.ui.viewsupport.SelectionProviderMediator
 */
public class CompareEditorSelectionProvider implements IPostSelectionProvider {

	private class InternalListener implements ISelectionChangedListener, FocusListener {
		/*
	 	 * @see ISelectionChangedListener#selectionChanged
	 	 */
		public void selectionChanged(SelectionChangedEvent event) {
			doSelectionChanged(event);
		}

	    /*
	     * @see FocusListener#focusGained
	     */
	    public void focusGained(FocusEvent e) {
	    	// expecting a StyledText widget here
	    	doFocusChanged(e.widget);
	    }

	    /*
	     * @see FocusListener#focusLost
	     */
	    public void focusLost(FocusEvent e) {
	    	// do not reset due to focus behavior on GTK
	    	//fViewerInFocus= null;
	    }
	}

	private class InternalPostSelectionListener implements ISelectionChangedListener {
		public void selectionChanged(SelectionChangedEvent event) {
			doPostSelectionChanged(event);
		}

	}

	private TextViewer[] fViewers;

	private TextViewer fViewerInFocus;
	private ListenerList fSelectionChangedListeners;
	private ListenerList fPostSelectionChangedListeners;

	public CompareEditorSelectionProvider() {
		fSelectionChangedListeners = new ListenerList();
		fPostSelectionChangedListeners = new ListenerList();
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
		
		for (int i= 0; i < fViewers.length; i++) {
			TextViewer viewer= fViewers[i];
			viewer.addSelectionChangedListener(listener);
			viewer.addPostSelectionChangedListener(new InternalPostSelectionListener());
			StyledText textWidget = viewer.getTextWidget();
			textWidget.addFocusListener(listener);
		}
	}

	private void doFocusChanged(Widget control) {
		for (int i= 0; i < fViewers.length; i++) {
			if (fViewers[i].getTextWidget() == control) {
				propagateFocusChanged(fViewers[i]);
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

			Object[] listeners= fSelectionChangedListeners.getListeners();
			for (int i= 0; i < listeners.length; i++) {
				ISelectionChangedListener listener= (ISelectionChangedListener) listeners[i];
				listener.selectionChanged(event);
			}
		}
	}

	private void firePostSelectionChanged() {
		if (fPostSelectionChangedListeners != null) {
			SelectionChangedEvent event= new SelectionChangedEvent(this, getSelection());

			Object[] listeners= fPostSelectionChangedListeners.getListeners();
			for (int i= 0; i < listeners.length; i++) {
				ISelectionChangedListener listener= (ISelectionChangedListener) listeners[i];
				listener.selectionChanged(event);
			}
		}
	}

	/*
	 * @see ISelectionProvider#addSelectionChangedListener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		fSelectionChangedListeners.add(listener);
	}

	/*
	 * @see ISelectionProvider#removeSelectionChangedListener
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		fSelectionChangedListeners.remove(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IPostSelectionProvider#addPostSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
		fPostSelectionChangedListeners.add(listener);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IPostSelectionProvider#removePostSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	public void removePostSelectionChangedListener(ISelectionChangedListener listener) {
		fPostSelectionChangedListeners.remove(listener);
	}

	/*
	 * @see ISelectionProvider#getSelection
	 */
	public ISelection getSelection() {
		if (fViewerInFocus != null) {
			return fViewerInFocus.getSelection();
		}
		return TextSelection.emptySelection();
	}

	/*
	 * @see ISelectionProvider#setSelection
	 */
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

		for (int i= 0; i < fViewers.length; i++)
			fViewers[i].setVisibleRegion(0, fViewers[i].getDocument().getLength());
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
