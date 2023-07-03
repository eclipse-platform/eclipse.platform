/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Alexander Kurtakov - Bug 460858
 *******************************************************************************/
package org.eclipse.help.internal.workingset;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.help.IHelpResource;
import org.eclipse.help.ITopic;
import org.w3c.dom.Element;

/**
 * Makes help resources adaptable and persistable
 */
public class AdaptableTopic extends AdaptableHelpResource {
	/**
	 * Map of all topics with this topic as ancestor
	 */
	private Map<String, IHelpResource> topicMap;

	/**
	 * This constructor will be called when wrapping help resources.
	 */
	public AdaptableTopic(ITopic element) {
		super(element);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == ITopic.class)
			return (T) element;
		return super.getAdapter(adapter);
	}

	@Override
	public AdaptableHelpResource[] getChildren() {
		ITopic[] topics = this.getSubtopics();
		AdaptableHelpResource[] adaptableTopic = new AdaptableTopic[topics.length];
		for (int i = 0; i < topics.length; i++) {
			adaptableTopic[i] = new AdaptableTopic(topics[i]);
		}
		return adaptableTopic;
	}

	public ITopic[] getSubtopics() {
		return ((ITopic) element).getSubtopics();
	}

	/**
	 * Returns a topic with the specified href. <br>
	 * It is possible that multiple tocs have the same href, in which case there
	 * is no guarantee which one is returned.
	 *
	 * @param href
	 *            The topic's href value.
	 */
	@Override
	public ITopic getTopic(String href) {
		if (href == null)
			return null;

		if (topicMap == null) {
			// traverse TOC and fill in the topicMap
			topicMap = new HashMap<>();
			topicMap.put(getHref(), element);
			Deque<ITopic> stack = new ArrayDeque<>();
			ITopic[] topics = getSubtopics();
			for (ITopic topic : topics)
				stack.push(topic);
			while (!stack.isEmpty()) {
				ITopic topic = stack.pop();
				if (topic != null) {
					String topicHref = topic.getHref();
					if (topicHref != null) {
						topicMap.put(topicHref, topic);
					}
					ITopic[] subtopics = topic.getSubtopics();
					for (ITopic subtopic : subtopics)
						stack.push(subtopic);
				}
			}
		}
		return (ITopic) topicMap.get(href);
	}

	@Override
	public void saveState(Element element) {
		AdaptableToc toc = (AdaptableToc) getParent();
		toc.saveState(element);
		AdaptableHelpResource[] topics = toc.getChildren();
		for (int i = 0; i < topics.length; i++)
			if (topics[i] == this)
				element.setAttribute("topic", String.valueOf(i)); //$NON-NLS-1$
	}
}
