/*******************************************************************************
 * Copyright (c) 2019 Paul Pazderski and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Paul Pazderski - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.TextConsoleViewer;
import org.junit.Test;

/**
 * Not really a test for {@link TextConsoleViewer} yet since it only test one
 * private method of it.
 */
public class TextConsoleViewerTest extends AbstractDebugTest {

	/**
	 * Test override of existing styles with a new style. Typically used to
	 * apply link styling on already styled console document.
	 */
	@Test
	public void testStyleOverride() throws Throwable {
		Color colorR = null;
		Color colorG = null;
		Color colorB = null;
		Color colorK = null;
		Color colorW = null;
		try {
			final Method method = TextConsoleViewer.class.getDeclaredMethod("overrideStyleRange", List.class, StyleRange.class);
			method.setAccessible(true);
			assertTrue("Required method <" + method + "> is not static.", Modifier.isStatic(method.getModifiers()));

			final List<StyleRange> styles = new ArrayList<>();
			colorR = new Color(null, 255, 0, 0);
			colorG = new Color(null, 0, 255, 0);
			colorB = new Color(null, 0, 0, 255);
			colorK = new Color(null, 0, 0, 0);
			colorW = new Color(null, 255, 255, 255);

			// overwrite in empty list
			method.invoke(null, styles, new StyleRange(5, 5, colorR, null));
			checkOverlapping(styles);
			assertStyle(styles, 2, 5, null);
			assertStyle(styles, 5, 10, colorR);
			assertStyle(styles, 10, 13, null);

			// overwrite unstyled part before
			method.invoke(null, styles, new StyleRange(0, 3, colorG, null));
			checkOverlapping(styles);
			assertStyle(styles, 0, 3, colorG);
			assertStyle(styles, 3, 5, null);
			assertStyle(styles, 5, 10, colorR);
			assertStyle(styles, 10, 13, null);

			// overwrite unstyled part after
			method.invoke(null, styles, new StyleRange(15, 5, colorB, null));
			checkOverlapping(styles);
			assertStyle(styles, 0, 3, colorG);
			assertStyle(styles, 3, 5, null);
			assertStyle(styles, 5, 10, colorR);
			assertStyle(styles, 10, 15, null);
			assertStyle(styles, 15, 20, colorB);
			assertStyle(styles, 20, 23, null);

			// overwrite existing: start exact, end exact
			method.invoke(null, styles, new StyleRange(0, 3, colorK, null));
			checkOverlapping(styles);
			assertStyle(styles, 0, 3, colorK);
			assertStyle(styles, 3, 5, null);
			assertStyle(styles, 5, 10, colorR);
			assertStyle(styles, 10, 15, null);
			assertStyle(styles, 15, 20, colorB);
			assertStyle(styles, 20, 23, null);

			// overwrite existing: start exact, end after
			method.invoke(null, styles, new StyleRange(0, 4, colorW, null));
			checkOverlapping(styles);
			assertStyle(styles, 0, 4, colorW);
			assertStyle(styles, 4, 5, null);
			assertStyle(styles, 5, 10, colorR);
			assertStyle(styles, 10, 15, null);
			assertStyle(styles, 15, 20, colorB);
			assertStyle(styles, 20, 23, null);

			// overwrite existing: start exact, end inside
			method.invoke(null, styles, new StyleRange(0, 2, colorK, null));
			checkOverlapping(styles);
			assertStyle(styles, 0, 2, colorK);
			assertStyle(styles, 2, 4, colorW);
			assertStyle(styles, 4, 5, null);
			assertStyle(styles, 5, 10, colorR);
			assertStyle(styles, 10, 15, null);
			assertStyle(styles, 15, 20, colorB);
			assertStyle(styles, 20, 23, null);

			// overwrite existing: start before, end exact
			method.invoke(null, styles, new StyleRange(13, 7, colorW, null));
			checkOverlapping(styles);
			assertStyle(styles, 0, 2, colorK);
			assertStyle(styles, 2, 4, colorW);
			assertStyle(styles, 4, 5, null);
			assertStyle(styles, 5, 10, colorR);
			assertStyle(styles, 10, 13, null);
			assertStyle(styles, 13, 20, colorW);
			assertStyle(styles, 20, 23, null);

			// overwrite existing: start inside, end exact
			method.invoke(null, styles, new StyleRange(15, 5, colorK, null));
			checkOverlapping(styles);
			assertStyle(styles, 0, 2, colorK);
			assertStyle(styles, 2, 4, colorW);
			assertStyle(styles, 4, 5, null);
			assertStyle(styles, 5, 10, colorR);
			assertStyle(styles, 10, 13, null);
			assertStyle(styles, 13, 15, colorW);
			assertStyle(styles, 15, 20, colorK);
			assertStyle(styles, 20, 23, null);

			// prepare new existing style
			styles.clear();
			method.invoke(null, styles, new StyleRange(10, 10, colorW, null));
			assertStyle(styles, 10, 20, colorW);
			method.invoke(null, styles, new StyleRange(10, 10, colorK, null));
			assertStyle(styles, 10, 20, colorK);
			assertEquals("Wrong number of styles.", 1, styles.size());

			// overwrite existing: start before, end after
			method.invoke(null, styles, new StyleRange(5, 15, colorR, null));
			checkOverlapping(styles);
			assertStyle(styles, 5, 20, colorR);

			// overwrite existing: start before, end inside
			method.invoke(null, styles, new StyleRange(0, 10, colorG, null));
			checkOverlapping(styles);
			assertStyle(styles, 0, 10, colorG);
			assertStyle(styles, 10, 20, colorR);

			// overwrite existing: start inside, end after
			method.invoke(null, styles, new StyleRange(15, 10, colorB, null));
			checkOverlapping(styles);
			assertStyle(styles, 0, 10, colorG);
			assertStyle(styles, 10, 15, colorR);
			assertStyle(styles, 15, 20, colorB);

			// overwrite existing: start inside, end inside
			method.invoke(null, styles, new StyleRange(6, 1, colorW, null));
			checkOverlapping(styles);
			assertStyle(styles, 0, 6, colorG);
			assertStyle(styles, 6, 7, colorW);
			assertStyle(styles, 7, 10, colorG);
			assertStyle(styles, 10, 15, colorR);
			assertStyle(styles, 15, 20, colorB);

			// overwrite many styles
			method.invoke(null, styles, new StyleRange(0, 25, colorK, null));
			checkOverlapping(styles);
			assertStyle(styles, 0, 25, colorK);
			assertStyle(styles, 25, 30, null);

			// prepare new existing style
			styles.clear();
			styles.add(new StyleRange(0, 10, colorR, null));
			styles.add(new StyleRange(15, 5, colorG, null));

			// overwrite: start in one style, end in other
			method.invoke(null, styles, new StyleRange(7, 11, colorB, null));
			checkOverlapping(styles);
			assertStyle(styles, 0, 7, colorR);
			assertStyle(styles, 7, 18, colorB);
			assertStyle(styles, 18, 20, colorG);
			assertStyle(styles, 20, 25, null);

			// prepare new existing style
			styles.clear();
			styles.add(new StyleRange(0, 10, colorR, null));
			styles.add(new StyleRange(15, 5, colorG, null));

			// overwrite: start in one style, end after other
			method.invoke(null, styles, new StyleRange(7, 15, colorB, null));
			checkOverlapping(styles);
			assertStyle(styles, 0, 7, colorR);
			assertStyle(styles, 7, 22, colorB);
			assertStyle(styles, 22, 25, null);

			// prepare new existing style
			styles.clear();
			styles.add(new StyleRange(5, 5, colorR, null));
			styles.add(new StyleRange(15, 5, colorG, null));

			// overwrite: start before one style, end in other
			method.invoke(null, styles, new StyleRange(2, 15, colorB, null));
			checkOverlapping(styles);
			assertStyle(styles, 0, 2, null);
			assertStyle(styles, 2, 17, colorB);
			assertStyle(styles, 17, 20, colorG);
			assertStyle(styles, 20, 25, null);

		} catch (InvocationTargetException e) {
			if (e.getTargetException() != null) {
				throw e.getTargetException();
			}
			throw e;
		} catch (Exception e) {
			// if this happened the method may have be renamed or moved
			throw e;
		}
	}

