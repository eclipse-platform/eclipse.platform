/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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

package org.eclipse.help.internal.criteria;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.ICriteria;
import org.eclipse.help.internal.HelpPlugin;

/**
 * Get criteria related parameter from preferences.ini
 *
 * @since 3.5
 */
public class CriteriaManager {

	private final static String SUPPORTED_CRITERIA = "supportedCriteria"; //$NON-NLS-1$
	private final static String ENABLE_CRITERIA = "enableCriteria"; //$NON-NLS-1$

	private List<String> supportedCriteria;
	private boolean criteriaEnabled;
	private Map<String, Map<String, Set<String>>> allCriteriaValues;

	private CriteriaDefinitionManager criteriaDefinitionManager;

	public CriteriaManager() {
		criteriaEnabled = Platform.getPreferencesService().getBoolean(HelpPlugin.PLUGIN_ID, ENABLE_CRITERIA, false, null);

		supportedCriteria = new ArrayList<>();
		StringTokenizer criteria = new StringTokenizer(Platform.getPreferencesService().getString(HelpPlugin.PLUGIN_ID, SUPPORTED_CRITERIA, "", null), ",;"); //$NON-NLS-1$ //$NON-NLS-2$
		while (criteria.hasMoreTokens()) {
			supportedCriteria.add(criteria.nextToken().toLowerCase().trim());
		}

		allCriteriaValues = new HashMap<>();

		if (criteriaDefinitionManager == null){
			criteriaDefinitionManager = new CriteriaDefinitionManager();
		}
	}

	public boolean isSupportedCriterion(String criterion){
		if(null != criterion && supportedCriteria.contains(criterion.toLowerCase())){
			return true;
		}
		return false;
	}

	public boolean isCriteriaEnabled(){
		return criteriaEnabled;
	}

	public void addCriteriaValues(ICriteria[] criteria, String locale){
		Map<String, Set<String>> criteriaInLocale = allCriteriaValues.get(locale);
		if(null == criteriaInLocale) {
			criteriaInLocale = new HashMap<>();
		}
		CriterionResource[] resources = CriterionResource.toCriterionResource(criteria);
		for(int i = 0; i < resources.length; ++ i){
			CriterionResource criterion = resources[i];
			String criterionName = criterion.getCriterionName();
			List<String> criterionValues = criterion.getCriterionValues();

			Set<String> existedValues = criteriaInLocale.get(criterionName);
			if (null == existedValues)
				existedValues = new HashSet<>();
			existedValues.addAll(criterionValues);
			criteriaInLocale.put(criterionName, existedValues);
		}
		allCriteriaValues.put(locale, criteriaInLocale);
	}

	public Map<String, Set<String>> getAllCriteriaValues(String locale) {
		Map<String, Set<String>> criteria = allCriteriaValues.get(locale);
		if(null == criteria) {
			criteria = new HashMap<>();
		}
		return criteria;
	}


	public String getCriterionDisplayName (String criterionId, String locale){
		return criteriaDefinitionManager.getCriterionName(criterionId, locale);
	}

	public String getCriterionValueDisplayName(String criterionId, String criterionValueId, String locale) {
		return criteriaDefinitionManager.getCriterionValueName(criterionId, criterionValueId, locale);
	}

}
