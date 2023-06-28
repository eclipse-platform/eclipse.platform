/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
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
 *     Wind River Systems - Ted Williams - [Memory View] Memory View: Workflow Enhancements (Bug 215432)
 *******************************************************************************/

package org.eclipse.debug.internal.ui.views.memory;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;

/**
 * Controls view tab enablement based on tab folder selection
 *
 * @since 3.0
 */
public class ViewTabEnablementManager implements SelectionListener {

	@Override
	public void widgetSelected(SelectionEvent e) {
		if (e.getSource() instanceof CTabFolder) {
			handleTabFolderSelection(e);
		}
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {

	}

	private void handleTabFolderSelection(SelectionEvent event) {
		CTabFolder folder = (CTabFolder) event.getSource();

		CTabItem selectedItem = folder.getSelection();
		IMemoryViewTab selectedViewTab;

		// get selected view tab
		if (selectedItem != null) {
			Object obj = selectedItem.getData();

			if (obj instanceof IMemoryViewTab) {
				selectedViewTab = (IMemoryViewTab) obj;
			} else {
				return;
			}
		} else {
			return;
		}

		// get all tabs
		CTabItem[] allTabs = folder.getItems();

		// check all tabs to make sure they are enabled/disabled properly
		for (CTabItem tab : allTabs) {
			IMemoryViewTab viewTab;
			Object obj = tab.getData();
			if (obj instanceof IMemoryViewTab) {
				viewTab = (IMemoryViewTab) obj;

				// if view tab matches the selected item
				if (viewTab == selectedViewTab && !viewTab.isEnabled()) {
					// if the item has been selected and this tab is not enabled
					// enable it.
					viewTab.setEnabled(true);
					viewTab.getRendering().activated();

				} else if (viewTab != selectedViewTab && viewTab.isEnabled()) {
					// if the tab is not selected, disable it
					viewTab.setEnabled(false);
					viewTab.getRendering().deactivated();
				}
			}
		}
	}

}
