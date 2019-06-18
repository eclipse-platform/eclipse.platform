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

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.ResourceModelParticipantAction;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.ide.IDE;

public abstract class CVSModelProviderAction extends ResourceModelParticipantAction {

	public CVSModelProviderAction(ISynchronizePageConfiguration configuration) {
		super(null, configuration);
		Utils.initAction(this, getBundleKeyPrefix(), Policy.getActionBundle());
	}
	
	/**
	 * Return the key to the action text in the resource bundle.
	 * The default is the fully qualified class name followed by a dot (.).
	 * @return the bundle key prefix
	 */
	protected String getBundleKeyPrefix() {
		return getClass().getName()  + "."; //$NON-NLS-1$
	}

	protected ResourceMapping[] getResourceMappings(IStructuredSelection selection) {
		List<ResourceMapping> mappings = new ArrayList<>();
		for (Iterator iter = selection.iterator(); iter.hasNext();) {
			Object element = iter.next();
			ResourceMapping mapping = Utils.getResourceMapping(element);
			if (mapping != null)
				mappings.add(mapping);
		}
		return mappings.toArray(new ResourceMapping[mappings.size()]);
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
	
	protected IResource[] getTargetResources() {
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
	
	@Override
	public void run() {
		if (saveDirtyEditors())
			execute();
	}

	protected abstract void execute();
}
