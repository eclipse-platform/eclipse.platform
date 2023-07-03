/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.other;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.eclipse.help.ITopic;
import org.eclipse.help.internal.toc.Toc;
import org.eclipse.ua.tests.help.util.DocumentCreator;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ConcurrentTocAccess {

	private boolean checkAttributes = true;

	// Set enableTimeout to false for debugging
	private boolean enableTimeout = true;

	private static class TocGenerator {
		private int[] dimensions;
		private StringBuilder result;

		public String generateToc(int dimensions[]) {
			this.dimensions = dimensions;
			result = new StringBuilder();
			result.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			result.append("<?NLS TYPE=\"org.eclipse.help.toc\"?>\n");
			result.append("<toc label=\"Test Toc\" >\n");
			generateTopics(0);
			result.append("</toc>");
			return result.toString();
		}

		private void generateTopics(int depth) {
			if (depth >= dimensions.length) {
				return;
			}
			int numChildren = dimensions[depth];
			for (int i = 0; i < numChildren; i++) {
				result.append("<topic label=\"topicLabel"  + i + "\" href=\"page" + i + ".html\">\n");
				generateTopics(depth + 1);
				result.append("</topic>\n");
			}
		}
	}

	/*
	 * Class which visits every topic in a TOC
	 */
	private class TocVisitor extends Thread {
		private final Toc toc;
		private int leafCount = -2;
		public Exception exception;

		TocVisitor(Toc toc) {
			this.toc = toc;
		}

		@Override
		public void run() {
			try {
				int result = traverseToc(toc);
				setLeafCount(result);
			} catch (Exception e) {
				setLeafCount(-1);
				this.exception = e;
			}
		}

		synchronized public void setLeafCount(int leafCount) {
			this.leafCount = leafCount;
		}

		synchronized public int getLeafCount() {
			return leafCount;
		}
	}

	private static class BadHrefException extends RuntimeException {
		private static final long serialVersionUID = 410319402417607912L;
	}
	private static class BadLabelException extends RuntimeException {
		private static final long serialVersionUID = -4581518572807575035L;
	}

	private void accessInParallel(int[] dimensions, int numberOfThreads) throws Exception {
		Toc toc = createToc(dimensions);
		int expectedLeafCount = computeNumberOfLeafTopics(dimensions);
		TocVisitor[] visitors = new TocVisitor[numberOfThreads];
		for (int i = 0; i < numberOfThreads; i++) {
			visitors[i] = new TocVisitor(toc);
		}
		for (int i = 0; i < numberOfThreads; i++) {
			visitors[i].start();
		}
		// Now wait for the threads to complete
		boolean complete = false;
		int iterations = 0;
		do {
			complete = true;
			iterations++;
			if (enableTimeout && iterations > 100) {
				fail("Test did not complete within 10 seconds");
			}
			for (int i = 0; i < numberOfThreads; i++) {
				if (visitors[i].isAlive()) {
					complete = false;
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						fail("Interrupted Exception");
					}
				}
			}
		} while (!complete);
		for (int i = 0; i < numberOfThreads; i++) {
			if (visitors[i].exception != null) {
				throw visitors[i].exception;
			}
			assertEquals(expectedLeafCount, visitors[i].getLeafCount());
		}
	}

	// Visit every child of a TOC and count the number of leaf topics
	private int traverseToc(Toc toc) {
		int leafNodes = 0;
		ITopic[] children = toc.getTopics();
		for (int i = 0; i < children.length; i++) {
			leafNodes += traverseTopic(children[i], i);
		}
		return leafNodes;
	}

	private int computeNumberOfLeafTopics(int[] dimensions) {
		int expectedLeaves = 1;
		for (int dimension : dimensions) {
			expectedLeaves = expectedLeaves * dimension;
		}
		return expectedLeaves;
	}

	private int traverseTopic(ITopic topic, int index) {
		if (checkAttributes) {
			String expectedLabel = "topicLabel" + index;
			String expectedHref = "page" + index + ".html";
			String label = topic.getLabel();
			String href = topic.getHref();
			if (!label.equals(expectedLabel)) {
				throw new BadLabelException();
			}
			if (!href.equals(expectedHref)) {
				throw new BadHrefException();
			}
		}
		ITopic[] children = topic.getSubtopics();
		if (children.length == 0) {
			return 1;
		}
		int leafNodes = 0;
		for (int i = 0; i < children.length; i++) {
			leafNodes += traverseTopic(children[i], i);
		}
		return leafNodes;
	}

	private Toc createToc(int[] dimensions) {
		String tocSource = new TocGenerator().generateToc(dimensions);
		Toc toc;
		Document doc;
		try {
			doc = DocumentCreator.createDocument(tocSource);
		} catch (Exception e) {
			fail("Exception creating TOC");
			doc = null;
		}
		Element tocElement = (Element) doc.getElementsByTagName("toc").item(0);
		toc = new Toc(tocElement);
		return toc;
	}

	@Test
	public void testFlatTocSize5() throws Exception {
		int[] dimensions = {5};
		accessInParallel(dimensions, 2);
	}

	@Test
	public void testFlatTocSize1000() throws Exception {
		int[] dimensions = {1000};
		accessInParallel(dimensions, 2);
	}

	@Test
	public void testFlatTocSize10000() throws Exception {
		int[] dimensions = {10000};
		accessInParallel(dimensions, 2);
	}

	@Test
	public void testTwoLevelToc() throws Exception {
		int[] dimensions = {50, 50};
		accessInParallel(dimensions, 2);
	}

	@Test
	public void testDeepToc() throws Exception {
		int[] dimensions = {2,2,2,2,2,2,2,2,2,2,2};
		accessInParallel(dimensions, 2);
	}

	@Test
	public void testFlatTocManyThreads() throws Exception {
		int[] dimensions = {100};
		accessInParallel(dimensions, 100);
	}

}
