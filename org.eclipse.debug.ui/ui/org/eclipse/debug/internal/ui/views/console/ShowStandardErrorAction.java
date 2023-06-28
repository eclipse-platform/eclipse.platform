/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.console;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.ui.PlatformUI;

/**
 * Toggles preference show the console when output is written
 * to standard error stream.
 *
 * @since 3.3
 */
public class ShowStandardErrorAction extends ShowWhenContentChangesAction {

	/**
	 * Constructs an action to toggle console auto activation preferences
	 */
	public ShowStandardErrorAction() {
		super(ConsoleMessages.ShowStandardErrorAction_0);
		setId(DebugUIPlugin.getUniqueIdentifier() + ".ShowWhenStderrChangesAction"); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.SHOW_WHEN_STDERR_CHANGES_ACTION);
		setImageDescriptor(DebugUITools.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_STANDARD_ERR));
	}

	@Override
	protected String getKey() {
		return IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR;
	}

}
