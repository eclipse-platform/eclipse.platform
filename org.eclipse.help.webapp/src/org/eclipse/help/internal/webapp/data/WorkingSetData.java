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
 *     Alexander Kurtakov - Bug 460858
 *******************************************************************************/
package org.eclipse.help.internal.webapp.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.IToc;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.base.scope.ScopeUtils;
import org.eclipse.help.internal.base.util.CriteriaUtilities;
import org.eclipse.help.internal.criteria.CriterionResource;
import org.eclipse.help.internal.webapp.servlet.WebappWorkingSetManager;
import org.eclipse.help.internal.workingset.AdaptableHelpResource;
import org.eclipse.help.internal.workingset.AdaptableToc;
import org.eclipse.help.internal.workingset.AdaptableTocsArray;
import org.eclipse.help.internal.workingset.AdaptableTopic;
import org.eclipse.help.internal.workingset.WorkingSet;

/**
 * This class manages help working sets
 */
public class WorkingSetData extends RequestData {
	public static final short STATE_UNCHECKED = 0;
	public static final short STATE_GRAYED = 1;
	public static final short STATE_CHECKED = 2;

	private WebappWorkingSetManager wsmgr;

	private AdaptableToc[] tocs;
	private boolean isEditMode;
	private AbstractHelpScope filter;

	public WorkingSetData(ServletContext context, HttpServletRequest request,
			HttpServletResponse response) {
		super(context, request, response);
		wsmgr = new WebappWorkingSetManager(request, response, getLocale());
		AdaptableTocsArray adaptableTocs = wsmgr.getRoot();
		tocs = (AdaptableToc[]) adaptableTocs.getChildren();
		isEditMode = "edit".equals(getOperation()); //$NON-NLS-1$
		filter = RequestScope.getScope(request, response, true);
	}

	public boolean isEditMode() {
		return isEditMode;
	}

	public String getWorkingSetName() {
		String name = request.getParameter("workingSet"); //$NON-NLS-1$
		if (name == null)
			name = ""; //$NON-NLS-1$
		return name;
	}

	public WorkingSet getWorkingSet() {
		String name = getWorkingSetName();
		if (name != null && name.length() > 0)
			return wsmgr.getWorkingSet(name);
		return null;
	}

	/**
	 * Returns the state of the TOC
	 *
	 * @return boolean
	 */
	public short getTocState(int toc) {
		if (!isEditMode())
			return STATE_UNCHECKED;
		WorkingSet ws = getWorkingSet();
		if (ws == null)
			return STATE_UNCHECKED;
		if (toc < 0 || toc >= tocs.length)
			return STATE_UNCHECKED;

		// See if the toc is in the working set
		AdaptableToc adaptableToc = tocs[toc];
		AdaptableHelpResource[] elements = ws.getElements();
		for (AdaptableHelpResource element : elements) {
			if (element == adaptableToc)
				return STATE_CHECKED;
		}

		// Check if it is grayed out
		int topics = adaptableToc.getChildren().length;
		boolean allTheSame = true;
		short baseValue = STATE_UNCHECKED;
		// base value is that of the first topic
		if (topics > 0)
			baseValue = getTopicState(toc, 0);
		for (int i = 1; allTheSame && i < topics; i++)
			allTheSame = allTheSame && (getTopicState(toc, i) == baseValue);

		if (!allTheSame)
			return STATE_GRAYED;
		return STATE_UNCHECKED;
	}

	public boolean isTocEnabled(int tocIndex) {
		AdaptableToc adaptableToc = tocs[tocIndex];
		IToc toc = adaptableToc.getAdapter(IToc.class);
		return ScopeUtils.showInTree(toc, filter);
	}

	public boolean isTopicEnabled(int tocIndex, int topicIndex) {
		AdaptableToc adaptableToc = tocs[tocIndex];
		IToc toc = adaptableToc.getAdapter(IToc.class);
		return ScopeUtils.showInTree(toc.getTopics()[topicIndex], filter);
	}

