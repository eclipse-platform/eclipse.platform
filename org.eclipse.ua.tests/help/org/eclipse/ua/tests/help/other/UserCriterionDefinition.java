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

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.help.ICriterionDefinition;
import org.eclipse.help.ICriterionValueDefinition;
import org.eclipse.help.IUAElement;

public class UserCriterionDefinition implements ICriterionDefinition {

	private String id;
	private String name;
	private List<ICriterionValueDefinition> children = new ArrayList<>();

	public UserCriterionDefinition(String id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public boolean isEnabled(IEvaluationContext context) {
		return true;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IUAElement[] getChildren() {
		return getCriterionValueDefinitions();
	}

	@Override
	public ICriterionValueDefinition[] getCriterionValueDefinitions() {
		return children.toArray(new ICriterionValueDefinition[0]);
	}

	public void addValue(ICriterionValueDefinition value) {
		children.add(value);
	}

}
