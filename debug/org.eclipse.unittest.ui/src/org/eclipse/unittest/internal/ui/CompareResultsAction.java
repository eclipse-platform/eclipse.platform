/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.unittest.internal.ui;

import org.eclipse.unittest.internal.model.TestElement;
import org.eclipse.unittest.model.ITestElement;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.PlatformUI;

/**
 * Action to enable/disable stack trace filtering.
 */
public class CompareResultsAction extends Action {

	private FailureTraceUIBlock fView;
	private CompareResultDialog fOpenDialog;

	/**
	 * Constructs a compare result object
	 *
	 * @param view a {@link FailureTraceUIBlock} object
	 */
	public CompareResultsAction(FailureTraceUIBlock view) {
		super(Messages.CompareResultsAction_label);
		setDescription(Messages.CompareResultsAction_description);
		setToolTipText(Messages.CompareResultsAction_tooltip);

		setDisabledImageDescriptor(Images.getImageDescriptor("dlcl16/compare.png")); //$NON-NLS-1$
		setHoverImageDescriptor(Images.getImageDescriptor("elcl16/compare.png")); //$NON-NLS-1$
		setImageDescriptor(Images.getImageDescriptor("elcl16/compare.png")); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IUnitTestHelpContextIds.ENABLEFILTER_ACTION);
		fView = view;
	}

	@Override
	public void run() {
		TestElement failedTest = fView.getFailedTest();
		if (fOpenDialog != null) {
			fOpenDialog.setInput(failedTest);
			fOpenDialog.getShell().setActive();

		} else {
			fOpenDialog = new CompareResultDialog(fView.getShell(), failedTest);
			fOpenDialog.create();
			fOpenDialog.getShell().addDisposeListener(e -> fOpenDialog = null);
			fOpenDialog.setBlockOnOpen(false);
			fOpenDialog.open();
		}
	}

	/**
	 * Updates the CompareResultDialog with a failed {@link ITestElement} as input
	 *
	 * @param failedTest a failed test element
	 */
	public void updateOpenDialog(TestElement failedTest) {
		if (fOpenDialog != null) {
			fOpenDialog.setInput(failedTest);
		}
	}
}
