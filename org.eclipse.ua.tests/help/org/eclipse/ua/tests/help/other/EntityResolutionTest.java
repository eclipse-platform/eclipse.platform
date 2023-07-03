/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ua.tests.help.other;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.Reader;

import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.junit.Test;
import org.xml.sax.InputSource;

public class EntityResolutionTest {

	public void resolve(String systemId, boolean isSupportedDtd) throws Exception {

		LocalEntityResolver resolver = new LocalEntityResolver();
		InputSource is = resolver.resolveEntity("publicId", systemId);
		try (Reader reader = is.getCharacterStream(); InputStream stream = is.getByteStream()) {
			int read;
			if (reader != null) {
				char[] cbuf = new char[5];
				read = reader.read(cbuf);
				reader.close();
			} else {
				byte buf[] = new byte[5];
				read = stream.read(buf);
			}
			if (isSupportedDtd) {
				assertTrue("Entity not found", read > 0);
			} else {
				assertTrue("Unsupported Entity did not return empty stream", read == -1);
			}
		}
	}

	@Test
	public void testXhtml1() throws Exception  {
		resolve("http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd", true);
	}

	@Test
	public void testFramset() throws Exception {
		resolve("http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd", true);
	}

	@Test
	public void testFlat() throws Exception {
		resolve("http://www.w3.org/TR/xhtml11/DTD/xhtml11-flat.dtd", true);
	}

	@Test
	public void testStrict() throws Exception {
		resolve("http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd", true);
	}

	@Test
	public void testTransitional() throws Exception {
		resolve("http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd", true);
	}

	@Test
	public void testLat1() throws Exception {
		resolve("http://www.w3.org/TR/xhtml1/DTD/xhtml-lat1.ent", true);
	}

	@Test
	public void testSpecial() throws Exception {
		resolve("http://www.w3.org/TR/xhtml1/DTD/xhtml-special.ent", true);
	}

	@Test
	public void testResolveSymbol() throws Exception {
		resolve("http://www.w3.org/TR/xhtml1/DTD/xhtml-symbol.ent", true);
	}

	@Test
	public void testUnsupportedDtds() throws Exception {
		resolve("xyz", false);
		resolve("", false);
		resolve("http://www.w3.org/TR/xhtml2/DTD/xhtml2-transitional.dtd", false);
	}

}
