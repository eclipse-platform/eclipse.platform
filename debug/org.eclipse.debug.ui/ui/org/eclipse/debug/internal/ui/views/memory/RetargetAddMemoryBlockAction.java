/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.actions.IAddMemoryBlocksTarget;
import org.eclipse.debug.ui.memory.IMemoryRenderingSite;

/**
 * This is the retargettable add memory block action in the Memory View. All
 * AddMemoryBlock actions in the view will use this action to make sure that
 * clients can override the "Add Memory Monitor" dialog.
 *
 */
public class RetargetAddMemoryBlockAction extends AddMemoryBlockAction {

	public RetargetAddMemoryBlockAction(IMemoryRenderingSite site) {
		super(site);
	}

	public RetargetAddMemoryBlockAction(IMemoryRenderingSite site, boolean addDefaultRenderings) {
		super(site, addDefaultRenderings);
	}

	public RetargetAddMemoryBlockAction(String text, int style, IMemoryRenderingSite site) {
		super(text, style, site);
	}

	@Override
	public void run() {
		// get current selection from Debug View
		Object debugContext = DebugUITools.getPartDebugContext(fSite.getSite());
		IAddMemoryBlocksTarget target = getAddMemoryBlocksTarget(debugContext);

		if (target != null) {
			try {
				if (target.supportsAddMemoryBlocks(getMemoryView())) {
					target.addMemoryBlocks(getMemoryView(), getMemoryView().getSite().getSelectionProvider().getSelection());
				} else
					super.run();
			} catch (CoreException e) {
				DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(), ActionMessages.RetargetAddMemoryBlockAction_0, ActionMessages.RetargetAddMemoryBlockAction_1, e);
			}
		} else {
			super.run();
		}
	}

	@Override
	protected void updateAction(Object debugContext) {

		try {
			IAddMemoryBlocksTarget target = getAddMemoryBlocksTarget(debugContext);

			if (target != null) {
				if (target.supportsAddMemoryBlocks(getMemoryView())) {
					if (getMemoryView().getSite().getSelectionProvider() != null)
						setEnabled(target.canAddMemoryBlocks(getMemoryView(), getMemoryView().getSite().getSelectionProvider().getSelection()));
					else
						super.updateAction(debugContext);
				} else
					super.updateAction(debugContext);
			} else {
				super.updateAction(debugContext);
			}
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
		}
	}

	private IAddMemoryBlocksTarget getAddMemoryBlocksTarget(Object debugContext) {
		IMemoryBlockRetrieval standardMemRetrieval = MemoryViewUtil.getMemoryBlockRetrieval(debugContext);

		if (standardMemRetrieval == null)
			return null;

		IAddMemoryBlocksTarget target = null;

		if (standardMemRetrieval instanceof IAddMemoryBlocksTarget) {
			target = (IAddMemoryBlocksTarget) standardMemRetrieval;
		} else if (standardMemRetrieval instanceof IAdaptable) {
			target = ((IAdaptable) standardMemRetrieval).getAdapter(IAddMemoryBlocksTarget.class);
		}
		return target;
	}
}
