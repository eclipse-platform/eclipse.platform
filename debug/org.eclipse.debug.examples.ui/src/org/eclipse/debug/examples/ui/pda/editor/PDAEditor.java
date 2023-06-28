/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.ui.pda.editor;

import java.util.ResourceBundle;

import org.eclipse.jface.action.IAction;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * PDA editor
 */
public class PDAEditor extends AbstractDecoratedTextEditor {

	/**
	 * Creates a PDE editor
	 */
	public PDAEditor() {
		super();
		setSourceViewerConfiguration(new PDASourceViewerConfiguration());
		setRulerContextMenuId("pda.editor.rulerMenu"); //$NON-NLS-1$
		setEditorContextMenuId("pda.editor.editorMenu"); //$NON-NLS-1$
	}

	@Override
	protected void createActions() {
		super.createActions();
		ResourceBundle bundle = ResourceBundle.getBundle("org.eclipse.debug.examples.ui.pda.editor.PDAEditorMessages"); //$NON-NLS-1$
		IAction action = new ContentAssistAction(bundle, "ContentAssistProposal.", this); //$NON-NLS-1$
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction("ContentAssistProposal", action); //$NON-NLS-1$
	}


}
