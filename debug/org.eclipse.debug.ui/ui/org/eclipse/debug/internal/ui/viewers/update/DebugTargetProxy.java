/*******************************************************************************
 *  Copyright (c) 2005, 2009 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.update;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.jface.viewers.Viewer;

/**
 * Default model proxy for a debug target.
 *
 * @since 3.2
 */
public class DebugTargetProxy extends EventHandlerModelProxy {

	private IDebugTarget fDebugTarget;

	public DebugTargetProxy(IDebugTarget target) {
		fDebugTarget = target;
	}

	@Override
	public synchronized void dispose() {
		super.dispose();
		fDebugTarget = null;
	}

	@Override
	protected boolean containsEvent(DebugEvent event) {
		Object source = event.getSource();
		if (source instanceof IDebugElement) {
			IDebugTarget debugTarget = ((IDebugElement) source).getDebugTarget();
			// an expression can return null for debug target
			if (debugTarget != null) {
				return debugTarget.equals(fDebugTarget);
			}
		}
		return false;
	}

	@Override
	protected DebugEventHandler[] createEventHandlers() {
		ThreadEventHandler threadEventHandler = new ThreadEventHandler(this);
		return new DebugEventHandler[] { new DebugTargetEventHandler(this), threadEventHandler,
				new StackFrameEventHandler(this, threadEventHandler) };
	}

	@Override
	public void installed(Viewer viewer) {
		// select any thread that is already suspended after installation
		IDebugTarget target = fDebugTarget;
		if (target != null) {
			ModelDelta delta = getNextSuspendedThreadDelta(null, false);
			if (delta == null) {
				try {
					ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
					ILaunch launch = target.getLaunch();
					int launchIndex = getLaunchIndex(launch);
					int targetIndex = getTargetIndex(target);
					delta = new ModelDelta(manager, IModelDelta.NO_CHANGE);
					ModelDelta node = delta.addNode(launch, launchIndex, IModelDelta.NO_CHANGE, target.getLaunch().getChildren().length);
					node = node.addNode(target, targetIndex, IModelDelta.EXPAND | IModelDelta.SELECT, target.getThreads().length);
				} catch (DebugException e) {
					// In case of exception do not fire delta
					return;
				}
			}
			// expand the target if no suspended thread
			fireModelChanged(delta);
		}
	}

	protected ModelDelta getNextSuspendedThreadDelta(IThread currentThread, boolean reverse) {
		IDebugTarget target = fDebugTarget;
		if (target != null) {
			try {
				IThread[] threads = target.getThreads();
				IThread chosen = null;
				int threadIndex = -1;
				// select the first thread with a breakpoint, or the first suspended thread
				// if none have breakpoints
				boolean takeNext = currentThread == null;
				int startIdx = reverse ? threads.length - 1 : 0;
				int endIdx = reverse ? -1 : threads.length;
				int increment = reverse ? -1 : 1;
				for (int i = startIdx; i != endIdx; i = i + increment) {
					IThread thread = threads[i];
					if (takeNext && thread.isSuspended()) {
						IBreakpoint[] bps = thread.getBreakpoints();
						if (bps != null && bps.length > 0) {
							chosen = thread;
							threadIndex = i;
							break;
						} else {
							if (chosen == null) {
								chosen = thread;
								threadIndex = i;
							}
						}
					}
					takeNext = takeNext || thread.equals(currentThread);
				}
				if (chosen != null) {
					IStackFrame frame = chosen.getTopStackFrame();
					if (frame != null) {
						ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
						ILaunch launch = target.getLaunch();
						int launchIndex = getLaunchIndex(launch);
						int targetIndex = getTargetIndex(target);
						int stackFrameIndex = getStackFrameIndex(frame);
						ModelDelta delta = new ModelDelta(manager, IModelDelta.NO_CHANGE);
						ModelDelta node = delta.addNode(launch, launchIndex, IModelDelta.NO_CHANGE, target.getLaunch().getChildren().length);
						node = node.addNode(target, targetIndex, IModelDelta.NO_CHANGE, threads.length);
						node = node.addNode(chosen, threadIndex, IModelDelta.NO_CHANGE | IModelDelta.EXPAND, chosen.getStackFrames().length);
						node = node.addNode(frame, stackFrameIndex, IModelDelta.NO_CHANGE | IModelDelta.SELECT, 0);
						return delta;
					}
				}
			} catch (DebugException e) {
			}
		}
		return null;
	}

	/**
	 * Computes the index of a launch at top level in the {@code Debug View} tree.
	 *
	 * @param launch The launch for which to compute the index.
	 *
	 * @return The index of the specified launch at top level in the
	 *         {@code Debug View}.
	 */
	protected int getLaunchIndex(ILaunch launch) {
		ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
		return indexOf(manager.getLaunches(), launch);
	}

	/**
	 * Computes the index of a debug target in its parent launch. The debug target
	 * index corresponds to the index in {@code Debug View} tree.
	 *
	 * @param target The debug target for which to compute the index.
	 *
	 * @return The index of the specified debug target in its launch.
	 */
	protected int getTargetIndex(IDebugTarget target) {
		return indexOf(target.getLaunch().getChildren(), target);
	}

	/**
	 * Computes the index of a stack frame in the thread suspended at that stack
	 * frame. The stack frame index corresponds to the index in {@code Debug View}
	 * tree.
	 *
	 * @param stackFrame The stack frame for which to compute the index.
	 *
	 * @return The index of the specified stack frame in its parent thread.
	 */
	protected int getStackFrameIndex(IStackFrame stackFrame) {
		return 0;
	}
}
