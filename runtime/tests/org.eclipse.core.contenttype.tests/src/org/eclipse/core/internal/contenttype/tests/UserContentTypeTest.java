/*******************************************************************************
 * Copyright (c) 2016 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Mickael Istria (Red Hat Inc.) - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.contenttype.tests;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.eclipse.core.internal.content.ContentTypeManager;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;

@SuppressWarnings("restriction")
public class UserContentTypeTest {

	private IContentType createdUserContentType;
	private IContentTypeManager manager;
	private IContentType[] initialContentTypes;

	@BeforeEach
	public void setUp() {
		this.createdUserContentType = null;
		this.manager = new ContentTypeManager();
		initialContentTypes = manager.getAllContentTypes();
		// check no user-defined Content-Type before test
		for (IContentType contentType : initialContentTypes) {
			assertThat(contentType).matches(not(IContentType::isUserDefined), "is not user defined");
		}
	}

	@AfterEach
	public void tearDown() throws CoreException {
		if (this.createdUserContentType != null) {
			this.manager.removeContentType(this.createdUserContentType.getId());
		}
		ContentTypeManager.shutdown();
	}

	@Test
	public void testCannotDeleteSystemContentType() {
		assertThat(initialContentTypes).as("check has content type to try").isNotEmpty();
		IContentType toDelete = initialContentTypes[0];
		assertThat(toDelete).matches(not(IContentType::isUserDefined), "is system and not user defined");
		assertThatThrownBy(() -> manager.removeContentType(toDelete.getId()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(manager.getAllContentTypes()).as("check number of content types has not changed")
				.hasSameSizeAs(initialContentTypes);
		assertThat(manager.getContentType(toDelete.getId())).as("check could access content type").isEqualTo(toDelete);
	}

	@Test
	public void testAddUserDefinedContentTypes() throws CoreException {
		String contentTypeIdentifier = "testContentType" + System.nanoTime();
		this.createdUserContentType = manager.addContentType(contentTypeIdentifier,
				"user-defined test content-type", null);
		assertThat(manager.getAllContentTypes()).as("check content type is registered")
				.hasSize(initialContentTypes.length + 1);
		assertThat(createdUserContentType).matches(IContentType::isUserDefined, "is user defined");
		assertThat(manager.getContentType(contentTypeIdentifier)).isEqualTo(this.createdUserContentType);
		for (IContentType contentType : manager.getAllContentTypes()) {
			if (contentType.equals(createdUserContentType)) {
				assertThat(contentType).matches(IContentType::isUserDefined, "is user defined");
			} else {
				assertThat(contentType).matches(not(IContentType::isUserDefined), "is not user defined");
			}
		}
	}

	@Test
	public void testPersistContentTypeAndAssociation() throws CoreException {
		testAddUserDefinedContentTypes();
		this.createdUserContentType.addFileSpec("fileSpec", IContentType.FILE_NAME_SPEC);
		// use a new manager to retrigger parsing and test persistency
		ContentTypeManager.shutdown(); // shut down the old one first
		this.manager = new ContentTypeManager();
		assertThat(manager.getAllContentTypes()).as("check content type is persisted")
				.hasSize(initialContentTypes.length + 1);
		this.createdUserContentType = this.manager.getContentType(this.createdUserContentType.getId());
		assertThat(createdUserContentType).as("find new content type in new manager").isNotNull();
		assertThat(createdUserContentType).matches(IContentType::isUserDefined, "is user defined");
		assertThat(createdUserContentType).matches(
				it -> Arrays.asList(it.getFileSpecs(IContentType.FILE_NAME_SPEC)).contains("fileSpec"),
				"has association persisted");
	}
}
