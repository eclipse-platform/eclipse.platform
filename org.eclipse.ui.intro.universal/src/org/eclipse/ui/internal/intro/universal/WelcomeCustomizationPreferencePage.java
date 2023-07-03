/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.universal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class WelcomeCustomizationPreferencePage extends PreferencePage implements IWorkbenchPreferencePage,
		IExecutableExtension {

	private CustomizationContentsArea contentsArea;

	public WelcomeCustomizationPreferencePage() {
	}

	public WelcomeCustomizationPreferencePage(String title) {
		super(title);
	}

	public WelcomeCustomizationPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	private CustomizationContentsArea getContentsArea() {
		if (contentsArea == null) {
			contentsArea = new CustomizationContentsArea();
		}
		return contentsArea;
	}

	@Override
	protected Control createContents(Composite parent) {
		getContentsArea().setShell(getShell());
		return getContentsArea().createContents(parent);
	}

	@Override
	public void dispose() {
		getContentsArea().dispose();
		super.dispose();
	}

	@Override
	public boolean performOk() {
		return getContentsArea().performOk();
	}

	@Override
	protected void performDefaults() {
		getContentsArea().performDefaults();
	}


	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	public void setCurrentPage(String pageId) {
		getContentsArea().setCurrentPage(pageId);
	}
}
