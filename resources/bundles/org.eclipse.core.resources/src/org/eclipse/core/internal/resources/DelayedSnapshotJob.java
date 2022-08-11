/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Christoph Läubrich - Issue #77 - SaveManager access the ResourcesPlugin.getWorkspace at init phase
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.internal.utils.Policy;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Performs periodic saving (snapshot) of the workspace.
 */
public class DelayedSnapshotJob extends Job {

	private static final String MSG_SNAPSHOT = Messages.resources_snapshot;
	private SaveManager saveManager;
	private Workspace workspace;

	public DelayedSnapshotJob(SaveManager manager, Workspace workspace) {
		super(MSG_SNAPSHOT);
		this.saveManager = manager;
		this.workspace = workspace;
		setRule(workspace.getRoot());
		setSystem(true);
	}

	/*
	 * @see Job#run()
	 */
	@Override
	public IStatus run(IProgressMonitor monitor) {
		if (monitor.isCanceled())
			return Status.CANCEL_STATUS;
		if (!workspace.isOpen()) {
			return Status.OK_STATUS;
		}
		try {
			return saveManager.save(ISaveContext.SNAPSHOT, null, Policy.monitorFor(null));
		} catch (CoreException e) {
			return e.getStatus();
		} finally {
			saveManager.operationCount = 0;
			saveManager.snapshotRequested = false;
		}
	}
}
