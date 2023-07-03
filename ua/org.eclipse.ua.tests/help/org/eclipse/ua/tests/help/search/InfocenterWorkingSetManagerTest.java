/*******************************************************************************
 *  Copyright (c) 2010, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.Cookie;

import org.eclipse.help.internal.criteria.CriterionResource;
import org.eclipse.help.internal.webapp.servlet.InfocenterWorkingSetManager;
import org.eclipse.help.internal.workingset.AdaptableHelpResource;
import org.eclipse.help.internal.workingset.AdaptableToc;
import org.eclipse.help.internal.workingset.AdaptableTopic;
import org.eclipse.help.internal.workingset.WorkingSet;
import org.eclipse.ua.tests.help.webapp.MockServletRequest;
import org.eclipse.ua.tests.help.webapp.MockServletResponse;
import org.junit.Test;

public class InfocenterWorkingSetManagerTest {
	@Test
	public void testIWSMWithToc() throws IOException {
		InfocenterWorkingSetManager mgr = new InfocenterWorkingSetManager
			(new MockServletRequest(), new MockServletResponse(), "en");
		WorkingSet wset = new WorkingSet("test");
		AdaptableToc toc = mgr.getAdaptableToc("/org.eclipse.ua.tests/data/help/toc/root.xml");
		assertNotNull(toc);
		wset.setElements(new AdaptableHelpResource[] { toc });
		mgr.addWorkingSet(wset);
		WorkingSet[] readWsets = mgr.getWorkingSets();
		assertEquals(1, readWsets.length);
		AdaptableHelpResource[] resources = readWsets[0].getElements();
		assertEquals(1, resources.length);
		assertTrue(resources[0].equals(toc));
	}

	@Test
	public void testSaveRestoreIWSMWithToc() throws IOException {
		MockServletRequest req = new MockServletRequest();
		MockServletResponse resp = new MockServletResponse();
		InfocenterWorkingSetManager mgr = new InfocenterWorkingSetManager(req, resp, "en");
		WorkingSet wset = new WorkingSet("test");
		AdaptableToc toc = mgr.getAdaptableToc("/org.eclipse.ua.tests/data/help/toc/root.xml");
		wset.setElements(new AdaptableHelpResource[] { toc });
		mgr.addWorkingSet(wset);
		MockServletRequest req2 = new MockServletRequest();
		MockServletResponse resp2 = new MockServletResponse();
		req2.setCookies(resp.getCookies());
		InfocenterWorkingSetManager mgr2 = new InfocenterWorkingSetManager(req2, resp2, "en");
		WorkingSet[] readWsets = mgr2.getWorkingSets();
		assertEquals(1, readWsets.length);
		AdaptableHelpResource[] resources = readWsets[0].getElements();
		assertEquals(1, resources.length);
		assertTrue(resources[0].equals(toc));
		checkCookies(resp);
		checkCookies(resp2);
	}

	@Test
	public void testIWSMWithOneTopic() throws IOException {
		MockServletRequest req = new MockServletRequest();
		MockServletResponse resp = new MockServletResponse();
		InfocenterWorkingSetManager mgr = new InfocenterWorkingSetManager(req, resp, "en");
		WorkingSet wset = new WorkingSet("test2");
		AdaptableTopic topic1 = mgr.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_1_");
		assertNotNull(topic1);
		wset.setElements(new AdaptableHelpResource[] { topic1 });
		mgr.addWorkingSet(wset);
		WorkingSet[] readWsets = mgr.getWorkingSets();
		assertEquals(1, readWsets.length);
		AdaptableHelpResource[] resources = readWsets[0].getElements();
		assertEquals(1, resources.length);
		Set<AdaptableTopic> topics = new HashSet<>();
		topics.add(topic1);
		assertTrue(topics.contains(resources[0]));
		checkCookies(resp);
	}

	@Test
	public void testIWSMWithTwoTopics() throws IOException {
		MockServletRequest req = new MockServletRequest();
		MockServletResponse resp = new MockServletResponse();
		InfocenterWorkingSetManager mgr = new InfocenterWorkingSetManager(req, resp, "en");
		WorkingSet wset = new WorkingSet("test2");
		AdaptableTopic topic1 = mgr.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_1_");
		AdaptableTopic topic3 = mgr.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_3_");
		assertNotNull(topic1);
		assertNotNull(topic3);
		wset.setElements(new AdaptableHelpResource[] { topic1, topic3 });
		mgr.addWorkingSet(wset);
		WorkingSet[] readWsets = mgr.getWorkingSets();
		assertEquals(1, readWsets.length);
		AdaptableHelpResource[] resources = readWsets[0].getElements();
		assertEquals(2, resources.length);
		Set<AdaptableTopic> topics = new HashSet<>();
		topics.add(topic1);
		topics.add(topic3);
		assertTrue(topics.contains(resources[0]));
		assertTrue(topics.contains(resources[1]));
		checkCookies(resp);
	}

	@Test
	public void testIWSMWithThreeTopics() throws IOException {
		MockServletRequest req = new MockServletRequest();
		MockServletResponse resp = new MockServletResponse();
		InfocenterWorkingSetManager mgr = new InfocenterWorkingSetManager(req, resp, "en");
		WorkingSet wset = new WorkingSet("test2");
		AdaptableTopic topic1 = mgr.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_1_");
		AdaptableTopic topic3 = mgr.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_3_");
		AdaptableTopic topic5 = mgr.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_5_");
		assertNotNull(topic1);
		assertNotNull(topic3);
		assertNotNull(topic5);
		wset.setElements(new AdaptableHelpResource[] { topic1, topic3, topic5 });
		mgr.addWorkingSet(wset);

		WorkingSet[] readWsets = mgr.getWorkingSets();
		assertEquals(1, readWsets.length);
		AdaptableHelpResource[] resources = readWsets[0].getElements();
		assertEquals(3, resources.length);
		Set<AdaptableTopic> topics = new HashSet<>();
		topics.add(topic1);
		topics.add(topic3);
		topics.add(topic5);
		assertTrue(topics.contains(resources[0]));
		assertTrue(topics.contains(resources[1]));
		assertTrue(topics.contains(resources[2]));
		checkCookies(resp);
	}

	@Test
	public void testSaveRestoreIWSMWithOneTopic() throws IOException {
		MockServletRequest req = new MockServletRequest();
		MockServletResponse resp = new MockServletResponse();
		InfocenterWorkingSetManager mgr = new InfocenterWorkingSetManager(req, resp, "en");
		WorkingSet wset = new WorkingSet("test2");
		AdaptableTopic topic1 = mgr.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_1_");
		assertNotNull(topic1);
		wset.setElements(new AdaptableHelpResource[] { topic1 });
		mgr.addWorkingSet(wset);

		MockServletRequest req2 = new MockServletRequest();
		MockServletResponse resp2 = new MockServletResponse();
		req2.setCookies(resp.getCookies());
		InfocenterWorkingSetManager mgr2 = new InfocenterWorkingSetManager(req2, resp2, "en");
		WorkingSet[] readWsets = mgr2.getWorkingSets();
		assertEquals(1, readWsets.length);
		assertEquals(1, readWsets.length);
		AdaptableHelpResource[] resources = readWsets[0].getElements();
		assertEquals(1, resources.length);
		Set<AdaptableTopic> topics = new HashSet<>();
		topics.add(topic1);
		assertTrue(topics.contains(resources[0]));
		checkCookies(resp);
		checkCookies(resp2);
	}

	@Test
	public void testSaveRestoreIWSMWithTwoTopics() throws IOException {
		MockServletRequest req = new MockServletRequest();
		MockServletResponse resp = new MockServletResponse();
		InfocenterWorkingSetManager mgr = new InfocenterWorkingSetManager(req, resp, "en");
		WorkingSet wset = new WorkingSet("test2");
		AdaptableTopic topic1 = mgr.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_1_");
		AdaptableTopic topic3 = mgr.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_3_");
		assertNotNull(topic1);
		assertNotNull(topic3);
		wset.setElements(new AdaptableHelpResource[] { topic1, topic3 });
		mgr.addWorkingSet(wset);

		MockServletRequest req2 = new MockServletRequest();
		MockServletResponse resp2 = new MockServletResponse();
		req2.setCookies(resp.getCookies());
		InfocenterWorkingSetManager mgr2 = new InfocenterWorkingSetManager(req2, resp2, "en");
		WorkingSet[] readWsets = mgr2.getWorkingSets();
		assertEquals(1, readWsets.length);
		AdaptableHelpResource[] resources = readWsets[0].getElements();
		assertEquals(2, resources.length);
		Set<AdaptableTopic> topics = new HashSet<>();
		topics.add(topic1);
		topics.add(topic3);
		assertTrue(topics.contains(resources[0]));
		assertTrue(topics.contains(resources[1]));
		checkCookies(resp);
		checkCookies(resp2);
	}

	@Test
	public void testSaveRestoreIWSMWithThreeTopics() throws IOException {
		MockServletRequest req = new MockServletRequest();
		MockServletResponse resp = new MockServletResponse();
		InfocenterWorkingSetManager mgr = new InfocenterWorkingSetManager(req, resp, "en");
		WorkingSet wset = new WorkingSet("test2");
		AdaptableTopic topic1 = mgr.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_1_");
		AdaptableTopic topic3 = mgr.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_3_");
		AdaptableTopic topic5 = mgr.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_5_");
		assertNotNull(topic1);
		assertNotNull(topic3);
		assertNotNull(topic5);
		wset.setElements(new AdaptableHelpResource[] { topic1, topic3, topic5 });
		mgr.addWorkingSet(wset);

		MockServletRequest req2 = new MockServletRequest();
		MockServletResponse resp2 = new MockServletResponse();
		req2.setCookies(resp.getCookies());
		InfocenterWorkingSetManager mgr2 = new InfocenterWorkingSetManager(req2, resp2, "en");
		WorkingSet[] readWsets = mgr2.getWorkingSets();
		assertEquals(1, readWsets.length);
		AdaptableHelpResource[] resources = readWsets[0].getElements();
		assertEquals(3, resources.length);
		Set<AdaptableTopic> topics = new HashSet<>();
		topics.add(topic1);
		topics.add(topic3);
		topics.add(topic5);
		assertTrue(topics.contains(resources[0]));
		assertTrue(topics.contains(resources[1]));
		assertTrue(topics.contains(resources[2]));
		checkCookies(resp);
		checkCookies(resp2);
	}

	@Test
	public void testIWSMWithMultipleWsets() throws IOException {
		MockServletRequest req = new MockServletRequest();
		MockServletResponse resp = new MockServletResponse();
		InfocenterWorkingSetManager mgr = new InfocenterWorkingSetManager(req, resp, "en");
		WorkingSet wset1 = new WorkingSet("test3");
		WorkingSet wset2 = new WorkingSet("test4");
		AdaptableTopic topic1 = mgr.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_1_");
		AdaptableTopic topic3 = mgr.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_3_");
		assertNotNull(topic1);
		assertNotNull(topic3);
		wset1.setElements(new AdaptableHelpResource[] { topic1 });
		wset2.setElements(new AdaptableHelpResource[] { topic3 });
		mgr.addWorkingSet(wset1);
		mgr.addWorkingSet(wset2);
		WorkingSet[] readWsets = mgr.getWorkingSets();
		assertEquals(2, readWsets.length);
		AdaptableHelpResource[] resourcesT3 = mgr.getWorkingSet("test3").getElements();
		assertEquals(1, resourcesT3.length);
		assertEquals(topic1, resourcesT3[0]);
		AdaptableHelpResource[] resourcesT4 = mgr.getWorkingSet("test4").getElements();
		assertEquals(1, resourcesT4.length);
		assertEquals(topic3, resourcesT4[0]);
		checkCookies(resp);
	}

	/**
	 * Verify that adding a second topic in the same toc only adds 4 more bytes to the cookie size
	 * @throws IOException
	 */
	@Test
	public void testCookieSizeOptimization() throws IOException {
		MockServletRequest req1 = new MockServletRequest();
		MockServletResponse resp1 = new MockServletResponse();
		MockServletRequest req2 = new MockServletRequest();
		MockServletResponse resp2 = new MockServletResponse();
		InfocenterWorkingSetManager mgr1 = new InfocenterWorkingSetManager(req1, resp1, "en");
		InfocenterWorkingSetManager mgr2 = new InfocenterWorkingSetManager(req2, resp2, "en");
		WorkingSet wset1 = new WorkingSet("test21");
		WorkingSet wset2 = new WorkingSet("test22");
		AdaptableTopic topic1 = mgr1.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_1_");
		AdaptableTopic topic3 = mgr1.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_3_");
		wset1.setElements(new AdaptableHelpResource[] { topic1 });
		mgr1.addWorkingSet(wset1);
		wset2.setElements(new AdaptableHelpResource[] { topic1, topic3 });
		mgr2.addWorkingSet(wset2);

		int length1 = cookieLength(resp1.getCookies());
		int length2 = cookieLength(resp2.getCookies());
		assertEquals(length1 + 4, length2);
		checkCookies(resp1);
		checkCookies(resp2);
	}

	private int cookieLength(Cookie[] cookies) {
		int total = 0;
		for (Cookie cookie : cookies) {
			total += cookie.getValue().length();
		}
		return total;
	}

	@Test
	public void testSaveRestoreIWSMWithMultipleWsets() throws IOException {
		MockServletRequest req = new MockServletRequest();
		MockServletResponse resp = new MockServletResponse();
		InfocenterWorkingSetManager mgr = new InfocenterWorkingSetManager(req, resp, "en");
		WorkingSet wset1 = new WorkingSet("test3");
		WorkingSet wset2 = new WorkingSet("test4");
		AdaptableTopic topic1 = mgr.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_1_");
		AdaptableTopic topic3 = mgr.getAdaptableTopic("/org.eclipse.ua.tests/data/help/toc/root.xml_3_");
		assertNotNull(topic1);
		assertNotNull(topic3);
		wset1.setElements(new AdaptableHelpResource[] { topic1 });
		wset2.setElements(new AdaptableHelpResource[] { topic3 });
		mgr.addWorkingSet(wset1);
		mgr.addWorkingSet(wset2);

		MockServletRequest req2 = new MockServletRequest();
		MockServletResponse resp2 = new MockServletResponse();
		req2.setCookies(resp.getCookies());
		InfocenterWorkingSetManager mgr2 = new InfocenterWorkingSetManager(req2, resp2, "en");
		WorkingSet[] readWsets = mgr2.getWorkingSets();

		assertEquals(2, readWsets.length);
		AdaptableHelpResource[] resourcesT3 = mgr2.getWorkingSet("test3").getElements();
		assertEquals(1, resourcesT3.length);
		assertEquals(topic1, resourcesT3[0]);
		AdaptableHelpResource[] resourcesT4 = mgr2.getWorkingSet("test4").getElements();
		assertEquals(1, resourcesT4.length);
		assertEquals(topic3, resourcesT4[0]);
		checkCookies(resp);
		checkCookies(resp2);
	}

	@Test
	public void testIWSMWithCriteria() throws IOException {
		MockServletRequest req = new MockServletRequest();
		MockServletResponse resp = new MockServletResponse();
		InfocenterWorkingSetManager mgr = new InfocenterWorkingSetManager(req, resp, "en");
		WorkingSet wset = new WorkingSet("test5");
		AdaptableToc toc = mgr.getAdaptableToc("/org.eclipse.ua.tests/data/help/toc/root.xml");
		assertNotNull(toc);
		wset.setElements(new AdaptableHelpResource[] { toc });
		CriterionResource[] criteria =  { new CriterionResource("version") };
		criteria[0].addCriterionValue("1.0");
		wset.setCriteria(criteria);
		mgr.addWorkingSet(wset);
		WorkingSet[] readWsets = mgr.getWorkingSets();
		assertEquals(1, readWsets.length);
		CriterionResource[] readResources = readWsets[0].getCriteria();
		assertEquals(1, readResources.length);
		checkCookies(resp);
	}

	@Test
	public void testSaveRestoreIWSMWithMCriteria() throws IOException {
		MockServletRequest req = new MockServletRequest();
		MockServletResponse resp = new MockServletResponse();
		InfocenterWorkingSetManager mgr = new InfocenterWorkingSetManager(req, resp, "en");
		WorkingSet wset = new WorkingSet("test6");
		AdaptableToc toc = mgr.getAdaptableToc("/org.eclipse.ua.tests/data/help/toc/root.xml");
		assertNotNull(toc);
		wset.setElements(new AdaptableHelpResource[] { toc });
		CriterionResource[] criteria =  { new CriterionResource("version") };
		criteria[0].addCriterionValue("1.0");
		wset.setCriteria(criteria);
		mgr.addWorkingSet(wset);

		MockServletRequest req2 = new MockServletRequest();
		MockServletResponse resp2 = new MockServletResponse();
		req2.setCookies(resp.getCookies());
		InfocenterWorkingSetManager mgr2 = new InfocenterWorkingSetManager(req2, resp2, "en");
		WorkingSet[] readWsets = mgr2.getWorkingSets();

		assertEquals(1, readWsets.length);
		CriterionResource[] readResources = readWsets[0].getCriteria();
		assertEquals(1, readResources.length);
		checkCookies(resp);
		checkCookies(resp2);
	}

	@Test
	public void testIWSMWithMultipleCriteria() throws IOException {
		MockServletRequest req = new MockServletRequest();
		MockServletResponse resp = new MockServletResponse();
		InfocenterWorkingSetManager mgr = new InfocenterWorkingSetManager(req, resp, "en");
		WorkingSet wset = new WorkingSet("test7");
		AdaptableToc toc = mgr.getAdaptableToc("/org.eclipse.ua.tests/data/help/toc/root.xml");
		assertNotNull(toc);
		wset.setElements(new AdaptableHelpResource[] { toc });
		CriterionResource[] criteria =  { new CriterionResource("version"), new CriterionResource("platform") };
		criteria[0].addCriterionValue("1.0");
		criteria[1].addCriterionValue("linux");
		criteria[1].addCriterionValue("MacOS");
		wset.setCriteria(criteria);
		mgr.addWorkingSet(wset);
		WorkingSet[] readWsets = mgr.getWorkingSets();
		assertEquals(1, readWsets.length);
		CriterionResource[] readResources = readWsets[0].getCriteria();
		checkResourceWithTwoChildren(readResources);
		checkCookies(resp);
	}

	@Test
	public void testSaveRestoreIWSMWithMultipleCriteria() throws IOException {
		MockServletRequest req = new MockServletRequest();
		MockServletResponse resp = new MockServletResponse();
		InfocenterWorkingSetManager mgr = new InfocenterWorkingSetManager(req, resp, "en");
		WorkingSet wset = new WorkingSet("test8");
		AdaptableToc toc = mgr.getAdaptableToc("/org.eclipse.ua.tests/data/help/toc/root.xml");
		assertNotNull(toc);
		wset.setElements(new AdaptableHelpResource[] { toc });
		CriterionResource[] criteria = createResourceWithTwoCriteria();
		wset.setCriteria(criteria);
		mgr.addWorkingSet(wset);
		MockServletRequest req2 = new MockServletRequest();
		MockServletResponse resp2 = new MockServletResponse();
		req2.setCookies(resp.getCookies());
		InfocenterWorkingSetManager mgr2 = new InfocenterWorkingSetManager(req2, resp2, "en");
		WorkingSet[] readWsets = mgr2.getWorkingSets();

		assertEquals(1, readWsets.length);
		CriterionResource[] readResources = readWsets[0].getCriteria();
		checkResourceWithTwoChildren(readResources);
		checkCookies(resp);
		checkCookies(resp2);
	}

	private void checkResourceWithTwoChildren(CriterionResource[] readResources) {
		assertEquals(2, readResources.length);
		CriterionResource readVersion;
		CriterionResource readPlatform;
		if (readResources[0].getCriterionName().equals("version")) {
			readVersion = readResources[0];
			readPlatform = readResources[1];
		} else {
			readVersion = readResources[0];
			readPlatform = readResources[1];
		}
		assertEquals("version", readVersion.getCriterionName());
		assertEquals(1, readVersion.getCriterionValues().size());
		assertTrue(readVersion.getCriterionValues().contains("1.0"));
		assertEquals("platform", readPlatform.getCriterionName());
		assertEquals(2, readPlatform.getCriterionValues().size());
		assertTrue(readPlatform.getCriterionValues().contains("linux"));
		assertTrue(readPlatform.getCriterionValues().contains("MacOS"));
	}

	private CriterionResource[] createResourceWithTwoCriteria() {
		CriterionResource[] criteria;
		criteria = new CriterionResource[2];
		criteria[0] = new CriterionResource("version");
		criteria[0].addCriterionValue("1.0");
		criteria[1] = new CriterionResource("platform");
		criteria[1].addCriterionValue("linux");
		criteria[1].addCriterionValue("MacOS");
		return criteria;
	}

	private void checkCookies(MockServletResponse resp) {
		String illegalChars = resp.getIllegalCharatersFound();
		if (illegalChars.isEmpty() || illegalChars.equals("<")) {
			return;
		}
		fail("Cookie contains these illegal characters " + illegalChars + '"');
	}

}
