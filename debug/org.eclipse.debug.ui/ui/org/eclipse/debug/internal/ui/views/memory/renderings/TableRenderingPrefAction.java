/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.views.memory.renderings;

import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.actions.ActionDelegate;

public class TableRenderingPrefAction extends ActionDelegate implements IViewActionDelegate {

	@Override
	public void run(IAction action) {
		IPreferencePage page = new TableRenderingPreferencePage(DebugUIMessages.TableRenderingPrefAction_0);
		showPreferencePage("org.eclipse.debug.ui.tableRenderingPreferencepage", page);	 //$NON-NLS-1$
	}

	@Override
	public void init(IViewPart view) {
	}

	protected void showPreferencePage(String id, IPreferencePage page) {
		final IPreferenceNode targetNode = new PreferenceNode(id, page);

		PreferenceManager manager = new PreferenceManager();
		manager.addToRoot(targetNode);
		final PreferenceDialog dialog = new PreferenceDialog(DebugUIPlugin.getShell(), manager);
		final boolean [] result = new boolean[] { false };
		BusyIndicator.showWhile(DebugUIPlugin.getStandardDisplay(), () -> {
			dialog.create();
			dialog.setMessage(targetNode.getLabelText());
			result[0] = (dialog.open() == Window.OK);
		});
	}

}
