/*******************************************************************************
 * Copyright (c) 2010, 2018 IBM Corporation and others.
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
 *     Alex Blewitt <alex.blewitt@gmail.com> - replace new Boolean with Boolean.valueOf - https://bugs.eclipse.org/470344
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.jface.action.Action;
import org.eclipse.team.internal.core.subscribers.SubscriberDiffTreeEventHandler;
import org.eclipse.team.internal.ui.synchronize.patch.ApplyPatchModelSynchronizeParticipant;
import org.eclipse.team.internal.ui.synchronize.patch.ApplyPatchSubscriber;
import org.eclipse.team.internal.ui.synchronize.patch.ApplyPatchSubscriberMergeContext;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class ReversePatchAction extends Action {

	private ISynchronizePageConfiguration configuration;
	private ApplyPatchModelSynchronizeParticipant participant;
	private ApplyPatchSubscriberMergeContext context;
	private ApplyPatchSubscriber subscriber;

	public ReversePatchAction(ISynchronizePageConfiguration configuration) {
		super("", AS_CHECK_BOX); //$NON-NLS-1$
		this.configuration = configuration;
		participant = (ApplyPatchModelSynchronizeParticipant) configuration
				.getParticipant();
		context = (ApplyPatchSubscriberMergeContext) participant.getContext();
		subscriber = (ApplyPatchSubscriber) context.getSubscriber();
	}

	@Override
	public boolean isChecked() {
		return subscriber.getPatcher().isReversed();
	}

	@Override
	public void run() {
		boolean oldValue = subscriber.getPatcher().isReversed();
		subscriber.getPatcher().setReversed(!oldValue);

		SubscriberDiffTreeEventHandler handler = context.getAdapter(SubscriberDiffTreeEventHandler.class);
		handler.reset();
		participant.refresh(configuration.getSite().getWorkbenchSite(), context
				.getScope().getMappings());

		firePropertyChange(CHECKED, Boolean.valueOf(oldValue), Boolean.valueOf(
				!oldValue));
	}

}
