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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.TestCase;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.ViewerDescriptor;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;

public class CompareUIPluginTest extends TestCase {

	private static class UnknownTypedElement implements ITypedElement {
		public Image getImage() {
			return null;
		}

		public String getName() {
			return "test";
		}

		public String getType() {
			return UNKNOWN_TYPE;
		}
	}

	private static class TextTypedElement implements ITypedElement {
		public Image getImage() {
			return null;
		}

		public String getName() {
			return "test";
		}

		public String getType() {
			return TEXT_TYPE;
		}
	}

	private static class TextTypedElementStreamAccessor implements ITypedElement, IStreamContentAccessor {
		public Image getImage() {
			return null;
		}

		public String getName() {
			return "test";
		}

		public String getType() {
			return TEXT_TYPE;
		}

		public InputStream getContents() throws CoreException {
			/*
			 * Whatever we return has no importance as long as it is not "null", this is only to make
			 * CompareUIPlugin#guessType happy. However, it is only happy if what we return resembles a text.
			 */
			return new ByteArrayInputStream(new byte[] {' '});
		}
	}

	public void testFindContentViewerDescriptor_UnknownType() {
		CompareConfiguration cc = new CompareConfiguration();
		DiffNode in = new DiffNode(new UnknownTypedElement(), new UnknownTypedElement());
		ViewerDescriptor[] result = CompareUIPlugin.getDefault().findContentViewerDescriptor(null, in, cc);

		// API Compatibility : "no descriptor found" should return a null array instead of a 0-lengthed one.
		assertNull(result);
	}

	public void testFindContentViewerDescriptor_TextType_NotStreamAccessor() {
		CompareConfiguration cc = new CompareConfiguration();
		DiffNode in = new DiffNode(new TextTypedElement(), new TextTypedElement());
		ViewerDescriptor[] result = CompareUIPlugin.getDefault().findContentViewerDescriptor(null, in, cc);

		/*
		 * "TextTypedElement" is "text" typed : it thus has a Content Viewer attached. However, this content
		 * viewer is currently NOT returned because of bug 293926
		 */
		assertNotNull(result);
		assertEquals(1, result.length);
	}

	public void testFindContentViewerDescriptorForTextType_StreamAccessor() {
		CompareConfiguration cc = new CompareConfiguration();
		DiffNode in = new DiffNode(new TextTypedElementStreamAccessor(), new TextTypedElementStreamAccessor());
		ViewerDescriptor[] result = CompareUIPlugin.getDefault().findContentViewerDescriptor(null, in, cc);

		/*
		 * "TextTypedElement" is "text" typed : it thus has a Content Viewer attached. However, the content
		 * viewer will only be returned because we made our "ITypedElement" be an IStreamContentAccessor.
		 */
		assertNotNull(result);
		assertEquals(1, result.length);
	}
}