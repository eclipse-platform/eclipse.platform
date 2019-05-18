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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberMergeContext;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.synchronize.ChangeSetCapability;
import org.eclipse.team.internal.ui.synchronize.IChangeSetProvider;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class ModelCompareParticipant extends CVSModelSynchronizeParticipant implements IChangeSetProvider {

	public static final String VIEWER_ID = "org.eclipse.team.cvs.ui.compareSynchronization"; //$NON-NLS-1$
	
	public class CompareChangeSetCapability extends ModelParticipantChangeSetCapability {
		@Override
		public CheckedInChangeSetCollector createCheckedInChangeSetCollector(ISynchronizePageConfiguration configuration) {
			return new CheckedInChangeSetCollector(configuration, getSubscriber());
		}
	}

	private CompareChangeSetCapability capability;
	
	public ModelCompareParticipant(SynchronizationContext context) {
		super(context);
		try {
			setInitializationData(TeamUI.getSynchronizeManager().getParticipantDescriptor("org.eclipse.team.cvs.ui.modelCompareParticipant")); //$NON-NLS-1$
		} catch (CoreException e) {
			TeamUIPlugin.log(e);
		}
		setSecondaryId(Long.toString(System.currentTimeMillis()));
	}

	@Override
	protected void initializeConfiguration(ISynchronizePageConfiguration configuration) {
		configuration.setProperty(ISynchronizePageConfiguration.P_VIEWER_ID, VIEWER_ID);
		super.initializeConfiguration(configuration);
	}
	
	public Subscriber getSubscriber() {
		return ((SubscriberMergeContext)getContext()).getSubscriber();
	}
	
	@Override
	public ChangeSetCapability getChangeSetCapability() {
		if (capability == null) {
			capability = new CompareChangeSetCapability();
		}
		return capability;
	}
}
