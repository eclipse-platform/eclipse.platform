/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.help.internal.webapp.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.criteria.CriterionResource;
import org.eclipse.help.internal.workingset.AdaptableHelpResource;
import org.eclipse.help.internal.workingset.AdaptableToc;
import org.eclipse.help.internal.workingset.AdaptableTocsArray;
import org.eclipse.help.internal.workingset.AdaptableTopic;
import org.eclipse.help.internal.workingset.IHelpWorkingSetManager;
import org.eclipse.help.internal.workingset.WorkingSet;

/**
 * Proxy for WorkingSetManager or InfocenterWorkingSetManager.
 *
 * @since 3.0
 */
public class WebappWorkingSetManager implements IHelpWorkingSetManager {
	IHelpWorkingSetManager wSetManager;
	// for keeping track if working set synchronized with working sets in UI
	//private static boolean workingSetsSynchronized = false;
	//private static final Object workingSetsSyncLock = new Object();

	/**
	 * Constructor
	 *
	 * @param locale
	 */
	public WebappWorkingSetManager(HttpServletRequest request,
			HttpServletResponse response, String locale) {
		if (BaseHelpSystem.getMode() == BaseHelpSystem.MODE_INFOCENTER) {
			wSetManager = new InfocenterWorkingSetManager(request, response,
					locale);
		} else {
			wSetManager = BaseHelpSystem.getWorkingSetManager();
		}
	}

	@Override
	public AdaptableTocsArray getRoot() {
		return wSetManager.getRoot();
	}
	/**
	 * Adds a new working set and saves it
	 */
	@Override
	public void addWorkingSet(WorkingSet workingSet) throws IOException {
		wSetManager.addWorkingSet(workingSet);
	}

	/**
	 * Creates a new working set
	 */
	@Override
	public WorkingSet createWorkingSet(String name,
			AdaptableHelpResource[] elements) {
		return wSetManager.createWorkingSet(name, elements);
	}

	@Override
	public WorkingSet createWorkingSet(String name, AdaptableHelpResource[] elements, CriterionResource[] criteria) {
		return wSetManager.createWorkingSet(name, elements, criteria);
	}

	/**
	 * Returns a working set by name
	 *
	 */
	@Override
	public WorkingSet getWorkingSet(String name) {
		return wSetManager.getWorkingSet(name);
	}

	@Override
	public WorkingSet[] getWorkingSets() {
		return wSetManager.getWorkingSets();
	}
	/**
	 * Removes specified working set
	 */
	@Override
	public void removeWorkingSet(WorkingSet workingSet) {
		wSetManager.removeWorkingSet(workingSet);
	}

	/**
	 * Persists all working sets. Should only be called by the webapp working
	 * set dialog.
	 *
	 * @param changedWorkingSet
	 *            the working set that has changed
	 */
	@Override
	public void workingSetChanged(WorkingSet changedWorkingSet)
			throws IOException {
		wSetManager.workingSetChanged(changedWorkingSet);
	}

	@Override
	public AdaptableToc getAdaptableToc(String href) {
		return wSetManager.getAdaptableToc(href);
	}

	@Override
	public AdaptableTopic getAdaptableTopic(String id) {
		return wSetManager.getAdaptableTopic(id);
	}

	@Override
	public String getCurrentWorkingSet() {
		return wSetManager.getCurrentWorkingSet();
	}

	@Override
	public void setCurrentWorkingSet(String scope) {
		wSetManager.setCurrentWorkingSet(scope);
	}

	@Override
	public boolean isCriteriaScopeEnabled(){
		return wSetManager.isCriteriaScopeEnabled();
	}

	@Override
	public String[] getCriterionIds() {
		return wSetManager.getCriterionIds();
	}


	@Override
	public String[] getCriterionValueIds(String criterionId) {
		return wSetManager.getCriterionValueIds(criterionId);
	}


	@Override
	public String getCriterionDisplayName(String criterionId) {
		return wSetManager.getCriterionDisplayName(criterionId);
	}

	@Override
	public String getCriterionValueDisplayName(String criterionId, String criterionValueId) {
		return wSetManager.getCriterionValueDisplayName(criterionId, criterionValueId);
	}


}
