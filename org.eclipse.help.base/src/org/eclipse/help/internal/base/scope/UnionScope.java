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
 * A scope which represents the union of two or more other scopes
 * An element is in scope if it is included in any scope passed to the constructor
 */

public class UnionScope extends AbstractHelpScope {

	AbstractHelpScope[] scopes;

	public UnionScope(AbstractHelpScope[] scopes) {
		this.scopes = scopes;
	}

	@Override
	public boolean inScope(IToc toc) {
		for (AbstractHelpScope scope : scopes) {
			if (scope.inScope(toc)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean inScope(ITopic topic) {
		for (AbstractHelpScope scope : scopes) {
			if (scope.inScope(topic)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean inScope(IIndexEntry entry) {
		for (AbstractHelpScope scope : scopes) {
			if (scope.inScope(entry)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean inScope(IIndexSee see) {
		for (AbstractHelpScope scope : scopes) {
			if (scope.inScope(see)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String getName(Locale locale) {
		return null;
	}

	@Override
	public boolean isHierarchicalScope() {
		for (AbstractHelpScope scope : scopes) {
			if (!scope.isHierarchicalScope()) {
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
				str+=' '+ScopeRegistry.SCOPE_OR+' ';
		}
		return str+')';
	}
}
