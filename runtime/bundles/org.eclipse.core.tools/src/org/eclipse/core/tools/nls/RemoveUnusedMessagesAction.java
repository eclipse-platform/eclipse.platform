/*******************************************************************************
 * Copyright (c) 2005, 2019 IBM Corporation and others.
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
package org.eclipse.core.tools.nls;

import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.internal.ui.actions.WorkbenchRunnableAdapter;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ltk.core.refactoring.CheckConditionsOperation;
import org.eclipse.ltk.core.refactoring.PerformRefactoringOperation;
import org.eclipse.ui.*;

public class RemoveUnusedMessagesAction implements IObjectActionDelegate {

	private ICompilationUnit fAccessorUnit;
	private IWorkbenchPart fPart;

	@Override
	public void setActivePart(IAction action, IWorkbenchPart part) {
		fPart = part;
	}

	@Override
	public void run(IAction action) {
		if (fAccessorUnit == null)
			return;
		try {
			final GotoResourceAction pAction = new GotoResourceAction(fPart);
			IRunnableWithProgress runnable = monitor -> pAction.run();
			PlatformUI.getWorkbench().getProgressService().run(false, false, runnable);
			IFile propertiesFile = (IFile) pAction.getResource();
			if (propertiesFile == null)
				return;
			RemoveUnusedMessages refactoring = new RemoveUnusedMessages(fAccessorUnit.getTypes()[0], propertiesFile);
			PerformRefactoringOperation op = new PerformRefactoringOperation(refactoring, CheckConditionsOperation.ALL_CONDITIONS);
			PlatformUI.getWorkbench().getProgressService().run(false, true, new WorkbenchRunnableAdapter(op));
		} catch (CoreException | InterruptedException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		Object element = ((IStructuredSelection) selection).getFirstElement();
		if (element instanceof ICompilationUnit) {
			fAccessorUnit = (ICompilationUnit) element;
		}
	}
}