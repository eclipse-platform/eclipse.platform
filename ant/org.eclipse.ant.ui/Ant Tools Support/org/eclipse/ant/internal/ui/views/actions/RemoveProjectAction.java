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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.IAntUIHelpContextIds;
import org.eclipse.ant.internal.ui.model.AntProjectNode;
import org.eclipse.ant.internal.ui.views.AntView;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Action that removes selected build files from an <code>AntView</code>
 */
public class RemoveProjectAction extends Action implements IUpdate {

	private final AntView view;

	public RemoveProjectAction(AntView view) {
		super(AntViewActionMessages.RemoveProjectAction_Remove, AntUIImages.getImageDescriptor(IAntUIConstants.IMG_REMOVE));
		this.view = view;
		setToolTipText(AntViewActionMessages.RemoveProjectAction_Remove_2);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IAntUIHelpContextIds.REMOVE_PROJECT_ACTION);
	}

	@Override
	public void run() {
		IStructuredSelection selection = (IStructuredSelection) view.getViewer().getSelection();
		Iterator<?> iter = selection.iterator();
		Object element;
		List<AntProjectNode> projectNodes = new ArrayList<>();
		while (iter.hasNext()) {
			element = iter.next();
			if (element instanceof AntProjectNode) {
				projectNodes.add((AntProjectNode) element);
			}
		}
		view.removeProjects(projectNodes);
	}

	@Override
	public void update() {
		IStructuredSelection selection = (IStructuredSelection) view.getViewer().getSelection();
		if (selection.isEmpty()) {
			setEnabled(false);
			return;
		}
		Object element;
		Iterator<?> iter = selection.iterator();
		while (iter.hasNext()) {
			element = iter.next();
			if (!(element instanceof AntProjectNode)) {
				setEnabled(false);
				return;
			}
		}
		setEnabled(true);
	}

}
