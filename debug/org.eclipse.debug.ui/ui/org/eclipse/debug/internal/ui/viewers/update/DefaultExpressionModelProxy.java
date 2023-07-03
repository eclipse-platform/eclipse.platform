/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers.update;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IVariable;

/**
 * @since 3.2
 *
 */
public class DefaultExpressionModelProxy extends EventHandlerModelProxy {

	private IExpression fExpression;

	public DefaultExpressionModelProxy(IExpression expression) {
		fExpression = expression;
	}

	@Override
	public synchronized void dispose() {
		super.dispose();
		fExpression = null;
	}

	@Override
	protected DebugEventHandler[] createEventHandlers() {
		return new DebugEventHandler[]{new ExpressionEventHandler(this)};
	}

	@Override
	protected boolean containsEvent(DebugEvent event) {
		// handles change events from expressions and debug targets
		if (event.getSource().equals(fExpression) || event.getSource().equals(fExpression.getDebugTarget())) {
			return true;
		}
		// have to consider change events on variables
		return event.getKind() == DebugEvent.CHANGE && event.getSource() instanceof IVariable;
	}

	/**
	 * Returns this model's expression, or <code>null</code> if disposed.
	 *
	 * @return expression or <code>null</code>
	 */
	protected IExpression getExpression() {
		return fExpression;
	}

}
