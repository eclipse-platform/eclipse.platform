/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.preferences;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.ant.internal.ui.model.AntUtil;

public class ClasspathModel extends AbstractClasspathEntry {
	
	public static final int ANT_HOME= 0;
	public static final int GLOBAL_USER= 1;
	public static final int USER= 2;
	
	private GlobalClasspathEntries antHomeEntry;
	private GlobalClasspathEntries userGlobalEntry;
	
	public Object addEntry(Object entry) {
		if (entry instanceof GlobalClasspathEntries) {
			if (!childEntries.contains(entry)) {
				childEntries.add(entry);
				return entry;
			}
			return null;
		} 
		ClasspathEntry newEntry= createEntry(entry, null);
		Iterator entries= childEntries.iterator();
		while (entries.hasNext()) {
			Object element = entries.next();
			if (element instanceof GlobalClasspathEntries) {
				if(((GlobalClasspathEntries)element).contains(newEntry)) {
					return null;
				}
			} else if (element.equals(newEntry)) {
				return null;
			}
		}
		childEntries.add(newEntry);
		return newEntry;
	}
	
	public Object addEntry(int entryType, Object entry) {
		IClasspathEntry entryParent= null;
		switch (entryType) {
			case ANT_HOME :
				if (antHomeEntry == null) {
					String name= AntPreferencesMessages.getString("ClasspathModel.2"); //$NON-NLS-1$
					antHomeEntry= createGlobalEntry(new IAntClasspathEntry[0], name, false, true, ANT_HOME);
				}
				entryParent= antHomeEntry;
				break;
			case GLOBAL_USER :
				if (userGlobalEntry == null) {
					String name= AntPreferencesMessages.getString("ClasspathModel.3"); //$NON-NLS-1$
					userGlobalEntry= createGlobalEntry(new IAntClasspathEntry[0], name, true, true, GLOBAL_USER);
				}
				entryParent= userGlobalEntry;
				break;
			default :
				break;
		}
			
		ClasspathEntry newEntry= createEntry(entry, entryParent);
		Iterator entries= childEntries.iterator();
		while (entries.hasNext()) {
			Object element = entries.next();
			if (element instanceof GlobalClasspathEntries) {
				if(((GlobalClasspathEntries)element).contains(newEntry)) {
					return null;
				}
			} else if (element.equals(newEntry)) {
				return null;
			}
		}
		if (entryParent != null) {
			((GlobalClasspathEntries)entryParent).addEntry(newEntry);
		} else {
			childEntries.add(newEntry);
		}
		return newEntry;		
	}
	
	/**
	 * Returns entries of the specified kind, possibly empty.
	 * 
	 * @param entryType kind of entries to retrieve
	 * @return entries of the specified kind, possibly empty
	 */
	public IAntClasspathEntry[] getEntries(int entryType) {
		switch (entryType) {
			case ANT_HOME :
				if (antHomeEntry != null) {
					return antHomeEntry.getEntries();
				}
				break;
			case GLOBAL_USER :
				if (userGlobalEntry != null) {
					return userGlobalEntry.getEntries();
				}
				break;
			case USER : 
				return getUserEntries();
		}
		return new IAntClasspathEntry[0];
	}
	
	public void remove(Object entry) {
		childEntries.remove(entry);
		if (entry == userGlobalEntry) {
			userGlobalEntry= null;
		}
	}
	
	public ClasspathEntry createEntry(Object entry, IClasspathEntry entryParent) {
		if (entryParent == null) {
			entryParent= this;
		} 
		return new ClasspathEntry(entry, entryParent);
	}

	public void removeAll() {
		if (antHomeEntry != null) {
			antHomeEntry.removeAll();
		} 
		if (userGlobalEntry != null) {
			userGlobalEntry.removeAll();
		}
	}
	
	public void removeAll(int entryType) {
		switch (entryType) {
			case ANT_HOME :
				if (antHomeEntry != null) {
					antHomeEntry.removeAll();
				}
				break;
			case GLOBAL_USER :
				if (userGlobalEntry != null) {
					userGlobalEntry.removeAll();
				}
				break;
			default :
				break;
		}
	}
	
	public void removeAll(Object[] entries) {
		
		for (int i = 0; i < entries.length; i++) {
			Object object = entries[i];
			if (object instanceof ClasspathEntry) {
				IClasspathEntry entryParent= ((ClasspathEntry)object).getParent();
				if (entryParent instanceof GlobalClasspathEntries) {
					((GlobalClasspathEntries)entryParent).removeEntry((ClasspathEntry) object);
				} else {
					remove(object);
				}
			} else {
				remove(object);
			}
		}
	}

	public void setAntHomeEntries(IAntClasspathEntry[] entries) {
		if (antHomeEntry == null) {
			String name= AntPreferencesMessages.getString("ClasspathModel.2"); //$NON-NLS-1$
			antHomeEntry= createGlobalEntry(entries, name, false, true, ANT_HOME);
		} else {
			antHomeEntry.removeAll();
			for (int i = 0; i < entries.length; i++) {
				antHomeEntry.addEntry(new ClasspathEntry(entries[i], antHomeEntry));
			}
		}
	}

	private GlobalClasspathEntries createGlobalEntry(IAntClasspathEntry[] entries, String name, boolean canBeRemoved, boolean addEntry, int type) {
		
		GlobalClasspathEntries global= new GlobalClasspathEntries(name, this, canBeRemoved, type);
		
		for (int i = 0; i < entries.length; i++) {
			global.addEntry(new ClasspathEntry(entries[i], global));
		}
		
		if (addEntry) {
			addEntry(global);
		}
		return global;
	}

