/*******************************************************************************
 *  Copyright (c) 2009, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ua.tests.help.toc;

import static org.junit.Assert.assertEquals;

import org.eclipse.help.ITopic;
import org.eclipse.help.internal.toc.Toc;
import org.eclipse.help.internal.toc.TocContribution;
import org.eclipse.help.internal.toc.TocFile;
import org.eclipse.help.internal.toc.TocFileParser;
import org.eclipse.help.internal.toc.TopicSorter;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class TopicSortingTest {
	@Test
	public void testSortTocChildren() throws Exception {
		TocFileParser parser = new TocFileParser();
		TocContribution contribution = parser.parse(new TocFile(FrameworkUtil.getBundle(getClass()).getSymbolicName(),
				"data/help/toc/assembler/sorted.xml", true, "en", null, null));
		TopicSorter sorter = new TopicSorter();
		Toc toc = (Toc) contribution.getToc();
		sorter.sortChildren(toc);
		ITopic[] children = toc.getTopics();
		assertEquals("A Topic (sorted)", children[0].getLabel());
		assertEquals("B Topic", children[1].getLabel());
		assertEquals("C Topic", children[2].getLabel());
	}

	@Test
	public void testSortNestedTopics() throws Exception {
		TocFileParser parser = new TocFileParser();
		TocContribution contribution = parser.parse(new TocFile(FrameworkUtil.getBundle(getClass()).getSymbolicName(),
				"data/help/toc/assembler/sorted.xml", true, "en", null, null));
		TopicSorter sorter = new TopicSorter();
		Toc toc = (Toc) contribution.getToc();
		sorter.sortChildren(toc);
		ITopic[] children = toc.getTopics();
		ITopic topicA = children[0];
		assertEquals("A Topic (sorted)", topicA.getLabel());
		ITopic[] childrenOfA = topicA.getSubtopics();
		assertEquals("A Child", childrenOfA[0].getLabel());
		assertEquals("B Child", childrenOfA[1].getLabel());
	}

	@Test
	public void testUnsortedNestedTopics() throws Exception {
		TocFileParser parser = new TocFileParser();
		TocContribution contribution = parser.parse(new TocFile(FrameworkUtil.getBundle(getClass()).getSymbolicName(),
				"data/help/toc/assembler/sorted.xml", true, "en", null, null));
		TopicSorter sorter = new TopicSorter();
		Toc toc = (Toc) contribution.getToc();
		sorter.sortChildren(toc);
		ITopic[] children = toc.getTopics();
		ITopic topicC = children[2];
		assertEquals("C Topic", topicC.getLabel());
		ITopic[] childrenOfC = topicC.getSubtopics();
		assertEquals("B Child of C", childrenOfC[0].getLabel());
		assertEquals("A Child of C", childrenOfC[1].getLabel());
	}

}
