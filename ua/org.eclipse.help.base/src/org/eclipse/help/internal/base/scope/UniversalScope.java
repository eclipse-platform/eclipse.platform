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

public class UniversalScope extends AbstractHelpScope {

	@Override
	public boolean inScope(IToc toc) {
		return true;
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

	@Override
	public String getName(Locale locale) {
		// TODO Auto-generated method stub
		return null;
	}

}