	public void setGlobalEntries(IAntClasspathEntry[] entries) {
		if (userGlobalEntry == null) {
			String name= AntPreferencesMessages.getString("ClasspathModel.3"); //$NON-NLS-1$
			userGlobalEntry= createGlobalEntry(entries, name, true, true, GLOBAL_USER);
		} else {
			userGlobalEntry.removeAll();
			for (int i = 0; i < entries.length; i++) {
				userGlobalEntry.addEntry(new ClasspathEntry(entries[i], userGlobalEntry));
			}
		}
	}
	
	private IAntClasspathEntry[] getUserEntries() {
		List userEntries= new ArrayList(childEntries.size());
		Iterator itr= childEntries.iterator();
		while (itr.hasNext()) {
			IClasspathEntry element = (IClasspathEntry) itr.next();
			if (element instanceof GlobalClasspathEntries) {
				continue;
			}
			userEntries.add(element);
		}
		return (IAntClasspathEntry[])userEntries.toArray(new IAntClasspathEntry[userEntries.size()]);
	}
	
	public String serializeClasspath(boolean defaultAntHome) {
		Iterator itr= childEntries.iterator();
		StringBuffer buff= new StringBuffer();
		while (itr.hasNext()) {
			IClasspathEntry element = (IClasspathEntry) itr.next();
			if (element instanceof GlobalClasspathEntries) {
				if (element == antHomeEntry) {
					if (!defaultAntHome || !isSameAsDefaultAntHome()) {
						IAntClasspathEntry[] antHomeEntries= antHomeEntry.getEntries();
						for (int i = 0; i < antHomeEntries.length; i++) {
							IAntClasspathEntry entry = antHomeEntries[i];
							buff.append('?');
							buff.append(entry.toString());
							buff.append(AntUtil.ATTRIBUTE_SEPARATOR);
						}
					} else {
						buff.append(AntUtil.ANT_HOME_CLASSPATH_PLACEHOLDER);
					}
				} else {
					buff.append(AntUtil.ANT_GLOBAL_USER_CLASSPATH_PLACEHOLDER);
				}
			} else {
				buff.append(element.toString());
			}
			buff.append(AntUtil.ATTRIBUTE_SEPARATOR);
		}
		if (buff.length() > 0) {
			return buff.substring(0, buff.length() - 1);
		}
		return ""; //$NON-NLS-1$
	}
	
	private boolean isSameAsDefaultAntHome() {
		AntCorePreferences prefs= AntCorePlugin.getPlugin().getPreferences();
		IAntClasspathEntry[] defaultAntHomeEntries= prefs.getAntHomeClasspathEntries();
		IAntClasspathEntry[] antHomeEntries= antHomeEntry.getEntries();
		if (antHomeEntries.length != defaultAntHomeEntries.length) {
			return false;
		}
		
		for (int i = 0; i < antHomeEntries.length; i++) {
			IAntClasspathEntry entry = antHomeEntries[i];
			IAntClasspathEntry defaultEntry= defaultAntHomeEntries[i];
			if (!sameURL(entry, defaultEntry)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean sameURL(IAntClasspathEntry first, IAntClasspathEntry second) {
		if (first == null || second == null) {
			return false;
		}
		File newFile= new File(first.getEntryURL().getFile());
		File existingFile= new File(second.getEntryURL().getFile());
		if (existingFile.equals(newFile)) {
			return true;
		}
		return false;
	}
	
	public ClasspathModel(String serializedClasspath, boolean customAntHome) {
		StringTokenizer tokenizer= new StringTokenizer(serializedClasspath, AntUtil.ATTRIBUTE_SEPARATOR);
		
		while (tokenizer.hasMoreTokens()) {
			String string = tokenizer.nextToken().trim();
			if (string.equals(AntUtil.ANT_HOME_CLASSPATH_PLACEHOLDER)) {
				IAntClasspathEntry[] antHomeEntries= new IAntClasspathEntry[0];
				if (!customAntHome) {
					antHomeEntries= AntCorePlugin.getPlugin().getPreferences().getAntHomeClasspathEntries();
				} 
				setAntHomeEntries(antHomeEntries);
			} else if (string.equals(AntUtil.ANT_GLOBAL_USER_CLASSPATH_PLACEHOLDER)) {
				setGlobalEntries(AntCorePlugin.getPlugin().getPreferences().getAdditionalClasspathEntries());
			} else {
				boolean isAntHomeEntry= false;
				Object entry= null;
				if (string.charAt(0) == '*') {
					//old customclasspath
					string= string.substring(1);
				}
				if (string.charAt(0) == '?') {
					//ant home entry
					string= string.substring(1);
					isAntHomeEntry= true;
				}
				try {
					entry=  new URL("file:" + string); //$NON-NLS-1$
				} catch (MalformedURLException e) {
					entry= string;
				}
				if (isAntHomeEntry) {
					addEntry(ANT_HOME, entry);
				} else {
					addEntry(entry);
				}
			}
		}
	}
	
	public ClasspathModel() {
		super();
	}

	/**
	 * @return
	 */
	public Object[] getRemovedGlobalEntries() {
		if (userGlobalEntry == null) {
			String name= AntPreferencesMessages.getString("ClasspathModel.3"); //$NON-NLS-1$
			return new Object[] {createGlobalEntry(new IAntClasspathEntry[0], name, true, false, GLOBAL_USER)};
		}
		return new Object[] {};
	}

	/**
	 * @return
	 */
	public Object getAntHomeEntry() {
		return antHomeEntry;
	}
}