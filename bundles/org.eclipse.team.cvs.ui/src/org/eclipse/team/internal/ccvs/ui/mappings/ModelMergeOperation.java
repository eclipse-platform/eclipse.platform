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

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.internal.ccvs.core.CVSMergeSubscriber;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.ui.synchronize.ModelSynchronizeParticipant;
import org.eclipse.ui.IWorkbenchPart;

public class ModelMergeOperation extends AbstractModelMergeOperation {

	private final Subscriber subscriber;
	private final boolean attempAutomerge;
	
	/**
	 * Create a merge operation for the given subscriber. The merge operation will cancel the subscriber
	 * when it is no longer needed.
	 * @param part the part
	 * @param mappings the mappings
	 * @param subscriber the subscriber
	 * @param attempAutomerge 
	 */
	public ModelMergeOperation(IWorkbenchPart part, ResourceMapping[] mappings, final CVSMergeSubscriber subscriber, boolean attempAutomerge) {
		super(part, new SubscriberScopeManager(subscriber.getName(), mappings, subscriber, true){
			@Override
			public void dispose() {
				subscriber.cancel();
				super.dispose();
			}
		}, true);
		this.subscriber = subscriber;
		this.attempAutomerge = attempAutomerge;
	}

	@Override
	protected SynchronizationContext createMergeContext() {
		return MergeSubscriberContext.createContext(getScopeManager(), subscriber);
	}
	
	@Override
	protected String getJobName() {
		return CVSUIMessages.MergeUpdateAction_jobName;
	}
	
	@Override
	protected ModelSynchronizeParticipant createParticipant() {
		setOwnsManager(false);
		return new ModelMergeParticipant((MergeSubscriberContext)createMergeContext());
	}
	
	@Override
	protected boolean isPreviewInDialog() {
		return false;
	}
	
	@Override
	public boolean isPreviewRequested() {
		return !attempAutomerge;
	}

}
