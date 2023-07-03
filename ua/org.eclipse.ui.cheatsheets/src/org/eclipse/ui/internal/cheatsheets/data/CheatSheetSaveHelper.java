/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;
import org.eclipse.ui.internal.cheatsheets.views.CoreItem;
import org.eclipse.ui.internal.cheatsheets.views.SubItemCompositeHolder;
import org.eclipse.ui.internal.cheatsheets.views.ViewItem;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.InputSource;

public class CheatSheetSaveHelper {

	// Get the path to the cheatsheet folder in the .metadata folder of
	// workspace.
	protected IPath savePath;

	private static final String DOT_XML = ".xml"; //$NON-NLS-1$

	/**
	 * Constructor for CheatSheetSaveHelper.
	 */
	public CheatSheetSaveHelper() {
		super();
		savePath = CheatSheetPlugin.getPlugin().getStateLocation();
	}


	/**
	 * Create the properties used to save the state of a cheatsheet
	 * @param currentItemNum the current item
	 * @param items a list of the items in this cheatsheet
	 * @param buttonIsDown
	 * @param expandRestoreStates
	 * @param csID the cheatsheet id
	 * @param contentPath will be null if the cheatsheet was launched using information from
	 * the registry, otherwise it is the url of the cheatsheet content file.
	 */
	public Properties createProperties(int currentItemNum, ArrayList<ViewItem> items,
			boolean buttonIsDown, ArrayList<String> expandRestoreStates, String csID, String contentPath) {
		Properties props = new Properties();
		Hashtable<String, String> subcompletedTable = new Hashtable<>(10);
		Hashtable<String, String> subskippedTable = new Hashtable<>(10);

		int buttonState = 0;
		if (buttonIsDown)
			buttonState = 1;

		props.put(IParserTags.ID, csID);
		props.put(IParserTags.CURRENT, Integer.toString(currentItemNum));
		if (contentPath != null) {
			props.put(IParserTags.CONTENT_URL, contentPath);
		}
		ArrayList<String> completedList = new ArrayList<>();
		ArrayList<String> expandedList = new ArrayList<>();

		if (expandRestoreStates == null)
			expandRestoreStates = new ArrayList<>();

		// Assemble lists of expanded items and completed items.
		for (int i = 0; i < items.size(); i++) {
			ViewItem item = items.get(i);
			if (item.isCompleted()) {
				completedList.add(Integer.toString(i));
			}
			if (item.isExpanded()) {
				expandedList.add(Integer.toString(i));
			}

			if (item instanceof CoreItem) {
				CoreItem withsubs = (CoreItem) item;
				ArrayList<SubItemCompositeHolder> compList = withsubs
						.getListOfSubItemCompositeHolders();
				if (compList != null) {
					StringBuilder skippedsubItems = new StringBuilder();
					StringBuilder completedsubItems = new StringBuilder();
					for (int j = 0; j < compList.size(); j++) {
						SubItemCompositeHolder sch = compList.get(j);
						if (sch.isCompleted())
							completedsubItems.append(Integer.toString(j) + ","); //$NON-NLS-1$
						if (sch.isSkipped())
							skippedsubItems.append(Integer.toString(j) + ","); //$NON-NLS-1$
					}
					if (completedsubItems.toString().length() > 0) {
						String csi = completedsubItems.toString();
						if (csi.endsWith(",")) //$NON-NLS-1$
							csi = csi.substring(0, csi.length() - 1);
						subcompletedTable.put(Integer.toString(i), csi);

					}
					if (skippedsubItems.toString().length() > 0) {
						String csi = skippedsubItems.toString();
						if (csi.endsWith(",")) //$NON-NLS-1$
							csi = csi.substring(0, csi.length() - 1);
						subskippedTable.put(Integer.toString(i), csi);
					}
				}
			}
		}

		// put expanded item list, completed list, button state
		props.put(IParserTags.COMPLETED, completedList);
		props.put(IParserTags.EXPANDED, expandedList);
		props.put(IParserTags.EXPANDRESTORE, expandRestoreStates);
		props.put(IParserTags.BUTTON, Integer.toString(buttonState));
		if (subcompletedTable != null)
			props.put(IParserTags.SUBITEMCOMPLETED, subcompletedTable);
		if (subskippedTable != null)
			props.put(IParserTags.SUBITEMSKIPPED, subskippedTable);

		return props;
	}

	/**
	 * Method parses attribute from named node map. Returns value as string.
	 */
	protected String getAttributeWithName(NamedNodeMap map, String name) {
		try {
			return map.getNamedItem(name).getNodeValue();
		} catch (Exception e) {
			return null;
		}
	}

	public IPath getStateFile(String csID) {
		return getStateFile(csID, savePath);
	}

	protected IPath getStateFile(String csID, IPath rootPath) {
		return IPath.fromOSString(rootPath.append(csID + ".xml").toOSString()); //$NON-NLS-1$
	}

	// Attempts to read an xml file from the provided url. Returns a Dom
	// Document object if parses ok,
	// returns null if the parse or read fails.
	protected Document readXMLFile(URL url) {
		InputStream is = null;
		InputSource source = null;

		try {
			is = url.openStream();
			if (is != null) {
				source = new InputSource(is);
			}
		} catch (Exception e) {
			return null;
		}

		if (source == null)
			return null;

		try {
			DocumentBuilder documentBuilder = CheatSheetPlugin.getPlugin()
					.getDocumentBuilder();
			return documentBuilder.parse(source);
		} catch (Exception e) {
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException ioe) {
			}
		}

