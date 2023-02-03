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
package org.eclipse.tips.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.tips.ui.internal.util.ImageUtil;
import org.junit.Test;

public class ImageUtilTest {

	private static String fImageBase64 = """
			iVBORw0KGgoAAAANSUhEUgAAACIAAAAkCAYAAADsHujfAAAAAXNSR0IArs4c6QAAAARnQU1BAACx
			jwv8YQUAAAAJcEhZcwAAJOgAACToAYJjBRwAAAEcSURBVFhH7dZBCsIwEIXh3MkbuPJubtwI0kOIK
			IJuddseo+eIPORBDZM0MyPaRQr/IiRtP9osEoa+j0uoQdKykPVtiLunPKdtcx/i9iHPMRECRDi+82K
			A4LNKGBHCG5kVM0UgjKV1SITgxdMHIC1Gg0DZPeLBaBEoC0EWjAWBihCkwVgRaBaCajAeBKqCoBLGi0
			DVECRhVtfPsQWBVBAkYZgVgdQQlPsy0traTJB0TzDuGUtqSA7BrBgVJEVgLP0mC6YaIiE49w1MFaSEYF
			7MLKQGwTyYIkSDYFZMFmJBMAtGhHgQTIsRIdObLQiWYnAWltYhEYJDrhfBiCkhUHGz/rIGSWuQtAZJC+M
			4xn93Ol9iiAu49oduKZAuvgC5R8NSTiN3qAAAAABJRU5ErkJggg==""";

	private static String fImageBase64HTML = """
			data:image/png;base64,
			iVBORw0KGgoAAAANSUhEUgAAACIAAAAkCAYAAADsHujfAAAAAXNSR0IArs4c6QAAAARnQU1BAACx
			jwv8YQUAAAAJcEhZcwAAJOgAACToAYJjBRwAAAEcSURBVFhH7dZBCsIwEIXh3MkbuPJubtwI0kOIK
			IJuddseo+eIPORBDZM0MyPaRQr/IiRtP9osEoa+j0uoQdKykPVtiLunPKdtcx/i9iHPMRECRDi+82K
			A4LNKGBHCG5kVM0UgjKV1SITgxdMHIC1Gg0DZPeLBaBEoC0EWjAWBihCkwVgRaBaCajAeBKqCoBLGi0
			DVECRhVtfPsQWBVBAkYZgVgdQQlPsy0traTJB0TzDuGUtqSA7BrBgVJEVgLP0mC6YaIiE49w1MFaSEYF
			7MLKQGwTyYIkSDYFZMFmJBMAtGhHgQTIsRIdObLQiWYnAWltYhEYJDrhfBiCkhUHGz/rIGSWuQtAZJC+M
			4xn93Ol9iiAu49oduKZAuvgC5R8NSTiN3qAAAAABJRU5ErkJggg==""";

	@Test
	public void decodeImageToBase64Test() throws IOException {
		Image image = new Image(null, ImageUtil.decodeToImage(fImageBase64));
		String base64Image = ImageUtil.decodeFromImage(image, SWT.IMAGE_PNG);
		image.dispose();
		image = new Image(null, ImageUtil.decodeToImage(base64Image));
		String base64Image2 = ImageUtil.decodeFromImage(image, SWT.IMAGE_PNG);
		assertTrue(base64Image, base64Image.equals(base64Image2));
		image.dispose();

		Image image2 = new Image(null, ImageUtil.decodeToImage(fImageBase64HTML));
		String base64Image3 = ImageUtil.decodeFromImage(image2, SWT.IMAGE_PNG);
		image2.dispose();
		image2 = new Image(null, ImageUtil.decodeToImage(base64Image3));
		String base64Image4 = ImageUtil.decodeFromImage(image2, SWT.IMAGE_PNG);
		assertTrue(base64Image3, base64Image3.equals(base64Image4));
		image2.dispose();
	}

