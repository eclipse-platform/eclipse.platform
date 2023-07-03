/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
 *     Alexander Kurtakov - Bug 460858
 *******************************************************************************/
package org.eclipse.ua.tests.doc.internal.linkchecker;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.help.internal.validation.TocValidator;
import org.eclipse.help.internal.validation.TocValidator.BrokenLink;
import org.junit.Assert;
import org.junit.Test;

/**
 * Contains tests for the documentation bundles that
 * are included with the Eclipse SDK. This tests that the
 * table of contents can be parsed and that the file
 * corresponding to each href actually exists.
 * It does not check for broken links within the files or
 * references to missing images, css files etc.
 *
 * Note that some API documents are generated as part of the
 * Eclipse build process. Tests for these documents contain
 * "Generated" in their name and are not expected to pass
 * if that project is checked out in your workspace.
 */

public class TocLinkChecker {

	private static final class ReferenceFilter extends TocValidator.Filter {
		@Override
		public boolean isIncluded(String href) {
			return href.startsWith("reference");
		}
	}

	private static final class NonReferenceFilter extends TocValidator.Filter {
		@Override
		public boolean isIncluded(String href) {
			return !href.startsWith("reference");
		}
	}

	private static final class NonReferenceNonSampleFilter extends TocValidator.Filter {
		@Override
		public boolean isIncluded(String href) {
			return !href.startsWith("reference") && !href.startsWith("samples");
		}
	}

	private static final class ReferenceOrSampleFilter extends TocValidator.Filter {
		@Override
		public boolean isIncluded(String href) {
			return href.startsWith("reference") || href.startsWith("samples");
		}
	}

	private static final String[] PLATFORM_USER = {"/org.eclipse.platform.doc.user/toc.xml"};
	private static final String[] PLATFORM_ISV = {"/org.eclipse.platform.doc.isv/toc.xml"};
	private static final String[] PDE_USER = {"/org.eclipse.pde.doc.user/toc.xml"};
	private static final String[] JDT_USER = {"/org.eclipse.jdt.doc.user/toc.xml"};
	private static final String[] JDT_ISV = {"/org.eclipse.jdt.doc.isv/toc.xml"};

	@Test
	public void testPlatformUser() throws Exception {
		ArrayList<BrokenLink> failures = TocValidator.validate(PLATFORM_USER);
		doAssert(failures);
	}

	@Test
	public void testPlatformIsvStatic() throws Exception {
		ArrayList<BrokenLink> failures = TocValidator.filteredValidate(PLATFORM_ISV, new NonReferenceNonSampleFilter());
		doAssert(failures);
	}

	@Test
	public void testPlatformIsvGenerated() throws Exception {
		ArrayList<BrokenLink> failures = TocValidator.filteredValidate(PLATFORM_ISV, new ReferenceOrSampleFilter());
		doAssert(failures);
	}

	@Test
	public void testPdeUserStatic() throws Exception {
		ArrayList<BrokenLink> failures = TocValidator.filteredValidate(PDE_USER, new NonReferenceFilter());
		doAssert(failures);
	}

	@Test
	public void testPdeUserGenerated() throws Exception {
		ArrayList<BrokenLink> failures = TocValidator.filteredValidate(PDE_USER, new ReferenceFilter());
		doAssert(failures);
	}

	@Test
	public void testJdtUser() throws Exception {
		ArrayList<BrokenLink> failures = TocValidator.validate(JDT_USER);
		doAssert(failures);
	}

	@Test
	public void testJdtIsvStatic() throws Exception {
		ArrayList<BrokenLink> failures = TocValidator.filteredValidate(JDT_ISV, new NonReferenceFilter());
		doAssert(failures);
	}

	@Test
	public void testJdtIsvGenerated() throws Exception {
		ArrayList<BrokenLink> failures = TocValidator.filteredValidate(JDT_ISV, new ReferenceFilter());
		doAssert(failures);
	}

	private void doAssert(List<BrokenLink> failures) {
		StringBuilder message = new StringBuilder();
		for (int i = 0; i < failures.size(); i++) {
			BrokenLink link = failures.get(i);
			message.append("Invalid link in \"" + link.getTocID() + "\": " + link.getHref() + "\n");
		}
		Assert.assertTrue(message.toString(), failures.isEmpty());
	}
}
