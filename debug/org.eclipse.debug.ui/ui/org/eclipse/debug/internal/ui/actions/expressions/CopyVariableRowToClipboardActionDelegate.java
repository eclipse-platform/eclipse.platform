/*******************************************************************************
 *  Copyright (c) 2026 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.expressions;

import java.util.Iterator;

import org.eclipse.debug.internal.ui.viewers.model.VirtualCopyToClipboardActionDelegate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.jface.viewers.IStructuredSelection;

public class CopyVariableRowToClipboardActionDelegate extends VirtualCopyToClipboardActionDelegate {

	@Override
	protected boolean getEnableStateForSelection(IStructuredSelection selection) {
		if (selection.size() == 0) {
			return false;
		}

		if (getViewer() instanceof TreeModelViewer treeViewer) {
			if (!treeViewer.isShowColumns()) {
				return false;
			}
		}

		Iterator<?> itr = selection.iterator();
		while (itr.hasNext()) {
			Object element = itr.next();
			if (!isEnabledFor(element)) {
				return false;
			}
		}
		return true;
	}
}
