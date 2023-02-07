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

package org.eclipse.help.internal.criteria;

import org.eclipse.help.ICriteriaDefinition;
import org.eclipse.help.ICriterionDefinition;
import org.eclipse.help.internal.UAElement;
import org.w3c.dom.Element;

public class CriteriaDefinition extends UAElement implements ICriteriaDefinition {

	public static final String NAME = "criteriaDefinition"; //$NON-NLS-1$

	public CriteriaDefinition() {
		super(NAME);
	}

	public CriteriaDefinition(ICriteriaDefinition src) {
		super(NAME, src);
		appendChildren(src.getChildren());
	}

	public CriteriaDefinition(Element src) {
		super(src);
	}

	@Override
	public ICriterionDefinition[] getCriterionDefinitions() {
		return getChildren(ICriterionDefinition.class);
	}

}
