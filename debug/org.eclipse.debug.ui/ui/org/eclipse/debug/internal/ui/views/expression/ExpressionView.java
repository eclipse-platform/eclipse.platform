/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
 *     Wind River - Pawel Piech - Drag/Drop to Expressions View (Bug 184057)
 *     Wind River - Pawel Piech - Fix viewer input race condition (Bug 234908)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.expression;


import java.util.LinkedHashMap;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.actions.expressions.EditWatchExpressinInPlaceAction;
import org.eclipse.debug.internal.ui.actions.expressions.PasteWatchExpressionsAction;
import org.eclipse.debug.internal.ui.actions.variables.ChangeVariableValueAction;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IViewerInputUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.views.variables.AvailableLogicalStructuresAction;
import org.eclipse.debug.internal.ui.views.variables.SelectionDragAdapter;
import org.eclipse.debug.internal.ui.views.variables.VariablesView;
import org.eclipse.debug.internal.ui.views.variables.VariablesViewMessages;
import org.eclipse.debug.internal.ui.views.variables.details.AvailableDetailPanesAction;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

/**
 * Displays expressions and their values with a detail
 * pane.
 */
public class ExpressionView extends VariablesView {

	private PasteWatchExpressionsAction fPasteAction;
	private EditWatchExpressinInPlaceAction fEditInPlaceAction;

	@Override
	protected String getHelpContextId() {
		return IDebugHelpContextIds.EXPRESSION_VIEW;
	}

	@Override
	protected void configureToolBar(IToolBarManager tbm) {
		super.configureToolBar(tbm);
		tbm.add(new Separator(IDebugUIConstants.EMPTY_EXPRESSION_GROUP));
		tbm.add(new Separator(IDebugUIConstants.EXPRESSION_GROUP));
	}

