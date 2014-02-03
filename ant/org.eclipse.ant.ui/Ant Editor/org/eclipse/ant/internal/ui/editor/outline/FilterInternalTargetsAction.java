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
package org.eclipse.ant.internal.ui.editor.outline;

import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.custom.BusyIndicator;

/** 
 * An action which toggles filtering of internal targets from the Ant outline.
 */
public class FilterInternalTargetsAction extends Action {
	
	private AntEditorContentOutlinePage fPage;
	
	public FilterInternalTargetsAction(AntEditorContentOutlinePage page) {
		super(AntOutlineMessages.FilterInternalTargetsAction_0);
		fPage = page;
		setImageDescriptor(AntUIImages.getImageDescriptor(IAntUIConstants.IMG_FILTER_INTERNAL_TARGETS));
		setToolTipText(AntOutlineMessages.FilterInternalTargetsAction_0);
		setChecked(fPage.filterInternalTargets());
	}
	
	/**
	 * Toggles the filtering of internal targets from the Ant outline
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		BusyIndicator.showWhile(fPage.getControl().getDisplay(), new Runnable() {
			public void run() {
				fPage.setFilterInternalTargets(isChecked());
			}
		});
	}
}
