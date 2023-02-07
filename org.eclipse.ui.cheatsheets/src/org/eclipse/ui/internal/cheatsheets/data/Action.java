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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.internal.cheatsheets.ActionRunner;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;
import org.w3c.dom.Node;

/**
 * Class that represents an {@literal <ACTION>} element in a cheatsheet. This class stores all
 * of the attributes associated with an Action and is capable of executing that Action.
 */
public class Action extends AbstractExecutable {
	private String actionClass;
	private String pluginID;
	private boolean hasClassAttr = false;
	private boolean hasPluginId = false;

	public Action() {
		super();
	}


	/**
	 * This method returns the class specified to be run when the "click to perform" button is pressed for this item.
	 * @return the class name to be run for the item
	 */
	public String getActionClass() {
		return actionClass;
	}

	/**
	 * This method returns the string id of the plugin that contains the action class to be run.
	 * @return the id of the plugin that has the action class
	 */
	public String getPluginID() {
		return pluginID;
	}

	/**
	 *  This method allows you to specify the class to be run when the perform button is pressed for this
	 * item in the cheat sheet.
	 * @param aclass the class to be run by the item in the cheat sheet
	 */
	public void setClass(String aclass) {
		this.actionClass = aclass;
	}

	/**
	 * This method allows to set the plugin id of the action to be run by this item in the cheat sheet.
	 * @param pluginId the id of the plugin containing the action class specified for this item
	 */
	public void setPluginID(String pluginId) {
		this.pluginID = pluginId;
	}

	@Override
	public boolean handleAttribute(Node attribute) {
		if (attribute.getNodeName().equals(IParserTags.PLUGINID)) {
			hasPluginId = true;
			setPluginID(attribute.getNodeValue());
			return true;
		} else if (attribute.getNodeName().equals(IParserTags.CLASS)) {
			hasClassAttr = true;
			setClass(attribute.getNodeValue());
			return true;
		}
		return false;
	}

	@Override
	public String checkAttributes(Node node) {
		if(!hasClassAttr) {
			return NLS.bind(Messages.ERROR_PARSING_NO_CLASS, (new Object[] {node.getNodeName()}));
		}
		if(!hasPluginId) {
			return NLS.bind(Messages.ERROR_PARSING_NO_PLUGINID, (new Object[] {node.getNodeName()}));
		}
		if(isConfirm() && !isRequired()) {
			return NLS.bind(Messages.ERROR_PARSING_REQUIRED_CONFIRM, (new Object[] {node.getNodeName()}));
		}
		return null;
	}

	@Override
	public boolean isCheatSheetManagerUsed() {
		return true;
	}


	@Override
	public IStatus execute(CheatSheetManager csm) {
		return new ActionRunner().runAction(this, csm);
	}

	@Override
	public boolean hasParams() {
		return true;
	}

}
