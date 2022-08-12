/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.team.internal.ui.ProjectSetImporter;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.ui.TeamOperation;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

public class ImportProjectSetOperation extends TeamOperation {

	private String psfFileContents;
	private String urlString;
	private String psfFile;
	private IWorkingSet[] workingSets;


	/**
	 * Operation for importing a Team Project Set stored in a String
	 *
	 * @param context
	 *            a runnable context,
	 *            <code>null</code> for running in background
	 * @param psfFileContents
	 *            the psf file content to load
	 * @param urlString
	 *            url or path to file loaded into <code>psfFileContents</code>
	 * @param workingSets
	 *            an array of working sets where imported project should be
	 *            added. If a working set doesn't exist it will be created. The
	 *            array cannot be <code>null</code>, pass an empty array if you
	 *            don't want to add projects to any working set.
	 */
	public ImportProjectSetOperation(IRunnableContext context,
			String psfFileContents, String urlString, IWorkingSet[] workingSets) {
		super(context);
		this.psfFileContents = psfFileContents;
		this.workingSets = workingSets;
		this.urlString = urlString;
	}

	/**
	 * Operation for importing a Team Project Set file
	 *
	 * @param context
	 *            a runnable context
	 * @param psfFile
	 *            a psf file name
	 * @param workingSets
	 *            an array of working sets where imported project should be
	 *            added. If a working set doesn't exist it will be created. The
	 *            array cannot be <code>null</code>, pass an empty array if you
	 *            don't want to add projects to any working set.
	 */
	public ImportProjectSetOperation(IRunnableContext context, String psfFile,
			IWorkingSet[] workingSets) {
		super(context);
		this.psfFile = psfFile;
		this.workingSets = workingSets;
	}

	private void runForStringContent(IProgressMonitor monitor) throws InvocationTargetException{
		IProject[] newProjects = ProjectSetImporter.importProjectSetFromString(
				psfFileContents, urlString, getShell(), monitor);
		createWorkingSet(workingSets, newProjects);
	}

	private void runForFile(IProgressMonitor monitor) throws InvocationTargetException{
		PsfFilenameStore.getInstance().remember(psfFile);
		IProject[] newProjects = ProjectSetImporter.importProjectSet(psfFile,
				getShell(), monitor);
		createWorkingSet(workingSets, newProjects);
	}

	@Override
	public void run(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException{
		if (psfFileContents != null) {
			runForStringContent(monitor);
		} else {
			runForFile(monitor);
		}
	}

	@Override
	protected boolean canRunAsJob() {
		return true;
	}

	@Override
	protected String getJobName() {
		return TeamUIMessages.ImportProjectSetMainPage_jobName;
	}

	private void createWorkingSet(IWorkingSet[] workingSets, IProject[] projects) {
		IWorkingSetManager manager = PlatformUI.getWorkbench().getWorkingSetManager();
		String workingSetName;
		for (IWorkingSet workingSet : workingSets) {
			workingSetName = workingSet.getName();
			IWorkingSet oldSet = manager.getWorkingSet(workingSetName);
			if (oldSet == null) {
				IWorkingSet newSet = manager.createWorkingSet(workingSetName, projects);
				manager.addWorkingSet(newSet);
			} else {
				//don't overwrite the old elements
				IAdaptable[] tempElements = oldSet.getElements();
				IAdaptable[] adaptedProjects = oldSet.adaptElements(projects);
				IAdaptable[] finalElementList = new IAdaptable[tempElements.length + adaptedProjects.length];
				System.arraycopy(tempElements, 0, finalElementList, 0, tempElements.length);
				System.arraycopy(adaptedProjects, 0,finalElementList, tempElements.length, adaptedProjects.length);
				oldSet.setElements(finalElementList);
			}
		}
	}
}
