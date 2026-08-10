/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
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
package org.eclipse.core.internal.runtime.bundlegroups.platformxml;

/**
 * Element and attribute names used in a platform.xml file.
 */
public interface ConfigurationConstants {

	String ELEMENT_CONFIG = "config"; //$NON-NLS-1$
	String ELEMENT_FEATURE = "feature"; //$NON-NLS-1$
	String ELEMENT_SITE = "site"; //$NON-NLS-1$

	String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	String ATTRIBUTE_PLUGIN_IDENTIFIER = "plugin-identifier"; //$NON-NLS-1$
	String ATTRIBUTE_PLUGIN_VERSION = "plugin-version"; //$NON-NLS-1$
	String ATTRIBUTE_SHARED_UR = "shared_ur"; //$NON-NLS-1$
	String ATTRIBUTE_URL = "url"; //$NON-NLS-1$
	String ATTRIBUTE_VERSION = "version"; //$NON-NLS-1$

}
