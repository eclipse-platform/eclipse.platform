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
package org.eclipse.ant.internal.ui.editor.outline;

import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.custom.BusyIndicator;

/**
 * An action which toggles filtering of properties from the Ant outline.
 */
public class FilterTopLevelAction extends Action {

	private final AntEditorContentOutlinePage fPage;

	public FilterTopLevelAction(AntEditorContentOutlinePage page) {
		super(AntOutlineMessages.FilterTopLevelAction_0);
		fPage = page;
		setImageDescriptor(AntUIImages.getImageDescriptor(IAntUIConstants.IMG_FILTER_TOP_LEVEL));
		setToolTipText(AntOutlineMessages.FilterTopLevelAction_0);
		setChecked(fPage.filterTopLevel());
	}

	/**
	 * Toggles the filtering of top level tasks and types from the Ant outline
	 *
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run() {
		BusyIndicator.showWhile(fPage.getControl().getDisplay(), () -> fPage.setFilterTopLevel(isChecked()));
	}
}