		return null;
	}

	/**
	 * @param saveProperties
	 * @param contentPath
	 * @param csm
	 */
	public IStatus saveState(Properties properties, CheatSheetManager csm) {
		String csID = (String) properties.get(IParserTags.ID);
		XMLMemento writeMemento = XMLMemento.createWriteRoot(IParserTags.CHEATSHEET_STATE);
		IStatus status = saveToMemento(properties, csm, writeMemento);
		if (!status.isOK()) {
			return status;
		}
		return CheatSheetPlugin.getPlugin().saveMemento(writeMemento, csID + DOT_XML);
	}

	@SuppressWarnings("unchecked")
	public IStatus saveToMemento(Properties properties, CheatSheetManager csm, IMemento writeMemento) {

		String csID = (String) properties.get(IParserTags.ID);
		try {
			writeMemento.putString(IParserTags.BUTTONSTATE, (String) properties
					.get(IParserTags.BUTTON));
			writeMemento.putString(IParserTags.ITEM, (String) properties.get(IParserTags.CURRENT));
			writeMemento.putString(IParserTags.ID, (String)properties.get(IParserTags.ID));
			String contentPath = (String)properties.get(IParserTags.CONTENT_URL);
			if (contentPath != null) {
				writeMemento.putString(IParserTags.CONTENT_URL, contentPath);
			}

			addListOfStringsToMemento(writeMemento,  properties, IParserTags.COMPLETED);
			addListOfStringsToMemento(writeMemento,  properties, IParserTags.EXPANDED);
			addListOfStringsToMemento(writeMemento,  properties, IParserTags.EXPANDRESTORE);

			addMapToMemento(writeMemento,  csm.getData(), IParserTags.MANAGERDATA);
			addMapToMemento(writeMemento, (Map<String, String>) properties.get(IParserTags.SUBITEMCOMPLETED),
					IParserTags.SUBITEMCOMPLETED);
			addMapToMemento(writeMemento, (Map<String, String>) properties.get(IParserTags.SUBITEMSKIPPED),
					IParserTags.SUBITEMSKIPPED);

		} catch (Exception e) {
			String message = NLS.bind(Messages.ERROR_SAVING_STATEFILE_URL,
					(new Object[] { csID }));
			IStatus status = new Status(IStatus.ERROR,
					ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, IStatus.OK,
					message, e);
			return status;
			//CheatSheetPlugin.getPlugin().getLog().log(status);
		}
		return Status.OK_STATUS;
	}

	/**
	 * @param csID The id of this cheatsheet
	 * @return The state of this cheatsheet or null
	 */
	public Properties loadState(String csID) {
		XMLMemento readMemento = CheatSheetPlugin.getPlugin().readMemento(csID + DOT_XML);
		if (readMemento == null) {
			return null;
		}
		return loadFromMemento(readMemento);
	}

	public Properties loadFromMemento(IMemento memento) {
		Properties properties = new Properties();
		properties.put(IParserTags.BUTTON, memento.getString(IParserTags.BUTTONSTATE));
		properties.put(IParserTags.CURRENT, memento.getString(IParserTags.ITEM));
		properties.put(IParserTags.ID, memento.getString(IParserTags.ID));
		String contentURL = memento.getString(IParserTags.CONTENT_URL);
		if (contentURL != null) {
			properties.put(IParserTags.CONTENT_URL, contentURL);
		}

		getListOfStringsFromMemento(memento,  properties, IParserTags.COMPLETED);
		getListOfStringsFromMemento(memento,  properties, IParserTags.EXPANDED);
		getListOfStringsFromMemento(memento,  properties, IParserTags.EXPANDRESTORE);

		getMapFromMemento(memento,  properties, IParserTags.SUBITEMCOMPLETED);
		getMapFromMemento(memento,  properties, IParserTags.SUBITEMSKIPPED);
		getMapFromMemento(memento,  properties, IParserTags.MANAGERDATA);
		return properties;
	}

	private void addListOfStringsToMemento(IMemento memento, Properties properties, String key) {
		@SuppressWarnings("unchecked")
		List<String> list = (List<String>) properties.get(key);
		if (list == null) {
			return;
		}
		for (String string : list) {
			IMemento childMemento = memento.createChild(key);
			childMemento.putString(IParserTags.ITEM, string);
		}
	}


	private void addMapToMemento(IMemento memento, Map<String, String> map, String mapName) {
		if (map == null) {
			return;
		}
		for (Map.Entry<String, String> entry : map.entrySet()) {
			String itemKey = entry.getKey();
			IMemento childMemento = memento.createChild(mapName);
			childMemento.putString(IParserTags.MANAGERDATAKEY,(itemKey));
			childMemento.putString(IParserTags.MANAGERDATAVALUE, entry.getValue());
		}
	}


	private void getMapFromMemento(IMemento memento, Properties properties, String mapName) {
		IMemento[] children = memento.getChildren(mapName);
		Map<String, String> map = new Hashtable<>();
		for (IMemento element : children) {
			map.put(element.getString(IParserTags.MANAGERDATAKEY), element.getString(IParserTags.MANAGERDATAVALUE));
		}
		properties.put(mapName, map);
	}

	private void getListOfStringsFromMemento(IMemento memento, Properties properties, String key) {
		IMemento[] children = memento.getChildren(key);
		List<String> list = new ArrayList<>();
		for (IMemento element : children) {
			list.add(element.getString(IParserTags.ITEM));
		}
		properties.put(key, list);
	}

}
