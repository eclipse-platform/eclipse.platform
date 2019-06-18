/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.mapping;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.team.core.diff.IDiffChangeEvent;
import org.eclipse.team.core.diff.IDiffChangeListener;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.mapping.MergeActionHandler;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.ide.IDE;

public abstract class ResourceMergeActionHandler extends MergeActionHandler implements IDiffChangeListener, IPropertyChangeListener {

	public ResourceMergeActionHandler(ISynchronizePageConfiguration configuration) {
		super(configuration);
		getSynchronizationContext().getDiffTree().addDiffChangeListener(this);
		configuration.addPropertyChangeListener(this);
	}

	/**
	 * Prompt to save all dirty editors and return whether to proceed
	 * or not.
	 * @return whether to proceed
	 * or not
	 */
	public final boolean saveDirtyEditors() {
		if(needsToSaveDirtyEditors()) {
			if(!saveAllEditors(getTargetResources(), confirmSaveOfDirtyEditor())) {
				return false;
			}
		}
		return true;
	}

	private IResource[] getTargetResources() {
		IStructuredSelection selection = getStructuredSelection();
		Object[] objects = selection.toArray();
		Set<IResource> roots = new HashSet<>();
		for (Object object : objects) {
			ResourceMapping mapping = Utils.getResourceMapping(object);
			if (mapping != null) {
				try {
					ResourceTraversal[] traversals = mapping.getTraversals(ResourceMappingContext.LOCAL_CONTEXT, null);
					for (ResourceTraversal traversal : traversals) {
						IResource[] resources = traversal.getResources();
						Collections.addAll(roots, resources);
					}
				} catch (CoreException e) {
					TeamUIPlugin.log(e);
				}
			}
		}
		return roots.toArray(new IResource[roots.size()]);
	}

	/**
	 * Save all dirty editors in the workbench that are open on files that may
	 * be affected by this operation. Opens a dialog to prompt the user if
	 * <code>confirm</code> is true. Return true if successful. Return false
	 * if the user has canceled the command. Must be called from the UI thread.
	 * @param resources the root resources being operated on
	 * @param confirm prompt the user if true
	 * @return boolean false if the operation was canceled.
	 */
	public final boolean saveAllEditors(IResource[] resources, boolean confirm) {
		return IDE.saveAllEditors(resources, confirm);
	}

	/**
	 * Return whether dirty editor should be saved before this action is run.
	 * Default is <code>true</code>.
	 *
	 * @return whether dirty editor should be saved before this action is run
	 */
	protected boolean needsToSaveDirtyEditors() {
		return true;
	}

	/**
	 * Returns whether the user should be prompted to save dirty editors. The
	 * default is <code>true</code>.
	 *
	 * @return whether the user should be prompted to save dirty editors
	 */
	protected boolean confirmSaveOfDirtyEditor() {
		return true;
	}

	protected ISynchronizationContext getSynchronizationContext() {
		return (ISynchronizationContext)getConfiguration().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT);
	}

	@Override
	public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
		// Nothing to do
	}

	@Override
	public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
		Utils.syncExec((Runnable) () -> updateEnablement(getStructuredSelection()), (StructuredViewer)getConfiguration().getPage().getViewer());

	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty() == ISynchronizePageConfiguration.P_MODE) {
			Utils.syncExec((Runnable) () -> updateEnablement(getStructuredSelection()), (StructuredViewer)getConfiguration().getPage().getViewer());
		}
	}

}
