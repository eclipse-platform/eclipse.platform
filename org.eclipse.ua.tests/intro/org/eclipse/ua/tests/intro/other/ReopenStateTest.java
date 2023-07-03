/*******************************************************************************
 *  Copyright (c) 2008, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.intro.other;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.ui.internal.intro.impl.util.ReopenUtil;
import org.junit.Test;

/*
 * Tests the intro parser on valid intro content.
 */
public class ReopenStateTest {
	@Test
	public void testReopenState() {
		ReopenUtil.setReopenPreference(true);
		assertTrue(ReopenUtil.isReopenPreference());
		ReopenUtil.setReopenPreference(false);
		assertFalse(ReopenUtil.isReopenPreference());
		ReopenUtil.setReopenPreference(false);
		assertFalse(ReopenUtil.isReopenPreference());
		ReopenUtil.setReopenPreference(true);
		assertTrue(ReopenUtil.isReopenPreference());
		ReopenUtil.setReopenPreference(false);
		assertFalse(ReopenUtil.isReopenPreference());
	}


}