	/**
	 * Returns the state of the topic. The state is not dependent on the parent
	 * toc, but only whether it was part of the working set. To get the real
	 * state, the caller must use the parent state as well. This is not done
	 * here for performance reasons. In the JSP, by the time one looks at the
	 * topic, the parent toc has already been processed.
	 *
	 * @param toc
	 * @param topic
	 * @return short
	 */
	public short getTopicState(int toc, int topic) {
		if (!isEditMode)
			return STATE_UNCHECKED;
		WorkingSet ws = getWorkingSet();
		if (ws == null)
			return STATE_UNCHECKED;
		if (toc < 0 || toc >= tocs.length)
			return STATE_UNCHECKED;

		AdaptableToc parent = tocs[toc];
		AdaptableTopic[] topics = (AdaptableTopic[]) parent.getChildren();
		if (topic < 0 || topic >= topics.length)
			return STATE_UNCHECKED;
		AdaptableTopic adaptableTopic = topics[topic];
		AdaptableHelpResource[] elements = ws.getElements();
		for (AdaptableHelpResource element : elements) {
			if (element == adaptableTopic)
				return STATE_CHECKED;
		}
		return STATE_UNCHECKED;
	}

	public String getOperation() {
		return request.getParameter("operation"); //$NON-NLS-1$
	}

	// Accessor methods to avoid exposing help classes directly to JSP.
	// Note: this seems ok for now, but maybe we need to reconsider this
	//       and allow help classes in JSP's.

	public int getTocCount() {
		return tocs.length;
	}

	public String getTocLabel(int i) {
		return tocs[i].getLabel();
	}

	public String getTocHref(int i) {
		return tocs[i].getHref();
	}

	public int getTopicCount(int toc) {
		return tocs[toc].getTopics().length;
	}

	public String getTopicLabel(int toc, int topic) {
		return tocs[toc].getTopics()[topic].getLabel();
	}

	public String getDefaultName() {
		for (int i = 1; i < 100; i++) {
			String name = ServletResources.getString("DefaultScopeName", request) + i; //$NON-NLS-1$
			if (wsmgr.getWorkingSet(name) == null) {
				return name;
			}
		}
		return ""; //$NON-NLS-1$
	}

	public boolean isCriteriaScopeEnabled(){
		return wsmgr.isCriteriaScopeEnabled();
	}

	public String[] getCriterionIds() {
		return wsmgr.getCriterionIds();
	}


	public String[] getCriterionValueIds(String criterionId) {
		return wsmgr.getCriterionValueIds(criterionId);
	}


	public String getCriterionDisplayName(String criterionId) {
		return wsmgr.getCriterionDisplayName(criterionId);
	}

	public String getCriterionValueDisplayName(String criterionId, String criterionValueId) {
		return wsmgr.getCriterionValueDisplayName(criterionId, criterionValueId);
	}

	public short getCriterionCategoryState(int index) {
		String[] categories = getCriterionIds();

		if (!isEditMode())
			return STATE_UNCHECKED;
		WorkingSet ws = getWorkingSet();
		if (ws == null)
			return STATE_UNCHECKED;
		if (index < 0 || index >= categories.length)
			return STATE_UNCHECKED;

		String category = categories[index];
		Map<String, Set<String>> criteriaMap = new HashMap<>();
		CriterionResource[] criteria = ws.getCriteria();
		CriteriaUtilities.addCriteriaToMap(criteriaMap, criteria);
		if(!criteriaMap.containsKey(category))
			return STATE_UNCHECKED;

		Set<String> criterionValuesFromWS = criteriaMap.get(category);
		Set<String> criterionValuesSet = new HashSet<>(Arrays.asList(getCriterionValueIds(category)));
		if(criterionValuesFromWS.containsAll(criterionValuesSet)){
			return STATE_CHECKED;
		}else{
			return STATE_GRAYED;
		}
	}

	public short getCriterionValueState(int categoryIndex, int valueIndex) {
		String[] categories = getCriterionIds();
		if (!isEditMode)
			return STATE_UNCHECKED;
		WorkingSet ws = getWorkingSet();
		if (ws == null)
			return STATE_UNCHECKED;
		if (categoryIndex < 0 || categoryIndex >= categories.length)
			return STATE_UNCHECKED;

		String category = categories[categoryIndex];
		Map<String, Set<String>> criteriaMap = new HashMap<>();
		CriterionResource[] criteria = ws.getCriteria();
		CriteriaUtilities.addCriteriaToMap(criteriaMap, criteria);

		Set<String> criterionValuesFromWS = criteriaMap.get(category);
		String[] crietriaValues = getCriterionValueIds(category);
		if (valueIndex < 0 || valueIndex >= crietriaValues.length){
			return STATE_UNCHECKED;
		}
		String relatedCriterionValue = crietriaValues[valueIndex];
		if(criterionValuesFromWS.contains(relatedCriterionValue)){
			return STATE_CHECKED;
		}else{
			return STATE_UNCHECKED;
		}
	}

}
