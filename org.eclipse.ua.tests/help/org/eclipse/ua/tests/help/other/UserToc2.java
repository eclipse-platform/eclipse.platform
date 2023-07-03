/*******************************************************************************
 * Copyright (c) 2010, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.other;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.help.ICriteria;
import org.eclipse.help.IToc2;
import org.eclipse.help.IUAElement;

public class UserToc2 extends UserToc implements IToc2 {

	public UserToc2(String label, String href, boolean isEnabled) {
		super(label, href, isEnabled);
	}

	private List<ICriteria> criteria = new ArrayList<>();

	@Override
	public IUAElement[] getChildren() {
		IUAElement[] criteriaElements = getCriteria();
		IUAElement[] topics = getTopics();
		IUAElement[] result = new IUAElement[criteriaElements.length + topics.length];
		System.arraycopy(topics, 0, result, 0, topics.length);
		System.arraycopy(criteriaElements, 0, result, topics.length, criteriaElements.length);
		return result;
	}

	public void addCriterion(ICriteria child) {
		criteria.add(child);
	}

	@Override
	public ICriteria[] getCriteria() {
		return criteria.toArray(new ICriteria[0]);
	}

	@Override
	public String getIcon() {
		return null;
	}

	@Override
	public boolean isSorted() {
		return false;
	}

}
