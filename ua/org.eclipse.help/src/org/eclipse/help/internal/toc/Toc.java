/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.help.internal.toc;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.help.ICriteria;
import org.eclipse.help.IToc;
import org.eclipse.help.IToc2;
import org.eclipse.help.ITocContribution;
import org.eclipse.help.ITopic;
import org.eclipse.help.ITopic2;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.UAElement;
import org.w3c.dom.Element;

public class Toc extends UAElement implements IToc2 {

	public static final String NAME = "toc"; //$NON-NLS-1$
	public static final String ATTRIBUTE_LABEL = "label"; //$NON-NLS-1$
	public static final String ATTRIBUTE_HREF = "href"; //$NON-NLS-1$
	public static final String ATTRIBUTE_TOPIC = "topic"; //$NON-NLS-1$
	public static final String ATTRIBUTE_LINK_TO = "link_to"; //$NON-NLS-1$
	public static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	public static final String ATTRIBUTE_ICON = "icon"; //$NON-NLS-1$
	public static final String ATTRIBUTE_SORT = "sort"; //$NON-NLS-1$

	private ITocContribution contribution;
	private ITopic topic;
	private Map<String, ITopic> href2TopicMap;

	public Toc(IToc src) {
		super(NAME, src);
		setHref(src.getHref());
		setLabel(src.getLabel());
		ITopic topic = src.getTopic(null);
		if (topic != null) {
			setTopic(topic.getHref());
		}
		appendChildren(src.getChildren());
	}

	public Toc(Element src) {
		super(src);
	}

	/*
	 * Creates a mapping of all topic hrefs to ITopics.
	 */
	private Map<String, ITopic> createHref2TopicMap() {
		Map<String, ITopic> map = new HashMap<>();
		if (topic != null) {
			map.put(topic.getHref(), topic);
		}
		ITopic[] topics = getTopics();
		for (int i = 0; i < topics.length; ++i) {
			createHref2TopicMapAux(map, topics[i]);
		}
		return map;
	}

	/*
	 * Creates a mapping of all topic hrefs to ITopics under the given ITopic and stores in the
	 * given Map.
	 */
	private void createHref2TopicMapAux(Map<String, ITopic> map, ITopic topic) {
		String href = topic.getHref();
		if (href != null) {
			map.put(href, topic);
			int anchorIx = href.lastIndexOf("#"); //$NON-NLS-1$
			if (anchorIx >= 0) { // anchor exists, drop it and add href again to map
				String simpleHref = href.substring(0, anchorIx);
				if (!map.containsKey(simpleHref)) {
					map.put(simpleHref, topic);
				}
			}
		}
		ITopic[] subtopics = topic.getSubtopics();
		if (subtopics != null) {
			for (int i = 0; i < subtopics.length; ++i) {
				if (subtopics[i] != null) {
					createHref2TopicMapAux(map, subtopics[i]);
				}
			}
		}
	}

	@Override
	public String getHref() {
		return getAttribute(ATTRIBUTE_HREF);
	}

	@Override
	public String getIcon() {
		return getAttribute(ATTRIBUTE_ICON);
	}

	@Override
	public boolean isSorted() {
		return "true".equalsIgnoreCase(getAttribute(ATTRIBUTE_SORT)); //$NON-NLS-1$
	}

	/*
	 * Returns a mapping of all topic hrefs to ITopics.
	 */
	private Map<String, ITopic> getHref2TopicMap() {
		if (href2TopicMap == null) {
			href2TopicMap = createHref2TopicMap();
		}
		return href2TopicMap;
	}

	@Override
	public String getLabel() {
		return getAttribute(ATTRIBUTE_LABEL);
	}

	public String getLinkTo() {
		return getAttribute(ATTRIBUTE_LINK_TO);
	}

	public String getTopic() {
		return getAttribute(ATTRIBUTE_TOPIC);
	}

	@Override
	public ITopic getTopic(String href) {
		if (href == null) {
			if (topic == null) {
				topic = new ITopic2() {

					@Override
					public String getHref() {
						return getTopic();
					}

					@Override
					public String getLabel() {
						return Toc.this.getLabel();
					}

					@Override
					public ITopic[] getSubtopics() {
						return getTopics();
					}

					@Override
					public boolean isEnabled(IEvaluationContext context) {
						return Toc.this.isEnabled(context);
					}

					@Override
					public IUAElement[] getChildren() {
						return new IUAElement[0];
					}

					@Override
					public ICriteria[] getCriteria() {
						return Toc.this.getCriteria();
					}

					@Override
					public String getIcon() {
						return null;
					}

					@Override
					public boolean isSorted() {
						return false;
					}
				};
			}
			return topic;
		} else {
			return getHref2TopicMap().get(href);
		}
	}

	@Override
	public ITopic[] getTopics() {
		return getChildren(ITopic.class);
	}

	@Override
	public ICriteria[] getCriteria() {
		return getChildren(ICriteria.class);
	}

	public void setLabel(String label) {
		setAttribute(ATTRIBUTE_LABEL, label);
	}

	public void setLinkTo(String linkTo) {
		setAttribute(ATTRIBUTE_LINK_TO, linkTo);
	}

	public void setTopic(String href) {
		setAttribute(ATTRIBUTE_TOPIC, href);
	}

	public void setHref(String href) {
		setAttribute(ATTRIBUTE_HREF, href);
	}

	public ITocContribution getTocContribution() {
		return contribution;
	}

	public void setTocContribution(ITocContribution contribution) {
		this.contribution = contribution;
	}

}
