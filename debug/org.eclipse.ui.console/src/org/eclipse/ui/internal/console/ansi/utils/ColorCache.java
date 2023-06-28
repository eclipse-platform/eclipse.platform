/*******************************************************************************
 * Copyright (c) 2012-2022 Mihai Nita and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ui.internal.console.ansi.utils;

import java.util.HashMap;

import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;

public class ColorCache {
	private static final HashMap<RGB, Color> CACHE = new HashMap<>();

	private ColorCache() {
		// Utility class, should not be instantiated
	}

	public static Color get(RGB rgb) {
		return CACHE.computeIfAbsent(rgb, color -> new Color(null, color));
	}
}
