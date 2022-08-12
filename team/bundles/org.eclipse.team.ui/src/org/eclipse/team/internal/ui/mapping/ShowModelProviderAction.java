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
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.mapping.ITeamContentProviderDescriptor;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;

public class ShowModelProviderAction extends Action {

	private final ISynchronizePageConfiguration configuration;
	private final ModelProvider provider;

	public ShowModelProviderAction(ISynchronizePageConfiguration configuration, ModelProvider provider) {
		super(Utils.getLabel(provider), IAction.AS_RADIO_BUTTON);
		this.configuration = configuration;
		this.provider = provider;
		setImageDescriptor(getImageDescriptor(provider));
	}

	private ImageDescriptor getImageDescriptor(ModelProvider provider) {
		ITeamContentProviderManager manager = TeamUI.getTeamContentProviderManager();
		ITeamContentProviderDescriptor desc = manager.getDescriptor(provider.getId());
		return desc.getImageDescriptor();
	}

	@Override
	public void run() {
		configuration.setProperty(
				ModelSynchronizeParticipant.P_VISIBLE_MODEL_PROVIDER,
				provider.getDescriptor().getId());
	}

	public String getProviderId() {
		return provider.getDescriptor().getId();
	}

}
