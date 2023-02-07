/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
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

package org.eclipse.help.ui.internal.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.IHelpBaseConstants;
import org.eclipse.help.ui.internal.DefaultHelpUI;
import org.eclipse.ui.PlatformUI;

/**
 * Default handler for the "Help/Index" command
 */

public class ShowIndexHandler extends AbstractHandler {

	/*
	 * Currently returns true, could be controlled by a preference
	 * in the future
	 */
	private boolean isOpenInHelpView() {
		boolean searchFromBrowser =
			Platform.getPreferencesService().getBoolean(HelpBasePlugin.PLUGIN_ID,IHelpBaseConstants.P_KEY_SEARCH_FROM_BROWSER, false, null);
		return !searchFromBrowser;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (isOpenInHelpView()) {
			openInHelpView();
		} else {
			openInBrowser();
		}
		return null;
	}

	private void openInBrowser() {
		PlatformUI.getWorkbench().getHelpSystem();
		BaseHelpSystem.getHelpDisplay().displayHelpResource("tab=index", false); //$NON-NLS-1$
	}

	private void openInHelpView() {
		DefaultHelpUI.showIndex();
	}

}
