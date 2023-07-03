/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.help.ICriteria;
import org.eclipse.help.IToc;
import org.eclipse.help.IToc2;
import org.eclipse.help.ITopic;
import org.eclipse.help.ITopic2;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.base.scope.CriteriaHelpScope;
import org.eclipse.help.internal.base.util.CriteriaUtilities;
import org.eclipse.help.internal.criteria.CriterionResource;
import org.eclipse.help.internal.toc.Toc;
import org.eclipse.help.internal.toc.TocContribution;
import org.eclipse.help.internal.toc.TocFile;
import org.eclipse.help.internal.toc.TocFileParser;
import org.eclipse.ua.tests.help.other.UserCriteria;
import org.eclipse.ua.tests.help.other.UserToc2;
import org.eclipse.ua.tests.help.other.UserTopic2;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;
import org.xml.sax.SAXException;

public class ParseTocWithCriteria {

	private IToc2 parseToc(String filename) throws IOException, SAXException,
			ParserConfigurationException {
		IToc toc;
		TocFileParser parser = new TocFileParser();
		TocContribution cToc = parser.parse(new TocFile(
				FrameworkUtil.getBundle(getClass()).getSymbolicName(), filename, true, "en",
				null, null));
		toc = cToc.getToc();
		return (IToc2) toc;
	}

	@Test
	public void testTocWithCriteria() throws Exception {
		IToc2 toc = parseToc("data/help/criteria/c1.xml");
		Map<String, Set<String>> criteria = new HashMap<>();
		CriteriaUtilities.addCriteriaToMap(criteria, toc.getCriteria());
		assertEquals(2, criteria.size());
		Set<String> versions = criteria.get("version");
		assertNotNull(versions);
		assertEquals(2, versions.size());
		assertTrue(versions.contains("1.0"));
		assertTrue(versions.contains("2.0"));

		Set<String> platforms = criteria.get("platform");
		assertNotNull(platforms);
		assertEquals(2, platforms.size());
		assertTrue(platforms.contains("linux"));
		assertTrue(platforms.contains("win32"));
	}

	@Test
	public void testCopyTocWithCriteria() throws Exception {
		IToc toc0 = parseToc("data/help/criteria/c1.xml");
		Toc toc = new Toc(toc0);
		Map<String, Set<String>> criteria = new HashMap<>();
		CriteriaUtilities.addCriteriaToMap(criteria, toc.getCriteria());
		assertEquals(2, criteria.size());
		Set<String> versions = criteria.get("version");
		assertNotNull(versions);
		assertEquals(2, versions.size());
		assertTrue(versions.contains("1.0"));
		assertTrue(versions.contains("2.0"));

		Set<String> platforms = criteria.get("platform");
		assertNotNull(platforms);
		assertEquals(2, platforms.size());
		assertTrue(platforms.contains("linux"));
		assertTrue(platforms.contains("win32"));
	}

	@Test
	public void testTopicWithCriteria() throws Exception {
		IToc toc = parseToc("data/help/criteria/c1.xml");
		ITopic[] topics = toc.getTopics();
		assertEquals(topics.length, 2);
		// First topic
		Map<String, Set<String>> criteria = new HashMap<>();
		assertTrue(topics[0] instanceof ITopic2);
		CriteriaUtilities.addCriteriaToMap(criteria, ((ITopic2)topics[0]).getCriteria());
		assertEquals(2, criteria.size());
		Set<String> versions = criteria.get("version");
		assertNotNull(versions);
		assertEquals(1, versions.size());
		assertTrue(versions.contains("1.0"));
		assertFalse(versions.contains("2.0"));

		// Second topic

		criteria = new HashMap<>();
		assertTrue(topics[1] instanceof ITopic2);
		CriteriaUtilities.addCriteriaToMap(criteria, ((ITopic2)topics[1]).getCriteria());
		versions = criteria.get("version");
		assertNotNull(versions);
		assertEquals(1, versions.size());
		assertTrue(versions.contains("2.0"));
		assertFalse(versions.contains("1.0"));
	}

