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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.internal.content.ContentTypeManager;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.content.IContentTypeMatcher;
import org.eclipse.core.runtime.content.IContentTypeSettings;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Tests content type matcher with a non-default context for user preferences.
 */
public class SpecificContextTest {

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
	public void testContentTypeLookup(TestInfo testInfo) throws CoreException {
		String testName = testInfo.getDisplayName();
		IContentTypeManager global = Platform.getContentTypeManager();
		final SingleNodeScope scope = new SingleNodeScope();
		IContentTypeMatcher local = global.getMatcher(new LocalSelectionPolicy(), scope);
		IContentType textContentType = global.getContentType(Platform.PI_RUNTIME + '.' + "text");
		// added "<test case name>.global" to the text content type as a global file
		// spec
		textContentType.addFileSpec(testName + ".global", IContentType.FILE_NAME_SPEC);
		// added "<test case name>.local" to the text content type as a local
		// (scope-specific) file spec
		textContentType.getSettings(scope).addFileSpec(testName + ".local", IContentType.FILE_NAME_SPEC);
		// make ensure associations are properly recognized when doing content type
		// lookup
		assertEquals(textContentType, global.findContentTypeFor(testName + ".global"));
		assertNull(local.findContentTypeFor(testName + ".global"));
		assertEquals(textContentType, local.findContentTypeFor(testName + ".local"));
		assertNull(global.findContentTypeFor(testName + ".local"));

		textContentType.removeFileSpec(testName + ".global", IContentType.FILE_NAME_SPEC);
	}

	@Test
	public void testIsAssociatedWith() throws CoreException {
		IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
		final SingleNodeScope scope = new SingleNodeScope();
		IContentType textContentType = contentTypeManager.getContentType(Platform.PI_RUNTIME + '.' + "text");
		IContentTypeSettings localSettings = null;
		localSettings = textContentType.getSettings(scope);
		// haven't added association yet
		assertFalse(textContentType.isAssociatedWith("hello.foo", scope));
		assertFalse(textContentType.isAssociatedWith("hello.foo"));
		// associate at the scope level
		localSettings.addFileSpec("foo", IContentType.FILE_EXTENSION_SPEC);
		localSettings = textContentType.getSettings(scope);
		// scope-specific settings should contain the filespec we just added
		String[] fileSpecs = localSettings.getFileSpecs(IContentType.FILE_EXTENSION_SPEC);
		assertThat(fileSpecs).containsExactly("foo");
		// now it is associated at the scope level...
		assertTrue(textContentType.isAssociatedWith("hello.foo", scope));
		// ...but not at the global level
		assertFalse(textContentType.isAssociatedWith("hello.foo"));
	}

}