	@Override
	protected void fillContextMenu(IMenuManager menu) {
		menu.add(new Separator(IDebugUIConstants.EMPTY_EXPRESSION_GROUP));
		menu.add(new Separator(IDebugUIConstants.EXPRESSION_GROUP));
		IAction action;
		if (DebugPlugin.getDefault().getExpressionManager().getExpressions().length > 0) {
			action = getAction(FIND_ACTION);
			action.setImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_FIND_ACTION));
			menu.add(action);
		}
		if (!getClipboardText().isEmpty()) {
			menu.appendToGroup(IDebugUIConstants.EXPRESSION_GROUP, fPasteAction);
		}
		ChangeVariableValueAction changeValueAction = (ChangeVariableValueAction)getAction("ChangeVariableValue"); //$NON-NLS-1$
		if (changeValueAction.isApplicable()) {
			menu.add(changeValueAction);
		}
		menu.add(new Separator());
		action = new AvailableLogicalStructuresAction(this);
		if (action.isEnabled()) {
			menu.add(action);
		}
		action = new AvailableDetailPanesAction(this);
		if (isDetailPaneVisible() && action.isEnabled()) {
			menu.add(action);
		}
		menu.add(new Separator(IDebugUIConstants.EMPTY_RENDER_GROUP));
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	@Override
	protected void contextActivated(ISelection selection) {
		if (!isAvailable() || !isVisible()) {
			return;
		}
		if (selection == null || selection.isEmpty()) {
			super.contextActivated(new StructuredSelection(DebugPlugin.getDefault().getExpressionManager()));
		} else {
			super.contextActivated(selection);
		}
		if (isAvailable() && isVisible()) {
			updateAction("ContentAssist"); //$NON-NLS-1$
		}
	}

	@Override
	protected void viewerInputUpdateComplete(IViewerInputUpdate update) {
		IStatus status = update.getStatus();
		if ( (status == null || status.isOK()) && update.getElement() != null) {
			setViewerInput(update.getInputElement());
		} else {
			setViewerInput(DebugPlugin.getDefault().getExpressionManager());
		}
		updateAction(FIND_ACTION);
	}

	@Override
	protected String getDetailPanePreferenceKey() {
		return IDebugPreferenceConstants.EXPRESSIONS_DETAIL_PANE_ORIENTATION;
	}

	@Override
	protected String getToggleActionLabel() {
		return VariablesViewMessages.ExpressionView_4;
	}

	@Override
	protected String getPresentationContextId() {
		return IDebugUIConstants.ID_EXPRESSION_VIEW;
	}

	@Override
	protected void initDragAndDrop(TreeModelViewer viewer) {
		viewer.addDragSupport(DND.DROP_MOVE, new Transfer[] {LocalSelectionTransfer.getTransfer()}, new SelectionDragAdapter(viewer));
		viewer.addDropSupport(DND.DROP_MOVE|DND.DROP_COPY, new Transfer[] {LocalSelectionTransfer.getTransfer(), TextTransfer.getInstance()}, new ExpressionDropAdapter(getSite(), viewer));
	}

	@Override
	protected void createActions() {
		super.createActions();
		fPasteAction = new PasteWatchExpressionsAction(this);
		configure(fPasteAction, IWorkbenchCommandConstants.EDIT_PASTE, PASTE_ACTION, ISharedImages.IMG_TOOL_PASTE);
		fEditInPlaceAction = new EditWatchExpressinInPlaceAction(this);
		configure(fEditInPlaceAction, IWorkbenchCommandConstants.FILE_RENAME, ActionFactory.RENAME.getId(), null);
	}

	@Override
	public void dispose() {
		fEditInPlaceAction.dispose();
		super.dispose();
	}

	/**
	 * Configures the action to override the global action, and registers the
	 * action with this view.
	 *
	 * @param action
	 * 		action
	 * @param defId
	 * 		action definition id
	 * @param globalId
	 * 		global action id
	 * @param imgId
	 * 		image identifier
	 */
	private void configure(IAction action, String defId, String globalId,
			String imgId) {
		setAction(globalId, action);
		action.setActionDefinitionId(defId);
		setGlobalAction(globalId, action);
		if (imgId != null) {
			action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(imgId));
		}
	}

	/**
	 * Returns whether the given selection can be pasted into the expressions
	 * view.
	 *
	 * @return whether the given selection can be pasted into the given target
	 */
	public boolean canPaste() {
		// Checking the content of Clipboard can freeze the UI Bug 562608
		return true;
	}

	/**
	 * Pastes the selection into the given target
	 *
	 * @return whether successful
	 */
	public boolean performPaste() {
		String clipboardText = getClipboardText();

		if (clipboardText != null && clipboardText.length() > 0) {
			if (clipboardText.matches("(?s).*\\R.*")) { //$NON-NLS-1$
				IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
				String pref = store.getString(IDebugPreferenceConstants.PREF_PROMPT_PASTE_MULTILINE_EXPRESSIONS);
				if (pref.equals(IInternalDebugUIConstants.EXPRESSION_PASTE_PROMPT)) {
					LinkedHashMap<String, Integer> buttons = new LinkedHashMap<>();
					buttons.put(ActionMessages.ExpressionPasteMultiButton, IDialogConstants.YES_ID);
					buttons.put(ActionMessages.ExpressionPasteSingleButton, IDialogConstants.NO_ID);
					MessageDialogWithToggle dialog = new MessageDialogWithToggle(DebugUIPlugin.getShell(),
							ActionMessages.ExpressionPasteTitle, null,
							ActionMessages.ExpressionPasteDialog,
							MessageDialog.QUESTION, buttons, 0, ActionMessages.ExpressionPasteRemember, false);
					dialog.open();
					if (dialog.getReturnCode() == IDialogConstants.YES_ID) {
						for (String expression : clipboardText.split("\n")) { //$NON-NLS-1$
							createExpression(expression);
						}
						store.setValue(IDebugPreferenceConstants.PREF_PROMPT_PASTE_MULTILINE_EXPRESSIONS,
								dialog.getToggleState() ? IInternalDebugUIConstants.EXPRESSION_PASTE_AS_MUTLY
										: IInternalDebugUIConstants.EXPRESSION_PASTE_PROMPT);
					} else {
						createExpression(clipboardText);
						store.setValue(IDebugPreferenceConstants.PREF_PROMPT_PASTE_MULTILINE_EXPRESSIONS,
								dialog.getToggleState() ? IInternalDebugUIConstants.EXPRESSION_PASTE_AS_SINGLE
										: IInternalDebugUIConstants.EXPRESSION_PASTE_PROMPT);
					}
				} else if (pref.equals(IInternalDebugUIConstants.EXPRESSION_PASTE_AS_SINGLE)) {
					createExpression(clipboardText);
				} else {
					for (String expression : clipboardText.split("\n")) { //$NON-NLS-1$
						createExpression(expression);
					}
				}
				return true;
			}
			createExpression(clipboardText);
			return true;
		}
		return false;
	}

	/**
	 * Creates an expression in <b>Expression's View</b> for the given snippet
	 *
	 * @param expression snippet to be converted as expression
	 */
	private void createExpression(String expression) {
		if (expression.isEmpty() || expression.matches("[\\t\\n\\r]+")) { //$NON-NLS-1$
			return;
		}
		IExpressionManager expressionManager = DebugPlugin.getDefault().getExpressionManager();
		IWatchExpression watchExpression = expressionManager.newWatchExpression(expression);
		expressionManager.addExpression(watchExpression);
		watchExpression.setExpressionContext(getContext());
	}

	// TODO: duplicate code from WatchExpressionAction
	protected IDebugElement getContext() {
		IAdaptable object = DebugUITools.getPartDebugContext(getSite());
		IDebugElement context = null;
		if (object instanceof IDebugElement) {
			context = (IDebugElement) object;
		} else if (object instanceof ILaunch) {
			context = ((ILaunch) object).getDebugTarget();
		}
		return context;
	}

	protected String getClipboardText() {
		Clipboard clipboard = new Clipboard(Display.getDefault());
		try {
			TextTransfer textTransfer = TextTransfer.getInstance();
			return (String) clipboard.getContents(textTransfer);
		} finally {
			clipboard.dispose();
		}
	}


}
