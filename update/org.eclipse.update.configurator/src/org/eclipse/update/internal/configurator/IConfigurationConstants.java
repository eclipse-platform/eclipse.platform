/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import org.eclipse.update.configurator.IPlatformConfiguration.*;

/**
 * Constants
 */
public interface IConfigurationConstants {
	public static final String CFG = "config"; //$NON-NLS-1$
	public static final String CFG_BOOT_PLUGIN = "bootstrap"; //$NON-NLS-1$
	public static final String CFG_SITE = "site"; //$NON-NLS-1$
	public static final String CFG_URL = "url"; //$NON-NLS-1$
	public static final String CFG_POLICY = "policy"; //$NON-NLS-1$
	public static final String[] CFG_POLICY_TYPE = { "USER-INCLUDE", "USER-EXCLUDE" }; //$NON-NLS-1$ //$NON-NLS-2$
	public static final String CFG_POLICY_TYPE_UNKNOWN = "UNKNOWN"; //$NON-NLS-1$
	public static final String CFG_LIST = "list"; //$NON-NLS-1$
	public static final String CFG_STAMP = "stamp"; //$NON-NLS-1$
	public static final String CFG_FEATURE_STAMP = "stamp.features"; //$NON-NLS-1$
	public static final String CFG_PLUGIN_STAMP = "stamp.plugins"; //$NON-NLS-1$
	public static final String CFG_UPDATEABLE = "updateable"; //$NON-NLS-1$
	public static final String CFG_LINK_FILE = "linkfile"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY = "feature"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY_DEFAULT = "feature.default.id"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY_ID = "id"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY_PRIMARY = "primary"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY_VERSION = "version"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY_PLUGIN_VERSION = "plugin-version"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY_PLUGIN_IDENTIFIER = "plugin-identifier"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY_APPLICATION = "application"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY_ROOT = "root"; //$NON-NLS-1$
	public static final String CFG_DATE = "date";
	public static final String CFG_PLUGIN = "plugin";
	public static final String CFG_FRAGMENT = "fragment";
	public static final String CFG_ENABLED = "enabled";
	public static final String CFG_SHARED_URL = "shared_ur";
	

	public static final String CFG_VERSION = "version"; //$NON-NLS-1$
	public static final String CFG_TRANSIENT = "transient"; //$NON-NLS-1$
	public static final String VERSION = "3.0"; //$NON-NLS-1$
//	public static final String EOF = "eof"; //$NON-NLS-1$
	public static final int CFG_LIST_LENGTH = 10;

	public static final int DEFAULT_POLICY_TYPE = ISitePolicy.USER_EXCLUDE;
	public static final String[] DEFAULT_POLICY_LIST = new String[0];
	
	public static final String PLUGINS = "plugins"; //$NON-NLS-1$
	public static final String FEATURES = "features"; //$NON-NLS-1$
	public static final String PLUGIN_XML = "plugin.xml"; //$NON-NLS-1$
	public static final String FRAGMENT_XML = "fragment.xml"; //$NON-NLS-1$
	public static final String META_MANIFEST_MF = "META-INF/MANIFEST.MF"; //$NON-NLS-1$
	public static final String FEATURE_XML = "feature.xml"; //$NON-NLS-1$
}
