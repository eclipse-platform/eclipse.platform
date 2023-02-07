/*******************************************************************************
 * Copyright (c) 2002, 2017 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetRegistryReader;

/**
 * This is used to store the most recently used (MRU) list
 * of cheatsheet for the entire workbench.
 */
public class CheatSheetHistory {

	private static final int DEFAULT_DEPTH = 5;

	private ArrayList<CheatSheetElement> history;
	private CheatSheetRegistryReader reg;
	private ListenerList<IPropertyListener> listeners = new ListenerList<>();

	public CheatSheetHistory(CheatSheetRegistryReader reg) {
		this.history = new ArrayList<>(DEFAULT_DEPTH);
		this.reg = reg;
	}

	public void addListener(IPropertyListener l) {
		listeners.add(l);
	}

	public void removeListener(IPropertyListener l) {
		listeners.remove(l);
	}

	private void fireChange() {
		for (IPropertyListener element : listeners) {
			element.propertyChanged(this, 0);
		}
	}

	public IStatus restoreState(IMemento memento) {
		IMemento [] children = memento.getChildren("element"); //$NON-NLS-1$
		for (int i = 0; i < children.length && i < DEFAULT_DEPTH; i++) {
			CheatSheetElement element =
				reg.findCheatSheet(children[i].getID());
			if (element != null)
				history.add(element);
		}
		return new Status(IStatus.OK,ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID,0,ICheatSheetResource.EMPTY_STRING,null);
	}

	public IStatus saveState(IMemento memento) {
		Iterator<CheatSheetElement> iter = history.iterator();
		while (iter.hasNext()) {
			CheatSheetElement element = iter.next();
			if(element != null) {
				memento.createChild("element", element.getID()); //$NON-NLS-1$
			}
		}
		return new Status(IStatus.OK,ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID,0,ICheatSheetResource.EMPTY_STRING,null);
	}

	public void add(String id) {
		CheatSheetElement element = reg.findCheatSheet(id);
		if (element != null)
			add(element);
	}

	public void add(CheatSheetElement element) {
		// Avoid duplicates
		if (history.contains(element))
			return;

		// If the shortcut list will be too long, remove oldest ones
		int size = history.size();
		int preferredSize = DEFAULT_DEPTH;
		while (size >= preferredSize) {
			size--;
			history.remove(size);
		}

		// Insert at top as most recent
		history.add(0, element);
		fireChange();
	}

	public void refreshFromRegistry() {
		boolean change = false;

		Iterator<CheatSheetElement> iter = history.iterator();
		while (iter.hasNext()) {
			CheatSheetElement element = iter.next();
			if (reg.findCheatSheet(element.getID()) == null) {
				iter.remove();
				change = true;
			}
		}

		if (change)
			fireChange();
	}

	/**
	 * Copy the requested number of items from the history into
	 * the destination list at the given index.
	 *
	 * @param dest destination list to contain the items
	 * @param destStart index in destination list to start copying items at
	 * @param count number of items to copy from history
	 * @return the number of items actually copied
	 */
	public int copyItems(List<CheatSheetElement> dest, int destStart, int count) {
		int itemCount = count;
		if (itemCount > history.size())
			itemCount = history.size();

		for (int i = 0; i < itemCount; i++)
			dest.add(destStart + i, history.get(i));

		return itemCount;
	}
}

