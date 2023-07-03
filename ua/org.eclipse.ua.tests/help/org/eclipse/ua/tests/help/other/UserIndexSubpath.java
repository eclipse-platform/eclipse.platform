/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
import org.eclipse.help.IIndexSubpath;
import org.eclipse.help.IUAElement;


public class UserIndexSubpath implements IIndexSubpath {

	private String keyword;

	public UserIndexSubpath(String keyword) {
		this.keyword = keyword;
	}


	@Override
	public String getKeyword() {
		return keyword;
	}

	@Override
	public IUAElement[] getChildren() {
		return new IUAElement[0];
	}

	@Override
	public boolean isEnabled(IEvaluationContext context) {
		return true;
	}

}
