/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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

package org.eclipse.ui.internal.intro.impl.model;

import org.eclipse.core.runtime.IConfigurationElement;

/**
 * An intro standby content part registration. This model class does not appear
 * as a child under any of the other model classes. It is returned by the
 * ExtensionPointManager when asked for registration parts.
 */
public class IntroStandbyContentPart extends AbstractIntroIdElement {

	public static final String TAG_STANDBY_CONTENT_PART = "standbyContentPart"; //$NON-NLS-1$

	private static final String ATT_PLUGIN_ID = "pluginId"; //$NON-NLS-1$
	private static final String ATT_CLASS = "class"; //$NON-NLS-1$

	private String pluginId;
	private String className;

	/**
	 * Note: model class with public constructor because it is not instantiated
	 * by the model root.
	 *
	 * @param element
	 */
	public IntroStandbyContentPart(IConfigurationElement element) {
		super(element);
		pluginId = element.getAttribute(ATT_PLUGIN_ID);
		className = element.getAttribute(ATT_CLASS);
	}


	/**
	 * @return Returns the className.
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @return Returns the pluginId.
	 */
	public String getPluginId() {
		return pluginId;
	}

	@Override
	public int getType() {
		// this model class does not need a type so far.
		return 0;
	}
}
