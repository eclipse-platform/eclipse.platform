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
package org.eclipse.help.internal.toc;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.InvalidRegistryObjectException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.AbstractTocProvider;
import org.eclipse.help.ITocContribution;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.util.ResourceLocator;
import org.xml.sax.SAXParseException;

/*
 * Provides toc data from toc XML files to the help system.
 */
public class TocFileProvider extends AbstractTocProvider {

	public static final String EXTENSION_POINT_ID_TOC = HelpPlugin.PLUGIN_ID + ".toc"; //$NON-NLS-1$
	public static final String ELEMENT_NAME_TOC = "toc"; //$NON-NLS-1$
	public static final String ATTRIBUTE_NAME_FILE = "file"; //$NON-NLS-1$
	public static final String ATTRIBUTE_NAME_PRIMARY = "primary"; //$NON-NLS-1$
	public static final String ATTRIBUTE_NAME_EXTRADIR = "extradir"; //$NON-NLS-1$
	public static final String ATTRIBUTE_NAME_CATEGORY = "category"; //$NON-NLS-1$

	@Override
	public ITocContribution[] getTocContributions(String locale) {
		List<ITocContribution> contributions = new ArrayList<>();
		TocFile[] tocFiles = getTocFiles(locale);
		TocFileParser parser = new TocFileParser();
		for (int i=0;i<tocFiles.length;++i) {
			try {
				ITocContribution toc = parser.parse(tocFiles[i]);
				contributions.add(toc);
			}
			catch (Throwable t) {
				String locationInfo = ""; //$NON-NLS-1$
				if (t instanceof SAXParseException) {
					SAXParseException spe = (SAXParseException) t;
					locationInfo = " at line " + spe.getLineNumber()  //$NON-NLS-1$
								 + ", column " + spe.getColumnNumber(); //$NON-NLS-1$
				}
				String pluginId = tocFiles[i].getPluginId();
				String file = tocFiles[i].getFile();
				String msg = "Error reading help table of contents file /\""  //$NON-NLS-1$
					+ ResourceLocator.getErrorPath(pluginId, file, locale)
					+ locationInfo
					+ "\" (skipping file)"; //$NON-NLS-1$
				Platform.getLog(getClass()).error(msg, t);
			}
		}
		return contributions.toArray(new ITocContribution[contributions.size()]);
	}

	/*
	 * Returns all available TocFiles for the given locale.
	 */
	protected TocFile[] getTocFiles(String locale) {
		List<TocFile> tocFiles = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(EXTENSION_POINT_ID_TOC);
		for (int i=0;i<elements.length;++i) {
			IConfigurationElement elem = elements[i];
			String pluginId;
			try {
				pluginId = elem.getNamespaceIdentifier();
			}
			catch (InvalidRegistryObjectException e) {
				// no longer valid; skip it
				continue;
			}

			if (elem.getName().equals(ELEMENT_NAME_TOC)) {
				String file = elem.getAttribute(ATTRIBUTE_NAME_FILE);
				boolean primary = "true".equalsIgnoreCase(elem.getAttribute(ATTRIBUTE_NAME_PRIMARY)); //$NON-NLS-1$
				String extradir = elem.getAttribute(ATTRIBUTE_NAME_EXTRADIR);
				String category = elem.getAttribute(ATTRIBUTE_NAME_CATEGORY);
				TocFile tocFile = new TocFile(pluginId, file, primary, locale, extradir, category);
				tocFiles.add(tocFile);
			}
		}
		return tocFiles.toArray(new TocFile[tocFiles.size()]);
	}

	@Override
	public int getPriority() {
		return TOC_FILE_PRIORITY;
	}
}
