/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *     Lars.Vogel <Lars.Vogel@vogell.com> - Ongoing maintenance
 *******************************************************************************/

package org.eclipse.core.tools.search;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tools.search.FindUnusedMembers.IResultReporter;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.ISourceRange;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.*;

/**
 *
 */
public class FindUnusedSearchResult extends AbstractTextSearchResult implements IResultReporter {

	private final FindUnusedSearchQuery fQuery;

	public FindUnusedSearchResult(FindUnusedSearchQuery query) {
		fQuery = query;
	}

	@Override
	public IEditorMatchAdapter getEditorMatchAdapter() {
		return null;
	}

	@Override
	public IFileMatchAdapter getFileMatchAdapter() {
		return null;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getLabel() {
		if (getMatchCount() == 1)
			return "1 unused element found"; //$NON-NLS-1$
		return getMatchCount() + " unused elements found"; //$NON-NLS-1$
	}

	@Override
	public ISearchQuery getQuery() {
		return fQuery;
	}

	@Override
	public String getTooltip() {
		return getLabel();
	}

	@Override
	public void unusedElementFound(IMember member) throws CoreException {
		ISourceRange nameRange = member.getNameRange();
		addMatch(new Match(member, nameRange.getOffset(), nameRange.getLength()));
	}

}
