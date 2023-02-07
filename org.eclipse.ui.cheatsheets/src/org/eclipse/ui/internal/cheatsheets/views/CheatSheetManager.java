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
package org.eclipse.ui.internal.cheatsheets.views;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.ui.cheatsheets.CheatSheetListener;
import org.eclipse.ui.cheatsheets.ICheatSheetEvent;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;

/**
 * Cheat sheet manager class.  Manages cheat sheet data and optional listner.
 */
public class CheatSheetManager implements ICheatSheetManager {

	private static final String PARENT_PREFIX = "parent."; //$NON-NLS-1$
	private static final String VARIABLE_END = "}"; //$NON-NLS-1$
	private static final String VARIABLE_BEGIN = "${"; //$NON-NLS-1$
	private String cheatsheetID;
	private List<CheatSheetListener> listeners;
	private Map<String, String> dataTable = null;
	private ICheatSheetManager parent;

	public CheatSheetManager(CheatSheetElement element) {
		cheatsheetID = element.getID();
		listeners = new ArrayList<>();
		CheatSheetListener listener = element.createListenerInstance();
		if (listener != null) {
			addListener(listener);
		}
	}

	@Override
	public String getCheatSheetID() {
		return cheatsheetID;
	}

	public void fireEvent(int eventType) {
		// Send an event to every listener
		for (CheatSheetListener listener : listeners) {
			ICheatSheetEvent event = new CheatSheetEvent(eventType, cheatsheetID, this);
			listener.cheatSheetEvent(event);
		}
	}

	/**
	 * returns the hashtable with all manager data stored.
	 */
	public Map<String, String> getData() {
		return dataTable;
	}

	/**
	 * Initialize all variables
	 * @param data a map containg values for all variables
	 */
	public void setData(Map<String, String> data) {
		dataTable = data;
	}

	@Override
	public String getData(String key) {
		if (dataTable == null)
			return null;
		return dataTable.get(key);
	}

	/**
	 * Similar to get data except that if the key is prefixed with "parent."
	 * get the data from the parent
	 * @param qualifiedKey
	 * @return The data for this key
	 */
	public String getDataQualified(String qualifiedKey) {
		if (qualifiedKey.startsWith(PARENT_PREFIX) && parent != null) {
			return parent.getData(qualifiedKey.substring(PARENT_PREFIX.length()));
		} else {
			return getData(qualifiedKey);
		}
	}

	public String getVariableData(String variable) {
		String result = variable;
		if(variable != null && variable.startsWith(VARIABLE_BEGIN) && variable.endsWith(VARIABLE_END)) {
			result = variable.substring(2,variable.length()-1);
			result = getDataQualified(result);
		}
		return result;
	}

	/**
	 * Substitute occurences of ${data} with values from the cheatsheetmanager.
	 * @param input The input string
	 * @param csm The cheatsheet manager
	 * @return The input string with substitutions made for any cheatsheet
	 * variables encountered.
	 */
	public String performVariableSubstitution(String input)
	{
		String remaining = input;
		String output = ""; //$NON-NLS-1$
		while (remaining.length() > 0) {
			int varIndex = remaining.indexOf(VARIABLE_BEGIN);
			int endIndex = remaining.indexOf(VARIABLE_END, varIndex + 1);
			if (varIndex < 0 || endIndex < 0) {
				output += remaining;
				remaining = ""; //$NON-NLS-1$
			} else {
				String varName = remaining.substring(varIndex + VARIABLE_BEGIN.length(),
												 endIndex);
				String value = getDataQualified(varName);
				output += remaining.substring(0, varIndex);
				if (value != null) {
					output += value;
				}
				remaining = remaining.substring(endIndex + VARIABLE_END.length());
			}
		}
		return output;
	}

	/* package */ void setData(Hashtable<String, String> data) {
		dataTable = data;
	}

	@Override
	public void setData(String key, String data) {
		if (key == null) {
			throw new IllegalArgumentException();
		}

		if(data == null && dataTable != null) {
			dataTable.remove(key);
			return;
		}

		if (dataTable == null) {
			dataTable = new Hashtable<>(30);
		}

		dataTable.put(key, data);
	}

	/**
	 * Similar to setData except that if the key is prefixed by "parent."
	 * set the data in the parent.
	 * @param qualifiedKey A key which may be prefixed by parent.
	 * @param data The value to set
	 */
	public void setDataQualified(String qualifiedKey, String data) {
		if (qualifiedKey == null) {
			throw new IllegalArgumentException();
		}
		if (qualifiedKey.startsWith(PARENT_PREFIX) && parent != null) {
			parent.setData(qualifiedKey.substring(PARENT_PREFIX.length()), data);
		} else {
			setData(qualifiedKey, data);
		}
	}

	/**
	 * Add a listener for cheatsheet events
	 * @param listener
	 */
	public void addListener(CheatSheetListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	@Override
	public ICheatSheetManager getParent() {
		return parent;
	}

	public void setParent(ICheatSheetManager parent) {
		this.parent = parent;
	}

	@Override
	public Set<String> getKeySet() {
		if (dataTable == null) {
			return new HashSet<>();
		} else {
			return dataTable.keySet();
		}
	}
}
