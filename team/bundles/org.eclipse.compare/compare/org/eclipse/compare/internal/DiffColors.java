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
import org.eclipse.ui.PlatformUI;

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

	private static final String INFORMATION_BACKGROUND_COLOR = "org.eclipse.ui.workbench.INFORMATION_BACKGROUND"; //$NON-NLS-1$

	/** Returns {@code true} when the active workbench theme is dark. */
	public static boolean isDarkTheme() {
		var reg = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
		var color = reg.getRGB(INFORMATION_BACKGROUND_COLOR);
		return color != null && color.red + color.green + color.blue < 3 * 128;
	}

	/** Scale for the border/stroke color; adapts to dark vs. light themes. */
	public static double borderScale(boolean dark) {
		return dark ? 0.3 : BORDER_SCALE;
	}

	/** Scale for the line-band fill; adapts to dark vs. light themes. */
	public static double fillScale(boolean dark) {
		return dark ? 0.79 : FILL_SCALE;
	}

	/** Scale for the word-level detail highlight; adapts to dark vs. light themes. */
	public static double detailScale(boolean dark) {
		return dark ? 0.5 : TEXT_FILL_SCALE;
	}

	/** Border color of an addition/deletion hunk; adapts to dark vs. light themes. */
	public static RGB borderColor(RGB diffColor, RGB background) {
		return interpolate(diffColor, background, borderScale(isDarkTheme()));
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
