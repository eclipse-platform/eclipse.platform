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

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.internal.intro.impl.util.ImageUtil;
import org.eclipse.ui.intro.config.IntroElement;

/**
 * An Intro Config component that captures launch bar shortcut information.
 *
 * @since 3.1
 */
public class IntroLaunchBarShortcut extends AbstractIntroElement {

	protected static final String TAG_SHORTCUT = "shortcut"; //$NON-NLS-1$

	private static final String ATT_TOOLTIP = "tooltip"; //$NON-NLS-1$
	private static final String ATT_ICON = "icon"; //$NON-NLS-1$
	private static final String ATT_URL = "url"; //$NON-NLS-1$

	private IntroElement ielement;

	IntroLaunchBarShortcut(IConfigurationElement element, IntroElement ielement) {
		super(element);
		this.ielement = ielement;
	}

	IntroLaunchBarShortcut(IConfigurationElement element) {
		super(element);
	}


	@Override
	public int getType() {
		return AbstractIntroElement.LAUNCH_BAR_SHORTCUT;
	}

	private String getAttribute(String name) {
		if (ielement!=null)
			return ielement.getAttribute(name);
		return getCfgElement().getAttribute(name);
	}

	/**
	 * Returns the URL of this shortcut.
	 *
	 * @return
	 */
	public String getURL() {
		return getAttribute(ATT_URL);
	}

	/**
	 * Returns the tooltip of this shortcut.
	 *
	 * @return
	 */
	public String getToolTip() {
		return getAttribute(ATT_TOOLTIP);
	}

	/**
	 * Returns the relative icon path of this shortcut.
	 *
	 * @return
	 */
	private String getIcon() {
		return getAttribute(ATT_ICON);
	}

	/**
	 * Returns the icon image of this shortcut, or <code>null</code> if not
	 * found.
	 *
	 * @return
	 */
	public ImageDescriptor getImageDescriptor() {
		String icon = getIcon();
		if (icon!=null) {
			try {
				URL imageUrl = new URL(icon);
				ImageDescriptor desc = ImageDescriptor.createFromURL(imageUrl);
				return desc;
			}
			catch (MalformedURLException e) {
				// not a full url
			}
		}
		return ImageUtil.createImageDescriptor(getBundle(), getIcon());
	}
}
