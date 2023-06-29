/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;


/**
 * @since 3.2
 */
public class DebugTargetEventHandler extends DebugEventHandler {

	/**
	 * Constructs an event handler for a debug target on the given viewer.
	 *
	 * @param viewer
	 */
	public DebugTargetEventHandler(AbstractModelProxy proxy) {
		super(proxy);
	}

	@Override
	protected boolean handlesEvent(DebugEvent event) {
		return event.getSource() instanceof IDebugTarget;
	}

	@Override
	protected void handleChange(DebugEvent event) {
		int flags = IModelDelta.STATE;
		if (event.getDetail() == DebugEvent.CONTENT) {
			flags = flags | IModelDelta.CONTENT;
		}
		fireDelta((IDebugTarget) event.getSource(), flags);
	}

	@Override
	protected void handleCreate(DebugEvent event) {
		fireDelta((IDebugTarget) event.getSource(), IModelDelta.EXPAND);
	}

	@Override
	protected void handleResume(DebugEvent event) {
		fireDelta((IDebugTarget) event.getSource(), IModelDelta.CONTENT | IModelDelta.STATE | IModelDelta.SELECT);
	}

	@Override
	protected void handleSuspend(DebugEvent event) {
		fireDelta((IDebugTarget) event.getSource(), IModelDelta.CONTENT | IModelDelta.STATE);
	}

	@Override
	protected void handleTerminate(DebugEvent event) {
		fireDelta((IDebugTarget) event.getSource(), IModelDelta.CONTENT | IModelDelta.STATE | IModelDelta.UNINSTALL);
	}

	private void fireDelta(IDebugTarget target, int flags) {
		ModelDelta root = new ModelDelta(DebugPlugin.getDefault().getLaunchManager(), IModelDelta.NO_CHANGE);
		ModelDelta delta = root.addNode(target.getLaunch(), IModelDelta.NO_CHANGE);
		delta.addNode(target, flags);
		fireDelta(root);
	}

}
