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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.help.ICriteria;
import org.eclipse.help.IIndexEntry;
import org.eclipse.help.IIndexSee;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.IUAElement;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.base.util.CriteriaUtilities;
import org.eclipse.help.internal.criteria.CriteriaProviderRegistry;
import org.eclipse.help.internal.criteria.CriterionResource;
import org.eclipse.help.internal.workingset.AdaptableHelpResource;
import org.eclipse.help.internal.workingset.IHelpWorkingSetManager;
import org.eclipse.help.internal.workingset.WorkingSet;

public class WorkingSetScope extends AbstractHelpScope {

	private static final String UNCATEGORIZED = "Uncategorized"; //$NON-NLS-1$

	private WorkingSet workingSet;
	private AdaptableHelpResource[] elements;
	private CriterionResource[] criteria;
	private String name;

	public WorkingSetScope(String scope, IHelpWorkingSetManager manager, String name) {
		workingSet = manager.getWorkingSet(scope);
		elements = workingSet.getElements();
		criteria = workingSet.getCriteria();
		this.name = name;
	}

	public WorkingSetScope(WorkingSet wset, String name) {
		workingSet = wset;
		elements = workingSet.getElements();
		criteria = workingSet.getCriteria();
		this.name = name;
	}

	@Override
	public boolean inScope(IToc toc) {
		if(HelpPlugin.getCriteriaManager().isCriteriaEnabled()) {
			return inContentScope(toc) && inCriteriaScope(toc);
		} else {
			return inContentScope(toc);
		}
	}

	private boolean inContentScope(IToc toc) {
		for (int i = 0; i < elements.length; i++) {
			for (AdaptableHelpResource adaptable = elements[i]; adaptable != null; ) {
				Object itoc = adaptable.getAdapter(IToc.class);
				if (toc == itoc) {
					return true;
				}
				IAdaptable parent= adaptable.getParent();
				if (parent instanceof AdaptableHelpResource) {
					adaptable = (AdaptableHelpResource) parent;
				} else {
					adaptable = null;
				}
			}
		}
		return false;
	}

	private boolean inCriteriaScope(IToc toc) {
		if(null == toc){
			if(null == criteria || 0 == criteria.length){
				return true;
			}
			return false;
		}
		ICriteria[] criteriaOfToc = CriteriaProviderRegistry.getInstance().getAllCriteria(toc);
		return isCriteriaInScope(criteriaOfToc);
	}

	private boolean isCriteriaInScope(ICriteria[] criteriaOfTopic) {
		if(null ==criteria){
			return true;
		}
		Map<String, Set<String>> ownCriteria = getCriteriaInfo(criteriaOfTopic);
		Map<String, Set<String>> scope = getCriteriaInfo(criteria);
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
	public boolean inScope(ITopic topic) {
		if(HelpPlugin.getCriteriaManager().isCriteriaEnabled()) {
			return inContentScope(topic) && inCriteriaScope(topic);
		} else {
			return inContentScope(topic);
		}
	}

	private boolean inContentScope(ITopic topic) {
		Set<IUAElement> topics = new HashSet<>();
		IToc toc = null;
		topics.add(topic);
		if (topic instanceof UAElement) {
			for (UAElement uae = (UAElement) topic; uae != null; ) {
				if (uae instanceof IToc)  {
					toc = (IToc) uae;
					uae = null;
				} else if (uae instanceof IIndexEntry) {
					return isHrefInScope(topic.getHref());
				} else {
					if (uae instanceof ITopic) {
						topics.add(uae);
					}
					uae = uae.getParentElement();
				}
			}
		}
		for (int i = 0; i < elements.length; i++) {
			AdaptableHelpResource adaptable = elements[i];
			if (toc != null) {
				Object itoc = adaptable.getAdapter(IToc.class);
				if (toc == itoc) {
					return true;
				}
			}
			Object itopic = adaptable.getAdapter(ITopic.class);
			if (topic != null && topics.contains(itopic)) {
				return true;
			}
			IAdaptable parent = adaptable.getParent();
			if (parent instanceof AdaptableHelpResource) {
				adaptable = (AdaptableHelpResource) parent;
			} else {
				adaptable = null;
			}
		}
		return false;
	}

	private boolean isHrefInScope(String href) {
		String anchorlessHref;
		int index = href.indexOf('#');
		if (index != -1) {
			anchorlessHref = href.substring(0, index);
		} else {
			anchorlessHref = href;
		}

		for (AdaptableHelpResource adaptable : elements) {
			if (adaptable.getTopic(anchorlessHref) != null) {
				return true;
			}
		}
		return false;
	}

	private boolean inCriteriaScope(ITopic topic) {
		if(null == topic){
			if(null == criteria || 0 == criteria.length){
				return true;
			}
			return false;
		}
		ICriteria[] criteriaOfTopic = CriteriaProviderRegistry.getInstance().getAllCriteria(topic);
		return isCriteriaInScope(criteriaOfTopic);
	}

	@Override
	public boolean inScope(IIndexEntry entry) {
		return hasInScopeChildren(entry);
	}

	@Override
	public boolean inScope(IIndexSee see) {
		return hasInScopeChildren(see);
	}

	@Override
	public String getName(Locale locale) {
		return name;
	}

}