	@Test
	public void testGetWidth() {
		assertEquals(100, ImageUtil.getWidth(1, 1000, 100));
		assertEquals(100, ImageUtil.getWidth(1, 100, 1000));
		assertEquals(99, ImageUtil.getWidth(1, 100, 99));
		assertEquals(98, ImageUtil.getWidth(1, 100, 98));
		assertEquals(100, ImageUtil.getWidth(1, 100, 100));
		assertEquals(99, ImageUtil.getWidth(1, 99, 100));
		assertEquals(77, ImageUtil.getWidth(1, 77, 77));
		assertEquals(100, ImageUtil.getWidth(1, 101, 100));
		assertEquals(100, ImageUtil.getWidth(1, 149, 100));
		assertEquals(200, ImageUtil.getWidth(1, 200, 300));
		assertEquals(10, ImageUtil.getWidth(1, 11, 10));

		assertEquals(50, ImageUtil.getWidth(0.5, 1000, 100));
		assertEquals(100, ImageUtil.getWidth(0.5, 100, 1000));
		assertEquals(49, ImageUtil.getWidth(0.5, 100, 99));
		assertEquals(49, ImageUtil.getWidth(0.5, 100, 98));
		assertEquals(50, ImageUtil.getWidth(0.5, 100, 100));
		assertEquals(50, ImageUtil.getWidth(0.5, 99, 100));
		assertEquals(38, ImageUtil.getWidth(0.5, 77, 77));
		assertEquals(50, ImageUtil.getWidth(0.5, 101, 100));
		assertEquals(50, ImageUtil.getWidth(0.5, 149, 100));
		assertEquals(150, ImageUtil.getWidth(0.5, 200, 300));
		assertEquals(5, ImageUtil.getWidth(0.5, 11, 10));

		assertEquals(200, ImageUtil.getWidth(2, 1000, 100));
		assertEquals(100, ImageUtil.getWidth(2, 100, 1000));
		assertEquals(100, ImageUtil.getWidth(2, 100, 99));
		assertEquals(100, ImageUtil.getWidth(2, 100, 50));
		assertEquals(98, ImageUtil.getWidth(2, 100, 49));
		assertEquals(100, ImageUtil.getWidth(2, 100, 98));
		assertEquals(100, ImageUtil.getWidth(2, 100, 100));
		assertEquals(99, ImageUtil.getWidth(2, 99, 100));
		assertEquals(77, ImageUtil.getWidth(2, 77, 77));
		assertEquals(101, ImageUtil.getWidth(2, 101, 100));
		assertEquals(149, ImageUtil.getWidth(2, 149, 100));
		assertEquals(200, ImageUtil.getWidth(2, 200, 300));
		assertEquals(11, ImageUtil.getWidth(2, 11, 10));
	}

	@Test
	public void testGetHeight() {
		assertEquals(100, ImageUtil.getHeight(1, 1000, 100));
		assertEquals(100, ImageUtil.getHeight(1, 100, 1000));
		assertEquals(99, ImageUtil.getHeight(1, 100, 99));
		assertEquals(98, ImageUtil.getHeight(1, 100, 98));
		assertEquals(100, ImageUtil.getHeight(1, 100, 100));
		assertEquals(99, ImageUtil.getHeight(1, 99, 100));
		assertEquals(77, ImageUtil.getHeight(1, 77, 77));
		assertEquals(100, ImageUtil.getHeight(1, 101, 100));
		assertEquals(100, ImageUtil.getHeight(1, 149, 100));
		assertEquals(200, ImageUtil.getHeight(1, 200, 300));
		assertEquals(10, ImageUtil.getHeight(1, 11, 10));

		assertEquals(100, ImageUtil.getHeight(0.5, 1000, 100));
		assertEquals(200, ImageUtil.getHeight(0.5, 100, 1000));
		assertEquals(99, ImageUtil.getHeight(0.5, 100, 99));
		assertEquals(98, ImageUtil.getHeight(0.5, 100, 98));
		assertEquals(100, ImageUtil.getHeight(0.5, 100, 100));
		assertEquals(100, ImageUtil.getHeight(0.5, 99, 100) );
		assertEquals(77, ImageUtil.getHeight(0.5, 77, 77));
		assertEquals(100, ImageUtil.getHeight(0.5, 101, 100));
		assertEquals(100, ImageUtil.getHeight(0.5, 149, 100));
		assertEquals(300, ImageUtil.getHeight(0.5, 200, 300));
		assertEquals(10, ImageUtil.getHeight(0.5, 11, 10));

		assertEquals(100, ImageUtil.getHeight(2, 1000, 100));
		assertEquals(50, ImageUtil.getHeight(2, 100, 1000));
		assertEquals(50, ImageUtil.getHeight(2, 100, 99));
		assertEquals(50, ImageUtil.getHeight(2, 100, 50));
		assertEquals(49, ImageUtil.getHeight(2, 100, 49));
		assertEquals(50, ImageUtil.getHeight(2, 100, 98));
		assertEquals(50, ImageUtil.getHeight(2, 100, 100));
		assertEquals(49, ImageUtil.getHeight(2, 99, 100));
		assertEquals(38, ImageUtil.getHeight(2, 77, 77));
		assertEquals(50, ImageUtil.getHeight(2, 101, 100));
		assertEquals(74, ImageUtil.getHeight(2, 149, 100));
		assertEquals(100, ImageUtil.getHeight(2, 200, 300));
		assertEquals(5, ImageUtil.getHeight(2, 11, 10));
	}
}
