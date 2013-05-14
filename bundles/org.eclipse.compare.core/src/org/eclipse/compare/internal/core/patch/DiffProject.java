/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.core.patch;

import java.util.HashSet;
import java.util.Set;

/**
 * A diff project represents a project that was read from a workspace patch.
 * It contains the set of file diffs that were associated with the project
 * in the patch file.
 */
public class DiffProject {

	private String project;
	private Set fDiffs= new HashSet();

	/**
	 * Create a diff project for the given workspace project.
	 * @param project a workspace project
	 */
	public DiffProject(String project) {
		this.project= project;
	}

	/**
	 * Add the file diff to this project.
	 * @param diff the file diff.
	 */
	public void add(FilePatch2 diff) {
		this.fDiffs.add(diff);
		if (diff.getProject() != this)
			diff.setProject(this);
	}

	/**
	 * Return the name of this project.
	 * @return the name of this project
	 */
	public String getName() {
		return this.project;
	}

	/**
	 * Remove the file diff from this project.
	 * @param diff the diff to be removed
	 */
	public void remove(FilePatch2 diff) {
		this.fDiffs.remove(diff);
	}

	/**
	 * Return whether this project contains the given diff.
	 * @param diff a file diff
	 * @return whether this project contains the given diff
	 */
	public boolean contains(FilePatch2 diff) {
		return this.fDiffs.contains(diff);
	}

	/**
	 * Return the file diffs associated with this project.
	 * @return the file diffs associated with this project
	 */
	public FilePatch2[] getFileDiffs() {
		return (FilePatch2[]) this.fDiffs.toArray(new FilePatch2[this.fDiffs.size()]);
	}
}
