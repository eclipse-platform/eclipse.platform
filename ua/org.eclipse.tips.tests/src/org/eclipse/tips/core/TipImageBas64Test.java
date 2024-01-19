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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

import org.eclipse.core.runtime.AssertionFailedException;
import org.junit.Test;

public class TipImageBas64Test {

	private static final String BASE64 = "data:image/png;base64,thequickbrownfox";
	private static final String BASE64WRONG = "date:image/png;base64,thequickbrownfox";
	private static final String BASE64WRONG2 = "data:image/plip ;base64,thequickbrownfox";

	private TipImage getTipImage() {
		return new TipImage(BASE64);
	}

	@Test
	public void testAssertHeight() {
		assertThrows(AssertionFailedException.class, () -> new TipImage(BASE64).setAspectRatio(1000, 0, false));
	}

	@Test
	public void testAssertWidth() {
		assertThrows(AssertionFailedException.class, () -> new TipImage(BASE64).setAspectRatio(0, 100, false));
	}

	@Test
	public void testSetExtension() {
		assertThat(getTipImage().getBase64Image()).contains("png");
	}

	@Test
	public void testSetExtension2() {
		assertThat(getTipImage().setExtension("bmp").getBase64Image()).contains("bmp");
	}

	@Test
	public void testGetIMGAttributes() {
		String result = getTipImage().setAspectRatio(1.5).getIMGAttributes(740, 370).trim();
		assertThat(result).isEqualToIgnoringCase("width=\"555\" height=\"370\"");
	}

	@Test
	public void testGetBase64() {
		assertThat(getTipImage().getBase64Image()).isEqualTo(BASE64);
	}

	@Test
	public void testSetAspectRatioDouble() {
		String result = getTipImage().setAspectRatio(1.5).getIMGAttributes(740, 370).trim();
		assertThat(result).isEqualToIgnoringCase("width=\"555\" height=\"370\"");
	}

	@Test
	public void testSetAspectRatioIntIntFalse() {
		String result = getTipImage().setAspectRatio(200, 50, false).getIMGAttributes(100, 100).trim();
		assertThat(result).isEqualToIgnoringCase("width=\"100\" height=\"25\"");
	}

	@Test
	public void testSetAspectRatioIntIntTrue() {
		String result = getTipImage().setAspectRatio(400, 300, true).getIMGAttributes(740, 370).trim();
		assertThat(result).isEqualToIgnoringCase("width=\"400\" height=\"300\"");
	}

	@Test
	public void testSetMaxHeight() {
		String imgAttributes = new TipImage(BASE64).setAspectRatio(2).setMaxHeight(300).getIMGAttributes(200, 200);
		assertThat(imgAttributes.trim()).isEqualToIgnoringCase("width=\"200\" height=\"100\"");
	}

	@Test
	public void testSetMaxWidth() {
		String imgAttributes = new TipImage(BASE64).setAspectRatio(1.6).setMaxWidth(200).getIMGAttributes(400, 300);
		assertThat(imgAttributes.trim()).isEqualToIgnoringCase("width=\"200\" height=\"125\"");
	}

	@Test
	public void testTipImage() {
		new TipImage(BASE64);
	}

	@Test
	public void testTipImage2() {
		getTipImage();
	}

	@Test
	public void testTipImage3() {
		assertThrows(RuntimeException.class, () -> new TipImage(BASE64WRONG));
	}

	public void testTipImage4() {
		TipImage tipImage = new TipImage(BASE64WRONG2);
		assertThat(tipImage.getIMGAttributes(1, 1)).contains("plip");
	}
}
