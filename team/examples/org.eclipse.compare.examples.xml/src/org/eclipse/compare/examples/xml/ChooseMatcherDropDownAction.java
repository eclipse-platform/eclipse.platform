/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
package org.eclipse.compare.examples.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;


/**
 * Drop down menu to select a particular id mapping scheme
 */
class ChooseMatcherDropDownAction extends Action implements IMenuCreator {

	private final XMLStructureViewer fViewer;

	public ChooseMatcherDropDownAction(XMLStructureViewer viewer) {
		fViewer = viewer;
		setText(XMLCompareMessages.ChooseMatcherDropDownAction_text);
		setImageDescriptor(XMLPlugin.getDefault().getImageDescriptor("obj16/smartmode_co.gif")); //$NON-NLS-1$
		setToolTipText(XMLCompareMessages.ChooseMatcherDropDownAction_tooltip);
		setMenuCreator(this);
	}

	@Override
	public void dispose() {
		// nothing to do
	}

	@Override
	public Menu getMenu(Menu parent) {
		return null;
	}

	@Override
	public Menu getMenu(Control parent) {
		XMLPlugin plugin= XMLPlugin.getDefault();
		Menu menu= new Menu(parent);
		addActionToMenu(menu, new SelectMatcherAction(XMLStructureCreator.USE_UNORDERED, fViewer));
		addActionToMenu(menu, new SelectMatcherAction(XMLStructureCreator.USE_ORDERED, fViewer));
		new MenuItem(menu, SWT.SEPARATOR);
		HashMap<String, HashMap<String, String>> IdMaps = plugin.getIdMaps();
		HashMap<String, HashMap<String, String>> IdMapsInternal = plugin.getIdMapsInternal();

		Set<String> keySetIdMaps = IdMaps.keySet();
		Set<String> keySetIdMapsInternal = IdMapsInternal.keySet();
		ArrayList<String> internalIdMapsAL= new ArrayList<>();
		for (String idmap_name : keySetIdMapsInternal) {
			internalIdMapsAL.add(idmap_name);
		}
		Object[] internalIdMapsA= internalIdMapsAL.toArray();
		Arrays.sort(internalIdMapsA);
		for (Object internalIdA : internalIdMapsA) {
			addActionToMenu(menu, new SelectMatcherAction((String) internalIdA, fViewer));
		}
		new MenuItem(menu, SWT.SEPARATOR);

		ArrayList<String> userIdMapsAL= new ArrayList<>();
		for (String idmap_name : keySetIdMaps) {
			userIdMapsAL.add(idmap_name);
		}

		HashMap<String, ArrayList<String>> OrderedElements = plugin.getOrderedElements();
		Set<String> keySetOrdered = OrderedElements.keySet();
		for (String idmap_name : keySetOrdered) {
			if (!keySetIdMaps.contains(idmap_name)) {
				userIdMapsAL.add(idmap_name);
			}
		}

		Object[] userIdMapsA= userIdMapsAL.toArray();
		Arrays.sort(userIdMapsA);
		for (Object userIdA : userIdMapsA) {
			addActionToMenu(menu, new SelectMatcherAction((String) userIdA, fViewer));
		}

		return menu;
	}

	protected void addActionToMenu(Menu parent, Action action) {
		ActionContributionItem item= new ActionContributionItem(action);
		item.fill(parent, -1);
	}

	@Override
	public void run() {
		fViewer.contentChanged();
	}
}
