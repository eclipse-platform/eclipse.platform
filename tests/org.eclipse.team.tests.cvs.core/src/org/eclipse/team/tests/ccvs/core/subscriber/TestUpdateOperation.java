/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.subscriber;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.operations.UpdateOnlyMergableOperation;
import org.eclipse.team.internal.ccvs.ui.subscriber.WorkspaceUpdateOperation;
import org.eclipse.team.internal.ui.mapping.BuildScopeOperation;
import org.eclipse.team.tests.ccvs.core.EclipseTest;
import org.eclipse.team.tests.ccvs.ui.ReflectionUtils;

class TestUpdateOperation extends WorkspaceUpdateOperation {

	protected TestUpdateOperation(IDiffElement[] elements) {
		super(null, elements, false);
	}

	@Override
	protected boolean canRunAsJob() {
		return false;
	}
	
	@Override
	protected void warnAboutFailedResources(SyncInfoSet syncSet) {
		return;
	}

	@Override
	protected boolean promptForOverwrite(SyncInfoSet syncSet) {
		EclipseTest.fail("Should never prompt on update, simply update nodes that are valid.");
		return false;
	}
	
	@Override
	protected void safeUpdate(IProject project, IResource[] resources, LocalOption[] localOptions, IProgressMonitor monitor) throws TeamException {
		try {
			UpdateOnlyMergableOperation operation = new UpdateOnlyMergableOperation(getPart(), project, resources, localOptions) {
				@Override
				public ISynchronizationScope buildScope(IProgressMonitor monitor) throws InterruptedException, CVSException {
					if (getScopeManager() == null) {
						// manager = createScopeManager(consultModelsWhenBuildingScope && consultModelsForMappings());
						ReflectionUtils.setField(this, "manager", createScopeManager(consultModelsWhenBuildingScope && consultModelsForMappings()));
						BuildScopeOperation op = new BuildScopeOperation(getPart(), getScopeManager()) {
							@Override
							protected boolean promptForInputChange(String requestPreviewMessage, IProgressMonitor monitor) {
								return false; // do not prompt
							}
						};
						try {
							op.run(monitor);
						} catch (InvocationTargetException e) {
							throw CVSException.wrapException(e);
						}
					}
					return getScope();
				}
			};
			operation.run(monitor);
			// addSkippedFiles(operation.getSkippedFiles());
			ReflectionUtils.callMethod(this, "addSkippedFiles", new Object[] {operation.getSkippedFiles()});
		} catch (InvocationTargetException e) {
			throw CVSException.wrapException(e);
		} catch (InterruptedException e) {
			Policy.cancelOperation();
		}
	}
}
