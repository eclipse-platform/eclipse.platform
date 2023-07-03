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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.UAElement;

/*
 * Assembles individual criteria definition contributions into a complete, fully
 * criteria definition.
 */
public class CriteriaDefinitionAssembler {

	/*
	 * Assembles the given criteria definition contributions into a complete criteria definition.
	 * The originals are not modified.
	 */
	public CriteriaDefinition assemble(List<CriteriaDefinitionContribution> contributions) {
		return merge(contributions);
	}

	/*
	 * Merge all criteria definition contributions into one.
	 */
	private CriteriaDefinition merge(List<CriteriaDefinitionContribution> contributions) {
		CriteriaDefinition criteriaDefinition = new CriteriaDefinition();
		Iterator<CriteriaDefinitionContribution> iter = contributions.iterator();
		while (iter.hasNext()) {
			CriteriaDefinitionContribution contribution = iter.next();
			mergeChildren(criteriaDefinition, (CriteriaDefinition)contribution.getCriteriaDefinition());
			contribution.setCriteriaDefinition(null);
		}
		return criteriaDefinition;
	}

	/*
	 * Merges the children of nodes a and b, and stores them into a. If the two
	 * contain the same criterion id, only one is kept but its children are merged,
	 * recursively. In one criterion, if multiple criterion values exist with the
	 * same id, only the first one found is kept.
	 *
	 * Insure criterion has its id at least, and criterion value has both its id and name.
	 */
	private void mergeChildren(UAElement a, UAElement b) {
		Map<String, UAElement> criterionById = new HashMap<>();
		Set<String> criterionValueIds = new HashSet<>();

		IUAElement[] childrenA = a.getChildren();
		for(int i = 0; i < childrenA.length; ++i){
			UAElement childA = (UAElement)childrenA[i];
			if(childA instanceof CriterionDefinition) {
				String id = childA.getAttribute(CriterionDefinition.ATTRIBUTE_ID);
				if(null != id && id.trim().length() > 0) {
					criterionById.put(childA.getAttribute(CriterionDefinition.ATTRIBUTE_ID), childA);
				}
			} else if(childA instanceof CriterionValueDefinition) {
				String valueId = childA.getAttribute(CriterionValueDefinition.ATTRIBUTE_ID);
				String valueName = childA.getAttribute(CriterionValueDefinition.ATTRIBUTE_NAME);
				if(null != valueId && valueId.trim().length() > 0 && null != valueName && valueName.trim().length() > 0){
					criterionValueIds.add(childA.getAttribute(CriterionValueDefinition.ATTRIBUTE_ID));
				}
			}
		}

		IUAElement[] childrenB = b.getChildren();
		for(int i = 0; i < childrenB.length; ++i){
			UAElement childB = (UAElement) childrenB[i];
			if(childB instanceof CriterionDefinition) {
				String idB = childB.getAttribute(CriterionDefinition.ATTRIBUTE_ID);
				if(null != idB && idB.trim().length() > 0){
					if (criterionById.containsKey(idB)) {
						// duplicate id; merge children
						mergeChildren(criterionById.get(idB), childB);
					} else {
						// wasn't a duplicate
						a.appendChild(childB);
						criterionById.put(idB, childB);
					}
				}
			} else if(childB instanceof CriterionValueDefinition) {
				String valueIdB = childB.getAttribute(CriterionValueDefinition.ATTRIBUTE_ID);
				String valueNameB = childB.getAttribute(CriterionValueDefinition.ATTRIBUTE_NAME);
				if(null != valueIdB && valueIdB.trim().length() > 0 && null != valueNameB && valueNameB.trim().length() > 0){
					if (!criterionValueIds.contains(valueIdB)) {
						// For one criterion, add criterion value only if id doesn't exist yet
						a.appendChild(childB);
						criterionValueIds.add(valueIdB);
					}
				}
			}
		}
	}
}
