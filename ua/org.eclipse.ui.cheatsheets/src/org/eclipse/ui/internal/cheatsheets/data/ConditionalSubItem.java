/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
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

import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;

public class ConditionalSubItem extends AbstractSubItem implements ISubItemItem {
	private String condition;
	private ArrayList<AbstractSubItem> subItems;
	private SubItem selectedSubItem;

	/**
	 * Constructor for ConditionalSubItem.
	 */
	public ConditionalSubItem() {
		super();
	}

	public ConditionalSubItem(String condition) {
		super();
		this.condition = condition;
	}

	/**
	 * Returns the condition.
	 * @return String
	 */
	public String getCondition() {
		return condition;
	}

	/**
	 * Sets the condition.
	 * @param newCondition The new condition to set
	 */
	public void setCondition(String newCondition) {
		this.condition = newCondition;
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
	 * @return Returns the subItems.
	 */
	@Override
	public ArrayList<AbstractSubItem> getSubItems() {
		return subItems;
	}

	public SubItem getSelectedSubItem() {
		return selectedSubItem;
	}

	public void setSelectedSubItem(CheatSheetManager csm) {
		String conditionValue = csm.getVariableData(condition);

		for (AbstractSubItem abstractSubItem : subItems) {
			SubItem subItem = (SubItem) abstractSubItem;
			if(subItem.getWhen() != null && subItem.getWhen().equals(conditionValue)) {
				selectedSubItem = subItem;
				break;
			}
		}
	}
}
