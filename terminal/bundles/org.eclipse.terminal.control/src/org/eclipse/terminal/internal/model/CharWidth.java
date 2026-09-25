/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.internal.model;

/**
 * Display width of a code point, following Unicode Standard Annex #11
 * (East Asian Width). Used by the emulator to keep its column arithmetic in
 * step with what a terminal application assumes.
 * <p>
 * East Asian Wide (W) and Fullwidth (F) count as two columns. Ambiguous (A) is
 * treated as narrow, as UAX #11 recommends for a context with no East Asian
 * legacy encoding. Combining marks and non-printing characters count as zero.
 */
public final class CharWidth {

	private CharWidth() {
	}

	/**
	 * Wide and Fullwidth ranges from EastAsianWidth-17.0.0.txt, as inclusive
	 * [start, end] pairs in ascending order. Everything not listed here defaults
	 * to Narrow, which is what the file's {@code @missing} line specifies.
	 */
	private static final int[] WIDE_RANGES = {
			0x01100, 0x0115F, 0x0231A, 0x0231B, 0x02329, 0x0232A, 0x023E9, 0x023EC,
			0x023F0, 0x023F0, 0x023F3, 0x023F3, 0x025FD, 0x025FE, 0x02614, 0x02615,
			0x02630, 0x02637, 0x02648, 0x02653, 0x0267F, 0x0267F, 0x0268A, 0x0268F,
			0x02693, 0x02693, 0x026A1, 0x026A1, 0x026AA, 0x026AB, 0x026BD, 0x026BE,
			0x026C4, 0x026C5, 0x026CE, 0x026CE, 0x026D4, 0x026D4, 0x026EA, 0x026EA,
			0x026F2, 0x026F3, 0x026F5, 0x026F5, 0x026FA, 0x026FA, 0x026FD, 0x026FD,
			0x02705, 0x02705, 0x0270A, 0x0270B, 0x02728, 0x02728, 0x0274C, 0x0274C,
			0x0274E, 0x0274E, 0x02753, 0x02755, 0x02757, 0x02757, 0x02795, 0x02797,
			0x027B0, 0x027B0, 0x027BF, 0x027BF, 0x02B1B, 0x02B1C, 0x02B50, 0x02B50,
			0x02B55, 0x02B55, 0x02E80, 0x02E99, 0x02E9B, 0x02EF3, 0x02F00, 0x02FD5,
			0x02FF0, 0x0303E, 0x03041, 0x03096, 0x03099, 0x030FF, 0x03105, 0x0312F,
			0x03131, 0x0318E, 0x03190, 0x031E5, 0x031EF, 0x0321E, 0x03220, 0x03247,
			0x03250, 0x0A48C, 0x0A490, 0x0A4C6, 0x0A960, 0x0A97C, 0x0AC00, 0x0D7A3,
			0x0F900, 0x0FAFF, 0x0FE10, 0x0FE19, 0x0FE30, 0x0FE52, 0x0FE54, 0x0FE66,
			0x0FE68, 0x0FE6B, 0x0FF01, 0x0FF60, 0x0FFE0, 0x0FFE6, 0x16FE0, 0x16FE4,
			0x16FF0, 0x16FF6, 0x17000, 0x18CD5, 0x18CFF, 0x18D1E, 0x18D80, 0x18DF2,
			0x1AFF0, 0x1AFF3, 0x1AFF5, 0x1AFFB, 0x1AFFD, 0x1AFFE, 0x1B000, 0x1B122,
			0x1B132, 0x1B132, 0x1B150, 0x1B152, 0x1B155, 0x1B155, 0x1B164, 0x1B167,
			0x1B170, 0x1B2FB, 0x1D300, 0x1D356, 0x1D360, 0x1D376, 0x1F004, 0x1F004,
			0x1F0CF, 0x1F0CF, 0x1F18E, 0x1F18E, 0x1F191, 0x1F19A, 0x1F200, 0x1F202,
			0x1F210, 0x1F23B, 0x1F240, 0x1F248, 0x1F250, 0x1F251, 0x1F260, 0x1F265,
			0x1F300, 0x1F320, 0x1F32D, 0x1F335, 0x1F337, 0x1F37C, 0x1F37E, 0x1F393,
			0x1F3A0, 0x1F3CA, 0x1F3CF, 0x1F3D3, 0x1F3E0, 0x1F3F0, 0x1F3F4, 0x1F3F4,
			0x1F3F8, 0x1F43E, 0x1F440, 0x1F440, 0x1F442, 0x1F4FC, 0x1F4FF, 0x1F53D,
			0x1F54B, 0x1F54E, 0x1F550, 0x1F567, 0x1F57A, 0x1F57A, 0x1F595, 0x1F596,
			0x1F5A4, 0x1F5A4, 0x1F5FB, 0x1F64F, 0x1F680, 0x1F6C5, 0x1F6CC, 0x1F6CC,
			0x1F6D0, 0x1F6D2, 0x1F6D5, 0x1F6D8, 0x1F6DC, 0x1F6DF, 0x1F6EB, 0x1F6EC,
			0x1F6F4, 0x1F6FC, 0x1F7E0, 0x1F7EB, 0x1F7F0, 0x1F7F0, 0x1F90C, 0x1F93A,
			0x1F93C, 0x1F945, 0x1F947, 0x1F9FF, 0x1FA70, 0x1FA7C, 0x1FA80, 0x1FA8A,
			0x1FA8E, 0x1FAC6, 0x1FAC8, 0x1FAC8, 0x1FACD, 0x1FADC, 0x1FADF, 0x1FAEA,
			0x1FAEF, 0x1FAF8, 0x20000, 0x2FFFD, 0x30000, 0x3FFFD
	};

	/** @return 0 for combining and non-printing, 2 for East Asian W/F, else 1 */
	public static int of(int codePoint) {
		if (codePoint < 0x0080) {
			return codePoint < 0x20 || codePoint == 0x7F ? 0 : 1;
		}
		return isZeroWidth(codePoint) ? 0 : isWide(codePoint) ? 2 : 1;
	}

	/** @return the total display width of {@code s} */
	public static int ofString(String s) {
		return s.codePoints().map(CharWidth::of).sum();
	}

	/**
	 * A {@code '\000'} means one of two things in a line of cells: the filler that
	 * a wide character puts in the cell it also covers, which carries no text of
	 * its own, or a cell that was never written or has been erased, which reads as
	 * a space.
	 *
	 * @return whether the cell at {@code index} is the filler of the character
	 *         before it
	 */
	public static boolean isFiller(CharSequence text, int index) {
		return text.charAt(index) == '\000' && index > 0 && of(Character.codePointBefore(text, index)) == 2;
	}

	private static boolean isZeroWidth(int codePoint) {
		// Hangul conjoining jamo vowels and trailing consonants: EAW lists them as
		// neutral, but they combine into the leading consonant before them.
		if (codePoint >= 0x1160 && codePoint <= 0x11FF) {
			return true;
		}
		switch (Character.getType(codePoint)) {
		case Character.NON_SPACING_MARK:
		case Character.ENCLOSING_MARK:
		case Character.CONTROL:
		case Character.FORMAT:
			return true;
		default:
			return false;
		}
	}

	private static boolean isWide(int codePoint) {
		int lo = 0, hi = WIDE_RANGES.length / 2 - 1;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1, i = mid * 2;
			if (codePoint < WIDE_RANGES[i]) {
				hi = mid - 1;
			} else if (codePoint > WIDE_RANGES[i + 1])
				lo = mid + 1;
			else {
				return true;
			}
		}
		return false;
	}
}
