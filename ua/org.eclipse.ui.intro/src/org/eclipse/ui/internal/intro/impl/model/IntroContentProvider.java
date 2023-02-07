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

import org.osgi.framework.Bundle;
import org.w3c.dom.Element;

/**
 * An intro content provider element. This element allows intro page to
 * dynamically pull data from various sources (e.g., the web, eclipse, etc) and
 * provide content based on this dynamic data. The element's class must
 * implement the IIntroContentProvider interface. The pluginId attribute can be
 * used if the class doesn't come from the plugin that defined the markup. The
 * text content should be used only if we fail to load the class. <br>
 *
 * INTRO: model class has access to style-id attribute but it is not used in the
 * schema.
 */
public class IntroContentProvider extends AbstractTextElement {
	public static final String TAG_CONTENT_PROVIDER = "contentProvider"; //$NON-NLS-1$

	private static final String ATT_PLUGIN_ID = "pluginId"; //$NON-NLS-1$
	private static final String ATT_CLASS = "class"; //$NON-NLS-1$

	private String contentProvider;
	private String pluginId;


	public IntroContentProvider(Element element, Bundle bundle) {
		super(element, bundle);
		contentProvider = getAttribute(element, ATT_CLASS);
		pluginId = getAttribute(element, ATT_PLUGIN_ID);
	}

	/**
	 * Returns the content provider, which should implement
	 * IIntroContentProvider
	 *
	 * @return Returns the contentProvider.
	 */
	public String getClassName() {
		return contentProvider;
	}

	/**
	 * Returns the id of the plugin that contains the content provider class
	 *
	 * @return Returns the pluginId.
	 */
	public String getPluginId() {
		return pluginId;
	}

	@Override
	public int getType() {
		return AbstractIntroElement.CONTENT_PROVIDER;
	}
}
