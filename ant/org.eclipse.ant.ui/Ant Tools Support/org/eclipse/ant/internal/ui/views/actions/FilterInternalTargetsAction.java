/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.views.actions;

import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.views.AntView;
import org.eclipse.jface.action.Action;

/**
 * An action which toggles filtering of internal targets from the Ant view.
 */
public class FilterInternalTargetsAction extends Action {

	private final AntView fView;

	public FilterInternalTargetsAction(AntView view) {
		super(AntViewActionMessages.FilterInternalTargetsAction_0);
		fView = view;
		setImageDescriptor(AntUIImages.getImageDescriptor(IAntUIConstants.IMG_FILTER_INTERNAL_TARGETS));
		setToolTipText(AntViewActionMessages.FilterInternalTargetsAction_0);
		setChecked(fView.isFilterInternalTargets());
	}

	/**
	 * Toggles the filtering of internal targets from the Ant view
	 * 
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run() {
		fView.setFilterInternalTargets(isChecked());
	}
}
