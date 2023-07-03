/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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

package org.eclipse.help.internal.webapp.data;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.Platform;

public class IconFinder {

	private static final String LEAF = "_leaf"; //$NON-NLS-1$
	private static final String CLOSED = "_closed"; //$NON-NLS-1$
	private static final String OPEN = "_open"; //$NON-NLS-1$
	private static final String ALT = "_alt"; //$NON-NLS-1$
	private static final String EXT_PT = "org.eclipse.help.toc"; //$NON-NLS-1$
	private static final String TOC_ICON_ELEMENT = "tocIcon"; //$NON-NLS-1$
	private static final String TOC_ICON_ID = "id"; //$NON-NLS-1$
	private static final String OPEN_ICON_PATH = "openIcon"; //$NON-NLS-1$
	private static final String CLOSED_ICON_PATH = "closedIcon"; //$NON-NLS-1$
	private static final String LEAF_ICON_PATH = "leafIcon"; //$NON-NLS-1$
	private static final String ICON_ALT_TEXT = "altText"; //$NON-NLS-1$
	private static final String PATH_SEPARATOR = "/"; //$NON-NLS-1$
	private static boolean iconsInitialized = false;
	public static int TYPEICON_OPEN   = 0;
	public static int TYPEICON_CLOSED = 1;
	public static int TYPEICON_LEAF   = 2;


	private static Map<String, String> IconPathMap = null; // hash table

	private static void addIconPath(String IconKey, String IconPath) {
		if (IconPathMap == null) {
			IconPathMap = new HashMap<>();
			IconPathMap = new TreeMap<>(); // sorted map
		}
		IconPathMap.put(IconKey, IconPath);
	}
	private static String getIconPath(String IconKey) {
		return getEntry(IconKey);
	}

	private static String getIconAltText(String IconKey) {
		return getEntry(IconKey);
	}

	private static String getEntry(String IconKey) {
		if (IconPathMap == null)
			return null;
		Object key = IconPathMap.get(IconKey);
		return (String) key;
	}

	private static void setIconImagePath(String bundleId, String path, String key) {
		String iconPath = IconFinder.getIconPath(key);
		if(iconPath == null){
			iconPath = bundleId + PATH_SEPARATOR + path;
			IconFinder.addIconPath(key, iconPath);
		}
	}
	private static void setIconAltText(String value, String key) {
			IconFinder.addIconPath(key, value);
	}
	public static String getImagePathFromId(String iconId, int type) {
		if (iconId == null) {
			return null;
		}
		initializeTocIcons();
		String suffix;

		switch(type){
			case 0:suffix = OPEN;break;
			case 1:suffix = CLOSED;break;
			case 2:suffix = LEAF;break;
			default: suffix = OPEN; break;
		}
		String result = lookupImagePath(iconId + suffix);
		if (result != null) {
			return result;
		}
		return lookupImagePath(iconId + OPEN);
	}

	public static String getIconAltFromId(String iconId) {
		if (iconId == null) {
			return null;
		}
		initializeTocIcons();
		return getIconAltText(iconId + ALT);
	}

	/**
	 * Tests to see if an icon attribute was specified and the icon type was declared
	 * in an extension.
	 */
	public static boolean isIconDefined(String icon) {
		if (icon == null || icon.length() == 0) {
			return false;
		}
		String result = getImagePathFromId(icon, TYPEICON_OPEN);
		return result != null;
	}

	private static String lookupImagePath(String name) {
		return getIconPath(name);
	}

	private static void initializeTocIcons() {
		if (iconsInitialized) {
			return;
		}
		iconsInitialized = true;
		// Get extension points that contribute products
		IExtension[] extensionsFound = Platform.getExtensionRegistry()
				.getExtensionPoint(EXT_PT).getExtensions();

		for (IExtension element : extensionsFound) {

			IConfigurationElement[] configElements = element
					.getConfigurationElements();
			for (IConfigurationElement iconElem : configElements) {
				if (iconElem.getName().equals(TOC_ICON_ELEMENT)) {
					String[] attrs = iconElem.getAttributeNames();
					String contributorID = iconElem.getContributor().getName();

					for (String attr : attrs) {
						if (attr.equals(OPEN_ICON_PATH))
							IconFinder.setIconImagePath(contributorID, iconElem.getAttribute(OPEN_ICON_PATH),iconElem.getAttribute(TOC_ICON_ID) + OPEN);
						if (attr.equals(CLOSED_ICON_PATH))
							IconFinder.setIconImagePath(contributorID,iconElem.getAttribute(CLOSED_ICON_PATH),iconElem.getAttribute(TOC_ICON_ID)+ CLOSED);
						if (attr.equals(LEAF_ICON_PATH))
							IconFinder.setIconImagePath(contributorID, iconElem.getAttribute(LEAF_ICON_PATH),iconElem.getAttribute(TOC_ICON_ID) + LEAF);
						if (attr.equals(ICON_ALT_TEXT))
							IconFinder.setIconAltText(iconElem.getAttribute(ICON_ALT_TEXT),iconElem.getAttribute(TOC_ICON_ID) + ALT);
					}
				}
			}
		}
	}
}
