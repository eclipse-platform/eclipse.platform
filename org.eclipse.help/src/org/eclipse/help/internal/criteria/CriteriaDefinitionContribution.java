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

import org.eclipse.help.ICriteriaDefinition;
import org.eclipse.help.ICriteriaDefinitionContribution;


public class CriteriaDefinitionContribution implements ICriteriaDefinitionContribution{

	private String id;
	private ICriteriaDefinition criteriaDefinition;
	private String locale;

	@Override
	public String getId() {
		return id;
	}

	@Override
	public ICriteriaDefinition getCriteriaDefinition() {
		return criteriaDefinition;
	}

	@Override
	public String getLocale() {
		return locale;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setCriteriaDefinition(ICriteriaDefinition criteriaDefinition) {
		this.criteriaDefinition = criteriaDefinition;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}
}
