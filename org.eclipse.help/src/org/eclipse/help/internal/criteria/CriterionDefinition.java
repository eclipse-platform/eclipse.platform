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

import org.eclipse.help.ICriterionDefinition;
import org.eclipse.help.ICriterionValueDefinition;
import org.eclipse.help.internal.UAElement;
import org.w3c.dom.Element;

public class CriterionDefinition extends UAElement implements ICriterionDefinition {

	public static final String NAME = "criterion"; //$NON-NLS-1$
	public static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	public static final String ATTRIBUTE_NAME = "name"; //$NON-NLS-1$

	public CriterionDefinition(ICriterionDefinition src) {
		super(NAME, src);
		setId(src.getId());
		setName(src.getName());
		appendChildren(src.getChildren());
	}

	public CriterionDefinition(Element src) {
		super(src);
	}

	@Override
	public String getId() {
		return getAttribute(ATTRIBUTE_ID);
	}

	@Override
	public String getName() {
		return getAttribute(ATTRIBUTE_NAME);
	}

	public void setId(String id) {
		setAttribute(ATTRIBUTE_ID, id);
	}

	public void setName(String name){
		setAttribute(ATTRIBUTE_NAME, name);
	}

	@Override
	public ICriterionValueDefinition[] getCriterionValueDefinitions() {
		return getChildren(ICriterionValueDefinition.class);
	}

}
