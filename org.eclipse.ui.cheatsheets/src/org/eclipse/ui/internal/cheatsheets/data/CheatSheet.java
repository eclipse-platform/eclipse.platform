/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.data;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 */
public class CheatSheet implements ICheatSheet {

	protected String title;
	private Item introItem;
	private ArrayList<Item> items;
	private boolean containsCommandOrAction;

	/**
	 * Creates a new cheat sheet.
	 *
	 */
	public CheatSheet() {
	}

	/**
	 * This method sets the title of cheat sheet.
	 *
	 * @param title the title of cheat sheet
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * This method returns the title of the cheat sheet.
	 * @return the title of the cheat sheet
	 */
	public String getTitle(){
		return title;
	}

	/**
	 * Returns the intro item.
	 */
	public Item getIntroItem() {
		return introItem;
	}

	/**
	 * Returns the items.
	 */
	public ArrayList<Item> getItems() {
		return items;
	}

	/**
	 * Returns the intro item.
	 */
	public void setIntroItem(Item intro) {
		introItem = intro;
	}

	/**
	 * Adds an item to the cheat sheet.
	 *
	 * @param item the item to add
	 */
	public void addItem(Item item) {
		if(items == null) {
			items = new ArrayList<>();
		}
		items.add(item);
	}

	/**
	 * Adds all the items from the collection to the cheat sheet.
	 *
	 * @param c the collection of items to add
	 */
	public void addItems(Collection<Item> c) {
		if(items == null) {
			items = new ArrayList<>();
		}
		items.addAll(c);
	}

	public void setContainsCommandOrAction(boolean containsCommandOrAction) {
		this.containsCommandOrAction = containsCommandOrAction;
	}

	public boolean isContainsCommandOrAction() {
		return containsCommandOrAction;
	}
}
