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
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.jface.action.IAction;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.mappings.ModelUpdateOperation;
import org.eclipse.team.internal.ccvs.ui.operations.UpdateOperation;

/**
 * Action that runs an update without prompting the user for a tag.
 * 
 * @since 3.1
 */
public class UpdateSilentAction extends WorkspaceTraversalAction {
	
	@Override
	protected boolean isEnabledForAddedResources() {
		return false;
	}

	@Override
	protected boolean isEnabledForNonExistantResources() {
		return true;
	}
	
	@Override
	public void execute(IAction action) throws InterruptedException, InvocationTargetException {
		if (CVSUIPlugin.getPlugin().getPreferenceStore().getString(ICVSUIConstants.PREF_UPDATE_HANDLING).equals(ICVSUIConstants.PREF_UPDATE_HANDLING_TRADITIONAL)) {
			new UpdateOperation(getTargetPart(), getCVSResourceMappings(), Command.NO_LOCAL_OPTIONS, null /* no tag */).run();
		} else {
			new ModelUpdateOperation(getTargetPart(), getSelectedResourceMappings(CVSProviderPlugin.getTypeId())).run();
		}
	}
	
	@Override
	public String getId() {
		return ICVSUIConstants.CMD_UPDATE;
	}
}
