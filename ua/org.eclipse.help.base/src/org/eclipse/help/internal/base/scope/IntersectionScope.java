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

package org.eclipse.help.internal.base.scope;

import java.util.Locale;

import org.eclipse.help.IIndexEntry;
import org.eclipse.help.IIndexSee;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.base.AbstractHelpScope;

/**
 * A scope which represents the intersection of two or more other scopes
 * An element is in scope only if it is included in every scope passed to the constructor
 */

public class IntersectionScope extends AbstractHelpScope {

	AbstractHelpScope[] scopes;

	public IntersectionScope(AbstractHelpScope[] scopes) {
		this.scopes = scopes;
	}

	@Override
	public boolean inScope(IToc toc) {
		for (int scope = 0; scope < scopes.length; scope ++) {
			if (!scopes[scope].inScope(toc)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean inScope(ITopic topic) {
		for (int scope = 0; scope < scopes.length; scope ++) {
			if (!scopes[scope].inScope(topic)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean inScope(IIndexEntry entry) {
		for (int scope = 0; scope < scopes.length; scope ++) {
			if (!scopes[scope].inScope(entry)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean inScope(IIndexSee see) {
		for (int scope = 0; scope < scopes.length; scope ++) {
			if (!scopes[scope].inScope(see)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String getName(Locale locale) {
		return null;
	}

	@Override
	public boolean isHierarchicalScope() {
		for (int scope = 0; scope < scopes.length; scope ++) {
			if (!scopes[scope].isHierarchicalScope()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString()
	{
		String str = "("; //$NON-NLS-1$
		for (int s=0;s<scopes.length;s++)
		{
			str+=scopes[s];
			if (s<scopes.length-1)
				str+=' '+ScopeRegistry.SCOPE_AND+' ';
		}
		return str+')';
	}
}
