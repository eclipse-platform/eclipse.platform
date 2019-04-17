/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.operations;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkingSet;

/**
 * This operation checks out a single remote folder into the workspace as
 * a project.
 */
public class CheckoutSingleProjectOperation extends CheckoutProjectOperation {

	private boolean preconfigured;
	private IProject targetProject;
	private IWorkingSet[] workingSets;
	
	public CheckoutSingleProjectOperation(IWorkbenchPart part, ICVSRemoteFolder remoteFolder, IProject targetProject, String targetLocation, boolean preconfigured) {
		this(part,remoteFolder,targetProject,targetLocation,preconfigured,null);
	}
	
	public CheckoutSingleProjectOperation(IWorkbenchPart part, ICVSRemoteFolder remoteFolder, IProject targetProject, String targetLocation, boolean preconfigured, IWorkingSet[] workingSets) {
		super(part, new ICVSRemoteFolder[] { remoteFolder }, targetLocation);
		this.targetProject = targetProject;
		this.preconfigured = preconfigured;
		this.workingSets = workingSets;
	}

	private boolean isPreconfigured() {
		return preconfigured;
	}

	@Override
	public boolean needsPromptForOverwrite(IProject project) {
		// No need to prompt if the project was preconfigured
		if (isPreconfigured()) return false;
		return super.needsPromptForOverwrite(project);
	}

	@Override
	protected boolean performScrubProjects() {
		// Do not scrub the projects if they were preconfigured.
		return !isPreconfigured();
	}

	@Override
	protected IStatus checkout(ICVSRemoteFolder folder, IProgressMonitor monitor) throws CVSException {
		return checkout(folder, targetProject, monitor);
	}
	
	@Override
	protected IWorkingSet[] getWorkingSets(){
		return workingSets;
	}

}
