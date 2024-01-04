/*******************************************************************************
 * Copyright (c) 2006, 2023 IBM Corporation and others.
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
 *     Latha Patil (ETAS GmbH) - Issue #504 Show number of differences in the Compare editor
 *******************************************************************************/
package org.eclipse.compare.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareViewerPane;
import org.eclipse.compare.ICompareFilter;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.LabelContributionItem;
import org.eclipse.compare.contentmergeviewer.IIgnoreWhitespaceContributor;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.contentmergeviewer.TextMergeViewer;
import org.eclipse.compare.contentmergeviewer.TokenComparator;
import org.eclipse.compare.internal.ChangeCompareFilterPropertyAction;
import org.eclipse.compare.internal.IMergeViewerTestAdapter;
import org.eclipse.compare.internal.MergeViewerContentProvider;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.internal.merge.DocumentMerger;
import org.eclipse.compare.internal.merge.DocumentMerger.Diff;
import org.eclipse.compare.internal.merge.DocumentMerger.IDocumentMergerInput;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
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
	public void testCompareWithIgnoreWhitespaceContributor() throws Exception {
		String leftTxt = "str\n= \"Hello\nWorld\"";
		String rightTxt = "str\n\n= \"Hello\n\nWorld\""; // added newLine in offset 4 and 14

		DiffNode testNode = new DiffNode(new EditableTestElement(leftTxt.getBytes()),
				new EditableTestElement(rightTxt.getBytes()));

		CompareConfiguration cc = new CompareConfiguration();
		runInDialogWithIgnoreWhitespaceContributor(testNode, () -> {
			try {
				testDocumentMerger.doDiff();
			} catch (CoreException e) {
				fail("Cannot do diff in Document Merger");
			}

			Diff firstDiff = testDocumentMerger.findDiff(new Position(4), false); // first different, not in literal
			Diff secondDiff = testDocumentMerger.findDiff(new Position(14), false); // second different, in literal

			assertNotNull(firstDiff);
			assertNotNull(secondDiff);

			assertEquals("Change direction is wrong", RangeDifference.RIGHT, firstDiff.getKind());
			assertEquals("Change direction is wrong", RangeDifference.RIGHT, secondDiff.getKind());

			assertTrue("Change should be shown", testDocumentMerger.useChange(firstDiff)); // shows this diff in
																							// DocumentMerger
			assertTrue("Change should be shown", testDocumentMerger.useChange(secondDiff)); // shows this diff in
																							// DocumentMerger

			cc.setProperty(CompareConfiguration.IGNORE_WHITESPACE, true);// IGNORE_WHITESPACE set to active
			try {
				testDocumentMerger.doDiff();
			} catch (CoreException e) {
				fail("Cannot do diff in Document Merger");
			}

			firstDiff = testDocumentMerger.findDiff(new Position(4), false);
			secondDiff = testDocumentMerger.findDiff(new Position(14), false);

			assertNotNull(firstDiff);
			assertNotNull(secondDiff);

			assertEquals("Change direction is wrong", RangeDifference.RIGHT, firstDiff.getKind());
			assertEquals("Change direction is wrong", RangeDifference.RIGHT, secondDiff.getKind());

			org.junit.Assert.assertFalse("Change should not be shown", testDocumentMerger.useChange(firstDiff)); // whitespace
																													// not
																													// in
																								// literal, do not show
																								// in DocumentMerger
			assertTrue("Change should be shown", testDocumentMerger.useChange(secondDiff)); // whitespace in literal,
																							// show in DocumentMerger

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

	@Test
	public void testToolbarLabelContribution() throws Exception {

		IPath path = IPath.fromOSString("labelContributionData/" + "file1.java");
		URL url = new URL(CompareTestPlugin.getDefault().getBundle().getEntry("/"), path.toString());

		IPath path1= IPath.fromOSString("labelContributionData/" + "file2.java");
		 URL url1 = new URL(CompareTestPlugin.getDefault().getBundle().getEntry("/"), path1.toString());

		DiffNode parentNode = new DiffNode(new ParentTestElement(), new ParentTestElement());
		DiffNode testNode = new DiffNode(parentNode, Differencer.CHANGE, null, new EditableTestElement(url.openStream().readAllBytes()), new EditableTestElement(url1.openStream().readAllBytes()));

		runInDialogWithToolbarDiffLabel(testNode);
	}

	CompareViewerPane fCompareViewerPane;
	private void runInDialogWithToolbarDiffLabel(DiffNode testNode) throws Exception {

		CompareConfiguration compareConfig = new CompareConfiguration();
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		Dialog dialog = new Dialog(shell) {
			@Override
			protected Control createDialogArea(Composite parent) {
				Composite composite = (Composite) super.createDialogArea(parent);
				 fCompareViewerPane = new CompareViewerPane(composite, SWT.BORDER | SWT.FLAT);
				 composite.getChildren();
				viewer = new TestMergeViewer(fCompareViewerPane,  compareConfig);
				return composite;
			}
		};
		dialog.setBlockOnOpen(false);
		dialog.open();
		viewer.setInput(testNode);
		fCompareViewerPane.setContent(viewer.getControl());
		ToolBarManager toolbarManager = CompareViewerPane.getToolBarManager(fCompareViewerPane);

		processQueuedEvents();

			IContributionItem contributionItem = toolbarManager.find("DiffCount");
				assertNotNull(contributionItem);
				LabelContributionItem labelContributionItem=(LabelContributionItem) contributionItem;
				assertTrue(labelContributionItem.getToolbarLabel().getText().equals("7 Differences"));

		dialog.close();
		viewer = null;
	}

	private void processQueuedEvents() {
		while (Display.getCurrent().readAndDispatch()) {
					// Process all the events in the queue
		}

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

	private static DocumentMerger testDocumentMerger = null;

	private void runInDialogWithIgnoreWhitespaceContributor(Object input, Runnable runnable,
			final CompareConfiguration cc) throws Exception {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		Dialog dialog = new Dialog(shell) {
			@Override
			protected Control createDialogArea(Composite parent) {
				Composite composite = (Composite) super.createDialogArea(parent);
				viewer = new TestMergeViewer(composite, cc);
				testDocumentMerger = createDocumentMerger(viewer, cc);
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

	private static DocumentMerger createDocumentMerger(TestMergeViewer testMergeViewer, CompareConfiguration cc) {
		return new DocumentMerger(new IDocumentMergerInput() {

			@Override
			public Optional<IIgnoreWhitespaceContributor> createIgnoreWhitespaceContributor(IDocument document) {
				return Optional.of(new SimpleIgnoreWhitespaceContributor(document));
			}

			@Override
			public IDocument getDocument(char contributor) {
				IDocument document = Utilities.getDocument(contributor, testMergeViewer.getInput(), true, true);
				if (document == null) {
					return testMergeViewer.getAdapter(IMergeViewerTestAdapter.class).getDocument(contributor);
				}
				return document;
			}

			@Override
			public CompareConfiguration getCompareConfiguration() {
				return cc;
			}

			@Override
			public Position getRegion(char contributor) {
				return null;
			}

			@Override
			public boolean isIgnoreAncestor() {
				return false;
			}

			@Override
			public boolean isThreeWay() {
				return false;
			}

			@Override
			public ITokenComparator createTokenComparator(String line) {
				return new TokenComparator(line);
			}

			@Override
			public boolean isHunkOnLeft() {
				return false;
			}

			@Override
			public int getHunkStart() {
				return 0;
			}

			@Override
			public boolean isPatchHunk() {
				return false;
			}

			@Override
			public boolean isShowPseudoConflicts() {
				return false;
			}

			@Override
			public boolean isPatchHunkOk() {
				return false;
			}
		});
	}
}
