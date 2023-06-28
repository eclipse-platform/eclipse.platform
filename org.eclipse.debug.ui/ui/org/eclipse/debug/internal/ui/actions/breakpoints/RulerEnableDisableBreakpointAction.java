/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.breakpoints;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.ui.actions.RulerBreakpointAction;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.swt.SWT;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * @since 3.2
 *
 */
public class RulerEnableDisableBreakpointAction extends RulerBreakpointAction implements IUpdate {

	private IBreakpoint fBreakpoint;

	public RulerEnableDisableBreakpointAction(ITextEditor editor, IVerticalRulerInfo info) {
		super(editor, info);
	}

	@Override
	public void run() {
		if (fBreakpoint != null) {
			try {
				fBreakpoint.setEnabled(!fBreakpoint.isEnabled());
			} catch (CoreException e) {
				DebugUIPlugin.errorDialog(getEditor().getSite().getShell(), ActionMessages.RulerEnableDisableBreakpointAction_0, ActionMessages.RulerEnableDisableBreakpointAction_1, e.getStatus());
			}
		}
	}

	@Override
	public void update() {
		fBreakpoint = getBreakpoint();
		setEnabled(fBreakpoint != null);
		String accelerator = DebugUIPlugin.formatKeyBindingString(SWT.MOD2, ActionMessages.RulerEnableDisableBreakpointAction_4);
		if (fBreakpoint != null) {
			try {
				if (fBreakpoint.isEnabled()) {
					setText(ActionMessages.RulerEnableDisableBreakpointAction_2 + '\t' + accelerator);
				} else {
					setText(ActionMessages.RulerEnableDisableBreakpointAction_3 + '\t' + accelerator);
				}
			} catch (CoreException e) {
			}
		} else {
			setText(ActionMessages.RulerEnableDisableBreakpointAction_2 + '\t' + accelerator);
		}
	}

}
