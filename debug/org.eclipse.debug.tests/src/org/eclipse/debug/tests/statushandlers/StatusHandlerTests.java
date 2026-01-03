/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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
package org.eclipse.debug.tests.statushandlers;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IStatusHandler;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.core.Preferences;
import org.eclipse.debug.tests.DebugTestExtension;
import org.eclipse.debug.tests.TestsPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests status handlers
 */
@ExtendWith(DebugTestExtension.class)
public class StatusHandlerTests {

	/**
	 * Status for which a handler is registered.
	 */
	public static final IStatus STATUS = new Status(IStatus.ERROR, TestsPlugin.PLUGIN_ID, 333, "", null); //$NON-NLS-1$

	/**
	 * Tests that a status handler extension exists
	 */
	@Test
	public void testStatusHandlerExtension() {
		IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(STATUS);
		assertNotNull(handler, "missing status handler extension"); //$NON-NLS-1$
		assertTrue(handler instanceof StatusHandler, "Unexpected handler"); //$NON-NLS-1$
	}

	/**
	 * Tests that status handlers are not returned when preference is disabled
	 */
	@Test
	public void testDisableStatusHandlers() {
		try {
			Preferences.setBoolean(DebugPlugin.getUniqueIdentifier(), IInternalDebugCoreConstants.PREF_ENABLE_STATUS_HANDLERS, false, InstanceScope.INSTANCE);
			IStatusHandler handler = DebugPlugin.getDefault().getStatusHandler(STATUS);
			assertNull(handler, "status handler extension should be disabled"); //$NON-NLS-1$
		} finally {
			Preferences.setBoolean(DebugPlugin.getUniqueIdentifier(), IInternalDebugCoreConstants.PREF_ENABLE_STATUS_HANDLERS, true, InstanceScope.INSTANCE);
		}
	}

}
