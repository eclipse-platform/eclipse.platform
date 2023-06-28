/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.views.memory;

import java.util.ArrayList;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;

/**
 * Handles selection changes in a rendering view pane.
 *
 * @since 3.1
 *
 */
public class ViewPaneSelectionProvider implements ISelectionProvider {
	ArrayList<ISelectionChangedListener> fListeners = new ArrayList<>();
	ISelection fSelection;

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		if (!fListeners.contains(listener)) {
			fListeners.add(listener);
		}

	}

	@Override
	public ISelection getSelection() {
		return fSelection;
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		if (fListeners.contains(listener)) {
			fListeners.remove(listener);
		}

	}

	@Override
	public void setSelection(ISelection selection) {
		fSelection = selection;
		fireChanged();
	}

	public void fireChanged() {
		SelectionChangedEvent evt = new SelectionChangedEvent(this, getSelection());
		for (ISelectionChangedListener listener : fListeners) {
			listener.selectionChanged(evt);
		}
	}
}
