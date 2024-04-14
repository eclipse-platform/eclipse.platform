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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.actions.ActionFactory;

/**
 * Generic "clear contents" action. Sets action's text, tool tip text and icon.
 */

public class ClearTextAction extends GlobalAction {

	/**
	 * The document on which this action performs its duty.
	 */
	private IDocument document;

	/**
	 * Constructs a ClearTextAction action with the provided document.
	 *
	 * @param document the document to be cleared when this action is run.
	 */
	public ClearTextAction(IDocument document) {
		super("Cle&ar Contents"); //$NON-NLS-1$
		this.setToolTipText("Clear contents"); //$NON-NLS-1$
		this.document = document;
		// the delete key is not captured by the workbench
		// then we need to provide an action definition id
		// so clients can register this action in their key binding services
		this.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_DELETE);
		this.setImageDescriptor(ImageDescriptor.createFromURLSupplier(true, () -> ClearTextAction.class.getResource("/icons/clear.gif")));
	}

	/**
	 * Executes this action (clears associated document's contents).
	 */
	@Override
	public void run() {
		document.set(""); //$NON-NLS-1$
	}

	/**
	 * Registers this action as a global action handler.
	 *
	 * @param actionBars the action bars where this action will be registered.
	 * @see GlobalAction#registerAsGlobalAction(org.eclipse.ui.IActionBars)
	 */
	@Override
	public void registerAsGlobalAction(IActionBars actionBars) {
		actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), this);
	}
}
