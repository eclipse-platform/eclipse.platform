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
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.core.subscribers.SubscriberDiffTreeEventHandler;
import org.eclipse.team.internal.ui.synchronize.patch.ApplyPatchModelSynchronizeParticipant;
import org.eclipse.team.internal.ui.synchronize.patch.ApplyPatchSubscriber;
import org.eclipse.team.internal.ui.synchronize.patch.ApplyPatchSubscriberMergeContext;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class FuzzFactorAction extends Action {

	private ISynchronizePageConfiguration configuration;
	private ApplyPatchModelSynchronizeParticipant participant;
	private ApplyPatchSubscriberMergeContext context;
	private ApplyPatchSubscriber subscriber;

	public FuzzFactorAction(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
		participant = (ApplyPatchModelSynchronizeParticipant) configuration
				.getParticipant();
		context = (ApplyPatchSubscriberMergeContext) participant.getContext();
		subscriber = (ApplyPatchSubscriber) context.getSubscriber();
	}

	@Override
	public void run() {
		FuzzFactorDialog dialog = new FuzzFactorDialog(Display.getCurrent()
				.getActiveShell(), subscriber.getPatcher());
		if (dialog.open() == Window.OK) {
			int oldValue = subscriber.getPatcher().getFuzz();
			int newValue = dialog.getFuzzFactor();
			if (newValue != oldValue) {
				SubscriberDiffTreeEventHandler handler = context.getAdapter(SubscriberDiffTreeEventHandler.class);
				handler.reset();
				subscriber.getPatcher().setFuzz(newValue);
				participant.refresh(configuration.getSite().getWorkbenchSite(),
						context.getScope().getMappings());
			}
		}
	}

}
