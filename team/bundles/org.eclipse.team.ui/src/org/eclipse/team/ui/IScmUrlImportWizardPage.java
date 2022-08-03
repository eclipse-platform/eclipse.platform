/*******************************************************************************
 * Copyright (c) 2011, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.team.core.ScmUrlImportDescription;

/**
 * IScmUrlImportWizardPage defines the interface that users of the extension
 * point <code>org.eclipse.team.ui.scmUrlImportPages</code> must implement.
 *
 * <strong>EXPERIMENTAL</strong>. This class has been added as part of a work in
 * progress. There is no guarantee that this API will work or that it will
 * remain the same. Please do not use this API without consulting with the Team
 * team.
 *
 * @since 3.6
 */
public interface IScmUrlImportWizardPage extends IWizardPage {

	public static final String ATT_EXTENSION = "scmUrlImportPages"; //$NON-NLS-1$
	public static final String ATT_PAGE = "page"; //$NON-NLS-1$
	public static final String ATT_IMPORTER = "importer"; //$NON-NLS-1$

	/**
	 * Called when the import wizard is closed by selecting the finish button.
	 * Implementers may store the page result (new/changed bundle import
	 * descriptions in getSelection) here.
	 *
	 * @return if the operation was successful. The wizard will only close when
	 *         <code>true</code> is returned.
	 */
	public boolean finish();

	/**
	 * Return the import descriptions for the page. The descriptions may differ from those initially
	 * set using {@link #setSelection(ScmUrlImportDescription[])} if the user modified the import
	 * configuration.
	 *
	 * @return the SCM URLs descriptions for the page or <code>null</code> if no selection has been
	 *         set
	 */
	public ScmUrlImportDescription[] getSelection();

	/**
	 * Sets the import descriptions to be edited on the page. The passed
	 * descriptions can be edited and should be returned in
	 * {@link #getSelection()}.
	 *
	 * @param descriptions
	 *            the SCM URLs descriptions edited on the page.
	 */
	public void setSelection(ScmUrlImportDescription[] descriptions);
}
