/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.compare.*;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.internal.*;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.text.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.junit.Test;

public class TextMergeViewerTest  {

	/**
	 * Returns a boolean value indicating whether or not the contents
	 * of the given streams are considered to be equal. Closes both input streams.
	 * @param a stream a
	 * @param b stream b
	 * @return whether the two streams contain the same contents
	 */
	public static boolean compareContent(InputStream a, InputStream b) {
		int c, d;
		if (a == null && b == null)
			return true;
		try {
			if (a == null || b == null)
				return false;
			while ((c = a.read()) == (d = b.read()) && (c != -1 && d != -1)) {
				//body not needed
			}
			return (c == -1 && d == -1);
		} catch (IOException e) {
			return false;
		} finally {
			try {
				if (a != null)
					a.close();
			} catch (IOException e) {
				// ignore
			}
			try {
				if (b != null)
					b.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	public static class TestElement implements ITypedElement {
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

	/**
	 * A parent test element is an {@link IEditableContent} but is not directly editable.
	 * The purpose of the parent is to be able to copy a child into the destination element.
	 */
	public static class ParentTestElement extends TestElement implements IEditableContent {
		@Override
		public boolean isEditable() {
			return false;
		}
		@Override
		public ITypedElement replace(ITypedElement child, ITypedElement other) {
			if (child == null) {	// add child
				// clone the other and return it as the new child
				if (other instanceof EditableTestElement) {
					EditableTestElement ete = (EditableTestElement) other;
					return (ITypedElement)ete.clone();
				}
			}
			if (other == null) {	// delete child
				// Return null as the new child
				return null;
			}

			if (other instanceof IStreamContentAccessor && child instanceof IEditableContent) {
				IEditableContent dst= (IEditableContent) child;

				try (InputStream is= ((IStreamContentAccessor)other).getContents()) {
					byte[] bytes= is.readAllBytes();
					if (bytes != null)
						dst.setContent(bytes);
				} catch (CoreException | IOException ex) {
					throw new WrappedException(ex);
				}
			}
			return child;
		}
		@Override
		public void setContent(byte[] newContent) {
			// Node is not directly editable
		}
	}

	public static class EditableTestElement extends TestElement implements IStreamContentAccessor, IEditableContent {
		byte[] contents = new byte[0];
		public EditableTestElement(byte[] contents) {
			this.contents = contents;
		}
		@Override
		public String getType() {
			return TEXT_TYPE;
		}
		@Override
		public InputStream getContents() {
			return new ByteArrayInputStream(contents);
		}
		@Override
		protected Object clone() {
			return new EditableTestElement(contents);
		}
		@Override
		public boolean isEditable() {
			return true;
		}
		@Override
		public ITypedElement replace(ITypedElement dest, ITypedElement src) {
			// Nothing to do since this node has no children
			return null;
		}
		@Override
		public void setContent(byte[] newContent) {
			contents = newContent;
		}
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof EditableTestElement) {
				EditableTestElement other = (EditableTestElement) obj;
				return TextMergeViewerTest.compareContent(other.getContents(), getContents());
			}
			return false;
		}
		public Object getContentsAsString() {
			return new String(contents);
		}
	}

	public static class TestMergeViewer extends TextMergeViewer {
		public TestMergeViewer(Composite parent) {
			super(parent, new CompareConfiguration());
		}

		public TestMergeViewer(Composite parent, CompareConfiguration cc) {
			super(parent, cc);
		}

		@Override
		public void copy(boolean leftToRight) {
			super.copy(leftToRight);
		}
	}

	public static class WrappedException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		Exception exception;
		public WrappedException(Exception exception) {
			super();
			this.exception = exception;
		}
		public void throwException() throws Exception {
			throw exception;
		}
	}

	TestMergeViewer viewer;


	private void runInDialog(Object input, Runnable runnable) throws Exception {
		runInDialog(input, runnable, new CompareConfiguration());
	}

	private void runInDialog(Object input, Runnable runnable,
			final CompareConfiguration cc) throws Exception {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		Dialog dialog = new Dialog(shell) {
			@Override
			protected Control createDialogArea(Composite parent) {
				Composite composite = (Composite) super.createDialogArea(parent);
				viewer = new TestMergeViewer(composite, cc);
				return composite;
			}
		};
		dialog.setBlockOnOpen(false);
		dialog.open();
		viewer.setInput(input);
		try {
			runnable.run();
		} catch (WrappedException e) {
			e.throwException();
		}
		dialog.close();
		viewer = null;
	}

	protected void saveViewerContents() {
		viewer.flush(new NullProgressMonitor());
	}

	protected IDocument getDocument(boolean left) {
		char leg = left ? MergeViewerContentProvider.LEFT_CONTRIBUTOR : MergeViewerContentProvider.RIGHT_CONTRIBUTOR;
		IDocument document = Utilities.getDocument(leg, viewer.getInput(), true, true);
		if (document == null) {
			return viewer.getAdapter(IMergeViewerTestAdapter.class).getDocument(leg);
		}
		return document;
	}

	@Test
	public void testCopyRightToLeft() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		String copiedText = "hi there";
		DiffNode testNode = new DiffNode(parentNode, Differencer.CHANGE, null, new EditableTestElement("some text".getBytes()), new EditableTestElement(copiedText.getBytes()));
		runInDialog(testNode, () -> {
			viewer.copy(false /* rightToLeft */);
			saveViewerContents();
		});
		assertEquals(copiedText, ((EditableTestElement)testNode.getLeft()).getContentsAsString());
	}

