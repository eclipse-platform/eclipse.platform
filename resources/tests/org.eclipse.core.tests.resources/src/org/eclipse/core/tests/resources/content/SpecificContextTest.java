/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.internal.content.ContentTypeManager;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.content.*;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * Tests content type matcher with a non-default context for user preferences.
 */
public class SpecificContextTest extends ContentTypeTest {

	@Rule
	public TestName name = new TestName();

	/**
	 * A minimal scope implementation.
	 */
	private static class SingleNodeScope implements IScopeContext {
		private final IEclipsePreferences node;

		SingleNodeScope() {
			this.node = new EclipsePreferences();
		}

		@Override
		public IPath getLocation() {
			return null;
		}

		@Override
		public String getName() {
			return "";
		}

		@Override
		public IEclipsePreferences getNode(String qualifier) {
			assertEquals(ContentTypeManager.CONTENT_TYPE_PREF_NODE, qualifier);
			return this.node;
		}
	}

	@Test
	public void testContentTypeLookup() throws CoreException {
		IContentTypeManager global = Platform.getContentTypeManager();
		final SingleNodeScope scope = new SingleNodeScope();
		IContentTypeMatcher local = global.getMatcher(new LocalSelectionPolicy(), scope);
		IContentType textContentType = global.getContentType(Platform.PI_RUNTIME + '.' + "text");
		// added "<test case name>.global" to the text content type as a global file
		// spec
		textContentType.addFileSpec(name.getMethodName() + ".global", IContentType.FILE_NAME_SPEC);
		// added "<test case name>.local" to the text content type as a local
		// (scope-specific) file spec
		textContentType.getSettings(scope).addFileSpec(name.getMethodName() + ".local", IContentType.FILE_NAME_SPEC);
		// make ensure associations are properly recognized when doing content type
		// lookup
		assertEquals("1.0", textContentType, global.findContentTypeFor(name.getMethodName() + ".global"));
		assertEquals("1.1", null, local.findContentTypeFor(name.getMethodName() + ".global"));
		assertEquals("2.0", textContentType, local.findContentTypeFor(name.getMethodName() + ".local"));
		assertEquals("2.1", null, global.findContentTypeFor(name.getMethodName() + ".local"));

		try {
			textContentType.removeFileSpec(name.getMethodName() + ".global", IContentType.FILE_NAME_SPEC);
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testIsAssociatedWith() throws CoreException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		final SingleNodeScope scope = new SingleNodeScope();
		IContentType textContentType = contentTypeManager.getContentType(Platform.PI_RUNTIME + '.' + "text");
		IContentTypeSettings localSettings = null;
		localSettings = textContentType.getSettings(scope);
		// haven't added association yet
		assertFalse("1.0", textContentType.isAssociatedWith("hello.foo", scope));
		assertFalse("1.1", textContentType.isAssociatedWith("hello.foo"));
		// associate at the scope level
		localSettings.addFileSpec("foo", IContentType.FILE_EXTENSION_SPEC);
		localSettings = textContentType.getSettings(scope);
		// scope-specific settings should contain the filespec we just added
		String[] fileSpecs = localSettings.getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
		assertEquals("2.2", 1, fileSpecs.length);
		assertEquals("2.3", "foo", fileSpecs[0]);
		// now it is associated at the scope level...
		assertTrue("2.5", textContentType.isAssociatedWith("hello.foo", scope));
		// ...but not at the global level
		assertFalse("2.6", textContentType.isAssociatedWith("hello.foo"));
	}

}
