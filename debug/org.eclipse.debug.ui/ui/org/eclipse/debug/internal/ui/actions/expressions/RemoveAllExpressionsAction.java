/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.expressions;


import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.IExpressionsListener;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.AbstractRemoveAllActionDelegate;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Removes all expressions from the expressions view.
 */
public class RemoveAllExpressionsAction extends AbstractRemoveAllActionDelegate implements IExpressionsListener {

	@Override
	public void run(IAction action) {
		IWorkbenchWindow window = DebugUIPlugin.getActiveWorkbenchWindow();
		if (window != null) {
			IPreferenceStore store = DebugUIPlugin.getDefault().getPreferenceStore();
			boolean prompt = store.getBoolean(IDebugPreferenceConstants.PREF_PROMPT_REMOVE_ALL_EXPRESSIONS);
			boolean proceed = true;
			if (prompt) {
				MessageDialogWithToggle mdwt = MessageDialogWithToggle.openYesNoQuestion(window.getShell(), ActionMessages.RemoveAllExpressionsAction_0,
						ActionMessages.RemoveAllExpressionsAction_1, ActionMessages.RemoveAllBreakpointsAction_3, !prompt, null, null);
				if(mdwt.getReturnCode() !=  IDialogConstants.YES_ID){
					proceed = false;
				}
				else {
					store.setValue(IDebugPreferenceConstants.PREF_PROMPT_REMOVE_ALL_EXPRESSIONS, !mdwt.getToggleState());
				}
			}
			if (proceed) {
				IExpressionManager manager = DebugPlugin.getDefault().getExpressionManager();
				IExpression[] expressions= manager.getExpressions();
				manager.removeExpressions(expressions);
			}
		}
	}

	@Override
	protected boolean isEnabled() {
		return DebugPlugin.getDefault().getExpressionManager().hasExpressions();
	}

	@Override
	protected void initialize() {
		DebugPlugin.getDefault().getExpressionManager().addExpressionListener(this);
	}

	@Override
	public void dispose() {
		DebugPlugin.getDefault().getExpressionManager().removeExpressionListener(this);
		super.dispose();
	}

	@Override
	public void expressionsAdded(IExpression[] expressions) {
		update();
	}

	@Override
	public void expressionsRemoved(IExpression[] expressions) {
		update();
	}

	@Override
	public void expressionsChanged(IExpression[] expressions) {}
}
