/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.internal.ui.preferences;

import java.util.List;

public class GlobalClasspathEntries extends AbstractClasspathEntry {
	private String fName;
	private int fType;
	private boolean fCanBeRemoved= true;
	
	public GlobalClasspathEntries(String name, IClasspathEntry parent, boolean canBeRemoved, int type) {
		fParent= parent;
		fName= name;
		fCanBeRemoved= canBeRemoved;
		fType= type;
	}
		
	public void addEntry(ClasspathEntry entry) {
		fChildEntries.add(entry);
	}
	
	public void removeEntry(ClasspathEntry entry) {
		fChildEntries.remove(entry);
	}
	
	public boolean contains(ClasspathEntry entry) {
		return fChildEntries.contains(entry);
	}
	
	public String toString() {
		return fName;
	}

	public void removeAll() {
		fChildEntries.clear();
	}
	
	public boolean canBeRemoved() {
		return fCanBeRemoved;
	}

	/**
     * Set the child entries of this classpath entry.
	 * @param entries The child entries.
	 */
	public void setEntries(List entries) {
		fChildEntries= entries;
	}
	/**
	 * @return Returns the type of this global classpath entry.
	 * @see ClasspathModel#ANT_HOME
	 * @see ClasspathModel#GLOBAL_USER
	 * @see ClasspathModel#CONTRIBUTED
	 */
	public int getType() {
		return fType;
	}
}
