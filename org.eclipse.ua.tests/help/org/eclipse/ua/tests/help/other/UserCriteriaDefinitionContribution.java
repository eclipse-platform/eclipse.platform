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

package org.eclipse.ua.tests.help.other;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.help.ICriteriaDefinition;
import org.eclipse.help.ICriteriaDefinitionContribution;

public class UserCriteriaDefinitionContribution implements ICriteriaDefinitionContribution {

	private ICriteriaDefinition criteriaDefinition;
	private String id;
	private String locale;

	public boolean isEnabled(IEvaluationContext context) {
		return true;
	}

	public void setCriteriaDefinition(ICriteriaDefinition criteriaDefinition) {
		this.criteriaDefinition = criteriaDefinition;
	}

	@Override
	public ICriteriaDefinition getCriteriaDefinition() {
		return criteriaDefinition;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	@Override
	public String getLocale() {
		return locale;
	}

}
