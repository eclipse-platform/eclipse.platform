/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.base.remote;

import java.util.Map;
import java.util.TreeMap;

public class RemoteContentLocator {

	private static Map<String, String> InfoCenterMap = null; // hash table

	public RemoteContentLocator() {

	}

	public static void addContentPage(String contributorID, String InfoCenterUrl) {

		if (InfoCenterMap == null){
			InfoCenterMap = new TreeMap<>(); // sorted map
		}

		InfoCenterMap.put(contributorID, InfoCenterUrl);
	}

	public static String getUrlForContent(String contributorID) {

		if (InfoCenterMap == null)
			return null;

		Object key = InfoCenterMap.get(contributorID);
		return (String)key;
	}

	public static Map<String, String> getInfoCenterMap() {
		return InfoCenterMap;
	}

}
