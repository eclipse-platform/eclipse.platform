/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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

import java.util.Hashtable;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.ui.internal.intro.impl.FontSelection;
import org.eclipse.ui.internal.intro.impl.model.util.BundleUtil;
import org.osgi.framework.Bundle;
import org.w3c.dom.Element;


public class IntroTheme extends AbstractIntroIdElement {
	private static final String ATT_NAME = "name"; //$NON-NLS-1$
	private static final String ATT_PATH = "path"; //$NON-NLS-1$
	private String name;
	private String path;
	private Hashtable<String, String> properties;
	private boolean scalable;

	public IntroTheme(IConfigurationElement element) {
		super(element);
		name = element.getAttribute(ATT_NAME);
		path = element.getAttribute(ATT_PATH);
		path = BundleUtil.getResolvedResourceLocation(path, getBundle());
		scalable = "true".equals(element.getAttribute(FontSelection.ATT_SCALABLE)); //$NON-NLS-1$
		loadProperties(element);
	}

	public IntroTheme(Element element, Bundle bundle) {
		super(element, bundle);
	}

	public IntroTheme(Element element, Bundle bundle, String base) {
		super(element, bundle, base);
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	@Override
	public int getType() {
		return THEME;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public boolean isScalable() {
		return scalable;
	}

	private void loadProperties(IConfigurationElement element) {
		IConfigurationElement [] children = element.getChildren("property"); //$NON-NLS-1$
		if (children.length==0)
			return;
		properties = new Hashtable<>();
		for (int i=0; i<children.length; i++) {
			IConfigurationElement property = children[i];
			String name = property.getAttribute("name"); //$NON-NLS-1$
			String value = property.getAttribute("value"); //$NON-NLS-1$
			if (name!=null && value!=null)
				properties.put(name, value);
		}
		// Put the theme id in the properties too
		properties.put(ATT_ID, getId());
	}
}