	@Test
	public void testCriteriaScoping1() throws Exception {
		IToc toc = parseToc("data/help/criteria/c1.xml");
		ITopic[] topics = toc.getTopics();
		assertEquals(topics.length, 2);
		CriterionResource[] resource = new CriterionResource[1];
		resource[0] = new CriterionResource("version");
		resource[0].addCriterionValue("1.0");
		CriteriaHelpScope scope = new CriteriaHelpScope(resource);
		assertTrue(scope.inScope(toc));
		assertTrue(scope.inScope(topics[0]));
		assertFalse(scope.inScope(topics[1]));
	}

	@Test
	public void testCriteriaScoping2() throws Exception {
		IToc toc = parseToc("data/help/criteria/c1.xml");
		ITopic[] topics = toc.getTopics();
		assertEquals(topics.length, 2);
		CriterionResource[] resource = new CriterionResource[1];
		resource[0] = new CriterionResource("platform");
		resource[0].addCriterionValue("linux");
		CriteriaHelpScope scope = new CriteriaHelpScope(resource);
		assertTrue(scope.inScope(toc));
		assertTrue(scope.inScope(topics[0]));
		assertFalse(scope.inScope(topics[1]));
	}

	@Test
	public void testMultipleCriteriaScoping() throws Exception {
		IToc toc = parseToc("data/help/criteria/c1.xml");
		ITopic[] topics = toc.getTopics();
		assertEquals(topics.length, 2);
		CriterionResource[] resource = new CriterionResource[2];
		resource[0] = new CriterionResource("platform");
		resource[0].addCriterionValue("linux");
		resource[1] = new CriterionResource("version");
		resource[1].addCriterionValue("1.0");
		CriteriaHelpScope scope = new CriteriaHelpScope(resource);
		assertTrue(scope.inScope(toc));
		assertTrue(scope.inScope(topics[0]));
		assertFalse(scope.inScope(topics[1]));
	}

	@Test
	public void testMultipleCriteriaOnlyOneSatisfied() throws Exception {
		IToc toc = parseToc("data/help/criteria/c1.xml");
		ITopic[] topics = toc.getTopics();
		CriterionResource[] resource = new CriterionResource[2];
		resource[0] = new CriterionResource("platform");
		resource[0].addCriterionValue("linux");
		resource[1] = new CriterionResource("version");
		resource[1].addCriterionValue("2.0");
		assertEquals(topics.length, 2);
		CriteriaHelpScope scope = new CriteriaHelpScope(resource);
		assertTrue(scope.inScope(toc));
		assertFalse(scope.inScope(topics[0]));
		assertFalse(scope.inScope(topics[1]));
	}

	@Test
	public void testUserTocWithCriteria() throws Exception {
		UserToc2 toc = new UserToc2("myToc", null, true);
		UserCriteria criterion1 = new UserCriteria("version", "1.0", true);
		UserCriteria criterion2 = new UserCriteria("version", "2.0", true);
		toc.addCriterion(criterion1);
		toc.addCriterion(criterion2);

		ICriteria[] criteria = toc.getCriteria();
		assertEquals(2, criteria.length);
		assertEquals("version", criteria[0].getName());
		assertEquals("1.0", criteria[0].getValue());
		assertEquals("version", criteria[1].getName());
		assertEquals("2.0", criteria[1].getValue());
	}

	@Test
	public void testCopyUserTocWithCriteria() throws Exception {
		UserToc2 toc = new UserToc2("myToc", null, true);
		UserCriteria criterion1 = new UserCriteria("version", "1.0", true);
		UserCriteria criterion2 = new UserCriteria("version", "2.0", true);
		toc.addCriterion(criterion1);
		toc.addCriterion(criterion2);

		Toc copy = new Toc(toc);

		ICriteria[] criteria = copy.getCriteria();
		assertEquals(2, criteria.length);
		assertEquals("version", criteria[0].getName());
		assertEquals("1.0", criteria[0].getValue());
		assertEquals("version", criteria[1].getName());
		assertEquals("2.0", criteria[1].getValue());
	}

	@Test
	public void testUserTopicWithCriteria() throws Exception {
		UserTopic2 topic = new UserTopic2("myToc", null, true);
		UserCriteria criterion1 = new UserCriteria("version", "1.0", true);
		UserCriteria criterion2 = new UserCriteria("version", "2.0", true);
		topic.addCriterion(criterion1);
		topic.addCriterion(criterion2);

		Topic copy = new Topic(topic);

		ICriteria[] criteria = copy.getCriteria();
		assertEquals(2, criteria.length);
		assertEquals("version", criteria[0].getName());
		assertEquals("1.0", criteria[0].getValue());
		assertEquals("version", criteria[1].getName());
		assertEquals("2.0", criteria[1].getValue());
	}

