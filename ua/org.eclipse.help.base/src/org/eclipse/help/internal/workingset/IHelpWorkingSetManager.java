/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.workingset;

import java.io.IOException;

import org.eclipse.help.internal.criteria.CriterionResource;

/**
 * The working set manager stores help working sets. Working sets are persisted
 * whenever one is added or removed.
 *
 * @since 3.0
 */
public interface IHelpWorkingSetManager {

	public AdaptableTocsArray getRoot();
	/**
	 * Adds a new working set and saves it
	 */
	public void addWorkingSet(WorkingSet workingSet) throws IOException;

	/**
	 * Creates a new working set
	 */
	public WorkingSet createWorkingSet(String name,
			AdaptableHelpResource[] elements);

	public WorkingSet createWorkingSet(String name, AdaptableHelpResource[] elements, CriterionResource[] criteria);

	/**
	 * Returns a working set by name
	 *
	 */
	public WorkingSet getWorkingSet(String name);

	/**
	 * Implements IWorkingSetManager.
	 *
	 * @see org.eclipse.ui.IWorkingSetManager#getWorkingSets()
	 */
	public WorkingSet[] getWorkingSets();

	/**
	 * Removes specified working set
	 */
	public void removeWorkingSet(WorkingSet workingSet);

	/**
	 * Persists all working sets. Should only be called by the webapp working
	 * set dialog.
	 *
	 * @param changedWorkingSet
	 *            the working set that has changed
	 */
	public void workingSetChanged(WorkingSet changedWorkingSet)
			throws IOException;

	public AdaptableToc getAdaptableToc(String href);

	public AdaptableTopic getAdaptableTopic(String id);

	public String getCurrentWorkingSet();

	public void setCurrentWorkingSet(String workingSet);

	public boolean isCriteriaScopeEnabled();

	public String[] getCriterionIds();

	public String[] getCriterionValueIds(String criterionId);

	public String getCriterionDisplayName(String criterionId);

	public String getCriterionValueDisplayName(String criterionId, String criterionValueId);
}
