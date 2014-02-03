/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
import com.ibm.icu.text.MessageFormat;
import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.model.AntProjectNodeProxy;
import org.eclipse.ant.internal.ui.views.AntView;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * This action opens a dialog to search for build files and adds the resulting
 * projects to the ant view.
 */
public class SearchForBuildFilesAction extends Action {
	private AntView view;
	
	public SearchForBuildFilesAction(AntView view) {
		super(AntViewActionMessages.SearchForBuildFilesAction_Search_1, AntUIImages.getImageDescriptor(IAntUIConstants.IMG_SEARCH));
		setToolTipText(AntViewActionMessages.SearchForBuildFilesAction_Add_build_files_with_search_2);
		this.view= view;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IAntUIHelpContextIds.SEARCH_FOR_BUILDFILES_ACTION);
	}
	
	/**
	 * Opens the <code>SearchForBuildFilesDialog</code> and adds the results to
	 * the ant view.
	 */
	public void run() {
		SearchForBuildFilesDialog dialog= new SearchForBuildFilesDialog();
		if (dialog.open() != Window.CANCEL) {
			final IFile[] files= dialog.getResults();
			final boolean includeErrorNodes= dialog.getIncludeErrorResults();
			final AntProjectNode[] existingProjects= view.getProjects();
			try {
				PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						monitor.beginTask(AntViewActionMessages.SearchForBuildFilesAction_Processing_search_results_3, files.length);
						for (int i = 0; i < files.length && !monitor.isCanceled(); i++) {
							String buildFileName= files[i].getFullPath().toString();
							monitor.subTask(MessageFormat.format(AntViewActionMessages.SearchForBuildFilesAction_Adding__0__4, new String[] {buildFileName}));
							if (alreadyAdded(buildFileName)) {
								// Don't parse projects that have already been added.
								continue;
							}
							final AntProjectNodeProxy project= new AntProjectNodeProxy(buildFileName);
							// Force the project to be parsed so the error state is set.
							project.parseBuildFile();
							monitor.worked(1);
							if (includeErrorNodes || !(project.isErrorNode())) {
								Display.getDefault().asyncExec(new Runnable() {
									public void run() {
										view.addProject(project);
									}
								});
							}
						}
					}
					/**
					 * Returns whether or not the given build file already
					 * exists in the ant view.
					 */
					private boolean alreadyAdded(String buildFileName) {
						for (int j = 0; j < existingProjects.length; j++) {
							AntProjectNode existingProject = existingProjects[j];
							if (existingProject.getBuildFileName().equals(buildFileName)) {
								return true;
							}
						}
						return false;
					}
				});
			} catch (InvocationTargetException e) {
			} catch (InterruptedException e) {
			}
		}
	}
}
