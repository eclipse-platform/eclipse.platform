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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.tips.core.internal.TipManager;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("restriction")
public class TipProviderTest {

	private TestTipManager fManager;
	private TestTipProvider fProvider;

	@Before
	public void testTipProvider() {
		fManager = new TestTipManager();
		fManager.open(false);
		fProvider = (TestTipProvider) new TestTipProvider().setManager(fManager);
	}

	@Test
	public void testDispose() {
		fProvider.dispose();
	}

	@Test
	public void testGetDescription() {
		assertNotNull(fProvider.getDescription());
	}

	@Test
	public void testGetID() {
		assertNotNull(fProvider.getID());
		assertEquals(fProvider.getClass().getName(), fProvider.getID());
	}

	@Test
	public void testGetImage() {
		assertNotNull(fProvider.getImage());
	}

	@Test
	public void testGetTips() {
		assertEquals(0, fProvider.getTips(null).size());
		createTestData();
		fManager.setAsRead(fProvider.getNextTip());
		assertEquals(2, fProvider.getTips(null).size());
		assertEquals(2, fProvider.getTips(null).size());
		assertEquals(1, fProvider.getTips().size());
		((TipManager) fProvider.getManager()).setServeReadTips(true);
		assertEquals(2, fProvider.getTips(null).size());
	}

	private void createTestData() {
		fProvider.setTips(Arrays.asList(new TestTip(fProvider.getID(), "<b>bold</b>", "Tip 1"),
				new TestTip(fProvider.getID(), "<b>bold2</b>", "Tip 2")));
	}

	@Test
	public void testGetCurrentTip() {
		assertEquals(fProvider.getNextTip(), fProvider.getCurrentTip());
	}

	@Test
	public void testGetCurrentTip2() {
		assertEquals(fProvider.getPreviousTip(), fProvider.getCurrentTip());
	}

	@Test
	public void testGetNextTip() {
		createTestData();
		fManager.setAsRead(fProvider.getNextTip());
		assertEquals(fProvider.getCurrentTip(), fProvider.getNextTip());
		Tip nextTip = fProvider.getNextTip();
		fManager.setAsRead(nextTip);
		assertTrue(fManager.isRead(nextTip));
		Tip nextTip2 = fProvider.getNextTip();
		fManager.setAsRead(nextTip2);
		assertTrue(fManager.isRead(nextTip2));
		assertEquals("FinalTip", fProvider.getNextTip().getClass().getSimpleName());
		((TipManager) fProvider.getManager()).setServeReadTips(true);
		assertNotEquals("FinalTip", fProvider.getNextTip().getClass().getSimpleName());
	}

	@Test
	public void testGetPreviousTip() {
		assertEquals(fProvider.getCurrentTip(), fProvider.getPreviousTip());
		assertEquals(fProvider.getCurrentTip(), fProvider.getPreviousTip());
	}

	@Test
	public void testGetPreviousTip2() {
		assertNotNull(fProvider.getPreviousTip());
		assertEquals("FinalTip", fProvider.getNextTip().getClass().getSimpleName());
	}

	@Test
	public void testGetPreviousTip3() {
		((TipManager) fProvider.getManager()).setServeReadTips(true);
		assertEquals(fProvider.getCurrentTip(), fProvider.getPreviousTip());
	}

	@Test
	public void testGetPreviousTip4() {
		createTestData();
		assertNotNull(fProvider.getPreviousTip());
		assertNotNull(fProvider.getPreviousTip());
		assertNotNull(fProvider.getPreviousTip());
	}

	@Test
	public void testGetTipManager() {
		assertEquals(fManager, fProvider.getManager());
	}

	@Test
	public void testIsReady() {
		TestTipProvider p = (TestTipProvider) new TestTipProvider().setManager(fManager);
		assertFalse(p.isReady());
		p.setTips(Collections.emptyList());
		assertTrue(p.isReady());
	}

	@Test
	public void testLoad() {
		TestTipProvider p = (TestTipProvider) new TestTipProvider().setManager(fManager);
		assertFalse(p.isReady());
		p.loadNewTips(new NullProgressMonitor());
		assertTrue(p.isReady());
	}

	@Test
	public void testSetManager() {
		TestTipProvider p = new TestTipProvider();
		assertNull(p.getManager());
		p.setManager(fManager);
		assertNotNull(p.getManager());
	}

	@Test
	public void testSetTips() {
		TestTipProvider p = new TestTipProvider() {
			@Override
			public IStatus loadNewTips(IProgressMonitor pMonitor) {
				assertEquals(0, getTips(null).size());
				assertEquals(1, setTips(Arrays.asList(new TestTip(getID(), "DDD", "XXX"))).getTips(null).size());
				return Status.OK_STATUS;
			}
		};
		assertEquals(0, p.getTips(null).size());
		fManager.register(p);
		assertEquals(1, p.getTips(null).size());
	}

	@Test
	public void testAddTips() {
		TestTipProvider p = new TestTipProvider() {
			@Override
			public IStatus loadNewTips(IProgressMonitor pMonitor) {
				assertEquals(0, getTips(null).size());
				assertEquals(1, setTips(Arrays.asList(new TestTip(getID(), "DDD", "XXX"))).getTips(null).size());
				assertEquals(2, addTips(Arrays.asList(new TestTip(getID(), "DDD", "XXX"))).getTips(null).size());
				return Status.OK_STATUS;
			}
		};
		fManager.register(p);

	}
}
