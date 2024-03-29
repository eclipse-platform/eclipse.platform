/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.IIndexEntry;
import org.eclipse.help.IIndexEntry2;
import org.eclipse.help.IIndexSee;
import org.eclipse.help.ITopic;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.dynamic.DocumentWriter;
import org.eclipse.help.internal.index.Index;
import org.eclipse.help.internal.index.IndexAssembler;
import org.eclipse.help.internal.index.IndexContribution;
import org.eclipse.help.internal.index.IndexEntry;
import org.eclipse.help.internal.index.IndexFile;
import org.eclipse.help.internal.index.IndexFileParser;
import org.eclipse.help.internal.index.IndexSee;
import org.junit.jupiter.api.Test;
import org.osgi.framework.FrameworkUtil;
import org.xml.sax.SAXParseException;

public class IndexAssemblerTest {
	@Test
	public void testAssemble() throws Exception {
		IndexFileParser parser = new IndexFileParser();
		String bsn = FrameworkUtil.getBundle(getClass()).getSymbolicName();
		IndexContribution a = parser.parse(new IndexFile(bsn, "data/help/index/assembler/a.xml", "en"));
		IndexContribution b = parser.parse(new IndexFile(bsn, "data/help/index/assembler/b.xml", "en"));
		IndexContribution c = parser.parse(new IndexFile(bsn, "data/help/index/assembler/c.xml", "en"));
		IndexContribution result_a_b_c = parser
				.parse(new IndexFile(bsn, "data/help/index/assembler/result_a_b_c.xml", "en"));

		IndexAssembler assembler = new IndexAssembler();
		List<IndexContribution> contributions = new ArrayList<>(Arrays.asList(a, b, c));
		Index assembled = assembler.assemble(contributions, Platform.getNL());

		String expected = serialize((UAElement)result_a_b_c.getIndex());
		String actual = serialize(assembled);
		assertEquals(trimWhiteSpace(expected), trimWhiteSpace(actual));
	}

	@Test
	public void testAssembleWithSeeAlso() throws Exception {
		IndexFileParser parser = new IndexFileParser();
		IndexContribution contrib = parser.parse(new IndexFile(FrameworkUtil.getBundle(getClass()).getSymbolicName(),
				"data/help/index/assembler/d.xml", "en"));
		IndexAssembler assembler = new IndexAssembler();
		List<IndexContribution> contributions = new ArrayList<>(Arrays.asList(contrib));
		Index index = assembler.assemble(contributions, Platform.getNL());
		IIndexEntry[] children = index.getEntries();
		assertThat(children).hasSize(2);
		IIndexEntry eclipseEntry = children[0];
		assertEquals("eclipse", eclipseEntry.getKeyword());
		IUAElement[] eclipseChildren = eclipseEntry.getChildren();
		assertThat(eclipseChildren).hasSize(4);
		assertTrue(eclipseChildren[0] instanceof Topic);
		assertTrue(eclipseChildren[1] instanceof IndexEntry);
		assertTrue(eclipseChildren[2] instanceof IndexSee);
		assertTrue(eclipseChildren[3] instanceof IndexSee);
		IndexSee seeHelios = (IndexSee) eclipseChildren[2];
		IndexSee seeHeliosRelease = (IndexSee) eclipseChildren[3];
		assertThat(seeHelios.getSubpathElements()).isEmpty();
		assertThat(seeHeliosRelease.getSubpathElements()).hasSize(1);
		IIndexEntry heliosEntry = children[1];
		assertEquals("helios", heliosEntry.getKeyword());
		IIndexSee[] heliosSees = ((IIndexEntry2)heliosEntry).getSees();
		assertThat(heliosSees).hasSize(1);
		assertEquals("eclipse", heliosSees[0].getKeyword());
	}

	@Test
	public void testTitle() throws Exception{
		IndexFileParser parser = new IndexFileParser();
		IndexContribution contrib = parser.parse(new IndexFile(FrameworkUtil.getBundle(getClass()).getSymbolicName(),
				"data/help/index/assembler/hasTitle.xml", "en"));
		IndexAssembler assembler = new IndexAssembler();
		List<IndexContribution> contributions = new ArrayList<>(Arrays.asList(contrib));
		Index index = assembler.assemble(contributions, Platform.getNL());
		IIndexEntry[] children = index.getEntries();
		assertThat(children).hasSize(1);
		assertEquals("keyword1", children[0].getKeyword());
		ITopic[] topics = children[0].getTopics();
		assertThat(topics).hasSize(3);
		assertEquals("topic0", topics[0].getLabel());
		assertEquals("topic1", topics[1].getLabel());
		assertEquals("topic2", topics[2].getLabel());
	}

	@Test
	public void testInvalid() throws Exception {
		IndexFileParser parser = new IndexFileParser();
		assertThrows(SAXParseException.class, () -> {
			IndexContribution contrib = parser
					.parse(new IndexFile(FrameworkUtil.getBundle(getClass()).getSymbolicName(),
							"data/help/index/assembler/invalid.xml", "en"));
			IndexAssembler assembler = new IndexAssembler();
			List<IndexContribution> contributions = new ArrayList<>(Arrays.asList(contrib));
			assembler.assemble(contributions, Platform.getNL());
		});
	}

	// Replaces white space between ">" and "<" by a single newline

	private String trimWhiteSpace(String input) {
		StringBuilder result = new StringBuilder();
		boolean betweenElements = false;
		for (int i = 0; i < input.length(); i++) {
			char next = input.charAt(i);
			if (betweenElements) {
				if (!Character.isWhitespace(next)) {
					result.append(next);
					if (next == '<') {
						betweenElements = false;
					}
				}
			} else {
				result.append(next);
				if (next == '>') {
					betweenElements = true;
					result.append('\r');
					result.append('\n');
				}
			}
		}
		String resString = result.toString();
		return resString;
	}

	private String serialize(UAElement element) throws Exception {
		DocumentWriter writer = new DocumentWriter();
		return writer.writeString(element, true);
	}
}
