/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * Generic color manager.
 */
public class ColorManager implements ISharedTextColors {

	private static ColorManager fgColorManager;

	private ColorManager() {
	}

	public static ColorManager getDefault() {
		if (fgColorManager == null) {
			fgColorManager = new ColorManager();
		}
		return fgColorManager;
	}

	protected Map<RGB, Color> fColorTable = new HashMap<>(10);

	@Override
	public Color getColor(RGB rgb) {
		Color color = fColorTable.get(rgb);
		if (color == null) {
			synchronized (fColorTable) {
				color = fColorTable.get(rgb);
				if (color == null) {
					PlatformUI.getWorkbench().getDisplay().syncExec(() -> fColorTable.put(rgb, new Color(Display.getCurrent(), rgb)));
					color = fColorTable.get(rgb);
				}
			}
		}
		return color;
	}

}
