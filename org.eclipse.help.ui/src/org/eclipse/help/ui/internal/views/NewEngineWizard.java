/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.views;

import org.eclipse.help.ui.internal.HelpUIResources;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.wizard.Wizard;

public class NewEngineWizard extends Wizard {
	private EngineTypeDescriptor[] engineTypes;

	private EngineTypeWizardPage selectionPage;

	public NewEngineWizard(EngineTypeDescriptor[] engineTypes) {
		setWindowTitle(Messages.NewEngineWizard_wtitle);
		setDefaultPageImageDescriptor(HelpUIResources
				.getImageDescriptor(IHelpUIConstants.IMAGE_SEARCH_WIZ));
		this.engineTypes = engineTypes;
	}

	@Override
	public void addPages() {
		selectionPage = new EngineTypeWizardPage(engineTypes);
		addPage(selectionPage);
	}

	@Override
	public boolean performFinish() {
		return true;
	}

	public EngineTypeDescriptor getSelectedEngineType() {
		return selectionPage.getSelectedEngineType();
	}
}