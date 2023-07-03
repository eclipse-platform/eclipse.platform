/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.views;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Observable;

import org.eclipse.core.runtime.IPath;
import org.eclipse.help.ui.internal.HelpUIPlugin;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.FrameworkUtil;

/**
 * Manages the scope for the federated search.
 */
public class ScopeSetManager extends Observable {
	private ScopeSet activeSet;

	private ScopeSet lastExplicitSet;

	private static final String ACTIVE_SET = "activeScopeSet"; //$NON-NLS-1$

	private ArrayList<ScopeSet> sets;

	private ScopeSet defSet;

	public ScopeSetManager() {
		ensureLocation();
		loadScopeSets();
	}

	public void add(ScopeSet set) {
		sets.add(set);
		setChanged();
	}

	public void remove(ScopeSet set) {
		sets.remove(set);
		set.dispose();
		setChanged();
	}

	public void setActiveSet(ScopeSet set) {
		if (this.activeSet != null) {
			this.activeSet.save();
		}
		this.activeSet = set;
		if (!activeSet.isImplicit())
			lastExplicitSet = set;
		setChanged();
	}

	public boolean restoreLastExplicitSet() {
		if (activeSet != null && activeSet.isImplicit()
				&& lastExplicitSet != null) {
			setActiveSet(lastExplicitSet);
			setChanged();
			return true;
		}
		return false;
	}

	public static void ensureLocation() {
		IPath location = HelpUIPlugin.getDefault().getStateLocation();
		location = location.append(ScopeSet.SCOPE_DIR_NAME);
		File dir = location.toFile();
		if (dir.exists() == false)
			dir.mkdir();
	}

	public void save() {
		ensureLocation();
		for (int i = 0; i < sets.size(); i++) {
			ScopeSet set = sets.get(i);
			set.save();
		}
		IDialogSettings settings = PlatformUI.getDialogSettingsProvider(FrameworkUtil.getBundle(ScopeSetManager.class))
				.getDialogSettings();
		if (activeSet != null)
			settings.put(ACTIVE_SET, activeSet.getName());
	}

	public ScopeSet[] getScopeSets(boolean implicit) {
		ArrayList<ScopeSet> result = new ArrayList<>();
		for (int i = 0; i < sets.size(); i++) {
			ScopeSet set = sets.get(i);
			if (set.isImplicit() == implicit)
				result.add(set);
			if (!implicit && set.isImplicit() && activeSet==set)
				result.add(set);
		}
		return result.toArray(new ScopeSet[result.size()]);
	}

	private void loadScopeSets() {
		sets = new ArrayList<>();
		IPath location = HelpUIPlugin.getDefault().getStateLocation();
		location = location.append("scope_sets"); //$NON-NLS-1$
		File dir = location.toFile();
		defSet = null;
		if (dir.exists() && dir.isDirectory()) {
			File[] files = dir.listFiles((FilenameFilter) (dir1, name) -> name.endsWith(ScopeSet.EXT)
					|| name.endsWith(HistoryScopeSet.EXT));
			if(files == null) {
				files = new File[0];
			}
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				String name = file.getName();
				int loc = name.lastIndexOf(ScopeSet.EXT);
				if (loc != -1) {
					ScopeSet set = new ScopeSet(name.substring(0, loc));
					sets.add(set);
					if (set.isDefault())
						defSet = set;
					continue;
				}
				loc = name.lastIndexOf(HistoryScopeSet.EXT);
				if (loc != -1) {
					HistoryScopeSet set = new HistoryScopeSet(name.substring(0,
							loc), null);
					sets.add(set);
				}
			}
		}
		if (sets.size() == 1) {
			activeSet = sets.get(0);
		}
		if (defSet == null) {
			defSet = new ScopeSet();
			sets.add(defSet);
		}

	}

	public ScopeSet getDefaultScope() {
		return defSet;
	}

	/**
	 * @return Returns the activeSet.
	 */
	public ScopeSet getActiveSet() {
		if (activeSet == null) {
			IDialogSettings settings = PlatformUI
					.getDialogSettingsProvider(FrameworkUtil.getBundle(ScopeSetManager.class)).getDialogSettings();
			String name = settings.get(ACTIVE_SET);
			activeSet = findSet(name);
			if (activeSet == null) {
				return sets.get(0);
			}
			if (!activeSet.isImplicit())
				lastExplicitSet = activeSet;
		}
		return activeSet;
	}

	public ScopeSet findSet(String name) {
		return findSet(name, false);
	}

	public HistoryScopeSet findSearchSet(String expression) {
		for (int i = 0; i < sets.size(); i++) {
			ScopeSet set = sets.get(i);
			if (!set.isImplicit() || !(set instanceof HistoryScopeSet))
				continue;
			HistoryScopeSet sset = (HistoryScopeSet) set;
			if (sset.getExpression().equals(expression))
				return sset;
		}
		return null;
	}

	public ScopeSet findSet(String name, boolean implicit) {
		ScopeSet defaultSet = null;
		for (int i = 0; i < sets.size(); i++) {
			ScopeSet set = sets.get(i);
			if (name != null && set.isImplicit() == implicit) {
				if (set.getName().equals(name))
					return set;
			} else if (set.isDefault())
				defaultSet = set;
		}
		if (!implicit)
			return defaultSet;
		return null;
	}
}