/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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
import org.eclipse.help.IIndexSee;
import org.eclipse.help.IIndexSubpath;
import org.eclipse.help.IUAElement;

public class UserIndexSee implements IIndexSee {

	private List<IIndexSubpath> children = new ArrayList<>();
	private boolean isEnabled;
	private String keyword;

	@Override
	public IUAElement[] getChildren() {
		return getSubpathElements();
	}

	@Override
	public boolean isEnabled(IEvaluationContext context) {
		return isEnabled;
	}

	public void addSubpath(IIndexSubpath child) {
		children.add(child);
	}

	public UserIndexSee(String keyword, boolean isEnabled) {
		this.keyword = keyword;
		this.isEnabled = isEnabled;
	}

	@Override
	public String getKeyword() {
		return keyword;
	}

	@Override
	public IIndexSubpath[] getSubpathElements() {
		return children.toArray(new IIndexSubpath[0]);
	}

	@Override
	public boolean isSeeAlso() {
		return false;
	}

}
