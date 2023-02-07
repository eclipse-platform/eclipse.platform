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
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.index.Index;
import org.eclipse.help.internal.index.IndexAssembler;
import org.eclipse.help.internal.index.IndexContribution;
import org.eclipse.help.internal.index.IndexFile;
import org.eclipse.help.internal.index.IndexFileParser;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCase;
import org.osgi.framework.FrameworkUtil;
import org.xml.sax.SAXException;

public class IndexAssemblePerformanceTest extends PerformanceTestCase {

	public void testIndexAssemble() throws Exception {
		tagAsSummary("Assemble Index", Dimension.ELAPSED_PROCESS);

		// run the tests
		for (int i=0; i < 10; ++i) {
			boolean warmup = i == 0;
			if (!warmup) {
				startMeasuring();
			}

			assembleIndex();

			if (!warmup) {
				stopMeasuring();
			}
		}

		commitMeasurements();
		assertPerformance();
	}

	private void assembleIndex() throws IOException, SAXException,
			ParserConfigurationException {
		IndexFileParser parser = new IndexFileParser();
		String bsn = FrameworkUtil.getBundle(getClass()).getSymbolicName();
		IndexContribution a = parser.parse(new IndexFile(bsn, "data/help/performance/index/index1.xml", "en"));
		IndexContribution b = parser.parse(new IndexFile(bsn, "data/help/performance/index/index2.xml", "en"));
		IndexContribution c = parser.parse(new IndexFile(bsn, "data/help/performance/index/index3.xml", "en"));
		IndexAssembler assembler = new IndexAssembler();
		List<IndexContribution> contributions = new ArrayList<>(Arrays.asList(a, b, c));
		Index assembled = assembler.assemble(contributions, Platform.getNL());
		assertEquals(100, assembled.getChildren().length);
	}

}
