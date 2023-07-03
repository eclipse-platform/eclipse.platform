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
import org.eclipse.help.ICriteria;
import org.eclipse.help.IUAElement;

public class UserCriteria implements ICriteria {

	private String name;
	private String value;
	private boolean enabled;

	public UserCriteria(String name, String value, boolean enabled) {
		this.name = name;
		this.value = value;
		this.enabled = enabled;
	}

	@Override
	public boolean isEnabled(IEvaluationContext context) {
		return enabled;
	}

	@Override
	public IUAElement[] getChildren() {
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getValue() {
		return value;
	}

}
