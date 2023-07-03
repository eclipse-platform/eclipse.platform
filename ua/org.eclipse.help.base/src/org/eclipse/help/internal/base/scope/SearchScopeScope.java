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
import org.eclipse.help.internal.base.HelpBaseResources;

/**
 * This is a dummy scope which is used by the UI to allow the user to turn on
 * and off filtering by search scope. The inScope functions should never be called
 */

public class SearchScopeScope extends AbstractHelpScope {

	@Override
	public boolean inScope(IToc toc) {
		return false;
	}

	@Override
	public boolean inScope(ITopic topic) {
		return false;
	}

	@Override
	public boolean inScope(IIndexEntry entry) {
		return false;
	}

	@Override
	public boolean inScope(IIndexSee see) {
		return false;
	}

	/**
	 * @return the name for the system locale. This filter only applies to
	 * the help system running in workbench mode so there is not need to
	 * be able to return a name in any locale, just the current one.
	 */
	@Override
	public String getName(Locale locale) {
		return HelpBaseResources.SearchScopeFilterName;
	}

}
