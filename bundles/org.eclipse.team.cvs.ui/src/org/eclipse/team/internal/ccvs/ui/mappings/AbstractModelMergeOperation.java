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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.mappings;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.ui.synchronize.ModelParticipantMergeOperation;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.IWorkbenchPart;

public abstract class AbstractModelMergeOperation extends ModelParticipantMergeOperation {
	
	private boolean ownsManager = false;
	
	public AbstractModelMergeOperation(IWorkbenchPart part, ISynchronizationScopeManager manager, boolean ownsManager) {
		super(part, manager);
		this.ownsManager = ownsManager;
	}
	
	@Override
	protected boolean canRunAsJob() {
		return true;
	}
	
	@Override
	protected boolean isPreviewInDialog() {
		return CVSUIPlugin.getPlugin().getPreferenceStore().getString(ICVSUIConstants.PREF_UPDATE_PREVIEW).equals(ICVSUIConstants.PREF_UPDATE_PREVIEW_IN_DIALOG);
	}
	
	@Override
	protected void endOperation(IProgressMonitor monitor) throws InvocationTargetException {
		if (ownsManager) {
			ISynchronizationScopeManager manager = getScopeManager();
			manager.dispose();
		}
		super.endOperation(monitor);
	}
	
	@Override
	protected ModelSynchronizeParticipant createParticipant() {
		ModelSynchronizeParticipant participant = super.createParticipant();
		// Transfer ownership of the manager to the participant
		setOwnsManager(false);
		return participant;
	}

	public boolean isOwnsManager() {
		return ownsManager;
	}

	public void setOwnsManager(boolean ownsManager) {
		this.ownsManager = ownsManager;
	}

}
