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

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.jface.viewers.Viewer;

public class ProcessProxy extends EventHandlerModelProxy {

	private IProcess fProcess;

	private DebugEventHandler fProcessEventHandler = new DebugEventHandler(this) {
		@Override
		protected boolean handlesEvent(DebugEvent event) {
			return event.getSource().equals(fProcess);
		}

		@Override
		protected void handleChange(DebugEvent event) {
			fireProcessDelta(IModelDelta.STATE);
		}

		@Override
		protected void handleCreate(DebugEvent event) {
			// do nothing - Launch change notification handles this
		}

		@Override
		protected void handleTerminate(DebugEvent event) {
			fireProcessDelta(IModelDelta.STATE | IModelDelta.UNINSTALL);
		}


	};

	private void fireProcessDelta(int flags) {
		ModelDelta delta = null;
		synchronized (ProcessProxy.this) {
			if (!isDisposed()) {
				delta = new ModelDelta(DebugPlugin.getDefault().getLaunchManager(), IModelDelta.NO_CHANGE);
				ModelDelta node = delta;
				node = node.addNode(fProcess.getLaunch(), IModelDelta.NO_CHANGE);
				node.addNode(fProcess, flags);
			}
		}
		if (delta != null && !isDisposed()) {
			fireModelChanged(delta);
		}
	}

	@Override
	public synchronized void dispose() {
		super.dispose();
		fProcess = null;
	}

	public ProcessProxy(IProcess process) {
		fProcess = process;
	}

	@Override
	protected synchronized boolean containsEvent(DebugEvent event) {
		return event.getSource().equals(fProcess);
	}

	@Override
	protected DebugEventHandler[] createEventHandlers() {
		return new DebugEventHandler[] {fProcessEventHandler};
	}

	@Override
	public void installed(Viewer viewer) {
		super.installed(viewer);
		// select process if in run mode
		IProcess process = fProcess;
		if (process != null) {
			if (process.isTerminated()) {
				fireProcessDelta(IModelDelta.UNINSTALL);
			} else {
				ILaunch launch = process.getLaunch();
				if (launch != null && ILaunchManager.RUN_MODE.equals(launch.getLaunchMode())) {
					// select the process
					ModelDelta delta = new ModelDelta(DebugPlugin.getDefault().getLaunchManager(), IModelDelta.NO_CHANGE);
					ModelDelta node = delta.addNode(process.getLaunch(), IModelDelta.NO_CHANGE);
					node = node.addNode(process, IModelDelta.SELECT);
					fireModelChanged(delta);
				}
			}
		}
	}
}
