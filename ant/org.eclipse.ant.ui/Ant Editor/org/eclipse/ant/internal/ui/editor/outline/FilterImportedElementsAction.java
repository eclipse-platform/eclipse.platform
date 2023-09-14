/*******************************************************************************
 * Copyright (c) 2004, 2005 John-Mason P. Shackelford and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     John-Mason P. Shackelford - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor.outline;

import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.swt.custom.BusyIndicator;

/**
 * An action which toggles filtering of imported elements from the Ant outline.
 */
public class FilterImportedElementsAction extends Action {

	private final AntEditorContentOutlinePage fPage;

	public FilterImportedElementsAction(AntEditorContentOutlinePage page) {
		super(AntOutlineMessages.FilterImportedElementsAction_0);
		fPage = page;
		setImageDescriptor(AntUIImages.getImageDescriptor(IAntUIConstants.IMG_FILTER_IMPORTED_ELEMENTS));
		setToolTipText(AntOutlineMessages.FilterImportedElementsAction_0);
		setChecked(fPage.filterImportedElements());
	}

	/**
	 * Toggles the filtering of imported elements from the Ant outline
	 *
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public void run() {
		BusyIndicator.showWhile(fPage.getControl().getDisplay(), () -> fPage.setFilterImportedElements(isChecked()));
	}
}