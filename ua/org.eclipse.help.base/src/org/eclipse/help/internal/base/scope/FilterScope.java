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
import org.eclipse.help.internal.base.HelpEvaluationContext;

/**
 * A scope which tests for content filtering
 */

public class FilterScope extends AbstractHelpScope {

	@Override
	public boolean inScope(IToc toc) {
		if (!toc.isEnabled(HelpEvaluationContext.getContext())) {
			return false;
		}
		return hasInScopeChildren(toc);
	}

	@Override
	public boolean inScope(ITopic topic) {
		if (!topic.isEnabled(HelpEvaluationContext.getContext())) {
			return false;
		}
		if (topic.getHref() != null) {
			return true;
		}
		return ScopeUtils.hasInScopeDescendent(topic, this);
	}

	@Override
	public boolean inScope(IIndexEntry entry) {
		return entry.isEnabled(HelpEvaluationContext.getContext());
	}

	@Override
	public boolean inScope(IIndexSee see) {
		return see.isEnabled(HelpEvaluationContext.getContext());
	}

	@Override
	public String getName(Locale locale) {
		// TODO Auto-generated method stub
		return null;
	}

}
