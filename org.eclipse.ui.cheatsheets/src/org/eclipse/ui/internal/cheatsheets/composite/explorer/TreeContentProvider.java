/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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

package org.eclipse.ui.internal.cheatsheets.composite.explorer;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.internal.cheatsheets.composite.model.AbstractTask;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheet;
import org.eclipse.ui.internal.provisional.cheatsheets.ITaskGroup;

class TreeContentProvider implements ITreeContentProvider {
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof ICompositeCheatSheet) {
			final Object[] rootTask = {((ICompositeCheatSheet) parentElement).getRootTask()};
			return rootTask;
		}
		if (parentElement instanceof ITaskGroup)
			return ((ITaskGroup) parentElement).getSubtasks();
		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof  AbstractTask) {
			return ((AbstractTask)element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof ICompositeCheatSheet)
			return true;
		if (element instanceof ITaskGroup)
			return ((ITaskGroup) element).getSubtasks().length > 0;
		return false;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}
}
