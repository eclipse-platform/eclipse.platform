/*******************************************************************************
 * Copyright (c) 2002, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools;

import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;

/**
 * A concrete implementation for <code>AbstractCopySelectionAction</code> that
 * supports text selections.
 *
 * @see org.eclipse.jface.text.ITextSelection
 */
public class CopyTextSelectionAction extends AbstractCopySelectionAction {

	/**
	 * @see AbstractCopySelectionAction#AbstractCopySelectionAction
	 * (ISelectionProvider)
	 */
	public CopyTextSelectionAction(ISelectionProvider selectionProvider) {
		super(selectionProvider);
	}

	/**
	 * Returns the current text selection.
	 *
	 * @return a string containing the currently selected text
	 * @see org.eclipse.core.tools.AbstractCopySelectionAction#getContents()
	 */
	@Override
	protected String getContents() {
		return ((ITextSelection) selectionProvider.getSelection()).getText();
	}

}
