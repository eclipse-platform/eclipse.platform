/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * Action that prompts the user for build files and adds the selected files to an <code>AntView</code>
 */
public class AddBuildFilesAction extends Action {

	private final AntView view;

	public AddBuildFilesAction(AntView view) {
		super(AntViewActionMessages.AddBuildFilesAction_1, AntUIImages.getImageDescriptor(IAntUIConstants.IMG_ADD));
		this.view = view;
		setToolTipText(AntViewActionMessages.AddBuildFilesAction_0);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IAntUIHelpContextIds.ADD_BUILDFILE_ACTION);
	}

	@Override
	public void run() {
		String title = AntViewActionMessages.AddBuildFilesAction_2;
		String message = AntViewActionMessages.AddBuildFilesAction_4;
		String filterExtension = AntUtil.getKnownBuildFileExtensionsAsPattern();
		String filterMessage = AntViewActionMessages.AddBuildFilesAction_5;

		FileSelectionDialog dialog = new FileSelectionDialog(Display.getCurrent().getActiveShell(), getBuildFiles(), title, message, filterExtension, filterMessage);
		dialog.open();
		final Object[] result = dialog.getResult();
		if (result == null) {
			return;
		}

		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> {
				monitor.beginTask(AntViewActionMessages.AddBuildFilesAction_3, result.length);
				for (int i = 0; i < result.length && !monitor.isCanceled(); i++) {
					Object file = result[i];
					if (file instanceof IFile) {
						String buildFileName = ((IFile) file).getFullPath().toString();
						final AntProjectNode project = new AntProjectNodeProxy(buildFileName);
						project.getName();
						monitor.worked(1);
						Display.getDefault().asyncExec(() -> view.addProject(project));
					}
				}
			});
		}
		catch (InvocationTargetException e) {
			// do nothing
		}
		catch (InterruptedException e) {
			// do nothing
		}
	}

	private List<IFile> getBuildFiles() {
		AntProjectNode[] existingProjects = view.getProjects();
		List<IFile> buildFiles = new ArrayList<>(existingProjects.length);
		for (AntProjectNode existingProject : existingProjects) {
			buildFiles.add(AntUtil.getFile(existingProject.getBuildFileName()));
		}
		return buildFiles;
	}
}
