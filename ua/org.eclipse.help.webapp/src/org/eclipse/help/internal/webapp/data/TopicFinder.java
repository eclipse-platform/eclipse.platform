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
import java.util.StringTokenizer;

import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.base.scope.ScopeUtils;

/**
 * Class for finding a topic in a set of TOCs based on its href.  Some of this code was
 * refactored from the TocData class
 */

public class TopicFinder {

	private ITopic[] foundTopicPath;
	private int selectedToc;
	private IToc[] tocs;
	private String numericPath = null;
	private AbstractHelpScope scope;

	public TopicFinder(String topicHref, IToc[] tocs, AbstractHelpScope scope) {
		this.tocs = tocs;
		this.scope = scope;
		if (topicHref != null && topicHref.length() > 0) {
			int index = -1;
			do {
				selectedToc = findTocContainingTopic(topicHref);
				index = topicHref.indexOf("/nav/"); //$NON-NLS-1$
				if (index != -1) {
					foundTopicPath = getTopicPathFromNav(topicHref.substring(index + 5));

				} else {
					ITopic topic = findTopic(UrlUtil.getHelpURL(topicHref));
					if (topic != null && selectedToc >= 0) {
						foundTopicPath = getTopicPathInToc(topic,
								tocs[selectedToc]);
					}
				}
				// if no match has been found, check if there is an anchor
				if (foundTopicPath == null) {
					index = topicHref.indexOf('#');
					if (index != -1)
						topicHref = topicHref.substring(0, index);
				}
				// if there was an anchor, search again without it
			} while (foundTopicPath == null && index != -1);
		} else {
			selectedToc = -1;
			foundTopicPath = null;
		}
	}

	public ITopic[] getTopicPathFromNav(String nav) {
		StringTokenizer tok = new StringTokenizer(nav, "_"); //$NON-NLS-1$
		try {
			int segments = tok.countTokens();
			ITopic[] path = new ITopic[segments - 1];
			// first number is toc index
			int index = Integer.parseInt(tok.nextToken());
			IToc toc = tocs[index];
			ITopic current = toc.getTopic(null);
			for (int i = 0; tok.hasMoreTokens(); i++) {
				index = Integer.parseInt(tok.nextToken());
				appendFilteredIndex(index, current.getSubtopics());
				current = current.getSubtopics()[index];
				path[i] = current;
			}
			return path;
		} catch (Exception e) {
			numericPath = null;
			return null;
		}
	}

	public ITopic[] getTopicPath() {
		return foundTopicPath;
	}

	public int getSelectedToc() {
		return selectedToc;
	}

	public String getNumericPath() {
		return numericPath;
	}

	/*
	 * Finds a path of ITopics in the given IToc to the given topic. If the
	 * toc doesn't contain the topic, returns null.
	 */
	private ITopic[] getTopicPathInToc(ITopic topicToFind, IToc toc) {
		if (topicToFind.getLabel().equals(toc.getLabel())) {
			return new ITopic[0];
		}
		ITopic topics[] = toc.getTopics();
		if (topics != null) {
			for (int i = 0; i < topics.length; ++i) {
				// returns path in reverse order
				List<ITopic> reversePath = getTopicPathInTopic(topicToFind, topics[i]);
				if (reversePath != null) {
					prependFilteredIndex(i, topics);
					return invertPath(reversePath);
				}
			}
		}
		return null;
	}

	private ITopic[] invertPath(List<ITopic> reversePath) {
		// reverse and return
		ITopic[] path = new ITopic[reversePath.size()];
		for (int j = 0; j < path.length; ++j) {
			path[j] = reversePath.get((path.length - 1)
					- j);
		}
		return path;
	}

	private boolean sameTopic(ITopic topicToFind, ITopic topic) {
		if (! topic.getLabel().equals(topicToFind.getLabel())) {
			return false;
		}
		if (topicToFind.getHref() == null) {
			return topic.getHref() == null;
		}
		return topicToFind.getHref().equals(topic.getHref());
	}

	/*
	 * Finds the topic in the given topic sub-tree. Returns a path of ITopics to
	 * that topic in reverse order (from the topic up).
	 */
	private List<ITopic> getTopicPathInTopic(ITopic topicToFind, ITopic topic) {
		if (sameTopic(topicToFind, topic)) {
			// found it. start the list to be created recursively
			List<ITopic> path = new ArrayList<>();
			path.add(topic);
			return path;
		} else {
			ITopic[] subtopics = topic.getSubtopics();
			for (int i = 0; i < subtopics.length; ++i) {
				List<ITopic> path = getTopicPathInTopic(topicToFind, subtopics[i]);
				if (path != null) {
					// it was in a subtopic.. add to the path and return
					path.add(topic);
					// Add to the numeric path counting only enabled topics
					prependFilteredIndex(i, subtopics);
					return path;
				}
			}
		}
		return null;
	}

