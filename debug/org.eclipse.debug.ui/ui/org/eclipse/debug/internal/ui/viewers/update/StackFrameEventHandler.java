/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;

/**
 * Default stack frame event handler for the debug view.
 *
 * @since 3.3
 */
public class StackFrameEventHandler extends DebugEventHandler {

	ThreadEventHandler fThreadHandler = null;

	/**
	 * Constructs a new stack frame event handler
	 */
	public StackFrameEventHandler(AbstractModelProxy proxy, ThreadEventHandler threadHandler) {
		super(proxy);
		fThreadHandler = threadHandler;
	}

	@Override
	protected boolean handlesEvent(DebugEvent event) {
		return event.getSource() instanceof IStackFrame;
	}

	@Override
	protected void handleChange(DebugEvent event) {
		IStackFrame frame = (IStackFrame) event.getSource();
		ModelDelta root = fThreadHandler.buildRootDelta();
		ModelDelta delta = fThreadHandler.addPathToThread(root, frame.getThread());
		delta = delta.addNode(frame.getThread(), IModelDelta.NO_CHANGE);
		int flags = IModelDelta.NO_CHANGE;
		if (event.getDetail() == DebugEvent.CONTENT) {
			flags = flags | IModelDelta.CONTENT;
		} else if (event.getDetail() == DebugEvent.STATE) {
			flags = flags | IModelDelta.STATE;
		}
		delta = delta.addNode(frame, flags);
		fireDelta(root);
	}

}
