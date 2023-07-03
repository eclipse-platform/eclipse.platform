/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;
import org.w3c.dom.Node;

/**
 * The common base class for all of the cheatsheet elements which have
 * executable behavior, i.e. Action, Command and Linkable
 */

public abstract class AbstractExecutable {

	private String[] params;
	private boolean confirm = false;
	private String when;
	private boolean required = true;

	/**
	 * This method returns an array of parameters specified to be passed to the action class
	 * when it is run in the cheat sheet.
	 * @return an array of string parameters that are passed to the action class when it is run
	 */
	public String[] getParams() {
		return params;
	}

	/**
	 * This method returns the expression to be used when determining if this action should used.
	 * @return the when expression to be used for this action
	 */
	public String getWhen() {
		return when;
	}

	/**
	 * Returns whether this action needs to be manually confirmed by the user.
	 * @return <code>true</code> when the action needs to be confirmed and <code>false</code> otherwise.
	 */
	public boolean isConfirm() {
		return confirm;
	}

	/**
	 * @return true if this step or substep can only be completed by performing
	 * this executable.
	 */
	public boolean isRequired() {
		return required;
	}

	/**
	 * This method allows you to set the string parameters to be passed to the action class on running it
	 * in the cheat sheet.
	 * @param params an array of strings that is passed to the action class on running the action
	 */
	public void setParams(String[] params) {
		this.params = params;
	}

	/**
	 * Set whether this action needs to be manually confirmed by the user.
	 * @param value The new value of the confirm state.
	 */
	public void setConfirm(boolean value) {
		this.confirm = value;
	}

	/**
	 * Set whether this executable can be by passed.
	 * @param required if true this action must be performed to complete this
	 * step or substep.
	 */
	public void setRequired(boolean required) {
		this.required = required;
	}

	/**
	 * Indicates this action is to be used if and only if the value of the condition attribute
	 * of the containing {@literal <perform-when>} element matches this string value. This attribute is
	 * ignored if the {@literal <action>} element is not a child of a {@literal <perform-when>} element.
	 * @param when The expression to use when determine if this action should be used.
	 */
	public void setWhen(String when) {
		this.when = when;
	}

	/**
	 * Handle an attribute specific to this type of AbstractExecutable
	 * @param attribute
	 * @return true if this parameter is valid for this type of executable
	 */
	public abstract boolean handleAttribute(Node attribute);

	/**
	 * Check to see if all required attributes are present and are valid.
	 * This method is called after any calls to handleAttributes have been made
	 * @param node the node for this executable.
	 * @return null if the parameters are valid or an error message if the
	 * parameters are invalid or incomplete.
	 */
	public abstract String checkAttributes(Node node);

	/**
	 * @return true if calls to execute require a non-null CheatsheetManager
	 */
	public abstract boolean isCheatSheetManagerUsed();

	/**
	 * Execute and return a status
	 * @param csm A cheatsheet manager if this object uses a cheatsheet manager,
	 * otherwise null.
	 * @return OK status if the operation succeeds, warning status if an action
	 * completes with a failure result, error status if an exception was thrown
	 * or the executable could not be initiated.
	 */
	public abstract IStatus execute(CheatSheetManager csm);

	/**
	 * @return true if this executable can have parameters
	 */
	public abstract boolean hasParams();

}
