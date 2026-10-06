/*******************************************************************************
 * Copyright (c) 2026 SAP
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP - initial implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.swt.graphics.RGB;

/**
 * The shared color palette for diff rendering. Both the traditional two-way
 * {@code TextMergeViewer} and the unified diff interpolate the same diff color
 * towards the background at the same scales, so the scale factors and the
 * interpolation live here once.
 */
public final class DiffColors {

	private DiffColors() {
	}

	/** Border / {@code normal} weight: darker and more saturated than the fill. */
	public static final double BORDER_SCALE = 0.6;

	/** The detailed (word-level) diff text background. */
	public static final double TEXT_FILL_SCALE = 0.8;

	/** The hunk fill band; the lightest of the three. */
	public static final double FILL_SCALE = 0.9;

	/** Border color of an addition/deletion hunk; darker than the fill band. */
	public static RGB borderColor(RGB diffColor, RGB background) {
		return interpolate(diffColor, background, BORDER_SCALE);
	}

	/**
	 * Linearly interpolates between {@code fg} and {@code bg}: scale 0 returns
	 * {@code fg}, scale 1 returns {@code bg}.
	 */
	public static RGB interpolate(RGB fg, RGB bg, double scale) {
		if (fg != null && bg != null) {
			return new RGB((int) ((1.0 - scale) * fg.red + scale * bg.red),
					(int) ((1.0 - scale) * fg.green + scale * bg.green),
					(int) ((1.0 - scale) * fg.blue + scale * bg.blue));
		}
		if (fg != null) {
			return fg;
		}
		if (bg != null) {
			return bg;
		}
		return new RGB(128, 128, 128); // a gray
	}
}