	@Test
	public void testCopyUserTopicWithCriteria() throws Exception {
		UserTopic2 topic = new UserTopic2("myToc", null, true);
		UserCriteria criterion1 = new UserCriteria("version", "1.0", true);
		UserCriteria criterion2 = new UserCriteria("version", "2.0", true);
		topic.addCriterion(criterion1);
		topic.addCriterion(criterion2);
		ICriteria[] criteria = topic.getCriteria();
		assertEquals(2, criteria.length);
		assertEquals("version", criteria[0].getName());
		assertEquals("1.0", criteria[0].getValue());
		assertEquals("version", criteria[1].getName());
		assertEquals("2.0", criteria[1].getValue());
	}

	@Test
	public void testMultipleValues() throws Exception {
		IToc toc = parseToc("data/help/criteria/c2.xml");

		CriterionResource[] linuxResource = new CriterionResource[1];
		linuxResource[0] = new CriterionResource("platform");
		linuxResource[0].addCriterionValue("linux");
		CriteriaHelpScope linuxScope = new CriteriaHelpScope(linuxResource);
		assertTrue(linuxScope.inScope(toc));

		CriterionResource[] win32Resource = new CriterionResource[1];
		win32Resource[0] = new CriterionResource("platform");
		win32Resource[0].addCriterionValue("win32");
		CriteriaHelpScope win32scope = new CriteriaHelpScope(win32Resource);
		assertTrue(win32scope.inScope(toc));
	}

	@Test
	public void testValuesOfDifferentCases() throws Exception {
		IToc toc = parseToc("data/help/criteria/c2.xml");
		ITopic[] topics = toc.getTopics();

		CriterionResource[] linuxResource = new CriterionResource[1];
		linuxResource[0] = new CriterionResource("platform");
		linuxResource[0].addCriterionValue("linux");
		CriteriaHelpScope linuxScope = new CriteriaHelpScope(linuxResource);
		assertFalse(linuxScope.inScope(topics[0]));
	}

	@Test
	public void testValuesWithWhitespace() throws Exception {
		IToc toc = parseToc("data/help/criteria/c2.xml");
		ITopic[] topics = toc.getTopics();

		CriterionResource[] win32Resource = new CriterionResource[1];
		win32Resource[0] = new CriterionResource("platform");
		win32Resource[0].addCriterionValue("win32");
		CriteriaHelpScope win32Scope = new CriteriaHelpScope(win32Resource);
		assertTrue(win32Scope.inScope(topics[1]));
	}

	@Test
	public void testNoName() throws Exception {
		IToc toc = parseToc("data/help/criteria/c2.xml");
		ITopic[] topics = toc.getTopics();

		CriterionResource[] win32Resource = new CriterionResource[1];
		win32Resource[0] = new CriterionResource("platform");
		win32Resource[0].addCriterionValue("win32");
		CriteriaHelpScope win32Scope = new CriteriaHelpScope(win32Resource);
		assertFalse(win32Scope.inScope(topics[2]));
	}

	@Test
	public void testNoValue() throws Exception {
		IToc toc = parseToc("data/help/criteria/c2.xml");
		ITopic[] topics = toc.getTopics();

		CriterionResource[] win32Resource = new CriterionResource[1];
		win32Resource[0] = new CriterionResource("platform");
		win32Resource[0].addCriterionValue("win32");
		CriteriaHelpScope win32Scope = new CriteriaHelpScope(win32Resource);
		assertFalse(win32Scope.inScope(topics[3]));
	}

	@Test
	public void testNoCriteria() throws Exception {
		IToc toc = parseToc("data/help/criteria/c2.xml");
		ITopic[] topics = toc.getTopics();

		CriterionResource[] win32Resource = new CriterionResource[1];
		win32Resource[0] = new CriterionResource("platform");
		win32Resource[0].addCriterionValue("win32");
		CriteriaHelpScope win32Scope = new CriteriaHelpScope(win32Resource);
		assertFalse(win32Scope.inScope(topics[4]));
	}

}
