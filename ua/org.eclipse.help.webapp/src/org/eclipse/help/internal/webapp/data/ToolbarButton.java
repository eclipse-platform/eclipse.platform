/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.help.internal.webapp.data;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * This class calls eclipse API's directly, so it should only be instantiated in
 * the workbench scenario, not in the infocenter.
 */
public class ToolbarButton {
	private String name;
	private String tooltip;
	private String image;
	private String action;
	private String param;
	private String styleClass;
	private boolean state;
	private boolean isSeparator;

	public ToolbarButton() {
		isSeparator = true;
	}

	public ToolbarButton(String name, String tooltip, String image,
			String action, String param, String state) {
		this.name = name;
		this.tooltip = tooltip;
		this.image = image;
		this.action = action;
		this.param = param;
		this.state = state.equalsIgnoreCase("on")?true:false; //$NON-NLS-1$
		if (state.startsWith("hid")) //$NON-NLS-1$
			this.styleClass = "buttonHidden"; //$NON-NLS-1$
		else if ("menu".equals(action)) { //$NON-NLS-1$
			this.styleClass = "buttonMenu"; //$NON-NLS-1$
		}
		else
			this.styleClass = state.equalsIgnoreCase("on")?"buttonOn":"button";   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
	}

	public boolean isSeparator() {
		return isSeparator;
	}

	public boolean isMenu() {
		return "menu".equals(action); //$NON-NLS-1$
	}

	public String getName() {
		return name;
	}

	public String[][] getMenuData() {
		List<String[]> list = new ArrayList<>();
		StringTokenizer tok = new StringTokenizer(param, ","); //$NON-NLS-1$
		while(tok.hasMoreTokens()) {
			String token = tok.nextToken();
			int index = token.indexOf('=');
			list.add(new String[] { token.substring(0, index), token.substring(index + 1) });
		}
		return list.toArray(new String[list.size()][]);
	}

	public String getTooltip() {
		return tooltip;
	}

	/**
	 * Returns the image
	 *
	 * @return String
	 */
	public String getImage() {
		return image;
	}

	public String getAction() {
		return action;
	}

	public String getParam() {
		return param;
	}

	public boolean isOn() {
		return state;
	}

	public String getStyleClass() {
		return styleClass;
	}
}
