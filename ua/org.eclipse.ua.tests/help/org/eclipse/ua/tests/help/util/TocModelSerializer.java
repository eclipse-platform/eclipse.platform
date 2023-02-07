/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.util;

import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.UAContentFilter;
import org.eclipse.help.internal.base.HelpEvaluationContext;

public class TocModelSerializer {

	private static final String EMPTY_STRING = "";

	public static String serialize(IToc toc) {
		StringBuilder buf = new StringBuilder();
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
		buf.append(serializeAux(toc, ""));
		return buf.toString();
	}

	private static String serializeAux(IToc toc, String indent) {
		if (!UAContentFilter.isFiltered(toc, HelpEvaluationContext.getContext())) {
			StringBuilder buf = new StringBuilder();
			buf.append(indent + "<toc\n");
			buf.append(indent + "      label=\"" + toc.getLabel() + "\"\n");
			buf.append(indent + "      href=\"" + toc.getHref() + "\">\n");

			ITopic[] topics = toc.getTopics();
			for (ITopic topic : topics) {
				buf.append(serializeAux(topic, indent + "   "));
			}

			buf.append(indent + "</toc>");
			return buf.toString();
		}
		return EMPTY_STRING;
	}

	private static String serializeAux(ITopic topic, String indent) {
		if (!UAContentFilter.isFiltered(topic, HelpEvaluationContext.getContext())) {
			StringBuilder buf = new StringBuilder();
			buf.append(indent + "<topic\n");
			buf.append(indent + "      label=\"" + topic.getLabel() + "\"\n");
			buf.append(indent + "      href=\"" + topic.getHref() + "\">\n");

			ITopic[] subtopics = topic.getSubtopics();
			for (ITopic subtopic : subtopics) {
				buf.append(serializeAux(subtopic, indent + "   "));
			}

			buf.append(indent + "</topic>\n");
			return buf.toString();
		}
		return EMPTY_STRING;
	}
}
