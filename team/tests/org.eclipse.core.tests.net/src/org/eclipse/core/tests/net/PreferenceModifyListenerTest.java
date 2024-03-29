/*******************************************************************************
 * Copyright (c) 2013, 2017 IBM Corporation and others.
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
package org.eclipse.core.tests.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IExportedPreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.junit.jupiter.api.Test;
import org.osgi.service.prefs.BackingStoreException;

public class PreferenceModifyListenerTest  {
	private static final String NODE_NAME = "bug419228";
	private static final String KEY = "someKey";
	private static final String VALUE = "someValue";

	@Test
	public void testPreApply() throws BackingStoreException, CoreException {
		// create a dummy node and export it to a stream
		IEclipsePreferences toExport = InstanceScope.INSTANCE.getNode(NODE_NAME);
		toExport.put(KEY, VALUE);
		IPreferencesService service = Platform.getPreferencesService();
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		assertTrue(service.exportPreferences(toExport, stream, null).isOK());

		// read preferences from a stream
		IExportedPreferences exported = service.readPreferences(new ByteArrayInputStream(stream.toByteArray()));
		exported = (IExportedPreferences) exported.node(InstanceScope.SCOPE).node(NODE_NAME);

		// apply exported preferences to the global preferences hierarchy
		assertTrue(service.applyPreferences(exported).isOK());

		// verify that the node is not modified
		String debugString = ((EclipsePreferences) exported.node("/")).toDeepDebugString();
		assertFalse(exported.nodeExists("instance/org.eclipse.core.net"), debugString);
		assertFalse(exported.nodeExists("/instance/org.eclipse.core.net"), debugString);
		assertFalse(exported.nodeExists("configuration/org.eclipse.core.net"), debugString);
		assertFalse(exported.nodeExists("/configuration/org.eclipse.core.net"), debugString);
	}
}
