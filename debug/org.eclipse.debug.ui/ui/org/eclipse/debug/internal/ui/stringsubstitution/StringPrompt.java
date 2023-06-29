/*******************************************************************************
 * Copyright (c) 2000, 2005 Matt Conway and others.
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

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;

/**
 * Prompts the user to input a string and expands to the value entered
 */
public class StringPrompt extends PromptingResolver {

	/**
	 * Prompts the user to input a string.
	 * @see PromptExpanderBase#prompt()
	 */
	@Override
	public void prompt() {
		InputDialog dialog = new InputDialog(null, StringSubstitutionMessages.StringPromptExpander_0, dialogMessage, defaultValue == null ? lastValue : defaultValue, null);
		int dialogResult = dialog.open();
		if (dialogResult == Window.OK) {
			dialogResultString = dialog.getValue();
		}
	}

}
