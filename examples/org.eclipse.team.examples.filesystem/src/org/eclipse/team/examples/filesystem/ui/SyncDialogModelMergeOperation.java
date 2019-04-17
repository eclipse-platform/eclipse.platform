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
package org.eclipse.team.examples.filesystem.ui;

import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.examples.filesystem.subscriber.FileSystemMergeContext;
import org.eclipse.team.ui.synchronize.ModelParticipantMergeOperation;
import org.eclipse.ui.IWorkbenchPart;

/**
 * This merge operation will attempt a headless merge and then prompt
 * the user with a dialog if conflicts exist.
 */
public class SyncDialogModelMergeOperation extends
		ModelParticipantMergeOperation {

	public SyncDialogModelMergeOperation(IWorkbenchPart part, ISynchronizationScopeManager manager) {
		super(part, manager);
	}

	@Override
	protected SynchronizationContext createMergeContext() {
		return new FileSystemMergeContext(getScopeManager());
	}

}
