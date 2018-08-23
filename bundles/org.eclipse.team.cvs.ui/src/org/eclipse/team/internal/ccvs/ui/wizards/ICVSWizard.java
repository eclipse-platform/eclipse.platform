/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.wizards;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;

/**
 * Extended wizard interface that differentiates retrieving the next page for
 * display vs. for determining it's state.
 */
public interface ICVSWizard extends IWizard {

	/**
	 * Get the wizard page that follows the given page. If
	 * <code>aboutToShow</code> is <code>true</code> then the page will be
	 * shown. Otherwise, only its state will be queried.
	 * 
	 * @param page
	 *            a wizard page
	 * @param aboutToShow
	 *            true if the page returned will be shown
	 * @return the next wizard page
	 */
	public IWizardPage getNextPage(IWizardPage page, boolean aboutToShow);
}
