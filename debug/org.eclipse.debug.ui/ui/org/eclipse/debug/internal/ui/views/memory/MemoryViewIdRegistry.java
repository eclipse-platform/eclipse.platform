/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.memory;

import java.util.ArrayList;

/**
 * Class for managing the secondary ids for Memory View
 *
 */
public class MemoryViewIdRegistry {

	private static ArrayList<String> fgRegistry;

	public static void registerView(String secondaryId) {
		ArrayList<String> registry = getRegistry();

		if (!registry.contains(secondaryId)) {
			registry.add(secondaryId);
		}
	}

	public static void deregisterView(String secondaryId) {
		ArrayList<String> registry = getRegistry();

		if (registry.contains(secondaryId)) {
			registry.remove(secondaryId);
		}
	}

	public static String getUniqueSecondaryId(String viewId) {
		int cnt = 0;
		String id = viewId + "." + cnt; //$NON-NLS-1$
		ArrayList<String> registry = getRegistry();
		while (registry.contains(id)) {
			cnt++;
			id = viewId + "." + cnt; //$NON-NLS-1$
		}
		return id;
	}

	private static ArrayList<String> getRegistry() {
		if (fgRegistry == null)
			fgRegistry = new ArrayList<>();

		return fgRegistry;
	}
}
