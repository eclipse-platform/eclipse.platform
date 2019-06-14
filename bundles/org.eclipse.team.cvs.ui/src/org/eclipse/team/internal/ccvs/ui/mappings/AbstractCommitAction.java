/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.wizards.CommitWizard;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.PlatformUI;

public abstract class AbstractCommitAction extends CVSModelProviderAction {

	public AbstractCommitAction(ISynchronizePageConfiguration configuration) {
		super(configuration);
	}
	
	@Override
	public void execute() {
		final List resources = new ArrayList();
		try {
			final IStructuredSelection selection = getActualSelection();
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> {
				try {
					ResourceTraversal[] traversals = getCommitTraversals(selection, monitor);
					resources.add(getOutgoingChanges(getSynchronizationContext().getDiffTree(), traversals, monitor));
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			});
		} catch (InvocationTargetException e) {
			Utils.handleError(getConfiguration().getSite().getShell(), e, null, null);
		} catch (InterruptedException e) {
			// Ignore
		} catch (CVSException e) {
			Utils.handleError(getConfiguration().getSite().getShell(), e, null, null);
		}
		if (!resources.isEmpty() && ((IResource[])resources.get(0)).length > 0) {
			Shell shell= getConfiguration().getSite().getShell();
			try {
				CommitWizard.run(getConfiguration().getSite().getPart(), shell, ((IResource[])resources.get(0)));
			} catch (CVSException e) {
				CVSUIPlugin.log(e);
			}
		}
	}
	
	protected IStructuredSelection getActualSelection() throws CVSException {
		return getStructuredSelection();
	}

	protected abstract ResourceTraversal[] getCommitTraversals(IStructuredSelection selection, IProgressMonitor monitor) throws CoreException;
	
	public static IResource[] getOutgoingChanges(final IResourceDiffTree tree, ResourceTraversal[] traversals, IProgressMonitor monitor) {
		final List<IResource> resources = new ArrayList<>();
		IDiff[] diffs = tree.getDiffs(traversals);
		for (IDiff diff : diffs) {
			if (hasLocalChange(diff)) {
				IResource resource = ResourceDiffTree.getResourceFor(diff);
				if (resource != null)
					resources.add(resource);
			}
		}
		return resources.toArray(new IResource[resources.size()]);
	}
	
	public static boolean hasLocalChange(IDiff diff) {
		if (diff instanceof IThreeWayDiff) {
			IThreeWayDiff twd = (IThreeWayDiff) diff;
			return twd.getDirection() == IThreeWayDiff.OUTGOING 
				|| twd.getDirection() ==  IThreeWayDiff.CONFLICTING;
		}
		return false;
	}

}
