/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.intro.impl.IntroPlugin;
import org.eclipse.ui.intro.IIntroSite;


public class CustomizeAction extends Action {

	private IIntroSite site;

	public CustomizeAction(IIntroSite site) {
		this.site = site;
	}

	@Override
	public void run() {
		String pageId = IntroPlugin.getDefault().getIntroModelRoot().getCurrentPageId();
		run(pageId);
	}

	private void run(String pageId) {
		IWorkbenchWindow window = site.getWorkbenchWindow();
		CustomizationDialog dlg = new CustomizationDialog(window.getShell(), pageId);
		dlg.open();
	}

}