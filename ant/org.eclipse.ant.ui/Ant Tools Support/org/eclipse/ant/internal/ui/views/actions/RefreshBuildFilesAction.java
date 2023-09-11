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
import java.text.MessageFormat;
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
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Action which refreshes the selected buildfiles in the Ant view
 */
public class RefreshBuildFilesAction extends Action implements IUpdate {

	private final AntView fView;

	/**
	 * Creates a new <code>RefreshBuildFilesAction</code> which will refresh buildfiles in the given Ant view.
	 * 
	 * @param view
	 *            the Ant view whose selection this action will use when determining which buildfiles to refresh.
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
	@Override
	public void run() {
		final Set<AntProjectNode> projects = getSelectedProjects();
		if (projects.isEmpty()) {
			// If no selection, add all
			for (AntProjectNode antproject : fView.getProjects()) {
				projects.add(antproject);
			}
		}
		final Iterator<AntProjectNode> iter = projects.iterator();
		if (!iter.hasNext()) {
			return;
		}

		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(monitor -> {
				monitor.beginTask(AntViewActionMessages.RefreshBuildFilesAction_Refreshing_buildfiles_3, projects.size());
				AntProjectNodeProxy project;
				while (iter.hasNext()) {
					project = (AntProjectNodeProxy) iter.next();
					monitor.subTask(MessageFormat.format(AntViewActionMessages.RefreshBuildFilesAction_Refreshing__0__4, new Object[] {
							project.getBuildFileName() }));
					project.parseBuildFile(true);
					monitor.worked(1);
				}
			});
		}
		catch (InvocationTargetException e) {
			// do nothing
		}
		catch (InterruptedException e) {
			// do nothing
		}
		fView.getViewer().refresh();
	}

	/**
	 * Returns the selected project nodes to refresh
	 * 
	 * @return Set the selected <code>AntProjectNode</code>s to refresh.
	 */
	private Set<AntProjectNode> getSelectedProjects() {
		IStructuredSelection selection = (IStructuredSelection) fView.getViewer().getSelection();
		HashSet<AntProjectNode> set = new HashSet<>();
		Iterator<?> iter = selection.iterator();
		Object data;
		while (iter.hasNext()) {
			data = iter.next();
			if (data instanceof AntProjectNode) {
				set.add((AntProjectNode) data);
			} else if (data instanceof AntTargetNode) {
				set.add(((AntTargetNode) data).getProjectNode());
			}
		}
		return set;
	}

	/**
	 * Updates the enablement of this action based on the user's selection
	 */
	@Override
	public void update() {
		setEnabled(fView.getProjects().length > 0);
	}

}
