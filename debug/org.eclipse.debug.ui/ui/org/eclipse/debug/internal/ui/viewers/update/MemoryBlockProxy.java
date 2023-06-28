/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
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
 *     Mikhail Khodjaiants - Bug 383687 - Memory view is not updated when using Platform renderings
 *******************************************************************************/

package org.eclipse.debug.internal.ui.viewers.update;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ModelDelta;
import org.eclipse.jface.viewers.Viewer;

public class MemoryBlockProxy extends EventHandlerModelProxy  {

	private IMemoryBlock fMemoryBlock;
	private DebugEventHandler fDebugEventHandler = new DebugEventHandler(this)  {

		@Override
		protected boolean handlesEvent(DebugEvent event) {
			if (event.getKind() == DebugEvent.CHANGE && event.getSource() == fMemoryBlock)
				return true;

			Object src = event.getSource();
			if (src instanceof IDebugElement)
			{
				if (event.getKind() == DebugEvent.SUSPEND && ((IDebugElement)src).getDebugTarget() == fMemoryBlock.getDebugTarget())
					return true;
			}
			return false;
		}

		@Override
		protected void handleChange(DebugEvent event) {
			if (event.getDetail() == DebugEvent.STATE)
			{
				ModelDelta delta = new ModelDelta(fMemoryBlock, IModelDelta.STATE);
				fireModelChanged(delta);
			}
			else
			{
				ModelDelta delta = new ModelDelta(fMemoryBlock, IModelDelta.CONTENT);
				fireModelChanged(delta);
			}
		}

		@Override
		protected void handleSuspend(DebugEvent event) {
			ModelDelta delta = new ModelDelta(fMemoryBlock, IModelDelta.CONTENT);
			fireModelChanged(delta);
		}

		@Override
		public synchronized void dispose() {
			super.dispose();
		}};

	public MemoryBlockProxy(IMemoryBlock mb)
	{
		fMemoryBlock = mb;
	}

	@Override
	protected DebugEventHandler[] createEventHandlers() {
		return new DebugEventHandler[]{fDebugEventHandler};
	}

	@Override
	public void installed(Viewer viewer) {
		super.installed(viewer);
		setInstalled(true);
	}
}
