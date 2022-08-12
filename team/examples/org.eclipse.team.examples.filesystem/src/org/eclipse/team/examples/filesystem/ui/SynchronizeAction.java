/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem.ui;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemMergeContext;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemSubscriber;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;

/**
 * Action to synchronize the selected resources. This results
 * in a file-system participant being added to the synchronize view.
 */
public class SynchronizeAction extends FileSystemAction {

	@Override
	protected void execute(IAction action) {
		ResourceMapping[] mappings = getSelectedMappings();
		if (mappings.length == 0)
			return;
		SubscriberScopeManager manager = FileSystemOperation.createScopeManager(FileSystemSubscriber.getInstance().getName(), mappings);
		FileSystemMergeContext context = new FileSystemMergeContext(manager);
		FileSystemSynchronizeParticipant participant = new FileSystemSynchronizeParticipant(context);
		TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
		participant.run(getTargetPart());
	}

}
