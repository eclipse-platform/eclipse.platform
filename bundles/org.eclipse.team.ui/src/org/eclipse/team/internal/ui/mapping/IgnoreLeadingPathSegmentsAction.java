/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.diff.provider.DiffTree;
import org.eclipse.team.internal.core.subscribers.SubscriberDiffTreeEventHandler;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.synchronize.patch.*;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class IgnoreLeadingPathSegmentsAction extends Action {

	private ISynchronizePageConfiguration configuration;
	private ApplyPatchModelSynchronizeParticipant participant;
	private ApplyPatchSubscriberMergeContext context;
	private ApplyPatchSubscriber subscriber;
	private int maxValue;

	public IgnoreLeadingPathSegmentsAction(
			ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
		participant = (ApplyPatchModelSynchronizeParticipant) configuration
				.getParticipant();
		context = (ApplyPatchSubscriberMergeContext) participant.getContext();
		subscriber = (ApplyPatchSubscriber) context.getSubscriber();
	}

	@Override
	public boolean isEnabled() {
		return !subscriber.getPatcher().isWorkspacePatch();
	}

	@Override
	public void run() {
		int oldValue = subscriber.getPatcher().getStripPrefixSegments();
		maxValue = subscriber.getPatcher().calculatePrefixSegmentCount() - 1;

		InputDialog dlg = new InputDialog(
				Display.getCurrent().getActiveShell(),
				TeamUIMessages.IgnoreLeadingPathSegmentsDialog_title, NLS.bind(
						TeamUIMessages.IgnoreLeadingPathSegmentsDialog_message,
						Integer.valueOf(maxValue)), new Integer(oldValue)
						.toString(), new IInputValidator() {
					@Override
					public String isValid(String input) {
						try {
							int i = Integer.parseInt(input);
							if (i < 0 || i > maxValue)
								return TeamUIMessages.IgnoreLeadingPathSegmentsDialog_numberOutOfRange;
						} catch (NumberFormatException x) {
							return TeamUIMessages.IgnoreLeadingPathSegmentsDialog_notANumber;
						}
						return null;
					}
				});

		if (dlg.open() == Window.OK) {
			String input = dlg.getValue();
			int newValue = Integer.parseInt(input);
			if (newValue != oldValue) {
				DiffTree tree = (DiffTree)context.getDiffTree();
				tree.clear();
				SubscriberDiffTreeEventHandler handler = (SubscriberDiffTreeEventHandler) context
						.getAdapter(SubscriberDiffTreeEventHandler.class);
				handler.reset();
				subscriber.getPatcher().setStripPrefixSegments(newValue);
				participant.refresh(configuration.getSite().getWorkbenchSite(),
						context.getScope().getMappings());
			}
		}
	}
}
