/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.views;

import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.model.AntProjectNodeProxy;
import org.eclipse.core.resources.IFile;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;

/**
 * A drop adapter which adds files to the Ant view.
 */
public class AntViewDropAdapter extends DropTargetAdapter {

	private final AntView view;

	/**
	 * Creates a new drop adapter for the given Ant view.
	 * 
	 * @param view
	 *            the view which dropped files will be added to
	 */
	public AntViewDropAdapter(AntView view) {
		this.view = view;
	}

	@Override
	public void drop(DropTargetEvent event) {
		Object data = event.data;
		if (data instanceof String[]) {
			BusyIndicator.showWhile(null, () -> {
				for (String string : (String[]) data) {
					processString(string);
				}
			});
		}
	}

	/**
	 * Attempts to process the given string as a path to an XML file. If the string is determined to be a path to an XML file in the workspace, that
	 * file is added to the Ant view.
	 *
	 * @param buildFileName
	 *            the string to process
	 */
	private void processString(String buildFileName) {
		IFile buildFile = AntUtil.getFileForLocation(buildFileName, null);
		if (!AntUtil.isKnownAntFile(buildFile)) {
			return;
		}
		String name = buildFile.getFullPath().toString();
		for (AntProjectNode node : view.getProjects()) {
			AntProjectNodeProxy existingProject = (AntProjectNodeProxy) node;
			if (existingProject.getBuildFileName().equals(name)) {
				// Don't parse projects that have already been added.
				return;
			}
		}
		AntProjectNode project = new AntProjectNodeProxy(name);
		view.addProject(project);
	}

	@Override
	public void dragEnter(DropTargetEvent event) {
		event.detail = DND.DROP_COPY;
		super.dragEnter(event);
	}

	@Override
	public void dragOperationChanged(DropTargetEvent event) {
		event.detail = DND.DROP_COPY;
		super.dragOperationChanged(event);
	}
}