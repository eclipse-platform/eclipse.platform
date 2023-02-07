/*******************************************************************************
 * Copyright (c) 2010, 2017 IBM Corporation and others.
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
package org.eclipse.ua.tests.help.criteria;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.criteria.CriteriaDefinition;
import org.eclipse.help.internal.criteria.CriteriaDefinitionAssembler;
import org.eclipse.help.internal.criteria.CriteriaDefinitionContribution;
import org.eclipse.help.internal.criteria.CriteriaDefinitionFile;
import org.eclipse.help.internal.criteria.CriteriaDefinitionFileParser;
import org.eclipse.help.internal.dynamic.DocumentWriter;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class ParseCriteriaDefinition {
	@Test
	public void testAssemble() throws Exception {
		CriteriaDefinitionFileParser parser = new CriteriaDefinitionFileParser();
		String bsn = FrameworkUtil.getBundle(getClass()).getSymbolicName();
		CriteriaDefinitionContribution a = parser
				.parse(new CriteriaDefinitionFile(bsn, "data/help/criteria/criteria_definition/a.xml", "en"));
		CriteriaDefinitionContribution b = parser
				.parse(new CriteriaDefinitionFile(bsn, "data/help/criteria/criteria_definition/b.xml", "en"));
		CriteriaDefinitionContribution result_a_b = parser
				.parse(new CriteriaDefinitionFile(bsn, "data/help/criteria/criteria_definition/result_a_b.xml", "en"));

		CriteriaDefinitionAssembler assembler = new CriteriaDefinitionAssembler();
		List<CriteriaDefinitionContribution> contributions = new ArrayList<>(Arrays.asList(a, b));
		CriteriaDefinition assembled = assembler.assemble(contributions);

		String expected = serialize((UAElement)result_a_b.getCriteriaDefinition());
		String actual = serialize(assembled);
		assertEquals(trimWhiteSpace(expected), trimWhiteSpace(actual));
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
