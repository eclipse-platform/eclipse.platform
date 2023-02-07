/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.search;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.help.internal.workingset.AdaptableHelpResource;
import org.eclipse.help.internal.workingset.AdaptableToc;
import org.eclipse.help.internal.workingset.AdaptableTocsArray;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class HelpWorkingSetTreeContentProvider implements ITreeContentProvider {

	/**
	 * Constructor for HelpWorkingSetTreeContentProvider.
	 */
	public HelpWorkingSetTreeContentProvider() {
		super();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof AdaptableTocsArray)
			return ((AdaptableTocsArray) parentElement).getChildren();
		else if (parentElement instanceof AdaptableToc)
			return ((AdaptableToc) parentElement).getChildren();
		else
			return new IAdaptable[0];
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof AdaptableHelpResource)
			return ((AdaptableHelpResource) element).getParent();
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return (element instanceof AdaptableToc || element instanceof AdaptableTocsArray);
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
