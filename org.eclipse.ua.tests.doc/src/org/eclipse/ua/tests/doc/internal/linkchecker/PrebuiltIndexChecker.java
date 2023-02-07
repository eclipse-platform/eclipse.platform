/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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
package org.eclipse.ua.tests.doc.internal.linkchecker;

import static org.junit.Assert.assertNotNull;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class PrebuiltIndexChecker {

	private static final String PLATFORM_USER = "org.eclipse.platform.doc.user";
	private static final String PLATFORM_ISV = "org.eclipse.platform.doc.isv";
	private static final String PDE_USER = "org.eclipse.pde.doc.user";
	private static final String JDT_USER = "org.eclipse.jdt.doc.user";
	private static final String JDT_ISV = "org.eclipse.jdt.doc.isv";

	@Test
	public void testPlatformUserIndex() {
		validateIndex(PLATFORM_USER, "index");
	}

	@Test
	public void testPlatformIsvIndex() {
		validateIndex(PLATFORM_ISV, "index");
	}

	@Test
	public void testPdeUserIndex() {
		validateIndex(PDE_USER, "index");
	}

	@Test
	public void testJdtUserIndex() {
		validateIndex(JDT_USER, "index");
	}

	@Test
	public void testJdtIsvIndex() {
		validateIndex(JDT_ISV, "index");
	}

	private void validateIndex(String plugin, String filepath) {
		Bundle bundle = Platform.getBundle(plugin);
		assertNotNull(bundle);

		String[] suffixes = { "", "/indexed_contributions", "/indexed_docs", "/indexed_dependencies" };
		for (String suffixe : suffixes) {
			String fullPath = filepath + suffixe;
			IPath path = new Path(fullPath);
			URL url = FileLocator.find(bundle, path, null);
			assertNotNull("could not open: " + fullPath, url);
		}
	}

}
