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
 *     Phil Loats (IBM Corp.) - fix to use only foundation APIs
 *******************************************************************************/
package org.eclipse.help.internal.context;

import java.util.ArrayList;

import org.eclipse.help.ICommandLink;
import org.eclipse.help.IContext;
import org.eclipse.help.IContext2;
import org.eclipse.help.IContext3;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.ITopic;
import org.eclipse.help.internal.CommandLink;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.UAElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class Context extends UAElement implements IContext3 {

	public static final String ATTRIBUTE_TITLE = "title"; //$NON-NLS-1$
	public static final String NAME = "context"; //$NON-NLS-1$
	public static final String ELEMENT_DESCRIPTION = "description"; //$NON-NLS-1$
	public static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	public static final String ATTRIBUTE_PLUGIN_ID = "pluginId"; //$NON-NLS-1$

	public Context(Element src) {
		super(src);
	}

	public Context(IContext src, String id) {
		super(NAME);
		setId(id);
		children = new ArrayList<>();
		mergeContext(src);
	}

	public void mergeContext(IContext src) {
		String text = src.getText();
		if (getText() == null || getText().length() == 0) {
			setText(text);
		}
		if (src instanceof IContext2 && getTitle() == null) {
			String title  = ((IContext2)src).getTitle();
			if (title != null) {
				setAttribute(ATTRIBUTE_TITLE, title);
			}
		}
		if (src instanceof IContext3) {
			ICommandLink[] commands = ((IContext3)src).getRelatedCommands();
			for (int i=0;i<commands.length;++i) {
				appendChild(new CommandLink(commands[i]));
			}
		}
		IHelpResource[] topics = src.getRelatedTopics();
		for (int i=0;i<topics.length;++i) {
			if (topics[i] instanceof ITopic) {
				appendChild(new Topic((ITopic)topics[i]));
			}
			else {
				Topic topic = new Topic();
				topic.setHref(topics[i].getHref());
				topic.setLabel(topics[i].getLabel());
				appendChild(topic);
			}
		}
	}

	@Override
	public String getCategory(IHelpResource topic) {
		return null;
	}

	public String getId() {
		return getAttribute(ATTRIBUTE_ID);
	}

	@Override
	public ICommandLink[] getRelatedCommands() {
		return getChildren(ICommandLink.class);
	}

	@Override
	public IHelpResource[] getRelatedTopics() {
		return getChildren(IHelpResource.class);
	}

	@Override
	public String getStyledText() {
		return null;
	}

	@Override
	public String getText() {
		Node node = getElement().getFirstChild();
		while (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				if (ELEMENT_DESCRIPTION.equals(node.getNodeName())) {
					node.normalize();
					Node text = node.getFirstChild();
					if (text == null) {
						return new String();
					}
					if (text.getNodeType() == Node.TEXT_NODE) {
						return text.getNodeValue();
					}
				}
			}
			node = node.getNextSibling();
		}
		return null;
	}

	@Override
	public String getTitle() {
		String title = getAttribute(ATTRIBUTE_TITLE);
		if (title == null || title.length() == 0) {
			return null;
		}
		return title;
	}

	public void setId(String id) {
		setAttribute(ATTRIBUTE_ID, id);
	}

	public void setText(String text) {
		Node node = getElement().getFirstChild();
		while (node != null) {
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				if (ELEMENT_DESCRIPTION.equals(node.getNodeName())) {
					getElement().removeChild(node);
					break;
				}
			}
			node = node.getNextSibling();
		}
		if (text != null) {
			Document document = getElement().getOwnerDocument();
			Node description = getElement().appendChild(document.createElement(ELEMENT_DESCRIPTION));
			description.appendChild(document.createTextNode(text));
		}
	}

}
