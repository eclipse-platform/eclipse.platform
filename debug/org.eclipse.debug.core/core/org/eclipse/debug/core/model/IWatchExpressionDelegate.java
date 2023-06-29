/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.debug.core.model;

/**
 * A delegate which computes the value of a watch expression
 * when provided a context. Watch delegates are provided on a
 * per debug model basis. Watch expressions query the appropriate
 * delegate based on the debug model of the context element.
 * Plug-ins that wish to contribute watch expression delegates may do so using the
 * <code>org.eclipse.debug.core.watchExpressionDelegates</code>
 * extension point.
 * <p>
 * For example, the following is the definition of a watch expression
 * delegate for the com.example.foo plug-in:
 * <pre>
 * &lt;extension point="org.eclipse.debug.core.watchExpressionDelegates"&gt;
 *   &lt;watchExpressionDelegate
 *     debugModel="org.eclipse.jdt.debug"
 *     delegateClass="org.eclipse.jdt.internal.debug.ui.JavaWatchExpressionDelegate"/&gt;
 *  &lt;/extension&gt;
 * </pre>
 * <p>
 * Clients are intended to implement this interface.
 * </p>
 * @see org.eclipse.debug.core.model.IWatchExpression
 * @see org.eclipse.debug.core.model.IWatchExpressionListener
 *
 * @since 3.0
 */
public interface IWatchExpressionDelegate {

	/**
	 * Evaluates the given expression in the given context asynchronously and
	 * notifies the given listener when the evaluation finishes.
	 *
	 * @param expression the expression to evaluate
	 * @param context the context for the evaluation
	 * @param listener the listener to notify when the evaluation completes
	 */
	void evaluateExpression(String expression, IDebugElement context, IWatchExpressionListener listener);

}
