/*******************************************************************************
 * Copyright (c) 2018 Remain Software
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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.AssertionFailedException;
import org.junit.Test;

public class TipImageURLTest {

	private static final String URL = "http://remainsoftware.com/img.png";

	@Test(expected = Exception.class)
	public void testTipImage() throws IOException {
		new TipImage((URL) null);
	}

	@Test(expected = MalformedURLException.class)
	public void testTipImage3() throws IOException {
		new TipImage(new URL("0gl kjfslkfjsl dkfjsldkfjl"));
	}

	@Test
	public void testTipImage2() throws IOException {
		getTipImage();
	}

	private TipImage getTipImage() throws IOException {
		return new TipImage(new URL(URL));
	}

	@Test
	public void testSetMaxHeight() throws IOException {
		String imgAttributes = new TipImage(new URL(URL)).setAspectRatio(2).setMaxHeight(300).getIMGAttributes(200,
				200);
		assertTrue(imgAttributes, imgAttributes.trim().equalsIgnoreCase("width=\"200\" height=\"100\""));
	}

	@Test
	public void testSetMaxWidth() throws IOException {
		String imgAttributes = new TipImage(new URL(URL)).setAspectRatio(1.6).setMaxWidth(200).getIMGAttributes(400,
				300);
		assertTrue(imgAttributes, imgAttributes.trim().equalsIgnoreCase("width=\"200\" height=\"125\""));

	}

	@Test(expected = AssertionFailedException.class)
	public void testAssertWidth() throws IOException {
		new TipImage(new URL(URL)).setAspectRatio(0, 100, false);
	}

	@Test(expected = AssertionFailedException.class)
	public void testAssertHeight() throws IOException {
		new TipImage(new URL(URL)).setAspectRatio(1000, 0, false);
	}

	@Test
	public void testSetAspectRatioIntIntFalse() throws IOException {
		String result = getTipImage().setAspectRatio(200, 50, false).getIMGAttributes(100, 100).trim();
		assertTrue(result, result.equalsIgnoreCase("width=\"100\" height=\"25\""));
	}

	@Test
	public void testSetAspectRatioIntIntTrue() throws IOException {
		String result = getTipImage().setAspectRatio(400, 300, true).getIMGAttributes(740, 370).trim();
		assertTrue(result, result.equalsIgnoreCase("width=\"400\" height=\"300\""));
	}

	@Test
	public void testSetAspectRatioDouble() throws IOException {
		String result = getTipImage().setAspectRatio(1.5).getIMGAttributes(740, 370).trim();
		assertTrue(result, result.equalsIgnoreCase("width=\"555\" height=\"370\""));
	}

	@Test
	public void testGetIMGAttributes() throws IOException {
		String result = getTipImage().setAspectRatio(1.5).getIMGAttributes(740, 370).trim();
		assertTrue(result, result.equalsIgnoreCase("width=\"555\" height=\"370\""));
	}

	@Test
	public void testSetExtension() throws IOException {
		assertTrue(getTipImage().getBase64Image().contains("png"));
	}

	@Test
	public void testSetExtension2() throws IOException {
		assertTrue(getTipImage().setExtension("bmp").getBase64Image().contains("bmp"));
	}
}
