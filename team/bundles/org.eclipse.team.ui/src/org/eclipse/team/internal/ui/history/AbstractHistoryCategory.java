/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.history;

import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.history.IFileRevision;


public abstract class AbstractHistoryCategory {

	/**
	 * Returns the name of this category.
	 * @return a string
	 */
	abstract public String getName();

	/**
	 * Returns the image that will be displayed next to the category name or <code>null</code>
	 * if no image is required.
	 * @return an image or <code>null</code>
	 */
	public Image getImage() {
		return null;
	}

	/**
	 * Returns whether this category currently has any revisions associated with it.
	 * @return <code>true</code> if there are any revisions, <code>false</code> otherwise.
	 */
	abstract public boolean hasRevisions();

	/**
	 * Takes in an array of IFileRevision and collects the revisions that belong to this category.
	 * The shouldRemove flag indicates whether match file revisions need to be removed from the
	 * passed in file revision array (in order to increase efficency).
	 * @param fileRevisions	an array of IFileRevisions
	 * @param shouldRemove	<code>true</code> if the method should remove the matching revisions from fileRevisions, <code>false</code> otherwise
	 * @return	<code>true</code> if any revisions match this category, <code>false</code> otherwise
	 */
	abstract public boolean collectFileRevisions(IFileRevision[] fileRevisions, boolean shouldRemove);

	/**
	 * Returns the file revisions that are currently associated with this category or <code>null</code> if
	 * there are no file revisions associated with this category.
	 * @return an array of IFileRevision or <code>null</code>
	 */
	abstract public IFileRevision[] getRevisions();
}
