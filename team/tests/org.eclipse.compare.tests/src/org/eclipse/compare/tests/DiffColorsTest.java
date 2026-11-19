/*******************************************************************************
 * Copyright (c) 2026 Eclipse contributors and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse contributors - initial implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.eclipse.compare.internal.DiffColors;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.RGB;
import org.junit.jupiter.api.Test;

@SuppressWarnings("restriction")
public class DiffColorsTest {

	@Test
	public void testLightThemeKeepsExistingPalette() {
		RGB addition = new RGB(0, 128, 0);
		RGB background = new RGB(255, 255, 255);
		assertEquals(new RGB(153, 204, 153),
				DiffColors.interpolate(addition, background, DiffColors.borderScale(false)));
		assertEquals(new RGB(229, 242, 229),
				DiffColors.interpolate(addition, background, DiffColors.fillScale(false)));
		assertEquals(new RGB(204, 229, 204),
				DiffColors.interpolate(addition, background, DiffColors.detailScale(false)));
	}

	@Test
	public void testDarkPaletteMatchesAnnotationColors() throws Exception {
		String css;
		try (var stream = Platform.getBundle("org.eclipse.compare").getEntry("resources/css/dark.css").openStream()) {
			css = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
		RGB background = new RGB(30, 31, 34);
		RGB addition = new RGB(0, 128, 0);
		RGB deletion = new RGB(255, 0, 0);
		assertEquals(annotationColor(css, "inlineDiffColor"),
				DiffColors.interpolate(addition, background, DiffColors.fillScale(true)));
		assertEquals(annotationColor(css, "detailedInlineDiffColor"),
				DiffColors.interpolate(addition, background, DiffColors.detailScale(true)));
		assertEquals(annotationColor(css, "deletionInlineDiffColor"),
				DiffColors.interpolate(deletion, background, DiffColors.fillScale(true)));
		assertEquals(annotationColor(css, "detailedDeletionInlineDiffColor"),
				DiffColors.interpolate(deletion, background, DiffColors.detailScale(true)));
	}

	private static RGB annotationColor(String css, String key) {
		var matcher = Pattern.compile("'" + key + "=(\\d+),(\\d+),(\\d+)'").matcher(css);
		assertTrue(matcher.find(), "annotation preference must be present: " + key);
		return new RGB(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
				Integer.parseInt(matcher.group(3)));
	}

	@Test
	public void testCustomizedBaseColorIsPreserved() {
		RGB customized = new RGB(123, 76, 210);
		RGB background = new RGB(30, 31, 34);
		assertEquals(new RGB(76, 53, 122),
				DiffColors.interpolate(customized, background, DiffColors.detailScale(true)));
		assertEquals(new RGB(123, 76, 210), customized);
	}
}
