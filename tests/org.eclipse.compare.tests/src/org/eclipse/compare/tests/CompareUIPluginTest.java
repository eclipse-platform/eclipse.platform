/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
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
package org.eclipse.compare.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.eclipse.compare.*;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.ViewerDescriptor;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.junit.Test;

public class CompareUIPluginTest {

	private static class UnknownTypedElement implements ITypedElement {
		@Override
		public Image getImage() {
			return null;
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public String getType() {
			return UNKNOWN_TYPE;
		}
	}

	private static class TextTypedElement implements ITypedElement {
		@Override
		public Image getImage() {
			return null;
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public String getType() {
			return TEXT_TYPE;
		}
	}

	private static class TextTypedElementStreamAccessor implements ITypedElement, IStreamContentAccessor {
		@Override
		public Image getImage() {
			return null;
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public String getType() {
			return TEXT_TYPE;
		}

		@Override
		public InputStream getContents() throws CoreException {
			/*
			 * Whatever we return has no importance as long as it is not "null", this is
			 * only to make CompareUIPlugin#guessType happy. However, it is only happy if
			 * what we return resembles a text.
			 */
			return new ByteArrayInputStream(new byte[] { ' ' });
		}
	}

	@Test
	public void testFindContentViewerDescriptor_UnknownType() {
		CompareConfiguration cc = new CompareConfiguration();
		DiffNode in = new DiffNode(new UnknownTypedElement(), new UnknownTypedElement());
		ViewerDescriptor[] result = CompareUIPlugin.getDefault().findContentViewerDescriptor(null, in, cc);

		// API Compatibility : "no descriptor found" should return a null array instead
		// of a 0-lengthed one.
		assertNull(result);
	}

	@Test
	public void testFindContentViewerDescriptor_TextType_NotStreamAccessor() {
		CompareConfiguration cc = new CompareConfiguration();
		DiffNode in = new DiffNode(new TextTypedElement(), new TextTypedElement());
		ViewerDescriptor[] result = CompareUIPlugin.getDefault().findContentViewerDescriptor(null, in, cc);

		/*
		 * "TextTypedElement" is "text" typed : it thus has a Content Viewer attached.
		 * However, this content viewer is currently NOT returned because of bug 293926
		 */
		assertNotNull(result);
		assertEquals(1, result.length);
	}

	@Test
	public void testFindContentViewerDescriptorForTextType_StreamAccessor() {
		CompareConfiguration cc = new CompareConfiguration();
		DiffNode in = new DiffNode(new TextTypedElementStreamAccessor(), new TextTypedElementStreamAccessor());
		ViewerDescriptor[] result = CompareUIPlugin.getDefault().findContentViewerDescriptor(null, in, cc);

		/*
		 * "TextTypedElement" is "text" typed : it thus has a Content Viewer attached.
		 * However, the content viewer will only be returned because we made our
		 * "ITypedElement" be an IStreamContentAccessor.
		 */
		assertNotNull(result);
		assertEquals(1, result.length);
	}
}