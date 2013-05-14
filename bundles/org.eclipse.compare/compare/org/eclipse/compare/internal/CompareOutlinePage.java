/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.compare.*;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.Page;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 */
public class CompareOutlinePage extends Page implements IContentOutlinePage, IPropertyChangeListener {

	private CompareEditor fCompareEditor;
	private Control fControl;
	private CompareViewerSwitchingPane fStructurePane;
	private OutlineViewerCreator fCreator;
	
	CompareOutlinePage(CompareEditor editor) {
		fCompareEditor= editor;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		final Splitter h= new Splitter(parent, SWT.HORIZONTAL);
		fStructurePane= new CompareViewerSwitchingPane(h, SWT.BORDER | SWT.FLAT, true) {
			protected Viewer getViewer(Viewer oldViewer, Object input) {
				if (input instanceof ICompareInput)
					return findStructureViewer(oldViewer, (ICompareInput)input, this);
				return null;
			}
		};
		h.setVisible(fStructurePane, true);
		fControl = h;
		IPageSite site = getSite();
		site.setSelectionProvider(fStructurePane);
		h.layout();
		reset();
	}
	
	private Viewer findStructureViewer(Viewer oldViewer, ICompareInput input, Composite parent) {
		OutlineViewerCreator creator = getCreator();
		if (creator != null)
			return creator.findStructureViewer(oldViewer, input, parent, getCompareConfiguration());
		return null;
	}

	private CompareConfiguration getCompareConfiguration() {
		return fCompareEditor.getCompareConfiguration();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#getControl()
	 */
	public Control getControl() {
		return fControl;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.IPage#setFocus()
	 */
	public void setFocus() {
		if (fStructurePane != null)
			fStructurePane.setFocus();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionProvider#addSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		if (fStructurePane != null)
			fStructurePane.addSelectionChangedListener(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionProvider#getSelection()
	 */
	public ISelection getSelection() {
		if (fStructurePane != null)
			return fStructurePane.getSelection();
		return StructuredSelection.EMPTY;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionProvider#removeSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		if (fStructurePane != null)
			fStructurePane.removeSelectionChangedListener(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelectionProvider#setSelection(org.eclipse.jface.viewers.ISelection)
	 */
	public void setSelection(ISelection selection) {
		if (fStructurePane != null)
			fStructurePane.setSelection(selection);
	}
	
	private void setInput(Object input) {
		if (fStructurePane != null) {
			fStructurePane.setInput(input);
			((Splitter)fControl).layout();
		}
	}

	public OutlineViewerCreator getCreator() {
		if (fCreator == null) {
			fCreator = (OutlineViewerCreator)Utilities.getAdapter(fCompareEditor, OutlineViewerCreator.class);
			if (fCreator != null)
				fCreator.addPropertyChangeListener(this);
		}
		return fCreator;
	}

	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(OutlineViewerCreator.PROP_INPUT)) {
			fStructurePane.setInput(event.getNewValue());
			((Splitter)fControl).layout();
		}
	}
	
	public void dispose() {
		super.dispose();
		if (fCreator != null)
			fCreator.removePropertyChangeListener(this);
		fCreator = null;
	}

	public void reset() {
		if (fCreator != null)
			fCreator.removePropertyChangeListener(this);
		fCreator = null;
		OutlineViewerCreator creator = getCreator();
		if (creator != null)
			setInput(creator.getInput());
		else
			setInput(null);
	}
}
