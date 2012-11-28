/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.net.URL;
import java.util.Enumeration;
import java.util.ResourceBundle;

import org.eclipse.compare.ICompareFilter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.osgi.framework.Bundle;

/**
 * Describes compare filter extension.
 */
public class CompareFilterDescriptor {

	private final static String CLASS_ATTRIBUTE = "class"; //$NON-NLS-1$
	private final static String ID_ATTRIBUTE = "id"; //$NON-NLS-1$
	private final static String DEFINITION_ID_ATTRIBUTE = "definitionId"; //$NON-NLS-1$
	private final static String FILTER_IMAGE_ATTRIBUTE = "filter.image"; //$NON-NLS-1$

	private IConfigurationElement fConfiguration;
	private ResourceBundle fResourceBundle;
	private ImageDescriptor fImageDescriptor;

	private class ConfigurationKeysEnumeration implements Enumeration {

		private String[] keySet;
		private int cursor = 0;

		public ConfigurationKeysEnumeration(IConfigurationElement configuration) {
			super();
			this.keySet = configuration.getAttributeNames();
		}

		public boolean hasMoreElements() {
			return cursor >= keySet.length;
		}

		public Object nextElement() {
			return keySet[cursor++];
		}

	}

	public CompareFilterDescriptor(IConfigurationElement config) {
		fConfiguration = config;
		fResourceBundle = new ResourceBundle() {
			protected Object handleGetObject(String key) {
				return fConfiguration.getAttribute(key);
			}

			public Enumeration getKeys() {
				return new ConfigurationKeysEnumeration(fConfiguration);
			}
		};

		URL url = null;
		String pluginId = fConfiguration.getContributor().getName();
		Bundle bundle = Platform.getBundle(pluginId);
		if (bundle != null) {
			String path = Utilities.getString(fResourceBundle,
					FILTER_IMAGE_ATTRIBUTE, FILTER_IMAGE_ATTRIBUTE);
			if (path != null)
				url = FileLocator.find(bundle, new Path(path), null);
		}
		fImageDescriptor = (url == null) ? null : ImageDescriptor
				.createFromURL(url);
	}

	public ICompareFilter createCompareFilter() {
		try {
			return (ICompareFilter) fConfiguration
					.createExecutableExtension(CLASS_ATTRIBUTE);
		} catch (CoreException e) {
			CompareUIPlugin.log(e);
		}
		return null;
	}

	public String getFilterId() {
		return fConfiguration.getAttribute(ID_ATTRIBUTE);
	}

	public String getDefinitionId() {
		return fConfiguration.getAttribute(DEFINITION_ID_ATTRIBUTE);
	}

	public ResourceBundle getResourceBundle() {
		return fResourceBundle;
	}

	public ImageDescriptor getImageDescriptor() {
		return fImageDescriptor;
	}
}
