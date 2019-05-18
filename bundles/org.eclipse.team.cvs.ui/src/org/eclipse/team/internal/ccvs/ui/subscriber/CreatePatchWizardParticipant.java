/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.team.internal.ccvs.ui.wizards.GenerateDiffFileWizard;
import org.eclipse.team.internal.ui.synchronize.SynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ISynchronizeScope;

public class CreatePatchWizardParticipant extends WorkspaceSynchronizeParticipant {
	
	final GenerateDiffFileWizard fWizard;
	
	public CreatePatchWizardParticipant(ISynchronizeScope scope, GenerateDiffFileWizard wizard) {
		super(scope);
		fWizard= wizard;
	}

	@Override
	protected void initializeConfiguration( ISynchronizePageConfiguration configuration) {
		super.initializeConfiguration(configuration);
		configuration.setProperty(ISynchronizePageConfiguration.P_TOOLBAR_MENU, new String[] {ISynchronizePageConfiguration.LAYOUT_GROUP});
		configuration.setProperty(ISynchronizePageConfiguration.P_CONTEXT_MENU, ISynchronizePageConfiguration.DEFAULT_CONTEXT_MENU);
		((SynchronizePageConfiguration)configuration).setViewerStyle(SynchronizePageConfiguration.CHECKBOX);
		configuration.setSupportedModes(ISynchronizePageConfiguration.OUTGOING_MODE);
		configuration.setMode(ISynchronizePageConfiguration.OUTGOING_MODE);
	}

	@Override
	public boolean doesSupportSynchronize() {
		return false;
	}
}
