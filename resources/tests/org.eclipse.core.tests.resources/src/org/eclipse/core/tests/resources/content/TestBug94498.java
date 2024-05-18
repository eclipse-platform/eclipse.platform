/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestBug94498 {

	private static final String FILE_NAME = "foo.bar.zoo";

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	@Test
	@Order(1)
	public void test1() throws CoreException {
		IContentType text = Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT);
		assertThat(text).isNotNull();
		text.addFileSpec(FILE_NAME, IContentType.FILE_NAME_SPEC);
		String[] fileSpecs = text.getFileSpecs(IContentType.FILE_NAME_SPEC | IContentType.IGNORE_PRE_DEFINED);
		assertThat(fileSpecs).containsExactly(FILE_NAME);
	}

	@Test
	@Order(2)
	public void test2() {
		IContentType text = Platform.getContentTypeManager().getContentType(IContentTypeManager.CT_TEXT);
		assertThat(text).isNotNull();
		String[] fileSpecs = text.getFileSpecs(IContentType.FILE_NAME_SPEC | IContentType.IGNORE_PRE_DEFINED);
		assertThat(fileSpecs).containsExactly(FILE_NAME);
	}

}
