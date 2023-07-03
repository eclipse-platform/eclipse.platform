/*******************************************************************************
 * Copyright (c) 2004, 2011 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.memory;

import org.eclipse.debug.ui.memory.IMemoryRenderingContainer;
import org.eclipse.debug.ui.memory.IMemoryRenderingSite;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public class AddMemoryRenderingContextAction implements IViewActionDelegate {

	private IMemoryRenderingSite fMemoryView;

	@Override
	public void init(IViewPart view) {

		if (view instanceof IMemoryRenderingSite) {
			fMemoryView = (IMemoryRenderingSite) view;
		}
	}

	@Override
	public void run(IAction action) {

		if (fMemoryView == null) {
			return;
		}

		IMemoryRenderingContainer container = getRenderingContainer(action);
		if (container != null) {
			AddMemoryRenderingAction addAction = new AddMemoryRenderingAction(container);
			addAction.run();
			addAction.dispose();
		}
	}

	/**
	 * @param action the action to find the view pane for
	 * @return the first container that has the given action in it
	 */
	private IMemoryRenderingContainer getRenderingContainer(IAction action) {
		IMemoryRenderingContainer[] viewPanes = fMemoryView.getMemoryRenderingContainers();
		String actionId = action.getId();
		IMemoryRenderingContainer selectedPane = null;

		for (IMemoryRenderingContainer viewPane : viewPanes) {
			if (actionId.contains(viewPane.getId())) {
				selectedPane = viewPane;
				break;
			}
		}

		return selectedPane;
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		IMemoryRenderingContainer container = getRenderingContainer(action);
		if (container instanceof RenderingViewPane) {
			if (!((RenderingViewPane) container).canAddRendering()) {
				action.setEnabled(false);
			} else {
				action.setEnabled(true);
			}
		}
	}

}
