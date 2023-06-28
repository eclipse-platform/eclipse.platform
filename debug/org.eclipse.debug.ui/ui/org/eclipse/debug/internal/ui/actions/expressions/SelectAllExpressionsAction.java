/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.expressions;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IExpressionsListener;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.internal.ui.actions.SelectAllAction;
import org.eclipse.debug.ui.IDebugView;

public class SelectAllExpressionsAction extends SelectAllAction implements IExpressionsListener {

	@Override
	protected boolean isEnabled() {
		return DebugPlugin.getDefault().getExpressionManager().hasExpressions();
	}

	@Override
	protected String getActionId() {
		return IDebugView.SELECT_ALL_ACTION + ".Variables"; //$NON-NLS-1$
	}

	@Override
	protected void initialize() {
		DebugPlugin.getDefault().getExpressionManager().addExpressionListener(this);
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
	public void expressionsChanged(IExpression[] expressions) {
	}

	@Override
	public void dispose() {
		super.dispose();
		DebugPlugin.getDefault().getExpressionManager().removeExpressionListener(this);
	}
}
