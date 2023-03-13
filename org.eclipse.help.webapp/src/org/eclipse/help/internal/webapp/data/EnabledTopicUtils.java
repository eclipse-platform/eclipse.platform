/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
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

import org.eclipse.help.IIndexEntry;
import org.eclipse.help.IIndexEntry2;
import org.eclipse.help.IIndexSee;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.UAContentFilter;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.HelpEvaluationContext;

/**
 * Utilities to test for enabled topics, index entries etc.
 */

public class EnabledTopicUtils {

	/**
	 * Test whether a topic is enabled
	 * @param topic
	 * @return
	 */
	public static boolean isEnabled(ITopic topic) {
		if (!topic.isEnabled(HelpEvaluationContext.getContext())) {
			return false;
		}
		if (topic.getHref() != null) {
			return true;
		}
		return hasEnabledSubtopic(topic);
	}

	public static boolean hasEnabledSubtopic(ITopic topic) {
		ITopic[] subtopics = topic.getSubtopics();
		for (ITopic subtopic : subtopics) {
			if (isEnabled(subtopic)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Test whether a toc is enabled
	 * @param topic
	 * @return
	 */
	public static boolean isEnabled(IToc toc) {
		if (!HelpBasePlugin.getActivitySupport().isEnabled(toc.getHref()) ||
			UAContentFilter.isFiltered(toc, HelpEvaluationContext.getContext())) {
			return false;
		}
		// A toc is enabled only if at least one subtopic is enabled
		ITopic[] subtopics = toc.getTopics();
		for (ITopic subtopic : subtopics) {
			if (isEnabled(subtopic)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Test whether an entry is enabled
	 * @param entry
	 * @return
	 */
	public static boolean isEnabled(IIndexEntry entry) {
		if (UAContentFilter.isFiltered(entry, HelpEvaluationContext.getContext())) {
			return false;
		}
		ITopic[] topics = entry.getTopics();
		for (ITopic topic : topics) {
			if (isEnabled(topic)) {
				return true;
			}
		}
		IIndexEntry[] subentries = entry.getSubentries();
		for (IIndexEntry subentrie : subentries) {
			if (isEnabled(subentrie)) {
				return true;
			}
		}
		if (entry instanceof IIndexEntry2 indexEntry) {
			IIndexSee[] sees = indexEntry.getSees();
			for (IIndexSee see : sees) {
				if (isEnabled(see)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isEnabled(IIndexSee see) {
		return see.isEnabled(HelpEvaluationContext.getContext());
	}

	/**
	 * Filter out any disabled entries from an array
	 * @param entries an array of entries
	 * @return an array containing only those entries which are enabled
	 */
	public static IIndexEntry[] getEnabled(IIndexEntry[] entries) {
		for (int i=0;i<entries.length;++i) {
			if (!isEnabled(entries[i])) {
				List<IIndexEntry> list = new ArrayList<>(entries.length);
				for (int j=0;j<entries.length;++j) {
					if (j < i || isEnabled(entries[j])) {
						list.add(entries[j]);
					}
				}
				return list.toArray(new IIndexEntry[list.size()]);
			}
		}
		return entries;
	}

	/**
	 * Filter out any disable topics form an array
	 * @param topics an array of topics
	 * @return an array containing only those topics which are enabled
	 */
	public static ITopic[] getEnabled(ITopic[] topics) {
		for (int i=0;i<topics.length;++i) {
			if (!isEnabled(topics[i])) {
				List<ITopic> list = new ArrayList<>(topics.length);
				for (int j=0;j<topics.length;++j) {
					if (j < i || isEnabled(topics[j])) {
						list.add(topics[j]);
					}
				}
				return list.toArray(new ITopic[list.size()]);
			}
		}
		return topics;
	}

}
