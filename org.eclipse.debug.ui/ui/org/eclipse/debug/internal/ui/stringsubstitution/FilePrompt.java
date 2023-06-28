/*******************************************************************************
 * Copyright (c) 2000, 2017 Matt Conway and others.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;

/**
 * Prompts the user to choose a file and expands the selection
 */
public class FilePrompt extends PromptingResolver {

	/**
	 * Prompts the user to choose a file
	 * @see PromptExpanderBase#prompt()
	 */
	@Override
	public void prompt() {
		FileDialog dialog = new FileDialog(getShell(), SWT.SHEET);
		dialog.setText(dialogMessage);
		dialog.setFileName(lastValue == null ? defaultValue : lastValue);
		dialogResultString = dialog.open();
	}

}
