/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.synchronize.patch.ApplyPatchOperation;

public class ApplyPatchAction extends TeamAction {

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	protected void execute(IAction action) throws InvocationTargetException,
			InterruptedException {
		IResource[] resources = getSelectedResources();
		IResource resource = null;
		if (resources.length > 0) {
			resource = resources[0];
		}
		boolean isPatch = false;
		if (resource instanceof IFile) {
			try {
				isPatch = ApplyPatchOperation.isPatch((IFile)resource);
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}

		final ApplyPatchOperation op;
		if (isPatch) {
			op= new ApplyPatchOperation(getTargetPart(), (IFile)resource, null, new CompareConfiguration());
		} else {
			op= new ApplyPatchOperation(getTargetPart(), resource);
		}
		BusyIndicator.showWhile(Display.getDefault(), op);
	}

}
