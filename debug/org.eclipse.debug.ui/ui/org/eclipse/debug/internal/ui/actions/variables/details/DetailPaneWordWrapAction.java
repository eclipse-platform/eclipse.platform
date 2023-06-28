/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.variables.details;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;

/**
 * An check box action that allows the word wrap property to be set, determining if the detail pane
 * should wrap text.
 */
public class DetailPaneWordWrapAction extends Action {

	ITextViewer fTextViewer;

	public DetailPaneWordWrapAction(ITextViewer textViewer) {
		super(ActionMessages.DetailPaneWordWrapAction_0,IAction.AS_CHECK_BOX);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.DETAIL_PANE_WORD_WRAP_ACTION);

		fTextViewer = textViewer;
		setEnabled(true);

		boolean prefSetting = DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IDebugPreferenceConstants.PREF_DETAIL_PANE_WORD_WRAP);
		fTextViewer.getTextWidget().setWordWrap(prefSetting);
		setChecked(prefSetting);
	}

	@Override
	public void run() {
		fTextViewer.getTextWidget().setWordWrap(isChecked());
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(DebugUIPlugin.getUniqueIdentifier());
		if(node != null) {
			try {
				node.putBoolean(IDebugPreferenceConstants.PREF_DETAIL_PANE_WORD_WRAP, isChecked());
				node.flush();
			} catch (BackingStoreException e) {
				DebugUIPlugin.log(e);
			}
		}
	}
}
