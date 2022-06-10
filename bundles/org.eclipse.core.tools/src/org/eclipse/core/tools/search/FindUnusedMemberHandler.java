/*******************************************************************************
 * Copyright (c) 2015, 2018 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     vogella GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools.search;

import java.util.ArrayList;
import java.util.Collection;
import org.eclipse.core.commands.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class FindUnusedMemberHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection sel = HandlerUtil.getCurrentSelection(event);
		if (!(sel instanceof IStructuredSelection)) {
			return null;
		}

		IStructuredSelection currentSelection = HandlerUtil.getCurrentStructuredSelection(event);

		ArrayList<IJavaElement> allCus = new ArrayList<>();
		try {
			for (Object element : currentSelection.toArray()) {
				if (element instanceof IJavaElement) {
					collectCompilationUnits((IJavaElement) element, allCus);
				}
			}
		} catch (JavaModelException e) {
			ErrorDialog.openError(HandlerUtil.getActiveShell(event), "Find Unused Members", //$NON-NLS-1$
					"Problem collecting compilation units", e.getStatus()); //$NON-NLS-1$
			return null;
		}

		if (allCus.isEmpty()) {
			MessageDialog.openInformation(HandlerUtil.getActiveShell(event), "Find Unused Members", //$NON-NLS-1$
					"No compilation units in 'internal' packages selected"); //$NON-NLS-1$
			return null;
		}

		ICompilationUnit[] cus = allCus.toArray(new ICompilationUnit[allCus.size()]);

		NewSearchUI.runQueryInBackground(new FindUnusedSearchQuery(cus));

		return null;
	}

	private void collectCompilationUnits(IJavaElement current, Collection<IJavaElement> res) throws JavaModelException {
		if (current instanceof IJavaProject || current instanceof IPackageFragmentRoot) {
			handlePotentialCompilationUnitChildren(current, res);
		} else if (current instanceof IPackageFragment) {
			// uncomment this condition to only search API packages
			// if (current.getElementName().indexOf("internal") > 0) {
			// //$NON-NLS-1$
			handlePotentialCompilationUnitChildren(current, res);
			// }
		} else if (current instanceof ICompilationUnit) {
			res.add(current);
		}
	}

	private void handlePotentialCompilationUnitChildren(IJavaElement current, Collection<IJavaElement> res)
			throws JavaModelException {
		IJavaElement[] children = ((IParent) current).getChildren();
		for (IJavaElement element : children) {
			collectCompilationUnits(element, res);
		}
	}

}
