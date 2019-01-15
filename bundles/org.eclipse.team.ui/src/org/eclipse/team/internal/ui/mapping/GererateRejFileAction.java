/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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
import org.eclipse.team.internal.ui.synchronize.patch.ApplyPatchModelSynchronizeParticipant;
import org.eclipse.team.internal.ui.synchronize.patch.ApplyPatchSubscriber;
import org.eclipse.team.internal.ui.synchronize.patch.ApplyPatchSubscriberMergeContext;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class GererateRejFileAction extends Action {

	private ApplyPatchSubscriber subscriber;

	public GererateRejFileAction(ISynchronizePageConfiguration configuration) {
		super("", AS_CHECK_BOX); //$NON-NLS-1$
		ApplyPatchModelSynchronizeParticipant participant = (ApplyPatchModelSynchronizeParticipant) configuration
				.getParticipant();
		ApplyPatchSubscriberMergeContext context = (ApplyPatchSubscriberMergeContext) participant
				.getContext();
		subscriber = (ApplyPatchSubscriber) context.getSubscriber();
	}

	@Override
	public boolean isChecked() {
		return subscriber.getPatcher().isGenerateRejectFile();
	}

	@Override
	public void run() {
		boolean oldValue = subscriber.getPatcher().isGenerateRejectFile();
		subscriber.getPatcher().setGenerateRejectFile(!oldValue);

		firePropertyChange(CHECKED, Boolean.valueOf(oldValue), Boolean.valueOf(
				!oldValue));
	}

}
