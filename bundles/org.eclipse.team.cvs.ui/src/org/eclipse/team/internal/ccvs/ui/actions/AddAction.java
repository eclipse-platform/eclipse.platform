/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.internal.ccvs.ui.operations.AddOperation;
import org.eclipse.team.internal.ccvs.ui.wizards.AddWizard;

/**
 * AddAction performs a 'cvs add' command on the selected resources. If a
 * container is selected, its children are recursively added.
 */
public class AddAction extends WorkspaceTraversalAction {
    
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#execute(org.eclipse.jface.action.IAction)
	 */
	public void execute(IAction action) throws InterruptedException, InvocationTargetException {
        if (!promptForAddOfIgnored()) return;
        if (!promptForAdd()) return;
        AddOperation op = new AddOperation(getTargetPart(), getCVSResourceMappings());
        AddWizard.run(getShell(), op);
	}

	/*
	 * Prompt the user to avoid accidental adding a resource to version control
	 */
    private boolean promptForAdd() {
		return MessageDialog.openQuestion(getShell(), 
				CVSUIMessages.AddAction_confirmAddingResourcesTitle,
				CVSUIMessages.AddAction_confirmAddingResourcesMessage);
	}

	/*
	 * Prompt whether explicitly selected ignored resources should be added
	 */
	private boolean promptForAddOfIgnored() {
	    // Prompt if any of the traversal roots are ignored
	    // TODO: What about non-root resources that are part of the model but would be ignored?
		IResource[] resources = getSelectedResourcesWithOverlap();
		boolean prompt = false;
		for (int i = 0; i < resources.length; i++) {
			ICVSResource resource = getCVSResourceFor(resources[i]);
			try {
				if (resource.isIgnored()) {
					prompt = true;
					break;
				} 
			} catch (CVSException e) {
				handle(e);
			}
		}
		if (prompt) {
			return MessageDialog.openQuestion(getShell(), CVSUIMessages.AddAction_addIgnoredTitle, CVSUIMessages.AddAction_addIgnoredQuestion); // 
		}
		return true;
	}
	
    /**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForManagedResources()
	 */
	protected boolean isEnabledForManagedResources() {
		return false;
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForUnmanagedResources()
	 */
	protected boolean isEnabledForUnmanagedResources() {
		return true;
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForIgnoredResources()
	 */
	protected boolean isEnabledForIgnoredResources() {
		return true;
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForCVSResource(org.eclipse.team.internal.ccvs.core.ICVSResource)
	 */
	protected boolean isEnabledForCVSResource(ICVSResource cvsResource) throws CVSException {
		// Add to version control should never be enabled for linked resources
		IResource resource = cvsResource.getIResource();
		if (resource.isLinked()) return false;
		return super.isEnabledForCVSResource(cvsResource);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getId()
	 */
	public String getId() {
		return ICVSUIConstants.CMD_ADD;
	}
}
