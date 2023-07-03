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

public class RepeatedSubItem extends AbstractSubItem implements ISubItemItem {
	private String values;
	private ArrayList<AbstractSubItem> subItems;

	/**
	 * Constructor for RepeatedSubItem.
	 */
	public RepeatedSubItem() {
		super();
	}

	public RepeatedSubItem(String values) {
		super();
		this.values = values;
	}

	/**
	 * Returns the values.
	 * @return String
	 */
	public String getValues() {
		return values;
	}

	/**
	 * Sets the values.
	 * @param newValues The new values to set
	 */
	public void setValues(String newValues) {
		this.values = newValues;
	}

	/**
	 * @param subItem the SubItem to add.
	 */
	@Override
	public void addSubItem(AbstractSubItem subItem) {
		if(subItems == null) {
			subItems = new ArrayList<>();
		}
		subItems.add(subItem);
	}

	/**
	 * Returns a list which will always only contain at most 1 entry.
	 * @return Returns the subItems.
	 */
	@Override
	public ArrayList<AbstractSubItem> getSubItems() {
		return subItems;
	}
}
