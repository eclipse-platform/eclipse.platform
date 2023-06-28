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
 *     Samrat Dhillon samrat.dhillon@gmail.com - Bug 369012 - [expr] Modifying a variable value using cell editor is not reflected in view.
 *******************************************************************************/

package org.eclipse.debug.internal.ui.viewers.update;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;

/**
 * Event handler for an expression.
 *
 * @since 3.2
 *
 */
public class ExpressionEventHandler extends DebugEventHandler {

	public ExpressionEventHandler(AbstractModelProxy proxy) {
		super(proxy);
	}

	@Override
	protected boolean handlesEvent(DebugEvent event) {
		return event.getKind() == DebugEvent.CHANGE;
	}

	@Override
	protected void handleChange(DebugEvent event) {
		ModelDelta delta = new ModelDelta(DebugPlugin.getDefault().getExpressionManager(), IModelDelta.NO_CHANGE);
		IExpression expression = null;
		if (event.getSource() instanceof IExpression) {
			expression = (IExpression) event.getSource();
			int flags = IModelDelta.NO_CHANGE;
			if ((event.getDetail() & DebugEvent.STATE) != 0) {
				flags = flags | IModelDelta.STATE;
			}
			if ((event.getDetail() & DebugEvent.CONTENT) != 0) {
				flags = flags | IModelDelta.CONTENT;
			}
			delta.addNode(expression, flags);
			fireDelta(delta);
		}
		if (event.getSource() instanceof IVariable) {
			IVariable variable = (IVariable) event.getSource();
			int flags = IModelDelta.NO_CHANGE;
			if (event.getDetail()==DebugEvent.CONTENT) {
				flags = flags | IModelDelta.CONTENT;
			}
			delta.addNode(variable, flags);
			fireDelta(delta);
		}
	}

	@Override
	protected void refreshRoot(DebugEvent event) {
		ModelDelta delta = new ModelDelta(DebugPlugin.getDefault().getExpressionManager(), IModelDelta.CONTENT);
		fireDelta(delta);
	}

}
