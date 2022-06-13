/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *     Simon Scholz <simon.scholz@vogella.com> - Ongoing maintenance
 *******************************************************************************/
package org.eclipse.core.tools.search;

import java.util.Iterator;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.search.ui.*;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.TextSearchQueryProvider;
import org.eclipse.ui.*;

public class FindUnreferencedFilesAction implements IObjectActionDelegate {

	private IStructuredSelection selection;

	@Override
	public void setActivePart(IAction action, IWorkbenchPart part) {
		// not needed
	}

	@Override
	public void run(IAction action) {
		try {
			for (Iterator<?> it = selection.iterator(); it.hasNext();) {
				IResource resource = (IResource) it.next();
				findReferences(resource);
			}
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}

	private void findReferences(IResource resource) throws CoreException {
		if (resource instanceof IContainer) {
			IResource[] children = ((IContainer) resource).members();
			for (IResource element : children) {
				findReferences(element);
			}
		} else if (resource instanceof IFile) {
			String name = resource.getName();
			IRunnableContext context = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			ISearchQuery query = TextSearchQueryProvider.getPreferred().createQuery(name);
			NewSearchUI.runQueryInForeground(context, query);
			ISearchResult result = query.getSearchResult();
			if (result instanceof AbstractTextSearchResult) {
				int matches = ((AbstractTextSearchResult) result).getMatchCount();
				if (matches == 0) {
					System.out.println("Orphan file: " + resource.getFullPath());
				}
			}
		}

	}

	@Override
	public void selectionChanged(IAction action, ISelection aSelection) {
		if (aSelection instanceof IStructuredSelection)
			this.selection = (IStructuredSelection) aSelection;
	}
}