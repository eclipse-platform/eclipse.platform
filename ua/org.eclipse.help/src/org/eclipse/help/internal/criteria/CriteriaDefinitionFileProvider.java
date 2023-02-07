/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
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
package org.eclipse.help.internal.criteria;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.AbstractCriteriaDefinitionProvider;
import org.eclipse.help.ICriteriaDefinitionContribution;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.util.ResourceLocator;
import org.xml.sax.SAXParseException;

/*
 * Provides criteria definition data from XML files to the help system.
 */
public class CriteriaDefinitionFileProvider extends AbstractCriteriaDefinitionProvider {

	private static final String ERROR_READING_HELP_CRITERIA_DEFINITION_FILE = "Error reading criteria definition file /\""; //$NON-NLS-1$
	public static final String EXTENSION_POINT_ID_CRITERIA_DEFINITION = HelpPlugin.PLUGIN_ID + ".criteriaDefinition"; //$NON-NLS-1$
	public static final String ELEMENT_NAME_CRITERIA = "criteriaDefinition"; //$NON-NLS-1$
	public static final String ATTRIBUTE_NAME_FILE = "file"; //$NON-NLS-1$

	@Override
	public ICriteriaDefinitionContribution[] getCriteriaDefinitionContributions(String locale) {
		List<ICriteriaDefinitionContribution> contributions = new ArrayList<>();
		CriteriaDefinitionFile[] criteriaDefinitionFiles = getCriteriaDefinitionFiles(locale);
		CriteriaDefinitionFileParser parser = new CriteriaDefinitionFileParser();
		for (int i = 0; i < criteriaDefinitionFiles.length; ++i) {
			CriteriaDefinitionFile criteriaDefinitionFile = criteriaDefinitionFiles[i];
			try {
				ICriteriaDefinitionContribution criteria = parser.parse(criteriaDefinitionFile);
				contributions.add(criteria);
			}  catch (SAXParseException spe) {
				StringBuilder buffer = new StringBuilder(ERROR_READING_HELP_CRITERIA_DEFINITION_FILE);
				buffer.append(getCriteriaDefinitionFilePath(criteriaDefinitionFile));
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
				String msg = ERROR_READING_HELP_CRITERIA_DEFINITION_FILE + getCriteriaDefinitionFilePath(criteriaDefinitionFile) + "\" (skipping file)"; //$NON-NLS-1$
				Platform.getLog(getClass()).error(msg, t);
			}
		}
		return contributions.toArray(new ICriteriaDefinitionContribution[contributions.size()]);
	}

	private String getCriteriaDefinitionFilePath(CriteriaDefinitionFile criteriaDefinitionFile) {
		String pluginId = criteriaDefinitionFile.getPluginId();
		String file = criteriaDefinitionFile.getFile();
		return ResourceLocator.getErrorPath(pluginId, file, criteriaDefinitionFile.getLocale());
	}

	/*
	 * Returns all available CriteriaDefinitionFiles for the given locale.
	 */
	private CriteriaDefinitionFile[] getCriteriaDefinitionFiles(String locale) {
		List<CriteriaDefinitionFile> criteriaDefinitionFiles = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(EXTENSION_POINT_ID_CRITERIA_DEFINITION);
		for (int i=0;i<elements.length;++i) {
			IConfigurationElement elem = elements[i];
			String pluginId = elem.getContributor().getName();
			if (elem.getName().equals(ELEMENT_NAME_CRITERIA)) {
				String file = elem.getAttribute(ATTRIBUTE_NAME_FILE);
				CriteriaDefinitionFile criteriaDefinitionFile = new CriteriaDefinitionFile(pluginId, file, locale);
				criteriaDefinitionFiles.add(criteriaDefinitionFile);
			}
		}
		return criteriaDefinitionFiles.toArray(new CriteriaDefinitionFile[criteriaDefinitionFiles.size()]);
	}
}
