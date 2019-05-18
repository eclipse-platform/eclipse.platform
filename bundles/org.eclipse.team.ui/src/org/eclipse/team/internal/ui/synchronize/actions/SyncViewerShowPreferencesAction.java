/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.dialogs.PreferencePageContainerDialog;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class SyncViewerShowPreferencesAction extends Action {
	private final ISynchronizePageConfiguration configuration;

	public SyncViewerShowPreferencesAction(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
		Utils.initAction(this, "action.syncViewPreferences."); //$NON-NLS-1$
	}

	@Override
	public void run() {
		PreferencePage[] pages = configuration.getParticipant().getPreferencePages();
		PreferencePageContainerDialog dialog = new PreferencePageContainerDialog(configuration.getSite().getShell(), pages);
		dialog.setBlockOnOpen(true);
		dialog.open();
	}
}
