/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.views.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.model.AntProjectNodeProxy;
import org.eclipse.ant.internal.ui.preferences.FileSelectionDialog;
import org.eclipse.ant.internal.ui.views.AntView;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * Action that prompts the user for build files and adds the selected files to
 * an <code>AntView</code>
 */
public class AddBuildFilesAction extends Action {

	private AntView view;

	public AddBuildFilesAction(AntView view) {
		super(AntViewActionMessages.AddBuildFilesAction_1, AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ADD));
		this.view= view;
		setToolTipText(AntViewActionMessages.AddBuildFilesAction_0);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IAntUIHelpContextIds.ADD_BUILDFILE_ACTION);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	public void run() {
		String title= AntViewActionMessages.AddBuildFilesAction_2;
		String message= AntViewActionMessages.AddBuildFilesAction_4;
		String filterExtension= AntUtil.getKnownBuildFileExtensionsAsPattern();
		String filterMessage= AntViewActionMessages.AddBuildFilesAction_5;
		
		FileSelectionDialog dialog = new FileSelectionDialog(Display.getCurrent().getActiveShell(), getBuildFiles(), title, message, filterExtension, filterMessage);
		dialog.open();
		final Object[] result= dialog.getResult();
		if (result == null) {
			return;
		}

		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					monitor.beginTask(AntViewActionMessages.AddBuildFilesAction_3, result.length);
					for (int i = 0; i < result.length && !monitor.isCanceled(); i++) {
						Object file = result[i];
						if (file instanceof IFile) {
							String buildFileName= ((IFile)file).getFullPath().toString();
							final AntProjectNode project= new AntProjectNodeProxy(buildFileName);
							project.getName();
							monitor.worked(1);
							Display.getDefault().asyncExec(new Runnable() {
								public void run() {
									view.addProject(project);
								}
							});
						}
					}
				}
			});
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}
	}

	private List getBuildFiles() {
		AntProjectNode[] existingProjects= view.getProjects();
		List buildFiles= new ArrayList(existingProjects.length);
		for (int j = 0; j < existingProjects.length; j++) {
			AntProjectNode existingProject = existingProjects[j];
			buildFiles.add(AntUtil.getFile(existingProject.getBuildFileName()));
		}
		return buildFiles;
	}
}