	@Test
	public void testCopyLeftToRight() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		String copiedText = "hi there";
		DiffNode testNode = new DiffNode(parentNode, Differencer.CHANGE, null, new EditableTestElement(copiedText.getBytes()), new EditableTestElement("some text".getBytes()));
		runInDialog(testNode, () -> {
			viewer.copy(true /* leftToRight */);
			saveViewerContents();
		});
		assertEquals(copiedText, ((EditableTestElement)testNode.getRight()).getContentsAsString());
	}

	@Test
	public void testCopyRightToEmptyLeft() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		DiffNode testNode = new DiffNode(parentNode, Differencer.ADDITION, null, null, new EditableTestElement("hi there".getBytes()));
		runInDialog(testNode, () -> {
			viewer.copy(false /* rightToLeft */);
			saveViewerContents();
		});
		assertEquals(testNode.getRight(), testNode.getLeft());
	}

	@Test
	public void testCopyLeftToEmptyRight() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		DiffNode testNode = new DiffNode(parentNode, Differencer.DELETION, null, new EditableTestElement("hi there".getBytes()), null);
		runInDialog(testNode, () -> {
			viewer.copy(true /* leftToRight */);
			saveViewerContents();
		});
		assertEquals(testNode.getRight(), testNode.getLeft());
	}

	@Test
	public void testCopyEmptyLeftToRight() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		DiffNode testNode = new DiffNode(parentNode, Differencer.ADDITION, null, null, new EditableTestElement("hi there".getBytes()));
		runInDialog(testNode, () -> {
			viewer.copy(true /* leftToRight */);
			saveViewerContents();
		});
		assertNull(testNode.getLeft());
		assertNull(testNode.getRight());
	}

	@Test
	public void testCopyEmptyRightToLeft() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		DiffNode testNode = new DiffNode(parentNode, Differencer.DELETION, null, new EditableTestElement("hi there".getBytes()), null);
		runInDialog(testNode, () -> {
			viewer.copy(false /* rightToLeft */);
			saveViewerContents();
		});
		assertNull(testNode.getLeft());
		assertNull(testNode.getRight());
	}

	@Test
	public void testModifyLeft() throws Exception {
		DiffNode testNode = new DiffNode(new EditableTestElement("hi there".getBytes()), null);
		final String newText = "New text";
		runInDialog(testNode, () -> {
			IDocument doc = getDocument(true /* left */);
			doc.set(newText);
			saveViewerContents();
		});
		assertEquals(newText, ((EditableTestElement)testNode.getLeft()).getContentsAsString());
	}

	@Test
	public void testModifyRight() throws Exception {
		DiffNode testNode = new DiffNode(null, new EditableTestElement("hi there".getBytes()));
		final String newText = "New text";
		runInDialog(testNode, () -> {
			IDocument doc = getDocument(false /* right */);
			doc.set(newText);
			saveViewerContents();
		});
		assertEquals(newText, ((EditableTestElement)testNode.getRight()).getContentsAsString());
	}

	@Test
	public void testCopyEmptyRightToLeftAndModify() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		DiffNode testNode = new DiffNode(parentNode, Differencer.ADDITION, null, null, new EditableTestElement("hi there".getBytes()));
		final String newText = "New text";
		runInDialog(testNode, () -> {
			viewer.copy(false /* rightToLeft */);
			IDocument doc = getDocument(true /* left */);
			doc.set(newText);
			saveViewerContents();
		});
		assertEquals(newText, ((EditableTestElement)testNode.getLeft()).getContentsAsString());
	}

	@Test
	public void testCopyEmptyLeftToRightAndModify() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		DiffNode testNode = new DiffNode(parentNode, Differencer.DELETION, null, new EditableTestElement("hi there".getBytes()), null);
		final String newText = "New text";
		runInDialog(testNode, () -> {
			viewer.copy(true /* leftToRight */);
			IDocument doc = getDocument(false /* right */);
			doc.set(newText);
			saveViewerContents();
		});
		assertEquals(newText, ((EditableTestElement)testNode.getRight()).getContentsAsString());
	}

	@Test
	public void testCompareFilter() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(),
				new ParentTestElement());

		final String leftString = "HI there";
		final String rightString = "hi there";
		final EditableTestElement leftElement = new EditableTestElement(
				leftString.getBytes());
		final EditableTestElement rightElement = new EditableTestElement(
				rightString.getBytes());
		DiffNode testNode = new DiffNode(parentNode, Differencer.CHANGE, null,
				leftElement, rightElement);
		final CompareConfiguration cc = new CompareConfiguration();
		runInDialog(testNode, () -> {
			Object adapter = viewer
					.getAdapter(IMergeViewerTestAdapter.class);
			if (adapter instanceof IMergeViewerTestAdapter) {
				IMergeViewerTestAdapter ta = (IMergeViewerTestAdapter) adapter;
				assertEquals(ta.getChangesCount(), 1);

				Map<String, ICompareFilter> filters = new HashMap<>();
				filters.put("filter.id", new ICompareFilter() {
					@Override
					public void setInput(Object input, Object ancestor,
							Object left, Object right) {
						assertTrue(leftElement == left);
						assertTrue(rightElement == right);
					}

					@Override
					public IRegion[] getFilteredRegions(
							HashMap lineComparison) {
						Object thisLine = lineComparison.get(THIS_LINE);
						Object thisContributor = lineComparison
								.get(THIS_CONTRIBUTOR);
						Object otherLine = lineComparison.get(OTHER_LINE);
						Object otherContributor = lineComparison
								.get(OTHER_CONTRIBUTOR);

						if (thisContributor.equals(Character.valueOf('L'))) {
							assertEquals(thisLine, leftString);
							assertEquals(otherContributor, Character.valueOf('R'));
							assertEquals(otherLine, rightString);
						} else {
							assertEquals(thisContributor,
									Character.valueOf('R'));
							assertEquals(thisLine, rightString);
							assertEquals(otherContributor, Character.valueOf('L'));
							assertEquals(otherLine, leftString);
						}

						if (thisContributor.equals(Character.valueOf('L')))
							return new IRegion[] { new Region(0, 1),
									new Region(1, 1) };

						return new IRegion[] { new Region(0, 2) };
					}

					@Override
					public boolean isEnabledInitially() {
						return false;
					}

					@Override
					public boolean canCacheFilteredRegions() {
						return true;
					}

				});

				cc.setProperty(
						ChangeCompareFilterPropertyAction.COMPARE_FILTERS,
						filters);
				assertEquals(ta.getChangesCount(), 0);

				cc.setProperty(
						ChangeCompareFilterPropertyAction.COMPARE_FILTERS,
						null);
				assertEquals(ta.getChangesCount(), 1);
			}
		}, cc);
	}

	@Test
	public void testDocumentAsTypedElement() throws Exception {
		class DocumentAsTypedElement extends Document implements ITypedElement {

			@Override
			public String getName() {
				return "file";
			}

			@Override
			public Image getImage() {
				return null;
			}

			@Override
			public String getType() {
				return ITypedElement.UNKNOWN_TYPE;
			}
		}
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		DocumentAsTypedElement leftDoc = new DocumentAsTypedElement();
		DocumentAsTypedElement rightDoc = new DocumentAsTypedElement();
		DiffNode testNode = new DiffNode(parentNode, Differencer.DELETION, null, leftDoc, rightDoc);
		runInDialogWithPartioner(testNode, () -> {
			//Not needed
		}, new CompareConfiguration());
		assertNotNull(leftDoc.getDocumentPartitioner());
		assertNotNull(rightDoc.getDocumentPartitioner());
	}


	private void runInDialogWithPartioner(Object input, Runnable runnable, final CompareConfiguration cc) throws Exception {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		Dialog dialog = new Dialog(shell) {
			@Override
			protected Control createDialogArea(Composite parent) {
				Composite composite = (Composite) super.createDialogArea(parent);
				viewer = new TestMergeViewerWithPartitioner(composite, cc);
				return composite;
			}
		};
		dialog.setBlockOnOpen(false);
		dialog.open();
		viewer.setInput(input);
		try {
			runnable.run();
		} catch (WrappedException e) {
			e.throwException();
		}
		dialog.close();
		viewer = null;
	}

	//This viewer is used to provide a dummy partitioner
	public static class TestMergeViewerWithPartitioner extends TestMergeViewer {
		public class DummyPartitioner implements IDocumentPartitioner {
			@Override
			public void connect(IDocument document) {
				//Nothing to do
			}

			@Override
			public void disconnect() {
				//Nothing to do
			}

			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {
				//Nothing to do
			}

			@Override
			public boolean documentChanged(DocumentEvent event) {
				return false;
			}

			@Override
			public String[] getLegalContentTypes() {
				return null;
			}

			@Override
			public String getContentType(int offset) {
				return null;
			}

			@Override
			public ITypedRegion[] computePartitioning(int offset, int length) {
				return null;
			}

			@Override
			public ITypedRegion getPartition(int offset) {
				return null;
			}

		}
		public TestMergeViewerWithPartitioner(Composite parent) {
			super(parent, new CompareConfiguration());
		}

		public TestMergeViewerWithPartitioner(Composite parent, CompareConfiguration cc) {
			super(parent, cc);
		}

		@Override
		public void copy(boolean leftToRight) {
			super.copy(leftToRight);
		}
		@Override
		protected IDocumentPartitioner getDocumentPartitioner() {
			return new DummyPartitioner();
		}
	}
}
