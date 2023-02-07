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
package org.eclipse.help.internal.toc;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.help.ITopic;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.UAElement;


/*
 * Handles the "sort" attribute on topics and tocs
 */
public class TopicSorter {

	private TopicComparator comparator;

	public void sortChildren(Toc toc) {
		if (comparator == null) {
			comparator = new TopicComparator();
		}
		if (toc.isSorted()) {
			sort(toc, toc.getTopics());
		}
		ITopic[] childTopics = toc.getTopics();
		for (int i = 0; i < childTopics.length; i++) {
			sortChildren((Topic)childTopics[i]);
		}
	}

	private void sortChildren(Topic topic) {
		if (topic.isSorted()) {
			sort(topic, topic.getSubtopics());
		}
		ITopic[] childTopics = topic.getSubtopics();
		for (int i = 0; i < childTopics.length; i++) {
			sortChildren((Topic)childTopics[i]);
		}
	}

	private static class TopicComparator implements Comparator<ITopic> {
		Collator collator = Collator.getInstance();

		@Override
		public int compare(ITopic o1, ITopic o2) {
			String label1 = o1.getLabel();
			String label2 = o2.getLabel();
			return collator.compare(label1, label2);
		}
	}

	/*
	 * Sort the given node's descendants recursively using the given
	 * Comparator.
	 */
	private void sort(UAElement element, ITopic[] children) {
		// sort children
		if (children.length > 1) {
			for (int i=0;i<children.length;++i) {
				element.removeChild((UAElement)children[i]);
			}
			Arrays.sort(children, comparator);
			for (int i=0;i<children.length;++i) {
				element.appendChild((UAElement)children[i]);
			}
		}
	}
}

