/*******************************************************************************
 * Copyright (c) 2022 Michael Keppler.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Michael Keppler - initial implementation
 *******************************************************************************/
package org.eclipse.debug.ui.launchview.internal.view;

import org.eclipse.debug.ui.launchview.internal.model.LaunchObjectContainerModel;
import org.eclipse.debug.ui.launchview.internal.model.LaunchObjectFavoriteContainerModel;
import org.eclipse.debug.ui.launchview.internal.model.LaunchObjectModel;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;

/**
 * The launch view uses a {@link DelegatingStyledCellLabelProvider} which is not
 * handled correctly by the default ViewerComparator implementation. Therefore
 * the label extraction must be reimplemented in this class.
 */
public class LaunchViewComparator extends ViewerComparator {

	public LaunchViewComparator() {
		super(String.CASE_INSENSITIVE_ORDER);
	}

	@Override
	public int category(Object element) {
		// have favorite launch configs first
		if (element instanceof LaunchObjectFavoriteContainerModel) {
			return -1;
		}
		return super.category(element);
	}

	/**
	 * The super class getLabel() implementation is private, therefore the
	 * complete super method is cloned and calls a modified getLabel() method.
	 */
	@Override
	public int compare(Viewer viewer, Object e1, Object e2) {
		int cat1 = category(e1);
		int cat2 = category(e2);

		if (cat1 != cat2) {
			return cat1 - cat2;
		}

		String name1 = getLabel(e1);
		String name2 = getLabel(e2);

		return getComparator().compare(name1, name2);
	}

	private String getLabel(Object object) {
		if (object instanceof LaunchObjectContainerModel) {
			return ((LaunchObjectContainerModel) object).getLabel().getString();
		}
		if (object instanceof LaunchObjectModel) {
			return ((LaunchObjectModel) object).getLabel().getString();
		}
		return object.toString();
	}
}
