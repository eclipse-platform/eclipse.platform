/**********************************************************************
 * Copyright (c) 2004, 2006 QNX Software Systems and others.
 *
 *   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
***********************************************************************/
package org.eclipse.debug.internal.ui.views.registers;

import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.internal.ui.views.variables.VariablesView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;

/**
 * Displays registers and their values with a detail area.
 */
public class RegistersView extends VariablesView {

	@Override
	protected String getHelpContextId() {
		return IDebugHelpContextIds.REGISTERS_VIEW;
	}

	@Override
	protected void configureToolBar(IToolBarManager tbm) {
		super.configureToolBar(tbm);
		tbm.add(new Separator(IDebugUIConstants.EMPTY_REGISTER_GROUP));
		tbm.add(new Separator(IDebugUIConstants.REGISTER_GROUP));
	}

	@Override
	protected String getDetailPanePreferenceKey() {
		return IDebugPreferenceConstants.REGISTERS_DETAIL_PANE_ORIENTATION;
	}

	@Override
	protected String getToggleActionLabel() {
		return RegistersViewMessages.RegistersView_0;
	}

	@Override
	protected String getPresentationContextId() {
		return IDebugUIConstants.ID_REGISTER_VIEW;
	}

}
