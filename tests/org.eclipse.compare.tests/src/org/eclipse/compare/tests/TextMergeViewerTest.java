/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ICompareFilter;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.internal.ChangeCompareFilterPropertyAction;
import org.eclipse.compare.internal.IMergeViewerTestAdapter;
import org.eclipse.compare.internal.MergeViewerContentProvider;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class TextMergeViewerTest extends TestCase {

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
	
	/**
	 * A parent test element is an {@link IEditableContent} but is not directly editable.
	 * The purpose of the parent is to be able to copy a child into the destination element.
	 */
	public static class ParentTestElement extends TestElement implements IEditableContent {
		public boolean isEditable() {
			return false;
		}
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
				
				try {
					InputStream is= ((IStreamContentAccessor)other).getContents();
					byte[] bytes= Utilities.readBytes(is);
					if (bytes != null)
						dst.setContent(bytes);
				} catch (CoreException ex) {
					throw new WrappedException(ex);
				}
			}
			return child;
		}
		public void setContent(byte[] newContent) {
			// Node is not directly editable
		}
	}
	
	public static class EditableTestElement extends TestElement implements IStreamContentAccessor, IEditableContent {
		byte[] contents = new byte[0];
		public EditableTestElement(byte[] contents) {
			this.contents = contents;
		}
		public String getType() {
			return TEXT_TYPE;
		}
		public InputStream getContents() {
			return new ByteArrayInputStream(contents);
		}
		protected Object clone() {
			return new EditableTestElement(contents);
		}
		public boolean isEditable() {
			return true;
		}
		public ITypedElement replace(ITypedElement dest, ITypedElement src) {
			// Nothing to do since this node has no children
			return null;
		}
		public void setContent(byte[] newContent) {
			contents = newContent;
		}
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
	
	public TextMergeViewerTest() {
		super();
	}

	public TextMergeViewerTest(String name) {
		super(name);
	}
	
	private void runInDialog(Object input, Runnable runnable) throws Exception {
		runInDialog(input, runnable, new CompareConfiguration());
	}

	private void runInDialog(Object input, Runnable runnable,
			final CompareConfiguration cc) throws Exception {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		Dialog dialog = new Dialog(shell) {
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
		try {
			viewer.save(new NullProgressMonitor());
		} catch (CoreException e) {
			throw new WrappedException(e);
		}
	}
	
	protected IDocument getDocument(boolean left) {
		char leg = left ? MergeViewerContentProvider.LEFT_CONTRIBUTOR : MergeViewerContentProvider.RIGHT_CONTRIBUTOR;
		IDocument document = Utilities.getDocument(leg, viewer.getInput(), true, true);
		if (document == null) {
			return ((IMergeViewerTestAdapter)viewer.getAdapter(IMergeViewerTestAdapter.class)).getDocument(leg);
		}
		return document;
	}
	
	public void testCopyRightToLeft() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		String copiedText = "hi there";
		DiffNode testNode = new DiffNode(parentNode, Differencer.CHANGE, null, new EditableTestElement("some text".getBytes()), new EditableTestElement(copiedText.getBytes()));
		runInDialog(testNode, new Runnable() {
			public void run() {
				viewer.copy(false /* rightToLeft */);
				saveViewerContents();
			}
		});
		assertEquals(copiedText, ((EditableTestElement)testNode.getLeft()).getContentsAsString());
	}
	
	public void testCopyLeftToRight() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		String copiedText = "hi there";
		DiffNode testNode = new DiffNode(parentNode, Differencer.CHANGE, null, new EditableTestElement(copiedText.getBytes()), new EditableTestElement("some text".getBytes()));
		runInDialog(testNode, new Runnable() {
			public void run() {
				viewer.copy(true /* leftToRight */);
				saveViewerContents();
			}
		});
		assertEquals(copiedText, ((EditableTestElement)testNode.getRight()).getContentsAsString());
	}
	
	public void testCopyRightToEmptyLeft() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		DiffNode testNode = new DiffNode(parentNode, Differencer.ADDITION, null, null, new EditableTestElement("hi there".getBytes()));
		runInDialog(testNode, new Runnable() {
			public void run() {
				viewer.copy(false /* rightToLeft */);
				saveViewerContents();
			}
		});
		assertEquals(testNode.getRight(), testNode.getLeft());
	}
	
	public void testCopyLeftToEmptyRight() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		DiffNode testNode = new DiffNode(parentNode, Differencer.DELETION, null, new EditableTestElement("hi there".getBytes()), null);
		runInDialog(testNode, new Runnable() {
			public void run() {
				viewer.copy(true /* leftToRight */);
				saveViewerContents();
			}
		});
		assertEquals(testNode.getRight(), testNode.getLeft());
	}
	
	public void testCopyEmptyLeftToRight() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		DiffNode testNode = new DiffNode(parentNode, Differencer.ADDITION, null, null, new EditableTestElement("hi there".getBytes()));
		runInDialog(testNode, new Runnable() {
			public void run() {
				viewer.copy(true /* leftToRight */);
				saveViewerContents();
			}
		});
		assertNull(testNode.getLeft());
		assertNull(testNode.getRight());
	}
	
	public void testCopyEmptyRightToLeft() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		DiffNode testNode = new DiffNode(parentNode, Differencer.DELETION, null, new EditableTestElement("hi there".getBytes()), null);
		runInDialog(testNode, new Runnable() {
			public void run() {
				viewer.copy(false /* rightToLeft */);
				saveViewerContents();
			}
		});
		assertNull(testNode.getLeft());
		assertNull(testNode.getRight());
	}
	
	public void testModifyLeft() throws Exception {
		DiffNode testNode = new DiffNode(new EditableTestElement("hi there".getBytes()), null);
		final String newText = "New text";
		runInDialog(testNode, new Runnable() {
			public void run() {
				IDocument doc = getDocument(true /* left */);
				doc.set(newText);
				saveViewerContents();
			}
		});
		assertEquals(newText, ((EditableTestElement)testNode.getLeft()).getContentsAsString());
	}
	
	public void testModifyRight() throws Exception {
		DiffNode testNode = new DiffNode(null, new EditableTestElement("hi there".getBytes()));
		final String newText = "New text";
		runInDialog(testNode, new Runnable() {
			public void run() {
				IDocument doc = getDocument(false /* right */);
				doc.set(newText);
				saveViewerContents();
			}
		});
		assertEquals(newText, ((EditableTestElement)testNode.getRight()).getContentsAsString());
	}
	
	public void testCopyEmptyRightToLeftAndModify() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		DiffNode testNode = new DiffNode(parentNode, Differencer.ADDITION, null, null, new EditableTestElement("hi there".getBytes()));
		final String newText = "New text";
		runInDialog(testNode, new Runnable() {
			public void run() {
				viewer.copy(false /* rightToLeft */);
				IDocument doc = getDocument(true /* left */);
				doc.set(newText);
				saveViewerContents();
			}
		});
		assertEquals(newText, ((EditableTestElement)testNode.getLeft()).getContentsAsString());
	}
	
	public void testCopyEmptyLeftToRightAndModify() throws Exception {
		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		DiffNode testNode = new DiffNode(parentNode, Differencer.DELETION, null, new EditableTestElement("hi there".getBytes()), null);
		final String newText = "New text";
		runInDialog(testNode, new Runnable() {
			public void run() {
				viewer.copy(true /* leftToRight */);
				IDocument doc = getDocument(false /* right */);
				doc.set(newText);
				saveViewerContents();
			}
		});
		assertEquals(newText, ((EditableTestElement)testNode.getRight()).getContentsAsString());
	}
	
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
		runInDialog(testNode, new Runnable() {
			public void run() {
				Object adapter = viewer
						.getAdapter(IMergeViewerTestAdapter.class);
				if (adapter instanceof IMergeViewerTestAdapter) {
					IMergeViewerTestAdapter ta = (IMergeViewerTestAdapter) adapter;
					assertEquals(ta.getChangesCount(), 1);

					Map filters = new HashMap();
					filters.put("filter.id", new ICompareFilter() {
						public void setInput(Object input, Object ancestor,
								Object left, Object right) {
							assertTrue(leftElement == left);
							assertTrue(rightElement == right);
						}

						public IRegion[] getFilteredRegions(
								HashMap lineComparison) {
							Object thisLine = lineComparison.get(THIS_LINE);
							Object thisContributor = lineComparison
									.get(THIS_CONTRIBUTOR);
							Object otherLine = lineComparison.get(OTHER_LINE);
							Object otherContributor = lineComparison
									.get(OTHER_CONTRIBUTOR);

							if (thisContributor.equals(new Character('L'))) {
								assertEquals(thisLine, leftString);
								assertEquals(otherContributor, new Character(
										'R'));
								assertEquals(otherLine, rightString);
							} else {
								assertEquals(thisContributor,
										new Character('R'));
								assertEquals(thisLine, rightString);
								assertEquals(otherContributor, new Character(
										'L'));
								assertEquals(otherLine, leftString);
							}

							if (thisContributor.equals(new Character('L')))
								return new IRegion[] { new Region(0, 1),
										new Region(1, 1) };

							return new IRegion[] { new Region(0, 2) };
						}

						public boolean isEnabledInitially() {
							return false;
						}

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
			}
		}, cc);
	}
}