	// Append an entry to the numeric path representing the position in the list
	// of filtered topics. Note that we need to convert the index in the unfiltered
	// list to an index in a filtered list of topics
	private void appendFilteredIndex(int indexInUnfilteredList, ITopic[] unfiltered) {
		int indexInFilteredList = 0;
		for (int i = 0; i < indexInUnfilteredList; i++) {
			if (ScopeUtils.showInTree(unfiltered[i], scope)) {
				indexInFilteredList++;
			}
		}

		if (numericPath == null) {
			numericPath = "" + indexInFilteredList; //$NON-NLS-1$
		} else {
			numericPath = numericPath  + '_' + indexInFilteredList;
		}
	}

	// Prepend an entry to the numeric path representing the position in the list
	// of filtered topics. Note that we need to convert the index in the unfiltered
	// list to an index in a filtered list of topics
	private void prependFilteredIndex(int indexInUnfilteredList, ITopic[] unfiltered) {
		int indexInFilteredList = 0;
		for (int i = 0; i < indexInUnfilteredList; i++) {
			if (ScopeUtils.showInTree(unfiltered[i], scope)) {
				indexInFilteredList++;
			}
		}

		if (numericPath == null) {
			numericPath = "" + indexInFilteredList; //$NON-NLS-1$
		} else {
			numericPath = "" + indexInFilteredList + '_' + numericPath; //$NON-NLS-1$
		}

	}

	/**
	 * Finds a TOC that contains specified topic
	 *
	 * @param topic
	 *            the topic href
	 * @return -1 if the toc is not found
	 */
	private int findTocContainingTopic(String topic) {
		if (topic == null || topic.isEmpty())
			return -1;

		int index = topic.indexOf("/topic/"); //$NON-NLS-1$
		if (index != -1) {
			topic = topic.substring(index + 6);
		} else {
			// auto-generated nav urls, e.g. "/help/nav/0_1_5"
			index = topic.indexOf("/nav/"); //$NON-NLS-1$
			if (index != -1) {
				// first number is toc index
				String nav = topic.substring(index + 5);
				String book;
				index = nav.indexOf('_');
				if (index == -1) {
					book = nav;
				} else {
					book = nav.substring(0, index);
				}

				try {
					return Integer.parseInt(book);
				} catch (Exception e) {
					// shouldn't happen
				}
			}
		}
		index = topic.indexOf('?');
		if (index != -1)
			topic = topic.substring(0, index);

		if (topic == null || topic.isEmpty())
			return -1;

		// try to find in enabled tocs first
		for (int i = 0; i < tocs.length; i++)
			if (isEnabled(tocs[i])) {
				if (tocs[i].getTopic(topic) != null) {
					return i;
				}
				ITopic tocTopic = tocs[i].getTopic(null);
				if (tocTopic != null && topic.equals(tocTopic.getHref())) {
					return i;
				}
			}
		// try disabled tocs second
		for (int i = 0; i < tocs.length; i++)
			if (!isEnabled(tocs[i]))
				if (tocs[i].getTopic(topic) != null)
					return i;
		// nothing found
		return -1;
	}

	/**
	 * Finds topic in a TOC
	 *
	 * @return ITopic or null
	 */
	private ITopic findTopic(String topic) {

		int index = topic.indexOf("/topic/"); //$NON-NLS-1$
		if (index != -1) {
			topic = topic.substring(index + 6);
		} else {
			// auto-generated nav urls, e.g. "/help/nav/0_1_5"
			index = topic.indexOf("/nav/"); //$NON-NLS-1$
			if (index != -1) {
				String nav = topic.substring(index + 5);
				StringTokenizer tok = new StringTokenizer(nav, "_"); //$NON-NLS-1$
				try {
					// first number is toc index
					index = Integer.parseInt(tok.nextToken());
					ITopic current = tocs[index].getTopic(null);
					while (tok.hasMoreTokens()) {
						index = Integer.parseInt(tok.nextToken());
						current = current.getSubtopics()[index];
					}
					return current;
				} catch (Exception e) {
					// shouldn't happen
				}
			}
		}
		index = topic.indexOf('?');
		if (index != -1)
			topic = topic.substring(0, index);

		if (topic == null || topic.isEmpty())
			return null;

		if (selectedToc < 0)
			return null;
		IToc selectedToc2 = tocs[selectedToc];
		if (selectedToc2 == null)
			return null;
		ITopic selectedTopic = selectedToc2.getTopic(topic);
		if (selectedTopic != null) {
			return selectedTopic;
		}

		ITopic tocTopic = selectedToc2.getTopic(null);
		if (tocTopic != null && topic.equals(tocTopic.getHref())) {
			return tocTopic;
		}
		return null;
	}

	/**
	 * Check if given TOC is visible and non empty
	 *
	 * @param toc
	 * @return true if TOC should be visible
	 */
	private boolean isEnabled(IToc toc) {
		boolean tocEnabled = ScopeUtils.showInTree(toc, scope);
		if (!tocEnabled)
			return false;
		ITopic[] topics = toc.getTopics();
		for (ITopic topic : topics) {
			if (ScopeUtils.showInTree(topic, scope)) {
				return true;
			}
		}
		return false;
	}
}
