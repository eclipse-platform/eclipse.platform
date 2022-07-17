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

import java.util.Arrays;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.eclipse.core.internal.content.ContentTypeManager;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;

@SuppressWarnings("restriction")
public class UserContentTypeTest {

	private IContentType createdUserContentType;
	private IContentTypeManager manager;
	private IContentType[] initialContentTypes;

	@Before
	public void setUp() {
		this.createdUserContentType = null;
		this.manager = new ContentTypeManager();
		initialContentTypes = manager.getAllContentTypes();
		// check no user-defined Content-Type before test
		for (IContentType contentType : initialContentTypes) {
			Assert.assertFalse(contentType.isUserDefined());
		}
	}

	@After
	public void tearDown() throws CoreException {
		if (this.createdUserContentType != null) {
			this.manager.removeContentType(this.createdUserContentType.getId());
		}
		ContentTypeManager.shutdown();
	}

	@Test
	public void testCannotDeleteSystemContentType() throws CoreException {
		Assert.assertNotEquals("No content-type to try", 0, initialContentTypes.length);
		IContentType toDelete = initialContentTypes[0];
		Assert.assertFalse("Content-type must be system, not user", toDelete.isUserDefined());
		try {
			manager.removeContentType(toDelete.getId());
			Assert.fail("Expected CoreException");
		} catch (IllegalArgumentException ex) {
			// OK
		}
		Assert.assertEquals("# of content-types shouldn't have changed", initialContentTypes.length,
				manager.getAllContentTypes().length);
		Assert.assertEquals("Couldn't access content-type", toDelete, manager.getContentType(toDelete.getId()));
	}

	@Test
	public void testAddUserDefinedContentTypes() throws CoreException {
		String contentTypeIdentifier = "testContentType" + System.nanoTime();
		this.createdUserContentType = manager.addContentType(contentTypeIdentifier,
				"user-defined test content-type", null);
		Assert.assertEquals("Content type isn't registered", initialContentTypes.length + 1,
				manager.getAllContentTypes().length);
		Assert.assertTrue("Content-Type not marked as user-defined", this.createdUserContentType.isUserDefined());
		Assert.assertEquals(this.createdUserContentType, manager.getContentType(contentTypeIdentifier));
		for (IContentType contentType : manager.getAllContentTypes()) {
			Assert.assertEquals(contentType.equals(this.createdUserContentType), contentType.isUserDefined());
		}
	}

	@Test
	public void testPersistContentTypeAndAssociation() throws CoreException {
		testAddUserDefinedContentTypes();
		this.createdUserContentType.addFileSpec("fileSpec", IContentType.FILE_NAME_SPEC);
		// use a new manager to retrigger parsing and test persistency
		ContentTypeManager.shutdown(); // shut down the old one first
		this.manager = new ContentTypeManager();
		Assert.assertEquals("Content type wasn't persisted", initialContentTypes.length + 1,
				manager.getAllContentTypes().length);
		this.createdUserContentType = this.manager.getContentType(this.createdUserContentType.getId());
		Assert.assertNotNull("Couldn't find the new content-type in new manager", this.createdUserContentType);
		Assert.assertTrue("Content-Type not marked as user-defined", this.createdUserContentType.isUserDefined());
		Assert.assertTrue("Association wasn't persisted", Arrays
				.asList(this.createdUserContentType.getFileSpecs(IContentType.FILE_NAME_SPEC)).contains("fileSpec"));
	}
}
