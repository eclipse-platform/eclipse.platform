/*******************************************************************************
 * Copyright (c) 2009 Adobe Systems, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Adobe Systems, Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.expressions;

import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.views.expression.ExpressionView;
import org.eclipse.ui.actions.SelectionListenerAction;

/**
 * Paste a watch expression into the expressions view.
 */
public class PasteWatchExpressionsAction extends SelectionListenerAction {

	private final ExpressionView fExpressionView;

	public PasteWatchExpressionsAction(ExpressionView expressionView) {
		super(ActionMessages.PasteWatchExpressionsAction_0);
		fExpressionView = expressionView;
//        setToolTipText(BreakpointGroupMessages.PasteWatchExpressionsAction_1);
//        PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.PASTE_WATCH_EXPRESSIONS_ACTION);
	}

	@Override
	public void run() {
		if (fExpressionView.canPaste()) {
			fExpressionView.performPaste();
		}
	}

	@Override
	public boolean isEnabled() {
		return fExpressionView.canPaste();
	}



}
