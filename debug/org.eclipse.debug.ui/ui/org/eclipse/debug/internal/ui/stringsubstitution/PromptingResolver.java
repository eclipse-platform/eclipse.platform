/*******************************************************************************
 * Copyright (c) 2000, 2018 Matt Conway and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Matt Conway - initial implementation
 *     IBM Corporation - integration and code cleanup
 *******************************************************************************/
package org.eclipse.debug.internal.ui.stringsubstitution;

import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.swt.widgets.Shell;

/**
 * Base implementation for variable resolvers that prompt the user
 * for their value.
 */
abstract class PromptingResolver implements IDynamicVariableResolver {

	/**
	 * A hint that helps the user choose their input. If a prompt
	 * hint is provider the user will be prompted:
	 * 	Please input a value for <code>promptHint</code>
	 */
	protected String promptHint = null;
	/**
	 * The prompt displayed to the user.
	 */
	protected String dialogMessage = null;
	/**
	 * The default value selected when the prompt is displayed
	 */
	protected String defaultValue = null;
	/**
	 * The last value chosen by the user for this variable
	 */
	protected String lastValue = null;
	/**
	 * The result returned from the prompt dialog
	 */
	protected String dialogResultString = null;

	/**
	 * Presents the user with the appropriate prompt for the variable to be expanded
	 * and sets the <code>dialogResultString</code> based on the user's selection.
	 */
	public abstract void prompt();

	/**
	 * Initializes values displayed when the user is prompted. If
	 * a prompt hint and default value are supplied in the given
	 * variable value, these are extracted for presentation
	 *
	 * @param varValue the value of the variable from which the prompt
	 * hint and default value will be extracted
	 */
	protected void setupDialog(String varValue) {
		promptHint = null;
		defaultValue = null;
		dialogResultString = null;
		if (varValue != null) {
			int idx = varValue.indexOf(':');
			if (idx != -1) {
				promptHint = varValue.substring(0, idx);
				defaultValue = varValue.substring(idx + 1);
			} else {
				promptHint = varValue;
			}
		}

		if (promptHint != null) {
			dialogMessage = MessageFormat.format(StringSubstitutionMessages.PromptExpanderBase_0, new Object[] { promptHint });
		} else {
			dialogMessage = StringSubstitutionMessages.PromptExpanderBase_1;
		}
	}

	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		String value = null;
		setupDialog(argument);

		DebugUIPlugin.getStandardDisplay().syncExec(this::prompt);
		if (dialogResultString != null) {
			value = dialogResultString;
			lastValue = dialogResultString;
		} else {
			// dialogResultString == null means prompt was cancelled
			throw new DebugException(new Status(IStatus.CANCEL, DebugUIPlugin.getUniqueIdentifier(), IStatus.CANCEL, MessageFormat.format(StringSubstitutionMessages.PromptingResolver_0, new Object[] { variable.getName() }), null));
		}
		return value;
	}

	protected Shell getShell() {
		return DebugUIPlugin.getShell();
	}

}
