/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui.preferences;

import java.util.List;

import org.eclipse.ant.core.IAntClasspathEntry;

public class GlobalClasspathEntries extends AbstractClasspathEntry {
	private final String fName;
	private final int fType;
	private boolean fCanBeRemoved = true;

	public GlobalClasspathEntries(String name, IClasspathEntry parent, boolean canBeRemoved, int type) {
		fParent = parent;
		fName = name;
		fCanBeRemoved = canBeRemoved;
		fType = type;
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

	@Override
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
	 * 
	 * @param entries
	 *            The child entries.
	 */
	public void setEntries(List<IAntClasspathEntry> entries) {
		fChildEntries = entries;
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
