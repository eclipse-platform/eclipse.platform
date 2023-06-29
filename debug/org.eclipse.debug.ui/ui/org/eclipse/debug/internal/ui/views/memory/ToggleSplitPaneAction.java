/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.memory;

import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * Toggles the visibility of the rendering view pane 2.
 *
 * @since 3.1
 *
 */
public class ToggleSplitPaneAction extends ToggleViewPaneAction {

	@Override
	public String getPaneId() {
		return IDebugUIConstants.ID_RENDERING_VIEW_PANE_2;
	}

}
