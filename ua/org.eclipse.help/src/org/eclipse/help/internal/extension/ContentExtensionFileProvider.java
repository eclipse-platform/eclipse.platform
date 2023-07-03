/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.extension;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.AbstractContentExtensionProvider;
import org.eclipse.help.IContentExtension;
import org.eclipse.help.internal.HelpPlugin;
import org.osgi.framework.Bundle;

/*
 * Provides the extensions from content extension XML files registered via
 * the org.eclipse.help.contentExtension extension point.
 */
public class ContentExtensionFileProvider extends AbstractContentExtensionProvider {

	private static final String EXTENSION_POINT_CONTENT_EXTENSION = HelpPlugin.PLUGIN_ID + ".contentExtension"; //$NON-NLS-1$
	private static final String ELEMENT_CONTENT_EXTENSION = "contentExtension"; //$NON-NLS-1$
	private static final String ATTRIBUTE_FILE = "file"; //$NON-NLS-1$
	private static final String ATTRIBUTE_CONTENT = "content"; //$NON-NLS-1$

	@Override
	public IContentExtension[] getContentExtensions(String locale) {
		List<IContentExtension> extensions = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		ContentExtensionFileParser parser = new ContentExtensionFileParser();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(EXTENSION_POINT_CONTENT_EXTENSION);
		for (int i=0;i<elements.length;++i) {
			if (ELEMENT_CONTENT_EXTENSION.equals(elements[i].getName())) {
				String file = elements[i].getAttribute(ATTRIBUTE_FILE);
				String bundleId = elements[i].getContributor().getName();
				Bundle bundle = Platform.getBundle(bundleId);
				try {
					ContentExtension[] ext = parser.parse(bundle, file);
					for (int j=0;j<ext.length;++j) {
						String content = ext[j].getAttribute(ATTRIBUTE_CONTENT);
						if (content != null) {
							ext[j].setAttribute(ATTRIBUTE_CONTENT, '/' + bundleId + '/' + content);
						}
						extensions.add(ext[j]);
					}
				}
				catch (Throwable t) {
					String msg = "Error reading user assistance content extension file /\"" + bundleId + '/' + file + "\" (skipping file)"; //$NON-NLS-1$ //$NON-NLS-2$
					ILog.of(getClass()).error(msg, t);
				}
			}
		}
		return extensions.toArray(new IContentExtension[extensions.size()]);
	}
}
