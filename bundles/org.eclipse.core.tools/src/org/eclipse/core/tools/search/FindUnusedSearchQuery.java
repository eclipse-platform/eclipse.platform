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

import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;

public class FindUnusedSearchQuery implements ISearchQuery {

	private final ICompilationUnit[] fCus;
	private FindUnusedSearchResult fSearchResult;

	public FindUnusedSearchQuery(ICompilationUnit[] cus) {
		fCus = cus;
		fSearchResult = new FindUnusedSearchResult(this);
	}

	@Override
	public boolean canRerun() {
		return true;
	}

	@Override
	public boolean canRunInBackground() {
		return true;
	}

	@Override
	public String getLabel() {
		return "Find Unreferenced Members"; //$NON-NLS-1$
	}

	@Override
	public ISearchResult getSearchResult() {
		return fSearchResult;
	}

	@Override
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException {
		fSearchResult.removeAll();

		FindUnusedMembers search = new FindUnusedMembers(fCus, fSearchResult);
		try {
			search.process(monitor);
		} catch (CoreException e) {
			return e.getStatus();
		}
		return Status.OK_STATUS;
	}

}
