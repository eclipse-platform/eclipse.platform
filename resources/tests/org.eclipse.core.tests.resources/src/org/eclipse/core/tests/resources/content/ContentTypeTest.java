/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.content;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.osgi.framework.BundleContext;

/**
 * Common superclass for all content type tests.
 */
public abstract class ContentTypeTest {
	public static final String TEST_FILES_ROOT = "Plugin_Testing/";

	public BundleContext getContext() {
		return Platform.getBundle(AutomatedResourceTests.PI_RESOURCES_TESTS).getBundleContext();
	}

	/**
	 * Return an input stream with some random text to use as contents for a file
	 * resource.
	 */
	public InputStream getRandomContents() {
		return new ByteArrayInputStream(getRandomString().getBytes());
	}

	/**
	 * Return String with some random text to use as contents for a file resource.
	 */
	public String getRandomString() {
		switch ((int) Math.round(Math.random() * 10)) {
		case 0:
			return "este e' o meu conteudo (portuguese)";
		case 1:
			return "ho ho ho";
		case 2:
			return "I'll be back";
		case 3:
			return "don't worry, be happy";
		case 4:
			return "there is no imagination for more sentences";
		case 5:
			return "Alexandre Bilodeau, Canada's first home gold. 14/02/2010";
		case 6:
			return "foo";
		case 7:
			return "bar";
		case 8:
			return "foobar";
		case 9:
			return "case 9";
		default:
			return "these are my contents";
		}
	}

}
