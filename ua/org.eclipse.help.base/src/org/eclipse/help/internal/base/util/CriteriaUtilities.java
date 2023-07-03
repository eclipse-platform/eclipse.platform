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

package org.eclipse.help.internal.base.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.help.ICriteria;
import org.eclipse.help.internal.criteria.CriterionResource;

public class CriteriaUtilities {

	public static List<String> getCriteriaValues(String rawValues) {
		List<String> result = new ArrayList<>();
		if (rawValues != null) {
			String[] values = rawValues.split(","); //$NON-NLS-1$
			for(int j = 0; j < values.length; ++j){
				String value = values[j].trim();
				if (value.length() > 0) {
					result.add(value);
				}
			}
		}
		return result;
	}

	public static void addCriteriaToMap(Map<String, Set<String>> map, ICriteria[] criteria) {
		for (ICriteria criterion : criteria) {
			String name = criterion.getName();
			List<String> values = CriteriaUtilities.getCriteriaValues(criterion.getValue());
			if (name != null && name.length() > 0 && values.size() > 0) {
				name = name.toLowerCase();
				Set<String> existingValueSet = map.get(name);
				if (null == existingValueSet) {
					existingValueSet = new HashSet<>();
				}
				existingValueSet.addAll(values);
				map.put(name, existingValueSet);
			}
		}
	}

	public static void addCriteriaToMap(Map<String, Set<String>> map, CriterionResource[] criteria) {
		for (CriterionResource criterion : criteria) {
			String criterionName = criterion.getCriterionName();
			List<String> criterionValues = criterion.getCriterionValues();

			Set<String> existedValueSet = map.get(criterionName);
			if (null == existedValueSet)
				existedValueSet = new HashSet<>();
			existedValueSet.addAll(criterionValues);
			map.put(criterionName, existedValueSet);
		}
	}

}
