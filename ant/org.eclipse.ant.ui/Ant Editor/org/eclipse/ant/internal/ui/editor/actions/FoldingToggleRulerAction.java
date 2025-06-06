/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.editor.actions;

import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.editors.text.IFoldingCommandIds;
import org.eclipse.ui.texteditor.AbstractRulerActionDelegate;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.TextOperationAction;

public class FoldingToggleRulerAction extends AbstractRulerActionDelegate {

	private IAction fUIAction;
	private TextOperationAction fAction;
	private ITextEditor fTextEditor;

	@Override
	protected IAction createAction(ITextEditor editor, IVerticalRulerInfo rulerInfo) {
		fTextEditor = editor;
		fAction = new TextOperationAction(AntEditorActionMessages.getResourceBundle(), "Projection.Toggle.", editor, ProjectionViewer.TOGGLE, true); //$NON-NLS-1$
		fAction.setActionDefinitionId(IFoldingCommandIds.FOLDING_TOGGLE);
		return fAction;
	}

	@Override
	public void setActiveEditor(IAction callerAction, IEditorPart targetEditor) {
		fUIAction = callerAction;
		super.setActiveEditor(callerAction, targetEditor);
	}

	@Override
	public void menuAboutToShow(IMenuManager manager) {
		update();
		super.menuAboutToShow(manager);
	}

	private void update() {
		if (fTextEditor instanceof AntEditor) {
			ISourceViewer viewer = ((AntEditor) fTextEditor).getViewer();
			if (viewer instanceof ProjectionViewer) {
				boolean enabled = ((ProjectionViewer) viewer).getProjectionAnnotationModel() != null;
				fUIAction.setChecked(enabled);
			}
		}
	}
}
