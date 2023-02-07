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
package org.eclipse.help.internal.index;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.AbstractIndexProvider;
import org.eclipse.help.IIndexContribution;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.util.ResourceLocator;
import org.xml.sax.SAXParseException;

/*
 * Provides index data from index XML files to the help system.
 */
public class IndexFileProvider extends AbstractIndexProvider {

	private static final String ERROR_READING_HELP_KEYWORD_INDEX_FILE = "Error reading help keyword index file /\""; //$NON-NLS-1$
	public static final String EXTENSION_POINT_ID_INDEX = HelpPlugin.PLUGIN_ID + ".index"; //$NON-NLS-1$
	public static final String ELEMENT_NAME_INDEX = "index"; //$NON-NLS-1$
	public static final String ATTRIBUTE_NAME_FILE = "file"; //$NON-NLS-1$

	@Override
	public IIndexContribution[] getIndexContributions(String locale) {
		List<IIndexContribution> contributions = new ArrayList<>();
		IndexFile[] indexFiles = getIndexFiles(locale);
		IndexFileParser parser = new IndexFileParser();
		for (int i=0;i<indexFiles.length;++i) {
			IndexFile indexFile = indexFiles[i];
			try {
				IIndexContribution toc = parser.parse(indexFile);
				contributions.add(toc);
			}  catch (SAXParseException spe) {
				StringBuilder buffer = new StringBuilder(ERROR_READING_HELP_KEYWORD_INDEX_FILE);
				buffer.append(getIndexFilePath(indexFile));
				buffer.append("\" at line "); //$NON-NLS-1$
				buffer.append(spe.getLineNumber());
				buffer.append(". "); //$NON-NLS-1$
				buffer.append(spe.getMessage());

				// Use the contained exception.
				Exception x = spe;
				if (spe.getException() != null)
					x = spe.getException();
				Platform.getLog(getClass()).error(buffer.toString(), x);

			}
			catch (Throwable t) {
				String msg = ERROR_READING_HELP_KEYWORD_INDEX_FILE + getIndexFilePath(indexFile) + "\" (skipping file)"; //$NON-NLS-1$
				Platform.getLog(getClass()).error(msg, t);
			}
		}
		return contributions.toArray(new IIndexContribution[contributions.size()]);
	}

	private String getIndexFilePath(IndexFile indexFile) {
		String pluginId = indexFile.getPluginId();
		String file = indexFile.getFile();
		return ResourceLocator.getErrorPath(pluginId, file, indexFile.getLocale());
	}

	/*
	 * Returns all available IndexFiles for the given locale.
	 */
	private IndexFile[] getIndexFiles(String locale) {
		List<IndexFile> indexFiles = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(EXTENSION_POINT_ID_INDEX);
		for (int i=0;i<elements.length;++i) {
			IConfigurationElement elem = elements[i];
			String pluginId = elem.getContributor().getName();
			if (elem.getName().equals(ELEMENT_NAME_INDEX)) {
				String file = elem.getAttribute(ATTRIBUTE_NAME_FILE);
				IndexFile indexFile = new IndexFile(pluginId, file, locale);
				indexFiles.add(indexFile);
			}
		}
		return indexFiles.toArray(new IndexFile[indexFiles.size()]);
	}
}
