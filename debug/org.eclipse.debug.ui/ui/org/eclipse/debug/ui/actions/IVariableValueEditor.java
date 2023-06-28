/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial implementation
 *******************************************************************************/
package org.eclipse.debug.ui.actions;

import org.eclipse.debug.core.model.IVariable;
import org.eclipse.swt.widgets.Shell;

/**
 * A variable value editor allows the user to edit a variable's value. Variable
 * value editors are contributed for a debug model via the
 * <code>org.eclipse.debug.ui.variableValueEditors</code> extension point.
 * <p>
 * Following is example plug-in XML for contributing a variable value editor.
 * </p>
 *
 * <pre>
 * &lt;extension point="org.eclipse.debug.ui.variableValueEditors"&gt;
 *    &lt;variableEditor
 *       modelId="com.examples.myDebugModel"
 *       class="com.examples.variables.MyVariableValueEditor"/&gt;
 * &lt;/extension&gt;
 * </pre>
 *
 * The attributes are specified as follows:
 * <ul>
 * <li><code>modelId</code> the debug model identifier for which the given
 * variable value editor is applicable</li>
 * <li><code>class</code> fully qualified name of a class that implements
 * {@link IVariableValueEditor}</li>
 * </ul>
 * <p>
 * Clients may implement this interface.
 * </p>
 *
 * @since 3.1
 */
public interface IVariableValueEditor {

	/**
	 * Edits the given variable, if appropriate. If this editor does not apply to
	 * the given variable this method returns false, which indicates that the
	 * Debug Platform's default variable edit dialog should be used.
	 *
	 * @param variable the variable to edit
	 * @param shell the currently active shell, which can be used to open a dialog
	 *  for the user
	 * @return whether this editor has completed the edit operation for the given variable.
	 *  <code>true</code> if no more work should be done, <code>false</code> if the debug
	 *  platform should prompt the user to edit the given variable using the default
	 *  variable editor
	 */
	boolean editVariable(IVariable variable, Shell shell);

	/**
	 * Saves the given expression to the given variable, if appropriate. If this
	 * editor does not set the given variable's value from the given expression, this
	 * method returns false. Returning false indicates that the Debug Platform should
	 * perform the default operation to set a variable's value based on a String.
	 *
	 * @param variable the variable to edit
	 * @param expression the expression to assign to the given variable
	 * @param shell the currently active shell, which can be used to report errors to the
	 *  user. May be <code>null</code> if no active shell could be found.
	 * @return whether this editor has completed the save operation for the given variable.
	 *  <code>true</code> if no more work should be done, <code>false</code> if the debug
	 *  platform should perform the default save operation
	 */
	boolean saveVariable(IVariable variable, String expression, Shell shell);
}
