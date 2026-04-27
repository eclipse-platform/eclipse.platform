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
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.TextConsolePage;

/**
 * Command handler to increase the font size of the focused console StyledText.
 *
 * @since 3.17
 */
public class ConsoleZoomInHandler extends AbstractHandler {
	private static final String ZOOM_FONT_KEY = TextConsolePage.class.getName() + ".zoomFont"; //$NON-NLS-1$
	private static final String DEBUG_CONSOLE_FONT_REGISTRY_KEY = "org.eclipse.debug.ui.consoleFont"; //$NON-NLS-1$
	private static final int MIN_FONT_SIZE = 6;
	private static final int MAX_FONT_SIZE = 72;
	private static final int STEP = 1;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		changeFocusedFont(STEP);
		return null;
	}

	private void changeFocusedFont(int delta) {
		Display display = Display.getCurrent();
		if (display == null) {
			return;
		}
		display.syncExec(() -> applyZoom(display, delta));
	}

	public static void applyZoom(Display display, int delta) {
		// Capture the focused control.
		Control focus = display.getFocusControl();
		StyledText st = focus instanceof StyledText ? (StyledText) focus : null;
		if (st == null || st.isDisposed()) {
			return;
		}
		Font current = st.getFont();
		if (current == null || current.isDisposed()) {
			return;
		}
		FontData[] fontData = current.getFontData();
		if (fontData == null || fontData.length == 0) {
			return;
		}
		int currentHeight = fontData[0].getHeight();
		int newHeight = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, currentHeight + delta));
		if (newHeight == currentHeight) {
			return;
		}
		FontData[] newFontData = fontData.clone();
		for (FontData fd : newFontData) {
			if (fd != null)
				fd.setHeight(newHeight);
		}

		Font oldZoom = (Font) st.getData(ZOOM_FONT_KEY);
		Font newZoom = new Font(st.getDisplay(), newFontData);
		st.setFont(newZoom);
		st.setData(ZOOM_FONT_KEY, newZoom);
		if (oldZoom == null) {
			st.addDisposeListener(e -> {
				Font z = (Font) st.getData(ZOOM_FONT_KEY);
				if (z != null && !z.isDisposed())
					z.dispose();
			});
		}
		if (oldZoom != null && !oldZoom.isDisposed()) {
			oldZoom.dispose();
		}

		// Update shared JFace registry so other listeners can observe the change
		JFaceResources.getFontRegistry().put(DEBUG_CONSOLE_FONT_REGISTRY_KEY, newFontData);
	}
}
