/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers.update;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener2;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;
import org.eclipse.jface.viewers.Viewer;

/**
 * Model proxy for launch manager.
 */
public class LaunchManagerProxy extends AbstractModelProxy implements ILaunchesListener2 {

	private ILaunchManager fLaunchManager;

	@Override
	public synchronized void init(IPresentationContext context) {
		super.init(context);
		fLaunchManager = DebugPlugin.getDefault().getLaunchManager();
		fLaunchManager.addLaunchListener(this);
	}

	@Override
	public void installed(Viewer viewer) {
		// expand existing launches
		ILaunch[] launches = fLaunchManager.getLaunches();
		if (launches.length > 0) {
			launchesAdded(launches);
		}
	}

	@Override
	public synchronized void dispose() {
		super.dispose();
		if (fLaunchManager != null) {
			fLaunchManager.removeLaunchListener(this);
			fLaunchManager = null;
		}
	}

	@Override
	public void launchesTerminated(ILaunch[] launches) {
	}

	@Override
	public void launchesRemoved(ILaunch[] launches) {
		fireDelta(launches, IModelDelta.REMOVED);
	}

	@Override
	public void launchesAdded(ILaunch[] launches) {
		fireDelta(launches, IModelDelta.ADDED | IModelDelta.INSTALL);
	}

	@Override
	public void launchesChanged(ILaunch[] launches) {
	}

	/**
	 * Convenience method for firing a delta
	 * @param launches the launches to set in the delta
	 * @param launchFlags the flags for the delta
	 */
	protected void fireDelta(ILaunch[] launches, int launchFlags) {
		ModelDelta delta = new ModelDelta(fLaunchManager, IModelDelta.NO_CHANGE);
		for (ILaunch launch : launches) {
			delta.addNode(launch, launchFlags);
		}
		fireModelChanged(delta);
	}

}
