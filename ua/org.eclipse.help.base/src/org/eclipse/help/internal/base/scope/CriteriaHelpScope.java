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

package org.eclipse.help.internal.base.scope;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.help.ICriteria;
import org.eclipse.help.IIndexEntry;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.base.util.CriteriaUtilities;
import org.eclipse.help.internal.criteria.CriteriaProviderRegistry;
import org.eclipse.help.internal.criteria.CriterionResource;

public class CriteriaHelpScope extends AbstractHelpScope {

	private static final String UNCATEGORIZED = "Uncategorized"; //$NON-NLS-1$

	private CriterionResource[] criteriaScope;

	public CriteriaHelpScope(CriterionResource[] criteriaScope) {
		this.criteriaScope = criteriaScope;
	}

	public CriteriaHelpScope(List<CriterionResource> criteriaScope){
		if(null == criteriaScope) {
			this.criteriaScope = new CriterionResource[0];
		} else {
			this.criteriaScope = new CriterionResource[criteriaScope.size()];
			criteriaScope.toArray(this.criteriaScope);
		}
	}

	@Override
	public boolean inScope(IToc toc) {
		if(null == toc){
			if(null == criteriaScope || 0 == criteriaScope.length){
				return true;
			}
			return false;
		}
		ICriteria[] criteriaOfToc = CriteriaProviderRegistry.getInstance().getAllCriteria(toc);
		return isCriteriaInScope(criteriaOfToc);
	}

	@Override
	public boolean inScope(ITopic topic) {
		if(null == topic){
			if(null == criteriaScope || 0 == criteriaScope.length){
				return true;
			}
			return false;
		}
		ICriteria[] criteriaOfTopic = CriteriaProviderRegistry.getInstance().getAllCriteria(topic);
		return isCriteriaInScope(criteriaOfTopic);
	}

	private boolean isCriteriaInScope(ICriteria[] criteriaOfTopic) {
		if(null == criteriaScope){
			return true;
		}
		Map<String, Set<String>> ownCriteria = getCriteriaInfo(criteriaOfTopic);
		Map<String, Set<String>> scope = getCriteriaInfo(criteriaScope);
		outer: for (Iterator<String> keyIterator = scope.keySet().iterator(); keyIterator.hasNext();) {
			String key = String.valueOf(keyIterator.next());
			for (Iterator<String> valueIterator = scope.get(key).iterator(); valueIterator.hasNext();) {
				String value = String.valueOf(valueIterator.next());
				if (value.equals(UNCATEGORIZED)) {
					if (!ownCriteria.containsKey(key)) {
						continue outer;
					}
				} else {
					if (null != ownCriteria.get(key) && ownCriteria.get(key).contains(value))
						continue outer;
				}
			}
			return false;
		}
		return true;
	}

	private Map<String, Set<String>> getCriteriaInfo(CriterionResource[] criteria) {
		Map<String, Set<String>> criteriaMap = new HashMap<>();
		CriteriaUtilities.addCriteriaToMap(criteriaMap, criteria);
		return criteriaMap;
	}

	private Map<String, Set<String>> getCriteriaInfo(ICriteria[] criteria) {
		Map<String, Set<String>> criteriaMap = new HashMap<>();
		CriteriaUtilities.addCriteriaToMap(criteriaMap, criteria);
		return criteriaMap;
	}

	@Override
	public boolean inScope(IIndexEntry entry) {
		return hasInScopeChildren(entry);
	}

	@Override
	public String getName(Locale locale) {
		return null;
	}

}
