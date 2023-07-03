/*****************************************************************
 * Copyright (c) 2009, 2013 Texas Instruments and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Chuong (Texas Instruments) - Initial API and implementation (Bug 286310)
 *     IBM Corporation - ongoing maintenance and enhancements
 *****************************************************************/
package org.eclipse.debug.examples.ui.pda.views;

import org.eclipse.debug.internal.ui.views.variables.VariablesView;
import org.eclipse.swt.SWT;

public class CheckboxView extends VariablesView {
	public static String ID = "CHECKBOX_VIEW_ID"; //$NON-NLS-1$

	@Override
	protected int getViewerStyle() {
		return SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.CHECK;
	}

	@Override
	protected String getHelpContextId() {
		return ID;
	}

	@Override
	protected String getPresentationContextId() {
		return ID;
	}
}
