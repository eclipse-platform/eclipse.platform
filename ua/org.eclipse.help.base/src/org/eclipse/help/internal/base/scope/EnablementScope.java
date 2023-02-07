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
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.HelpBaseResources;

public class EnablementScope extends AbstractHelpScope {

	@Override
	public boolean inScope(IToc toc) {
		return HelpBasePlugin.getActivitySupport().isEnabled(toc.getHref());
	}

	@Override
	public boolean inScope(ITopic topic) {
		return true;
	}

	@Override
	public boolean inScope(IIndexEntry entry) {
		return true;
	}

	@Override
	public boolean inScope(IIndexSee see) {
		return true;
	}

	/**
	 * @return the name for the system locale. This filter only applies to
	 * the help system running in workbench mode so there is not need to
	 * be able to return a name in any locale, just the current one.
	 */
	@Override
	public String getName(Locale locale) {
		return HelpBaseResources.EnabledTopicFilterName;
	}

}
