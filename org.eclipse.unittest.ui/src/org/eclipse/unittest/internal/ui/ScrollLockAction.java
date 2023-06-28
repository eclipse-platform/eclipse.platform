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

import org.eclipse.ui.PlatformUI;

/**
 * Toggles console auto-scroll
 */
public class ScrollLockAction extends Action {

	private TestRunnerViewPart fRunnerViewPart;

	/**
	 * Constructs a scroll lock toggle action
	 *
	 * @param viewer a test runner viewer part object
	 */
	public ScrollLockAction(TestRunnerViewPart viewer) {
		super(Messages.ScrollLockAction_action_label);
		fRunnerViewPart = viewer;
		setToolTipText(Messages.ScrollLockAction_action_tooltip);
		setDisabledImageDescriptor(Images.getImageDescriptor("dlcl16/lock.png")); //$NON-NLS-1$
		setHoverImageDescriptor(Images.getImageDescriptor("elcl16/lock.png")); //$NON-NLS-1$
		setImageDescriptor(Images.getImageDescriptor("elcl16/lock.png")); //$NON-NLS-1$
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IUnitTestHelpContextIds.OUTPUT_SCROLL_LOCK_ACTION);
		setChecked(false);
	}

	@Override
	public void run() {
		fRunnerViewPart.setAutoScroll(!isChecked());
	}
}
