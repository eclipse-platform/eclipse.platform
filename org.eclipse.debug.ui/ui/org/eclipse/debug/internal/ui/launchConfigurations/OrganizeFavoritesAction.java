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
package org.eclipse.debug.internal.ui.launchConfigurations;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.action.Action;

/**
 * OrganizeFavoritesAction
 */
public class OrganizeFavoritesAction extends Action {

	// launch group identifier
	private String fGroupId;

	/**
	 * @param text
	 */
	public OrganizeFavoritesAction(String launchGroupId) {
		super(LaunchConfigurationsMessages.OrganizeFavoritesAction_0);
		fGroupId = launchGroupId;
	}

	@Override
	public void run() {
		LaunchHistory history = DebugUIPlugin.getDefault().getLaunchConfigurationManager().getLaunchHistory(fGroupId);
		FavoritesDialog dialog = new FavoritesDialog(DebugUIPlugin.getShell(), history);
		dialog.open();
	}

}
