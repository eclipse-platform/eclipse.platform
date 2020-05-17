/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.team.examples.localhistory;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;
import org.eclipse.ui.actions.WorkspaceModifyOperation;

public class RevertAllOperation extends SynchronizeModelOperation {

	protected RevertAllOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		super(configuration, elements);
	}

	@Override
	protected boolean canRunAsJob() {
		return true;
	}

	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		final SyncInfo infos[] = getSyncInfoSet().getSyncInfos();
		if(infos.length == 0) return;

		WorkspaceModifyOperation operation= new WorkspaceModifyOperation() {
			@Override
			public void execute(IProgressMonitor pm) throws InvocationTargetException {
				try {
					pm.beginTask("Reverting from local history", 100 * infos.length);	 //$NON-NLS-1$
					for (SyncInfo info : infos) {
						LocalHistoryVariant state = (LocalHistoryVariant)info.getRemote();
						IFile file = (IFile)info.getLocal();
						if(file.exists()) {
							file.setContents(state.getFileState(), false, true, SubMonitor.convert(pm, 100));
						} else {
							// TODO: have to pre-create parents if they dont exist
							file.create(state.getFileState().getContents(), false, SubMonitor.convert(pm, 100));
						}
					}
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					pm.done();
				}
			}
		};
		operation.run(monitor);
	}
}
