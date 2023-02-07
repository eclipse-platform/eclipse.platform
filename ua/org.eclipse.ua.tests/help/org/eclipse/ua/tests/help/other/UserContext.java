/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.other;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.help.ICommandLink;
import org.eclipse.help.IContext3;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.ITopic;

/**
 * This class is used to test contexts created using the IContext API
 */

public class UserContext implements IContext3 {

	private String title;
	private String text;
	private List<ITopic> children = new ArrayList<>();

	public UserContext(String title, String text) {
		this.title = title;
		this.text = text;
	}

	@Override
	public ICommandLink[] getRelatedCommands() {
		return new ICommandLink[0];
	}

	@Override
	public String getCategory(IHelpResource topic) {
		return null;
	}

	@Override
	public String getStyledText() {
		return getText();
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public IHelpResource[] getRelatedTopics() {
		return children.toArray(new IHelpResource[0]);
	}

	@Override
	public String getText() {
		return text;
	}

	public void addTopic(ITopic child) {
		children.add(child);
	}


}