	/**
	 * Assert any offset in given range is styled with given foreground color.
	 * <p>
	 * Note: this test class only uses foreground color and assumes all other
	 * style attributes are set to there default values.
	 * </p>
	 *
	 * @param styles list of known style ranges
	 * @param offset inclusive start offset of range to check
	 * @param end exclusive end offset of range to check
	 * @param foregroundColor the expected foreground color for styles in given
	 *            range. May be <code>null</code> to check for unstyled ranges.
	 */
	private static void assertStyle(List<StyleRange> styles, int offset, int end, Color foregroundColor) {
		int o = offset;
		while (o < end) {
			final StyleRange expected = foregroundColor != null ? new StyleRange(0, 0, foregroundColor, null) : null;
			final StyleRange actual = getStyleAtOffset(styles, o);
			assertEquals("Got wrong style at offset " + o, generalizeStyle(expected), generalizeStyle(actual));
			final int step = actual != null ? actual.length : 1;
			o += Math.min(step, 1);
		}
	}

	/**
	 * Get style from list at given offset or <code>null</code>. If offset has
	 * more then one style it is undefined which one is returned. Does not
	 * return zero-length styles.
	 *
	 * @param styles list of styles
	 * @param offset offset of interest
	 * @return style at given offset or <code>null</code> if offset is not
	 *         styled
	 */
	private static StyleRange getStyleAtOffset(List<StyleRange> styles, int offset) {
		if (styles != null) {
			for (StyleRange style : styles) {
				if (style.start <= offset && style.start + style.length >= offset + 1) {
					return style;
				}
			}
		}
		return null;
	}

	/**
	 * Set styles start and length to <code>0</code> to compare only the styling
	 * parts using <code>equals</code>.
	 *
	 * @param style the original style. Not modified by this method.
	 * @return the style without position information.
	 */
	private static StyleRange generalizeStyle(StyleRange style) {
		if (style == null) {
			return new StyleRange();
		}
		final StyleRange copy = (StyleRange) style.clone();
		copy.start = copy.length = 0;
		return copy;
	}

	/**
	 * Check if styles are disjoint and sorted ascending by offset.
	 *
	 * @param styles the styles to check
	 */
	private static void checkOverlapping(List<StyleRange> styles) {
		if (styles == null || styles.size() <= 1) {
			return;
		}
		int lastEnd = Integer.MIN_VALUE;
		for (StyleRange s : styles) {
			assertTrue("Styles overlap or not sorted.", lastEnd <= s.start);
			assertTrue("Empty style.", s.length > 0);
			lastEnd = s.start + s.length;
		}
	}
}
