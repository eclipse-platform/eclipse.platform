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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

/**
 * Generic color manager.
 */
public class ColorManager implements ISharedTextColors {

	private static final ColorManager fgColorManager = new ColorManager();

	protected final Map<RGB, Color> fColorTable;

	private ColorManager() {
		fColorTable = new ConcurrentHashMap<>(10);
	}

	public static ColorManager getDefault() {
		return fgColorManager;
	}

	@Override
	public Color getColor(RGB rgb) {
		return fColorTable.computeIfAbsent(rgb, Color::new);
	}
}
