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

import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.internal.ui.viewers.model.VirtualCopyToClipboardActionDelegate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.VirtualItem;
import org.eclipse.jface.viewers.IStructuredSelection;

public class CopyExpressionToClipboardActionDelegate extends VirtualCopyToClipboardActionDelegate {

	@Override
	protected void append(VirtualItem item, StringBuilder buffer, int indent) {
		if (item.getData() instanceof IExpression expression) {
			if (!buffer.isEmpty()) {
				buffer.append(System.lineSeparator());
			}
			String expString = expression.getExpressionText();
			buffer.append(expString);
		}
	}

	@Override
	protected boolean getEnableStateForSelection(IStructuredSelection selection) {
		if (selection.size() == 0) {
			return false;
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
