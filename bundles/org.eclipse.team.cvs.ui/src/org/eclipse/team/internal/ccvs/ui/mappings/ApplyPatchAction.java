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
package org.eclipse.team.internal.ccvs.ui.mappings;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.synchronize.patch.ApplyPatchOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class ApplyPatchAction extends CVSModelProviderAction implements
		IDiffChangeListener {

	public ApplyPatchAction(ISynchronizePageConfiguration configuration) {
		super(configuration);
		getSynchronizationContext().getDiffTree().addDiffChangeListener(this);
	}

	protected void execute() {
		IResource resource = (IResource) Platform.getAdapterManager()
				.getAdapter(getStructuredSelection().getFirstElement(),
						IResource.class);

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

	protected boolean isEnabledForSelection(IStructuredSelection selection) {
		return internalIsEnabled(selection);
	}

	private boolean internalIsEnabled(IStructuredSelection selection) {
		Object firstElement = selection.getFirstElement();
		if (firstElement == null)
			return false;
		Object adapter = Platform.getAdapterManager().getAdapter(firstElement,
				IResource.class);
		return adapter != null;
	}

	public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
		updateEnablement();
	}

	public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
		// do nothing
	}

	protected String getBundleKeyPrefix() {
		return "ApplyPatchAction."; //$NON-NLS-1$
	}

}
