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

import java.util.List;

public class GlobalClasspathEntries extends AbstractClasspathEntry {
	private String name;
	private int type;
	
	private boolean canBeRemoved= true;
	
	public GlobalClasspathEntries(String name, IClasspathEntry parent, boolean canBeRemoved, int type) {
		this.parent= parent;
		this.name= name;
		this.canBeRemoved= canBeRemoved;
		this.type= type;
	}
		
	public void addEntry(ClasspathEntry entry) {
		childEntries.add(entry);
	}
	
	public void removeEntry(ClasspathEntry entry) {
		childEntries.remove(entry);
	}
	
	public boolean contains(ClasspathEntry entry) {
		return childEntries.contains(entry);
	}
	
	public String toString() {
		return name;
	}

	public void removeAll() {
		childEntries.clear();
	}
	
	public boolean canBeRemoved() {
		return canBeRemoved;
	}

	/**
     * Set the child entries of this classpath entry.
	 * @param entries The child entries.
	 */
	public void setEntries(List entries) {
		childEntries= entries;
	}
	/**
	 * @return Returns the type of this global classpath entry.
	 * @see ClasspathModel#ANT_HOME
	 * @see ClasspathModel#GLOBAL_USER
	 */
	public int getType() {
		return type;
	}
}
