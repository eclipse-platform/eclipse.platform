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

import org.eclipse.jface.action.Action;

/**
 * Show previous failure action
 */
public class ShowPreviousFailureAction extends Action {

	private TestRunnerViewPart fPart;

	/**
	 * Constructs a show previous failure action object
	 *
	 * @param part a test runner view part object
	 */
	public ShowPreviousFailureAction(TestRunnerViewPart part) {
		super(Messages.ShowPreviousFailureAction_label);
		setDisabledImageDescriptor(Images.getImageDescriptor("dlcl16/select_prev.png")); //$NON-NLS-1$
		setHoverImageDescriptor(Images.getImageDescriptor("elcl16/select_prev.png")); //$NON-NLS-1$
		setImageDescriptor(Images.getImageDescriptor("elcl16/select_prev.png")); //$NON-NLS-1$
		setToolTipText(Messages.ShowPreviousFailureAction_tooltip);
		fPart = part;
	}

	@Override
	public void run() {
		fPart.selectPreviousFailure();
	}
}
