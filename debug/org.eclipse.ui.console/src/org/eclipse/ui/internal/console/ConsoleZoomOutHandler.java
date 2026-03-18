/*******************************************************************************
 * Copyright (c) 2026 Advantest Europe GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 				Raghunandana Murthappa
 *******************************************************************************/
package org.eclipse.ui.internal.console;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Display;

/**
 * Command handler to decrease the font size of the focused console StyledText.
 *
 * @since 3.17
 */
public class ConsoleZoomOutHandler extends AbstractHandler {
	private static final int STEP = 1;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		changeFocusedFont(-STEP);
		return null;
	}

	private void changeFocusedFont(int delta) {
		Display display = Display.getCurrent();
		if (display == null) {
			return;
		}
		display.syncExec(() -> ConsoleZoomInHandler.applyZoom(display, delta));
	}
}
