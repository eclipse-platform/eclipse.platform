/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
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
package org.eclipse.debug.tests.launching;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.tests.DebugTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests accelerator adjustments for DBCS languages. See bug 186921.
 *
 * @since 3.3
 */
@ExtendWith(DebugTestExtension.class)
public class AcceleratorSubstitutionTests {

	/**
	 * tests a string with "..."
	 */
	@Test
	public void testWithEllipses() {
		assertEquals("Open Run Dialog(&R)...", DebugUIPlugin.adjustDBCSAccelerator("Open Run(&R) Dialog..."), "incorrect DBCS accelerator substitution");
	}

	/**
	 * tests a string without "..."
	 */
	@Test
	public void testWithoutEllipses() {
		assertEquals("Open Run Dialog(&R)", DebugUIPlugin.adjustDBCSAccelerator("Open Run(&R) Dialog"), "incorrect DBCS accelerator substitution");
	}

	/**
	 * tests a string that should not change (no DBCS style accelerator).
	 */
	@Test
	public void testWithoutDBCSAcclerator() {
		assertEquals("Open &Run Dialog...", DebugUIPlugin.adjustDBCSAccelerator("Open &Run Dialog..."), "incorrect DBCS accelerator substitution");
	}
}
