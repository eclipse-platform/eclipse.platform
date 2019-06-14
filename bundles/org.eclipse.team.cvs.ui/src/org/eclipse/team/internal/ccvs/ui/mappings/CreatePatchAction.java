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
package org.eclipse.team.internal.ccvs.ui.mappings;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.wizards.GenerateDiffFileWizard;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.PlatformUI;

public class CreatePatchAction extends CVSModelProviderAction implements IDiffChangeListener {

	public CreatePatchAction(ISynchronizePageConfiguration configuration) {
		super(configuration);
		getSynchronizationContext().getDiffTree().addDiffChangeListener(this);
	}

	@Override
	protected boolean isEnabledForSelection(IStructuredSelection selection) {
		return internalIsEnabled(selection);
	}
	
	private boolean internalIsEnabled(IStructuredSelection selection) {
		// Only enable commit in outgoing or both modes
		int mode = getConfiguration().getMode();
		if (mode == ISynchronizePageConfiguration.OUTGOING_MODE || mode == ISynchronizePageConfiguration.BOTH_MODE) {
			return getResourceMappings(selection).length > 0;
		}
		return getSynchronizationContext().getDiffTree().countFor(IThreeWayDiff.CONFLICTING, IThreeWayDiff.DIRECTION_MASK) > 0;
	}
	
	private IResource[] getVisibleResources(ResourceTraversal[] traversals) {
		final Set resources = new HashSet();
		final IResourceDiffTree diffTree = getSynchronizationContext().getDiffTree();
		IDiff[] diffs = diffTree.getDiffs(traversals);
		for (IDiff diff : diffs) {
			IResource child = diffTree.getResource(diff);
			if (child.getType() == IResource.FILE && diff instanceof IThreeWayDiff) {
				IThreeWayDiff twd = (IThreeWayDiff) diff;
				IDiff local = twd.getLocalChange();
				if (local != null && local.getKind() != IDiff.NO_CHANGE) {
					resources.add(child);
				}
			}
		}
		return (IResource[]) resources.toArray(new IResource[resources.size()]);
	}

	@Override
	protected String getBundleKeyPrefix() {
		return "GenerateDiffFileAction."; //$NON-NLS-1$
	}
	
	@Override
	public void execute() {
		final ResourceTraversal [][] traversals = new ResourceTraversal[][] { null };
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> {
				try {
					traversals[0] = getResourceTraversals(getStructuredSelection(), monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			});
		} catch (InvocationTargetException e) {
			Utils.handleError(getConfiguration().getSite().getShell(), e, null, null);
		} catch (InterruptedException e) {
			// Ignore
		}
		if (traversals[0] != null) {
			IResource[] resources = getVisibleResources(traversals[0]);
			if (resources.length == 0) {
				MessageDialog.openInformation(getConfiguration().getSite().getShell(), CVSUIMessages.CreatePatchAction_0, CVSUIMessages.CreatePatchAction_1);
			} else {
				GenerateDiffFileWizard.run(getConfiguration().getSite().getPart(), resources, false);
			}
		}
	}

	@Override
	public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
		updateEnablement();
	}

	@Override
	public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
		// Nothing to do
	}

}
