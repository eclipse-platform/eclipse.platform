/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.views.launch.LaunchView;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.handlers.CollapseAllHandler;

/**
 * CollapseAllAction for Debug Launch view
 */
public class LaunchCollapseAllAction extends AbstractRemoveAllActionDelegate implements ILaunchesListener2 {

	private IViewPart fView;
	private TreeModelViewer fViewer;

	@Override
	public void init(IViewPart view) {
		fView = view;
		LaunchView debugView = getView().getAdapter(LaunchView.class);
		if (debugView != null) {
			debugView.setAction(getActionId(), getAction());
		}
		Viewer viewer = ((LaunchView) getView()).getViewer();
		if (viewer instanceof TreeModelViewer) {
			fViewer = (TreeModelViewer) viewer;
		}
		super.init(view);
	}

	private IViewPart getView() {
		return fView;
	}

	private String getActionId() {
		return CollapseAllHandler.COMMAND_ID;
	}


	@Override
	public void run(IAction action) {
		if (!(getView() instanceof LaunchView)) {
			return;
		}
		if (fViewer != null) {
			try {
				fViewer.getControl().setRedraw(false);
				fViewer.collapseAll();
			} finally {
				fViewer.getControl().setRedraw(true);
			}
		}
	}

	@Override
	protected void initialize() {
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
	}

	@Override
	public void dispose() {
		super.dispose();
		DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
	}

	@Override
	protected boolean isEnabled() {
		if (!(getView() instanceof LaunchView)) {
			return false;
		}
		if (fViewer != null && fViewer.getInput() != null) {
			ILaunch[] launches = DebugPlugin.getDefault().getLaunchManager().getLaunches();
			if (launches != null && launches.length > 0) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void launchesRemoved(ILaunch[] launches) {
		update();
	}

	@Override
	public void launchesAdded(ILaunch[] launches) {
		update();
	}

	@Override
	public void launchesChanged(ILaunch[] launches) {
		// nothing to do
	}

	@Override
	public void launchesTerminated(ILaunch[] launches) {
		// nothing to do
	}
}
