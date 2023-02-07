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

public class SubItem extends AbstractSubItem implements IExecutableItem, IPerformWhenItem {

	private String label;
	private boolean skip = false;
	private String when;
	private boolean formatted = false;

	private AbstractExecutable executable;
	private PerformWhen performWhen;

	public SubItem() {
		super();
	}

	/**
	 * This method returns the label to be shown for the sub item.
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}

	/**
	 * This method sets the label that will be shown for the sub item.
	 * @param label the label to be shown
	 */
	public void setLabel(String string) {
		label = string;
	}

	/**
	 * This method returns the skip state for the sub item.
	 * @return the label
	 */
	public boolean isSkip() {
		return skip;
	}

	/**
	 * This method sets whether the sub item can be skipped.
	 * @param value the new value for skip
	 */
	public void setSkip(boolean value) {
		skip = value;
	}

	/**
	 * This method returns the when expression for the sub item.
	 * @return the label
	 */
	public String getWhen() {
		return when;
	}

	/**
	 * This method sets the when expression for the sub item.
	 * @param string the when expression to set
	 */
	public void setWhen(String string) {
		when = string;
	}

	/**
	 * @return Returns the action.
	 */
	@Override
	public AbstractExecutable getExecutable() {
		return executable;
	}

	/**
	 * @param executable The Executable to set.
	 */
	@Override
	public void setExecutable(AbstractExecutable executable) {
		this.executable = executable;
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

	public void setFormatted(boolean formatted) {
		this.formatted = formatted;
	}

	public boolean isFormatted() {
		return formatted;
	}
}
