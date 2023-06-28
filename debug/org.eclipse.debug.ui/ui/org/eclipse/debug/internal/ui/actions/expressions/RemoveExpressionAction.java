/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Wind River Systems - integration with non-standard debug models (Bug 209883)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.expressions;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.internal.ui.actions.AbstractSelectionActionDelegate;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.ui.progress.WorkbenchJob;

public class RemoveExpressionAction extends AbstractSelectionActionDelegate {

	protected IExpression[] getExpressions() {
		TreeSelection selection = (TreeSelection) getSelection();
		TreePath[] paths = selection.getPaths();
		List<IExpression> expressions = new ArrayList<>();
		for (int i = paths.length-1; i >=0; i--) {
			TreePath path = paths[i];
			Object segment = path.getFirstSegment();
			if (segment instanceof IExpression) {
				expressions.add((IExpression) segment);
			} else if (segment instanceof IAdaptable) {
				IExpression expression = ((IAdaptable)segment).getAdapter(IExpression.class);
				if (expression != null) {
					expressions.add(expression);
				}
			}
		}
		return expressions.toArray(new IExpression[expressions.size()]);
	}

	@Override
	public void run(IAction action) {
		WorkbenchJob job = new WorkbenchJob("remove expression") { //$NON-NLS-1$
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				IExpressionManager expManager = DebugPlugin.getDefault().getExpressionManager();
				IExpression[] exp = getExpressions();
				if (exp != null) {
					expManager.removeExpressions(exp);
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		schedule(job);
	}

	@Override
	protected boolean isEnabledFor(Object element) {
		return DebugPlugin.getAdapter(element, IExpression.class) != null;
	}
}

