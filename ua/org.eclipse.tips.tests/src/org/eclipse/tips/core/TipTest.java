/*******************************************************************************
 * Copyright (c) 2018, 2023 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class TipTest {

	private static final String HTML = "<head></head>";
	private static final String SUBJECT_TIP = "Tip Subject";
	private TestTipManager fManager;
	private TestTipProvider fProvider;
	private TestTip fTip;

	@Before
	public void setup() {
		fManager = new TestTipManager();
		fProvider = (TestTipProvider) new TestTipProvider().setManager(fManager);
		createTestDate();
		fTip = new TestTip(fProvider.getID(), HTML, SUBJECT_TIP) {
			@Override
			public List<TipAction> getActions() {
				ArrayList<TipAction> actions = new ArrayList<>();
				ArrayList<String> result = new ArrayList<>();
				Runnable runner = () -> result.add("entry");

				actions.add(new TipAction("text", "tooltip", runner, null));
				return actions;
			}
		};
	}

	@Test
	public void testHashCode() {
		assertNotEquals(0, fProvider.getNextTip().hashCode());
	}

	@Test
	public void testHashCode2() {
		TestTip testTip = new TestTip(fProvider.getID(), HTML, SUBJECT_TIP);
		TestTip testTip2 = new TestTip(fProvider.getID(), HTML, SUBJECT_TIP);
		assertEquals(testTip2.hashCode(), testTip.hashCode());
	}

	@Test
	public void testTip() {
		new TestTip(fProvider.getID(), HTML, SUBJECT_TIP);
	}

	@Test
	public void testGetAction() {
		assertTrue(fTip.getActions().size() > 0);
	}

	@Test
	public void testGetCreationDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(fTip.getCreationDate());
		assertEquals(11, calendar.get(Calendar.MONTH));
		assertEquals(1964, calendar.get(Calendar.YEAR));
		assertEquals(31, calendar.get(Calendar.DAY_OF_MONTH));
	}

	@Test
	public void testGetHTML() {
		assertNotNull(fTip.getHTML());
	}

	@Test
	public void testGetImage() {
		assertNotNull(fTip.getImage());
	}

	@Test
	public void testGetSubject() {
		assertNotNull(fTip.getSubject());
		assertEquals(SUBJECT_TIP, fTip.getSubject());
	}

	@Test
	public void testEqualsObject() {
		TestTip testTip = new TestTip(fProvider.getID(), HTML, SUBJECT_TIP);
		TestTip testTipx = new TestTip(fProvider.getID(), HTML, SUBJECT_TIP);
		assertFalse(testTip.equals(fTip));
		assertTrue(testTip.equals(testTipx));
		assertNotEquals(fTip, testTip);
		assertEquals(testTipx, testTip);

		TestTipProvider testTipProvider = new TestTipProvider() {
			@Override
			public String getID() {
				return "sss";
			}
		};

		TestTipProvider testTipProvider2 = new TestTipProvider() {
			@Override
			public String getID() {
				return null;
			}
		};

		TestTipProvider testTipProvider3 = new TestTipProvider() {
			@Override
			public String getID() {
				return "sss";
			}
		};

		assertNotEquals(testTipProvider2, testTipProvider);
		assertNotEquals(testTipProvider3, testTipProvider);
		TestTip testTip2 = new TestTip(fProvider.getID(), HTML, SUBJECT_TIP + "DDD");
		assertNotEquals(testTip2, testTip);
		assertNotEquals(testTip2, testTip);

		TestTip testTip3 = new TestTip(fProvider.getID(), HTML, SUBJECT_TIP + "DDD");
		assertNotEquals(testTip3, testTip);
		assertNotEquals(testTip3, testTip3);

		TestTip testTip4 = new TestTip(fProvider.getID(), HTML, SUBJECT_TIP + "DDD");
		assertNotEquals(testTip4, testTip);
		assertNotEquals(testTip, testTip4);

		TestTip testTip5 = new TestTip(fProvider.getID(), HTML, SUBJECT_TIP + "DDDWW");
		assertNotEquals(testTip5, testTip);
		assertNotEquals(testTip, testTip5);

		TestTip testTip6 = new TestTip(fProvider.getID(), HTML, null);
		assertNotEquals(testTip6, testTip);
		assertNotEquals(testTip, testTip6);

	}

	@Test
	public void testIsRead() {
		assertFalse(fManager.isRead(fTip));
		fManager.setAsRead(fTip);
		fManager.setAsRead(fTip);
		assertTrue(fManager.isRead(fTip));
	}

	private void createTestDate() {
		fProvider.setTips(Arrays.asList(new TestTip(fProvider.getID(), "<b>bold</b>", "Tip 1"),
				new TestTip(fProvider.getID(), "<b>bold2</b>", "Tip 2")));
	}
}