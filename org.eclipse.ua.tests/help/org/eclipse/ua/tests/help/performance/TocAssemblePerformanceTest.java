/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.performance;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.help.ITopic;
import org.eclipse.help.internal.toc.TocAssembler;
import org.eclipse.help.internal.toc.TocContribution;
import org.eclipse.help.internal.toc.TocFile;
import org.eclipse.help.internal.toc.TocFileParser;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCase;
import org.osgi.framework.FrameworkUtil;
import org.xml.sax.SAXException;

public class TocAssemblePerformanceTest extends PerformanceTestCase {

	private TocContribution parse(TocFileParser parser, String tocFile)
			throws IOException, SAXException, ParserConfigurationException {
		return parser.parse(
				new TocFile(FrameworkUtil.getBundle(getClass()).getSymbolicName(), tocFile, true, "en", null, null));
	}

	public void assembleToc() throws Exception {
		TocFileParser parser = new TocFileParser();
		List<TocContribution> contributions = new ArrayList<>();
		contributions.add(parse(parser, "data/help/performance/org.eclipse.jdt.doc.isv/jdttoc.xml"));
		contributions.add(parse(parser, "data/help/performance/org.eclipse.jdt.doc.isv/topics_Guide.xml"));
		contributions.add(parse(parser, "data/help/performance/org.eclipse.jdt.doc.isv/topics_Porting.xml"));
		contributions.add(parse(parser, "data/help/performance/org.eclipse.jdt.doc.isv/topics_Questions.xml"));
		contributions.add(parse(parser, "data/help/performance/org.eclipse.jdt.doc.isv/topics_Reference.xml"));
		contributions.add(parse(parser, "data/help/performance/org.eclipse.jdt.doc.isv/topics_Samples.xml"));
		contributions.add(parse(parser, "data/help/performance/org.eclipse.jdt.doc.isv/topics_Samples.xml"));
		TocAssembler assembler = new TocAssembler();
		contributions = assembler.assemble(contributions);
		assertEquals(1, contributions.size());
		TocContribution toc = contributions.get(0);
		assertEquals(101, countTopics(toc.getToc().getTopics()));
	}


	private int countTopics(ITopic[] topics) {
		int result = topics.length;
		for (ITopic topic : topics) {
			result = result + countTopics(topic.getSubtopics());
		}
		return result;
	}

	public void testTocAssemble() throws Exception {
		tagAsSummary("Assemble TOC", Dimension.ELAPSED_PROCESS);

		// run the tests
		for (int i=0; i < 100; ++i) {
			boolean warmup = i < 2;
			if (!warmup) {
				startMeasuring();
			}
			for (int j = 0; j < 20; j++) {
				assembleToc();
			}
			if (!warmup) {
				stopMeasuring();
			}
		}

		commitMeasurements();
		assertPerformance();
	}


}
