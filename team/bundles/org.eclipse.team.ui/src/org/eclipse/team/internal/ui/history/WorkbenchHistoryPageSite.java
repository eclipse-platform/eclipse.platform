/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.history;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.ui.history.IHistoryPageSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.part.IPageSite;

public class WorkbenchHistoryPageSite implements IHistoryPageSite {

	GenericHistoryView part;
	IPageSite site;

	public WorkbenchHistoryPageSite(GenericHistoryView part, IPageSite site) {
		this.part = part;
		this.site = site;
	}

	@Override
	public IPageSite getWorkbenchPageSite() {
		return site;
	}

	@Override
	public IWorkbenchPart getPart() {
		return part;
	}

	@Override
	public Shell getShell() {
		return site.getShell();
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
	public void setFocus() {
//		 Nothing to do
	}

	@Override
	public boolean isModal() {
		return false;
	}

	@Override
	public IToolBarManager getToolBarManager() {
		return site.getActionBars().getToolBarManager();
	}

}
