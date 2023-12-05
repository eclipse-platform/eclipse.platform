/*******************************************************************************
 * Copyright (c) 2018, 2023 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.runtime.AssertionFailedException;
import org.junit.Test;

public class TipImageBas64Test {

	private static final String BASE64 = "data:image/png;base64,thequickbrownfox";
	private static final String BASE64WRONG = "date:image/png;base64,thequickbrownfox";
	private static final String BASE64WRONG2 = "data:image/plip ;base64,thequickbrownfox";

	private TipImage getTipImage() {
		return new TipImage(BASE64);
	}

	@Test(expected = AssertionFailedException.class)
	public void testAssertHeight() {
		new TipImage(BASE64).setAspectRatio(1000, 0, false);
	}

	@Test(expected = AssertionFailedException.class)
	public void testAssertWidth() {
		new TipImage(BASE64).setAspectRatio(0, 100, false);
	}

	@Test
	public void testSetExtension() {
		assertTrue(getTipImage().getBase64Image().contains("png"));
	}

	@Test
	public void testSetExtension2() {
		assertTrue(getTipImage().setExtension("bmp").getBase64Image().contains("bmp"));
	}

	@Test
	public void testGetIMGAttributes() {
		String result = getTipImage().setAspectRatio(1.5).getIMGAttributes(740, 370).trim();
		assertTrue(result, result.equalsIgnoreCase("width=\"555\" height=\"370\""));
	}

	@Test
	public void testGetBase64() {
		assertEquals(BASE64, getTipImage().getBase64Image());
	}

	@Test
	public void testSetAspectRatioDouble() {
		String result = getTipImage().setAspectRatio(1.5).getIMGAttributes(740, 370).trim();
		assertTrue(result, result.equalsIgnoreCase("width=\"555\" height=\"370\""));
	}

	@Test
	public void testSetAspectRatioIntIntFalse() {
		String result = getTipImage().setAspectRatio(200, 50, false).getIMGAttributes(100, 100).trim();
		assertTrue(result, result.equalsIgnoreCase("width=\"100\" height=\"25\""));
	}

	@Test
	public void testSetAspectRatioIntIntTrue() {
		String result = getTipImage().setAspectRatio(400, 300, true).getIMGAttributes(740, 370).trim();
		assertTrue(result, result.equalsIgnoreCase("width=\"400\" height=\"300\""));
	}

	@Test
	public void testSetMaxHeight() {
		String imgAttributes = new TipImage(BASE64).setAspectRatio(2).setMaxHeight(300).getIMGAttributes(200, 200);
		assertTrue(imgAttributes, imgAttributes.trim().equalsIgnoreCase("width=\"200\" height=\"100\""));
	}

	@Test
	public void testSetMaxWidth() {
		String imgAttributes = new TipImage(BASE64).setAspectRatio(1.6).setMaxWidth(200).getIMGAttributes(400, 300);
		assertTrue(imgAttributes, imgAttributes.trim().equalsIgnoreCase("width=\"200\" height=\"125\""));
	}

	public void testTipImage() {
		new TipImage(BASE64);
	}

	@Test
	public void testTipImage2() {
		getTipImage();
	}

	@Test(expected = RuntimeException.class)
	public void testTipImage3() {
		new TipImage(BASE64WRONG);
	}

	public void testTipImage4() {
		TipImage tipImage = new TipImage(BASE64WRONG2);
		assertTrue(tipImage.getIMGAttributes(1, 1).contains("plip"));
	}
}
