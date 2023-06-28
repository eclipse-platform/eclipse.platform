/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers;

import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

/**
 * A dialog used to select elements from a list.
 *
 * @since 3.3
 *
 */
public class FindElementDialog extends ElementListSelectionDialog {

	/**
	 * Constructs a dialog to navigate to an element in the given viewer.
	 *
	 * @param shell shell to open on
	 * @param provider label provider
	 * @param elements elements to choose from
	 */
	public FindElementDialog(Shell shell, ILabelProvider provider, Object[] elements) {
		super(shell, provider);
		setElements(elements);
		setMultipleSelection(false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Control comp = super.createDialogArea(parent);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IDebugHelpContextIds.FIND_ELEMENT_DIALOG);
		return comp;
	}

}
