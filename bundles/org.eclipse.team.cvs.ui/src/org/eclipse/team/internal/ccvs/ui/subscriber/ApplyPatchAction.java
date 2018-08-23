/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.subscriber;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.synchronize.SynchronizeModelElement;
import org.eclipse.team.internal.ui.synchronize.patch.ApplyPatchOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizeModelOperation;

public class ApplyPatchAction extends CVSParticipantAction {

	public ApplyPatchAction(ISynchronizePageConfiguration configuration) {
		super(configuration);
	}

	protected SynchronizeModelOperation getSubscriberOperation(
			ISynchronizePageConfiguration configuration, IDiffElement[] elements) {
		return null;
	}

	public void runOperation() {

		IResource resource = ((SynchronizeModelElement) getStructuredSelection()
				.getFirstElement()).getResource();

		boolean isPatch = false;
		if (resource instanceof IFile) {
			try {
				isPatch = ApplyPatchOperation.isPatch((IFile) resource);
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}

		final ApplyPatchOperation op;
		if (isPatch) {
			op = new ApplyPatchOperation(
					getConfiguration().getSite().getPart(), (IFile) resource,
					null, new CompareConfiguration());
		} else {
			op = new ApplyPatchOperation(
					getConfiguration().getSite().getPart(), resource);
		}
		BusyIndicator.showWhile(Display.getDefault(), op);
	}

}
