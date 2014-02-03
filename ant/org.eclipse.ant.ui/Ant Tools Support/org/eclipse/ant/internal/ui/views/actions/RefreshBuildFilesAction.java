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
import com.ibm.icu.text.MessageFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.model.AntProjectNodeProxy;
import org.eclipse.ant.internal.ui.model.AntTargetNode;
import org.eclipse.ant.internal.ui.views.AntView;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Action which refreshes the selected buildfiles in the Ant view
 */
public class RefreshBuildFilesAction extends Action implements IUpdate {

	private AntView fView;

	/**
	 * Creates a new <code>RefreshBuildFilesAction</code> which will refresh buildfiles 
	 * in the given Ant view.
	 * @param view the Ant view whose selection this action will use when
	 * determining which buildfiles to refresh.
	 */
	public RefreshBuildFilesAction(AntView view) {
		super(AntViewActionMessages.RefreshBuildFilesAction_Refresh_Buildfiles_1, AntUIImages.getImageDescriptor(IAntUIConstants.IMG_REFRESH));
		setToolTipText(AntViewActionMessages.RefreshBuildFilesAction_Refresh_Buildfiles_1);
		fView = view;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IAntUIHelpContextIds.REFRESH_BUILDFILE_ACTION);
	}

	/**
	 * Refreshes the selected buildfiles (or all buildfiles if none selected) in the Ant view
	 */
	public void run() {
		final Set projects= getSelectedProjects();
		if (projects.isEmpty()) {
			// If no selection, add all
			AntProjectNode[] allProjects= fView.getProjects();
			for (int i = 0; i < allProjects.length; i++) {
				projects.add(allProjects[i]);
			}
		}
		final Iterator iter= projects.iterator();
		if (!iter.hasNext()) {
			return;
		}
		
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					monitor.beginTask(AntViewActionMessages.RefreshBuildFilesAction_Refreshing_buildfiles_3, projects.size());
					AntProjectNodeProxy project;
					while (iter.hasNext()) {
						project= (AntProjectNodeProxy) iter.next();
						monitor.subTask(MessageFormat.format(AntViewActionMessages.RefreshBuildFilesAction_Refreshing__0__4, new String[] {project.getBuildFileName()}));
						project.parseBuildFile(true);
						monitor.worked(1);
					}
				}
			});
		} catch (InvocationTargetException e) {
		} catch (InterruptedException e) {
		}
		fView.getViewer().refresh();
	}

	/**
	 * Returns the selected project nodes to refresh
	 * 
	 * @return Set the selected <code>ProjectNode</code>s to refresh.
	 */
	private Set getSelectedProjects() {
		IStructuredSelection selection = (IStructuredSelection) fView.getViewer().getSelection();
		HashSet set= new HashSet();
		Iterator iter = selection.iterator();
		Object data;
		while (iter.hasNext()) {
			data= iter.next();
			if (data instanceof AntProjectNode) {
				set.add(data);
			} else if (data instanceof AntTargetNode) {
				set.add(((AntTargetNode) data).getProjectNode());
			}
		}
		return set;
	}

	/**
	 * Updates the enablement of this action based on the user's selection
	 */
	public void update() {
		setEnabled(fView.getProjects().length > 0);
	}

}
