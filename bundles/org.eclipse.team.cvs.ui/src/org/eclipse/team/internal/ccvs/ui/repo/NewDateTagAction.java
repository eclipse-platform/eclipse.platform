/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.repo;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.DateTagDialog;

/**
 * Action for creating a CVS Date tag.
 */
public class NewDateTagAction extends CVSRepoViewAction {

	@Override
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException {
		ICVSRepositoryLocation[] locations = getSelectedRepositoryLocations();
		if (locations.length != 1) return;
		CVSTag tag = getDateTag(getShell(), locations[0]);
		CVSUIPlugin.getPlugin().getRepositoryManager().addDateTag(locations[0], tag);
	}

	public static CVSTag getDateTag(Shell shell, ICVSRepositoryLocation location) {
		DateTagDialog dialog = new DateTagDialog(shell);
		if (dialog.open() == Window.OK) {
			Date date = dialog.getDate();		
			CVSTag tag = new CVSTag(date);			
			return tag;
		}
		return null;
	}

	@Override
	public boolean isEnabled() {
		ICVSRepositoryLocation[] locations = getSelectedRepositoryLocations();
		if (locations.length != 1) return false;
		return true;
	}
}
