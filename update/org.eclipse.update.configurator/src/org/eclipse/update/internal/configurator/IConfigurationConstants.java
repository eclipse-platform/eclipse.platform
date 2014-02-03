/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
	public static final String ECLIPSE_PRODUCT = "eclipse.product"; //$NON-NLS-1$
	public static final String ECLIPSE_APPLICATION = "eclipse.application"; //$NON-NLS-1$
	public static final String CFG = "config"; //$NON-NLS-1$
	public static final String CFG_SITE = "site"; //$NON-NLS-1$
	public static final String CFG_URL = "url"; //$NON-NLS-1$
	public static final String CFG_POLICY = "policy"; //$NON-NLS-1$
	public static final String[] CFG_POLICY_TYPE = { "USER-INCLUDE", "USER-EXCLUDE", "MANAGED-ONLY" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	public static final String CFG_POLICY_TYPE_UNKNOWN = "UNKNOWN"; //$NON-NLS-1$
	public static final String CFG_LIST = "list"; //$NON-NLS-1$
	public static final String CFG_UPDATEABLE = "updateable"; //$NON-NLS-1$
	public static final String CFG_LINK_FILE = "linkfile"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY = "feature"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY_ID = "id"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY_PRIMARY = "primary"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY_VERSION = "version"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY_PLUGIN_VERSION = "plugin-version"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY_PLUGIN_IDENTIFIER = "plugin-identifier"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY_APPLICATION = "application"; //$NON-NLS-1$
	public static final String CFG_FEATURE_ENTRY_ROOT = "root"; //$NON-NLS-1$
	public static final String CFG_DATE = "date"; //$NON-NLS-1$
	public static final String CFG_PLUGIN = "plugin"; //$NON-NLS-1$
	public static final String CFG_FRAGMENT = "fragment"; //$NON-NLS-1$
	public static final String CFG_ENABLED = "enabled"; //$NON-NLS-1$
	public static final String CFG_SHARED_URL = "shared_ur"; //$NON-NLS-1$
	

	public static final String CFG_VERSION = "version"; //$NON-NLS-1$
	public static final String CFG_TRANSIENT = "transient"; //$NON-NLS-1$
	public static final String VERSION = "3.0"; //$NON-NLS-1$

	public static final int DEFAULT_POLICY_TYPE = ISitePolicy.USER_EXCLUDE;
	public static final String[] DEFAULT_POLICY_LIST = new String[0];
	
	public static final String PLUGINS = "plugins"; //$NON-NLS-1$
	public static final String FEATURES = "features"; //$NON-NLS-1$
	public static final String PLUGIN_XML = "plugin.xml"; //$NON-NLS-1$
	public static final String FRAGMENT_XML = "fragment.xml"; //$NON-NLS-1$
	public static final String META_MANIFEST_MF = "META-INF/MANIFEST.MF"; //$NON-NLS-1$
	public static final String FEATURE_XML = "feature.xml"; //$NON-NLS-1$
}
