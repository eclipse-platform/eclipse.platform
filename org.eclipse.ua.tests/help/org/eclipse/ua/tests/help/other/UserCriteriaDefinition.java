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
import org.eclipse.help.ICriteriaDefinition;
import org.eclipse.help.ICriterionDefinition;
import org.eclipse.help.IUAElement;

public class UserCriteriaDefinition implements ICriteriaDefinition {

	private List<ICriterionDefinition> children = new ArrayList<>();

	@Override
	public boolean isEnabled(IEvaluationContext context) {
		return true;
	}

	@Override
	public ICriterionDefinition[] getCriterionDefinitions() {
		return children.toArray(new ICriterionDefinition[0]);
	}

	public void addDefinition(ICriterionDefinition definition) {
		children.add(definition);
	}

	@Override
	public IUAElement[] getChildren() {
		return getCriterionDefinitions();
	}


}
