/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.search.SearchParticipantXML;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.cheatsheets.OpenCheatSheetAction;
import org.eclipse.ui.internal.cheatsheets.composite.parser.ICompositeCheatsheetTags;
import org.eclipse.ui.internal.cheatsheets.data.IParserTags;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetRegistryReader;
import org.xml.sax.Attributes;

public class CheatsheetSearchParticipant extends SearchParticipantXML {
	private static final String INTRO_DESC = "cheatsheet/intro/description"; //$NON-NLS-1$

	private static final String ITEM_DESC = "cheatsheet/item/description"; //$NON-NLS-1$

	private static final String CCS_DESC = "compositeCheatsheet/taskGroup/intro"; //$NON-NLS-1$

	/**
	 * Returns all the documents that this participant knows about. This method
	 * is only used for participants that handle documents outside of the help
	 * system's TOC.
	 *
	 * @return a set of hrefs for documents managed by this participant.
	 */
	@Override
	public Set<String> getAllDocuments(String locale) {
		HashSet<String> set = new HashSet<>();
		IConfigurationElement[] elements = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(
						ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID + '.'
						+ CheatSheetRegistryReader.CHEAT_SHEET_CONTENT);
		for (IConfigurationElement element : elements) {
			if (!element.getName().equals(CheatSheetRegistryReader.TAG_CHEATSHEET))
				continue;
			String fileName = element.getAttribute(CheatSheetRegistryReader.ATT_CONTENTFILE);
			String id = element.getAttribute("id"); //$NON-NLS-1$
			String pluginId = element.getContributor().getName();
			if (isExtensionValid(fileName, id, pluginId)) {
				try {
					fileName = resolveVariables(pluginId, fileName, locale);
					set.add("/" + pluginId + "/" + fileName + "?id=" + id); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				catch (Throwable t) {
					// log and skip
					CheatSheetPlugin.logError("Error parsing cheat sheet extension from plug-in " + pluginId + ", id " + id + ", file " + fileName, t); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
		}
		return set;
	}

	@Override
	public Set<String> getContributingPlugins() {
		IConfigurationElement[] elements = Platform.getExtensionRegistry()
				.getConfigurationElementsFor(
						ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID + '.'
						+ CheatSheetRegistryReader.CHEAT_SHEET_CONTENT);
		HashSet<String> set = new HashSet<>();
		for (IConfigurationElement element : elements) {
			if (element.getName().equals(CheatSheetRegistryReader.TAG_CHEATSHEET)) {
				set.add(element.getContributor().getName());
			}
		}
		return set;
	}

	@Override
	protected void handleStartElement(String name, Attributes attributes,
			IParsedXMLContent data) {
		switch (name) {
		case IParserTags.CHEATSHEET:
			data.setTitle(attributes.getValue(IParserTags.TITLE));
			data.addText(attributes.getValue(IParserTags.TITLE));
			break;
		case ICompositeCheatsheetTags.COMPOSITE_CHEATSHEET:
			data.addText(attributes.getValue(ICompositeCheatsheetTags.NAME));
			data.setTitle(attributes.getValue(ICompositeCheatsheetTags.NAME));
			break;
		case IParserTags.ITEM:
			data.addText(attributes.getValue(IParserTags.TITLE));
			break;
		case IParserTags.SUBITEM:
			data.addText(attributes.getValue(IParserTags.LABEL));
			break;
		case ICompositeCheatsheetTags.TASK:
		case ICompositeCheatsheetTags.TASK_GROUP:
			data.addText(attributes.getValue(ICompositeCheatsheetTags.NAME));
			break;
		default:
			break;
		}
	}

	@Override
	protected void handleEndElement(String name, IParsedXMLContent data) {
	}

	@Override
	protected void handleText(String text, IParsedXMLContent data) {
		String stackPath = getElementStackPath();
		String top = getTopElement();
		if (IParserTags.INTRO.equals(top)) {
			data.addText(text);
			if (stackPath.equalsIgnoreCase(CCS_DESC)) {
				data.addToSummary(text);
			}
		} else if (IParserTags.ON_COMPLETION.equals(top)) {
			data.addText(text);
		} else if (stackPath.equalsIgnoreCase(INTRO_DESC)) {
			data.addText(text);
			data.addToSummary(text);
			return;
		} else if (stackPath.equalsIgnoreCase(ITEM_DESC)) {
			data.addText(text);
			return;
		}
	}

	@Override
	public boolean open(String id) {
		Action openAction = new OpenCheatSheetAction(id);
		openAction.run();
		return true;
	}

	private static boolean isExtensionValid(String fileName, String id, String pluginId) {
		if (fileName.indexOf('\\') != -1) {
			CheatSheetPlugin.logError("Error in cheat sheet extension id " + id + " from plug-in " + pluginId + ": path should not contain back-slashes (\\): " + fileName + ". This cheat sheet will not be indexed for searching.", null); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			return false;
		}
		return true;
	}
}
