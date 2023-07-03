/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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

package org.eclipse.ui.internal.intro.impl.model.viewer;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroContainer;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroElement;

public class IntroModelContentProvider implements ITreeContentProvider {

	@Override
	public Object[] getChildren(Object element) {

		AbstractIntroElement introElement = null;
		if (element instanceof AbstractIntroElement)
			// synch the resource first.
			introElement = (AbstractIntroElement) element;

		if (introElement != null
				&& introElement
					.isOfType(AbstractIntroElement.ABSTRACT_CONTAINER))
			return ((AbstractIntroContainer) introElement).getChildren();

		return new Object[0];
	}

	@Override
	public Object getParent(Object element) {
		AbstractIntroElement introElement = null;
		if (element instanceof AbstractIntroElement) {
			// synch the resource first.
			introElement = (AbstractIntroElement) element;
			return introElement.getParent();
		}
		return null;
	}


	@Override
	public boolean hasChildren(Object element) {
		AbstractIntroElement introElement = null;
		if (element instanceof AbstractIntroElement)
			// synch the resource first.
			introElement = (AbstractIntroElement) element;
		if (introElement != null
				&& introElement
					.isOfType(AbstractIntroElement.ABSTRACT_CONTAINER))
			return true;
		return false;
	}


	@Override
	public Object[] getElements(Object element) {
		return getChildren(element);
	}


	@Override
	public void dispose() {
		// nothing to dispose
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// do nothing

	}

}
