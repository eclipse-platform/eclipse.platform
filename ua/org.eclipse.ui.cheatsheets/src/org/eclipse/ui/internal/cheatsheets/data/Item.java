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

import org.eclipse.ui.cheatsheets.AbstractItemExtensionElement;

public class Item extends Intro implements IExecutableItem, IPerformWhenItem, ISubItemItem {
	private String title;
	private boolean skip;
	private boolean dialog;
	private ArrayList<AbstractItemExtensionElement[]> itemExtensions;

	private AbstractExecutable executable;
	private PerformWhen performWhen;

	private ArrayList<AbstractSubItem> subItems;
	private String completionMessage;

	/**
	 * Constructor for Item.
	 */
	public Item() {
		super();
	}

	public Item(String title, String description, String href, String contextId, boolean skip, boolean dialog) {
		super(description, href, contextId);
		this.title = title;
		this.skip = skip;
		this.dialog = dialog;
	}

	/**
	 * Returns the title.
	 * @return String
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * Returns whether this item is dynamic. An item is dynamic if it
	 * has performWhen condition, conditionalSubItems or repeatedSubItems.
	 *
	 * @return <code>true</code> if this item is dynamic, and
	 *  <code>false</code> for normal items
	 */
	public boolean isDynamic() {
		if( performWhen != null || hasDynamicSubItems()) {
			return true;
		}

		return false;
	}

	/**
	 * Returns whether or not this item requires opening a dialog.
	 * @return whether the item requires opening a dialog
	 */
	public boolean isDialog() {
		return this.dialog;
	}

	/**
	 * Returns the skip.
	 * @return boolean
	 */
	public boolean isSkip() {
		return this.skip;
	}

	/**
	 * Sets whether or not this item requires opening a dialog.
	 * @param dialog whether the item requires opening a dialog
	 */
	public void setDialog(boolean dialog) {
		this.dialog = dialog;
	}

	/**
	 * @param skip The skip to set.
	 */
	public void setSkip(boolean skip) {
		this.skip = skip;
	}

	/**
	 * Sets the title.
	 * @param title The title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * Sets the item extensions for this item.
	 * @param exts the extensions to set
	 */
	public void setItemExtensions(ArrayList<AbstractItemExtensionElement[]> exts) {
		this.itemExtensions = exts;
	}

	/**
	 * Returns the item extensions, if any, for this item,.
	 * @return list of the extensions or <code>null</code>
	 */
	public ArrayList<AbstractItemExtensionElement[]> getItemExtensions() {
		return itemExtensions;
	}

	/**
	 * @return Returns the performWhen.
	 */
	@Override
	public PerformWhen getPerformWhen() {
		return performWhen;
	}

	/**
	 * @param performWhen The performWhen to set.
	 */
	@Override
	public void setPerformWhen(PerformWhen performWhen) {
		this.performWhen = performWhen;
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

	private boolean hasDynamicSubItems() {
		if( subItems != null) {
			for (AbstractSubItem subItem : subItems) {
				if (subItem instanceof RepeatedSubItem || subItem instanceof ConditionalSubItem
						|| subItem instanceof SubItem && ((SubItem) subItem).getPerformWhen() != null) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public AbstractExecutable getExecutable() {
		return executable;
	}

	@Override
	public void setExecutable(AbstractExecutable executable) {
		this.executable = executable;
	}

	public void setCompletionMessage(String message) {
		this.completionMessage = message;
	}

	public String getCompletionMessage() {
		return completionMessage;
	}
}
