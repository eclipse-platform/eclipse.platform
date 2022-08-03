/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.synchronize.ISynchronizePageSite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.part.IPageSite;

/**
 * Maps a workbench part to a synchronize page site.
 */
public class WorkbenchPartSynchronizePageSite implements ISynchronizePageSite {
	private IWorkbenchPart part;
	private IDialogSettings settings;
	private IPageSite site;

	public WorkbenchPartSynchronizePageSite(IWorkbenchPart part, IPageSite site, IDialogSettings settings) {
		this.part = part;
		this.site = site;
		this.settings = settings;
	}

	@Override
	public IWorkbenchPart getPart() {
		return part;
	}

	@Override
	public Shell getShell() {
		return part.getSite().getShell();
	}

	@Override
	public ISelectionProvider getSelectionProvider() {
		return site.getSelectionProvider();
	}

	@Override
	public void setSelectionProvider(ISelectionProvider provider) {
		site.setSelectionProvider(provider);
	}

	@Override
	public IWorkbenchSite getWorkbenchSite() {
		return part.getSite();
	}

	@Override
	public IKeyBindingService getKeyBindingService() {
		return part.getSite().getKeyBindingService();
	}

	@Override
	public void setFocus() {
		part.getSite().getPage().activate(part);
	}

	@Override
	public IDialogSettings getPageSettings() {
		return settings;
	}

	@Override
	public IActionBars getActionBars() {
		return site.getActionBars();
	}

	@Override
	public boolean isModal() {
		return false;
	}
}
