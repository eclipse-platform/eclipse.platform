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

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemMergeContext;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemSubscriber;
import org.eclipse.team.ui.synchronize.ISynchronizeParticipant;
import org.eclipse.team.ui.synchronize.ModelParticipantWizard;

/**
 * This class is registered as the file system synchronization wizard.
 */
public class SynchronizeWizard extends ModelParticipantWizard {
	private IWizard importWizard;
	
	/*
	 * Default no-arg constructor
	 */
	public SynchronizeWizard() {
		super();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ModelParticipantWizard#createParticipant(org.eclipse.core.resources.mapping.ResourceMapping[])
	 */
	protected ISynchronizeParticipant createParticipant(ResourceMapping[] selectedMappings) {
		SubscriberScopeManager manager = FileSystemOperation.createScopeManager(FileSystemSubscriber.getInstance().getName(), selectedMappings);
		FileSystemMergeContext context = new FileSystemMergeContext(manager);
		FileSystemSynchronizeParticipant participant = new FileSystemSynchronizeParticipant(context);
		return participant;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ParticipantSynchronizeWizard#getImportWizard()
	 */
	protected IWizard getImportWizard() {
		// We don't have an import wizard for the file system example but
		// if we did, we could return it here and it would be used if the
		// getRoots method returned an empty array.
		return importWizard;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ParticipantSynchronizeWizard#getPageTitle()
	 */
	protected String getPageTitle() {
		return "Synchronize File System Example";
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.ParticipantSynchronizeWizard#getRootResources()
	 */
	protected IResource[] getRootResources() {
		return FileSystemSubscriber.getInstance().roots();
	}
}
