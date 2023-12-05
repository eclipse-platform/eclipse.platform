/*******************************************************************************
 * Copyright (c) 2002, 2012 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH,
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial API and implementation
 * 	   IBM Corporation - bug fixes
 *     John-Mason P. Shackelford - bug 40255
 *******************************************************************************/

package org.eclipse.ant.internal.ui.editor;

import java.util.ResourceBundle;

import org.eclipse.ant.internal.ui.editor.actions.FoldingActionGroup;
import org.eclipse.ant.internal.ui.editor.actions.ToggleAutoReconcileAction;
import org.eclipse.ant.internal.ui.editor.actions.ToggleMarkOccurrencesAction;
import org.eclipse.ant.internal.ui.editor.actions.TogglePresentationAction;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.editors.text.TextEditorActionContributor;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.RetargetTextEditorAction;

/**
 * Contributes interesting Ant Editor actions to the desktop's Edit menu and the toolbar.
 */
public class AntEditorActionContributor extends TextEditorActionContributor {

	private final static String TOGGLE_MARK_OCCURRENCES_ID = "org.eclipse.ant.ui.toggleMarkOccurrences"; //$NON-NLS-1$
	protected RetargetTextEditorAction fContentAssistProposal;
	protected RetargetTextEditorAction fContentFormat;
	private final TogglePresentationAction fTogglePresentation;
	private final ToggleMarkOccurrencesAction fToggleMarkOccurrencesAction;
	private final ToggleAutoReconcileAction fToggleAutoReconcileAction;

	public AntEditorActionContributor() {
		super();
		ResourceBundle bundle = AntEditorMessages.getResourceBundle();
		fContentAssistProposal = new RetargetTextEditorAction(bundle, "ContentAssistProposal."); //$NON-NLS-1$
		fContentAssistProposal.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		fContentFormat = new RetargetTextEditorAction(bundle, "ContentFormat."); //$NON-NLS-1$
		fContentFormat.setActionDefinitionId(IJavaEditorActionDefinitionIds.FORMAT);
		fTogglePresentation = new TogglePresentationAction();
		fToggleMarkOccurrencesAction = new ToggleMarkOccurrencesAction();
		fToggleAutoReconcileAction = new ToggleAutoReconcileAction();

	}

	private void doSetActiveEditor(IEditorPart part) {
		super.setActiveEditor(part);

		ITextEditor editor = null;
		if (part instanceof ITextEditor) {
			editor = (ITextEditor) part;
		}

		fContentAssistProposal.setAction(getAction(editor, ITextEditorActionConstants.CONTENT_ASSIST));
		fContentFormat.setAction(getAction(editor, "ContentFormat")); //$NON-NLS-1$

		if (editor instanceof AntEditor) {
			AntEditor antEditor = (AntEditor) part;
			FoldingActionGroup foldingActions = antEditor.getFoldingActionGroup();
			if (foldingActions != null) {
				foldingActions.updateActionBars();
			}
		}

		if (fTogglePresentation != null) {
			fTogglePresentation.setEditor(editor);
		}
		if (fToggleMarkOccurrencesAction != null) {
			fToggleMarkOccurrencesAction.setEditor(editor);
		}
		if (fToggleAutoReconcileAction != null) {
			fToggleAutoReconcileAction.setEditor(editor);
		}
	}

	@Override
	public void init(IActionBars bars) {
		super.init(bars);

		IMenuManager menuManager = bars.getMenuManager();
		IMenuManager editMenu = menuManager.findMenuUsingPath(IWorkbenchActionConstants.M_EDIT);
		if (editMenu != null) {
			editMenu.add(new Separator());
			editMenu.add(fContentAssistProposal);
			editMenu.add(fContentFormat);
		}
	}

	@Override
	public void setActiveEditor(IEditorPart part) {
		doSetActiveEditor(part);
	}

	@Override
	public void dispose() {
		doSetActiveEditor(null);
		super.dispose();
	}

	@Override
	public void init(IActionBars bars, IWorkbenchPage page) {
		super.init(bars, page);
		bars.setGlobalActionHandler(ITextEditorActionDefinitionIds.TOGGLE_SHOW_SELECTED_ELEMENT_ONLY, fTogglePresentation);
		bars.setGlobalActionHandler(TOGGLE_MARK_OCCURRENCES_ID, fToggleMarkOccurrencesAction);
		bars.setGlobalActionHandler("org.eclipse.ant.ui.toggleAutoReconcile", fToggleAutoReconcileAction); //$NON-NLS-1$
	}
}
