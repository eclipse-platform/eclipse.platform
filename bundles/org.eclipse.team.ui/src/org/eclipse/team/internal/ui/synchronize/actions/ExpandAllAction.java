/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.actions;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.*;


public class ExpandAllAction extends Action implements ISelectionChangedListener {

	private final AbstractTreeViewer viewer;

	public ExpandAllAction(AbstractTreeViewer viewer) {
		this.viewer = viewer;
		viewer.addSelectionChangedListener(this);
	}
	@Override
	public void run() {
		expandAllFromSelection();
	}

	protected void expandAllFromSelection() {
		AbstractTreeViewer tree = viewer;
		if (tree == null) return;
		IStructuredSelection selection = tree.getStructuredSelection();
		if(! selection.isEmpty()) {
			Iterator elements = selection.iterator();
			try {
				tree.getControl().setRedraw(false);
				while (elements.hasNext()) {
					Object next = elements.next();
					tree.expandToLevel(next, AbstractTreeViewer.ALL_LEVELS);
				}
			} finally {
				tree.getControl().setRedraw(true);
			}
		}
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection)selection;
			setEnabled(!ss.isEmpty());
			return;
		}
		setEnabled(false);
	}
}
