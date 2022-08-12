/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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

package org.eclipse.team.internal.ui.history;

import org.eclipse.core.resources.IResource;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.ui.part.PluginDropAdapter;
import org.eclipse.ui.part.ResourceTransfer;

public class GenericHistoryDropAdapter extends PluginDropAdapter {

	private GenericHistoryView view;

	public GenericHistoryDropAdapter(GenericHistoryView view) {
		super(null);
		this.view = view;
	}

	/*
	 * Override dragOver to slam the detail to DROP_LINK, as we do not
	 * want to really execute a DROP_MOVE, although we want to respond
	 * to it.
	 */
	@Override
	public void dragOver(DropTargetEvent event) {
		if ((event.operations & DND.DROP_LINK) == DND.DROP_LINK) {
			event.detail = DND.DROP_LINK;
		}
		super.dragOver(event);
	}

	/*
	 * Override drop to slam the detail to DROP_LINK, as we do not
	 * want to really execute a DROP_MOVE, although we want to respond
	 * to it.
	 */
	@Override
	public void drop(DropTargetEvent event) {
		super.drop(event);
		event.detail = DND.DROP_LINK;
	}

	@Override
	public boolean performDrop(Object data) {
		if (data == null)
			return false;
		if (data instanceof IResource[]) {
			IResource[] sources = (IResource[]) data;
			if (sources.length == 0)
				return false;
			IResource resource = sources[0];
			//Allow all resources types through to the view, the individual pages can decide
			//which ones to handle
			view.showHistoryPageFor(resource, true, true, null);

			return true;
		}
		return false;
	}

	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		if (transferType != null && ResourceTransfer.getInstance().isSupportedType(transferType)) {
			return true;
		}

		return super.validateDrop(target, operation, transferType);
	}

	@Override
	protected Object getCurrentTarget() {
		return view;
	}
}
