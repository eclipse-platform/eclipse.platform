/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.variables.details;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IValueModification;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.VariableValueEditorManager;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.actions.StatusInfo;
import org.eclipse.debug.ui.actions.IVariableValueEditor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * Action which assigns a value to a variable from the detail pane
 * of the variables view.
 */
public class DetailPaneAssignValueAction extends Action{

	private IHandlerActivation fHandlerActivation;
	private IViewSite fViewSite;
	private ITextViewer fTextViewer;
	private IStructuredSelection fCurrentSelection;

	/**
	 * Attempts to evaluate the given string expression and assign the resulting value to the
	 * specified variable.  Displays error dialogs to the user if a problem is encountered.
	 *
	 * @see DetailPaneAssignValueAction
	 * @see org.eclipse.debug.internal.ui.elements.adapters.DefaultVariableCellModifier
	 *
	 * @param shell the shell to use to open dialogs
	 * @param variable the variable that is getting a new value
	 * @param newValueExpression the expression to evaluate and set as the new value
	 * @since 3.3.0
	 */
	public static void assignValue(Shell shell, IVariable variable, String newValueExpression){
		String modelIdentifier = variable.getModelIdentifier();
		IVariableValueEditor editor = VariableValueEditorManager.getDefault().getVariableValueEditor(modelIdentifier);
		if (editor != null) {
			if (editor.saveVariable(variable, newValueExpression, shell)) {
				// If we successfully delegate to an editor which performs the save,
				// don't do any more work.
				return;
			}
		}

		try {
			// If we failed to delegate to anyone, perform the default assignment.
			if (variable.verifyValue(newValueExpression)) {
				variable.setValue(newValueExpression);
			} else {
				if (shell != null) {
					DebugUIPlugin.errorDialog(shell, ActionMessages.DetailPaneAssignValueAction_2, MessageFormat.format(ActionMessages.DetailPaneAssignValueAction_3, new Object[] {
							newValueExpression, variable.getName() }), new StatusInfo(IStatus.ERROR, ActionMessages.DetailPaneAssignValueAction_4)); //
				}
			}
		} catch (DebugException e) {
			MessageDialog.openError(shell, ActionMessages.DetailPaneAssignValueAction_0, e.getStatus().getMessage());
		}
	}

	public DetailPaneAssignValueAction(ITextViewer textViewer, IViewSite viewSite) {
		super(ActionMessages.DetailPaneAssignValueAction_1);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.DETAIL_PANE_ASSIGN_VALUE_ACTION);

		fTextViewer = textViewer;
		fViewSite = viewSite;

		setEnabled(false);
		IHandlerService service = fViewSite.getService(IHandlerService.class);
		ActionHandler handler = new ActionHandler(this);
		fHandlerActivation = service.activateHandler(getActionDefinitionId(), handler);
	}

	public void dispose() {
		IHandlerService service = fViewSite.getService(IHandlerService.class);
		service.deactivateHandler(fHandlerActivation);
	}

	public void updateCurrentVariable(IStructuredSelection selection) {
		boolean enabled = false;
		if ((selection.size() == 1) && (selection.getFirstElement() instanceof IValueModification)) {
			IValueModification valMod = (IValueModification) selection.getFirstElement();
			if (valMod.supportsValueModification()) {
				fCurrentSelection = selection;
				enabled = true;
			}
		}
		setEnabled(enabled);
	}

	@Override
	public void run() {
		IVariable variable = (IVariable) fCurrentSelection.getFirstElement();

		Point selection = fTextViewer.getSelectedRange();
		String value = null;
		if (selection.y == 0) {
			value = fTextViewer.getDocument().get();
		} else {
			try {
				value = fTextViewer.getDocument().get(selection.x, selection.y);
			} catch (BadLocationException e1) {
			}
		}
		IWorkbenchWindow window= DebugUIPlugin.getActiveWorkbenchWindow();
		Shell activeShell= null;
		if (window != null) {
			activeShell= window.getShell();
		}

		assignValue(activeShell, variable, value);
	}

	@Override
	public String getActionDefinitionId() {
		return IWorkbenchCommandConstants.FILE_SAVE;
	}

}
