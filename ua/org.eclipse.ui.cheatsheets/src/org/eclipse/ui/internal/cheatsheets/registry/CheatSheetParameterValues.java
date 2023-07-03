/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.registry;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.commands.IParameterValues;

/**
 * Provides the parameter values for the open cheat sheet command.
 *
 * @since 3.2
 */
public class CheatSheetParameterValues implements IParameterValues {

	@Override
	public Map<String, String> getParameterValues() {
		Map<String, String> values = new TreeMap<>();

		CheatSheetCollectionElement cheatSheetCollection = CheatSheetRegistryReader
				.getInstance().getCheatSheets();
		populateValues(values, cheatSheetCollection);

		return values;
	}

	private void populateValues(Map<String, String> values,
			CheatSheetCollectionElement cheatSheetCollection) {

		Object[] cheatsheets = cheatSheetCollection.getCheatSheets();
		for (Object cheatsheet : cheatsheets) {
			if (cheatsheet instanceof CheatSheetElement) {
				CheatSheetElement element = (CheatSheetElement) cheatsheet;
				values.put(element.getLabel(null), element.getID());
			}
		}

		Object[] children = cheatSheetCollection.getChildren();
		for (Object child : children) {
			if (child instanceof CheatSheetCollectionElement) {
				populateValues(values, (CheatSheetCollectionElement) child);
			}
		}
	}

}
