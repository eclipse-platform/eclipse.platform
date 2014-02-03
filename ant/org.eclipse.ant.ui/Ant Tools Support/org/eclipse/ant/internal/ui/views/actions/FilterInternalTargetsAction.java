/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

	private AntView fView;
	
	public FilterInternalTargetsAction(AntView view) {
		super(AntViewActionMessages.FilterInternalTargetsAction_0);
		fView = view;
		setImageDescriptor(AntUIImages.getImageDescriptor(IAntUIConstants.IMG_FILTER_INTERNAL_TARGETS));
		setToolTipText(AntViewActionMessages.FilterInternalTargetsAction_0);
		setChecked(fView.isFilterInternalTargets());
	}
	
	/**
	 * Toggles the filtering of internal targets from the Ant view
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		fView.setFilterInternalTargets(isChecked());
	}
}
