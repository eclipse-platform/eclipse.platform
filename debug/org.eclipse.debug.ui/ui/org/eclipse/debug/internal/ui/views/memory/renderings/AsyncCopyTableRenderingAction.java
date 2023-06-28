/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.memory.renderings;

import java.util.ArrayList;

import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

/**
 * Copy action for <code>AbstractAsyncTableRendering</code>. Only copy what is
 * visible in the view.
 *
 */
public class AsyncCopyTableRenderingAction extends CopyTableRenderingToClipboardAction {

	public AsyncCopyTableRenderingAction(AbstractBaseTableRendering rendering, StructuredViewer viewer) {
		super(rendering, viewer);
	}

	@Override
	protected String concatenateTableAsString(TableItem[] itemList) {

		Table table = null;
		if (itemList.length > 0) {
			table = itemList[0].getParent();

			int topIndex = table.getTopIndex();
			int itemCount = table.getItemCount();
			int numVisibleLines = Math.min((table.getBounds().height / table.getItemHeight()) + 2, itemCount - topIndex);

			ArrayList<TableItem> items = new ArrayList<>();

			// start at top index until there is no more data in the table
			for (int i = topIndex; i < topIndex + numVisibleLines; i++) {
				if (itemList[i].getData() != null) {
					items.add(itemList[i]);
				} else {
					break;
				}
			}

			return super.concatenateTableAsString(items.toArray(new TableItem[items.size()]));
		}
		return IInternalDebugCoreConstants.EMPTY_STRING;
	}
}
