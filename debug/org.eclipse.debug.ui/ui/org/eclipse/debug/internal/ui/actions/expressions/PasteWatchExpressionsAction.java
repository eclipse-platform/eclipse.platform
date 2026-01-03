/*******************************************************************************
 * Copyright (c) 2009, 2025 Adobe Systems, Inc. and others.
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
 *     IBM Corporation - Improve paste expressions
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

	@Override
	public String getText() {
		return ActionMessages.PasteWatchExpressionsActionLabel;
	}


}
