/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
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
 *     Martin Karpisek - bug 195840
 *******************************************************************************/
package org.eclipse.ant.tests.ui.editor;

import java.io.File;

import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.ant.internal.ui.editor.text.XMLTextHover;
import org.eclipse.ant.tests.ui.editor.performance.EditorTestHelper;
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.ant.tests.ui.testplugin.ProjectCreationDecorator;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

@SuppressWarnings("restriction")
public class AntEditorTests extends AbstractAntUITest {

	public AntEditorTests(String name) {
		super(name);
	}

	public void testHoverForPath() throws PartInitException, BadLocationException {
		IFile file = getIFile("refid.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		XMLTextHover hover = new XMLTextHover(editor);
		int offset = getOffsetWithinLine(editor, 9, 20);
		IRegion region = hover.getHoverRegion(editor.getViewer(), offset);
		String hoverText = hover.getHoverInfo(editor.getViewer(), region);
		assertNotNull("Expecting a hovertext object", hoverText); //$NON-NLS-1$
		String correctResultRegEx = "<html><body text=\"#.*\" bgcolor=\"#.*\"><h5>Path Elements:</h5><ul><li>.*</html>"; //$NON-NLS-1$
		assertTrue("Expected the following hover text to match regex: " + correctResultRegEx, hoverText.matches(correctResultRegEx)); //$NON-NLS-1$
	}

	public void testHoverForProperty() throws PartInitException, BadLocationException {
		IFile file = getIFile("refid.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		XMLTextHover hover = new XMLTextHover(editor);
		int offset = getOffsetWithinLine(editor, 42, 13);
		IRegion region = hover.getHoverRegion(editor.getViewer(), offset);
		String hoverText = hover.getHoverInfo(editor.getViewer(), region);
		assertNotNull("Expecting a hovertext object", hoverText); //$NON-NLS-1$
		String correctResult = "<p>value with spaces</body></html>"; //$NON-NLS-1$
		assertTrue("Expected the following hover text to end with: " + correctResult, hoverText.endsWith(correctResult)); //$NON-NLS-1$
	}

	public void testPropertyOpenDeclaration() throws PartInitException, BadLocationException {
		IFile file = getIFile("refid.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		int offset = getOffsetWithinLine(editor, 42, 12);
		editor.selectAndReveal(offset, 2);

		editor.openReferenceElement();
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "property", selection.getText()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testPatternSetHover() throws PartInitException, BadLocationException {
		IFile file = getIFile("refid.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		XMLTextHover hover = new XMLTextHover(editor);
		int offset = getOffsetWithinLine(editor, 45, 25);
		IRegion region = hover.getHoverRegion(editor.getViewer(), offset);
		String hoverText = hover.getHoverInfo(editor.getViewer(), region);
		assertNotNull("Expecting a hovertext object", hoverText); //$NON-NLS-1$
		String correctResult = "<h5>Includes:</h5><li>*.xml</li><p><p><h5>Excludes:</h5><li>**/*Test*</li></body></html>"; //$NON-NLS-1$
		assertTrue("Expected the following hover text to end with: " + correctResult + "was: " + hoverText, hoverText.endsWith(correctResult)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testBadPatternSetHover() throws PartInitException, BadLocationException {
		IFile file = getIFile("refid.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		XMLTextHover hover = new XMLTextHover(editor);
		int offset = getOffsetWithinLine(editor, 46, 25);
		IRegion region = hover.getHoverRegion(editor.getViewer(), offset);
		String hoverText = hover.getHoverInfo(editor.getViewer(), region);
		assertNotNull("Expecting a hovertext object", hoverText); //$NON-NLS-1$
		String correctResult = "Ant UI Tests" + File.separatorChar + "buildfiles" + File.separatorChar + "nothere not found."; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("Expected the following hover text to ends with: " + correctResult, hoverText.endsWith(correctResult)); //$NON-NLS-1$
	}

	public void testFileSetHover() throws PartInitException, BadLocationException {
		IFile file = getIFile("refid.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		XMLTextHover hover = new XMLTextHover(editor);
		int offset = getOffsetWithinLine(editor, 44, 20);
		IRegion region = hover.getHoverRegion(editor.getViewer(), offset);
		String hoverText = hover.getHoverInfo(editor.getViewer(), region);
		assertNotNull("Expecting a hovertext object", hoverText); //$NON-NLS-1$
		assertTrue("Expected to see '<h5>Includes:</h5><li>include</li>'", hoverText.contains("<h5>Includes:</h5><li>include</li>")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Expected to see '<h5>Excludes:</h5><li>exclude</li>'", hoverText.contains("<h5>Excludes:</h5><li>exclude</li>")); //$NON-NLS-1$ //$NON-NLS-2$
		String text = "<li>**" + File.separator + "SCCS" + File.separator + "**</li>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("Expected to see '<li>**/SCCS/**</li>'", hoverText.contains(text)); //$NON-NLS-1$
		text = "<li>**" + File.separator + ".DS_Store</li>"; //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Expected to see '<li>**/.DS_Store</li>'", hoverText.contains(text)); //$NON-NLS-1$
		text = "<li>**" + File.separator + ".bzrignore</li>"; //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Expected to see '<li>**/.bzrignore</li>'", hoverText.contains(text)); //$NON-NLS-1$
		text = "<li>**" + File.separator + ".gitattributes</li>"; //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Expected to see '<li>**/.gitattributes</li>'", hoverText.contains(text)); //$NON-NLS-1$
		text = "<li>**" + File.separator + ".hgtags</li>"; //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Expected to see '<li>**/.hgtags</li>'", hoverText.contains(text)); //$NON-NLS-1$
	}

	public void testTaskdefOpenDeclaration() throws PartInitException, BadLocationException {
		IFile file = getIFile("taskdef.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		int offset = getOffsetWithinLine(editor, 9, 3);
		editor.selectAndReveal(offset, 0);

		editor.openReferenceElement();
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "taskdef", selection.getText()); //$NON-NLS-1$ //$NON-NLS-2$

		offset = getOffsetWithinLine(editor, 9, 10);
		editor.selectAndReveal(offset, 3);

		editor.openReferenceElement();
		selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "taskdef", selection.getText()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testMacroDefOpenDeclaration() throws PartInitException, BadLocationException {
		IFile file = getIFile("macrodef.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		int offset = getOffsetWithinLine(editor, 12, 9);
		editor.selectAndReveal(offset, 0);

		editor.openReferenceElement();
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "macrodef", selection.getText()); //$NON-NLS-1$ //$NON-NLS-2$

		offset = getOffsetWithinLine(editor, 14, 9);
		editor.selectAndReveal(offset, 1);

		editor.openReferenceElement();
		selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "macrodef", selection.getText()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * from bug 98853
	 */
	public void testMacroDefOpenDeclarationWithURI() throws PartInitException, BadLocationException {
		IFile file = getIFile("98853.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		int offset = getOffsetWithinLine(editor, 17, 9);
		editor.selectAndReveal(offset, 0);

		editor.openReferenceElement();
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct:", "macrodef", selection.getText()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Bug 95061
	 */
	public void testSelfClosingTagOpenDeclaration() throws PartInitException, BadLocationException {
		IFile file = getIFile("macrodef.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		int offset = getOffsetWithinLine(editor, 18, 10);
		editor.selectAndReveal(offset, 0);

		editor.openReferenceElement();
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "macrodef", selection.getText()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testMacroDefAttributeOpenDeclaration() throws PartInitException, BadLocationException {
		IFile file = getIFile("macrodef.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		int offset = getOffsetWithinLine(editor, 12, 16);
		editor.selectAndReveal(offset, 1);

		editor.openReferenceElement();
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "attribute", selection.getText()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testRefOpenDeclaration() throws PartInitException, BadLocationException {
		IFile file = getIFile("refid.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		int offset = getOffsetWithinLine(editor, 9, 20);
		editor.selectAndReveal(offset, 2);

		editor.openReferenceElement();
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "path", selection.getText()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testTargetOpenDeclaration() throws PartInitException, BadLocationException {
		IFile file = getIFile("refid.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		int offset = getOffsetWithinLine(editor, 0, 18);
		editor.selectAndReveal(offset, 1);

		editor.openReferenceElement();
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "target", selection.getText()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testExtensionPointOpenDeclaration() throws PartInitException, BadLocationException {
		IFile file = getIFile("antextpoint.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		int offset = getOffsetWithinLine(editor, 0, 18);
		editor.selectAndReveal(offset, 4);

		editor.openReferenceElement();
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "extension-point", selection.getText()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that the augment task can open in the Ant editor
	 * 
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=377075
	 * @throws Exception
	 */
	public void testAugmentOpenInEditor() throws Exception {
		IFile file = getIFile("bug377075.ent"); //$NON-NLS-1$
		IEditorPart part = EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		assertTrue("The opened editor must be the AntEditor", part instanceof AntEditor); //$NON-NLS-1$
	}

	/**
	 * Tests that the augment task can be shown and has the correct additions
	 * 
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=377075
	 * @throws Exception
	 */
	public void testAugmentOpenAndSelect() throws Exception {
		IFile file = getIFile("bug377075.ent"); //$NON-NLS-1$
		IEditorPart part = EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		assertTrue("The opened editor must be the AntEditor", part instanceof AntEditor); //$NON-NLS-1$
		AntEditor editor = (AntEditor) part;
		int offset = getOffsetWithinLine(editor, 5, 3);
		editor.selectAndReveal(offset, 7); // should have 'augment' selected
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "augment", selection.getText()); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that the element augmented by the augment task properly displays the augmented elements when hovering
	 * 
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=377075
	 * @throws Exception
	 */
	public void testAugmentOpenSelectHover() throws Exception {
		IFile file = getIFile("bug377075.ent"); //$NON-NLS-1$
		IEditorPart part = EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		assertTrue("The opened editor must be the AntEditor", part instanceof AntEditor); //$NON-NLS-1$
		AntEditor editor = (AntEditor) part;
		int offset = getOffsetWithinLine(editor, 1, 11);
		editor.selectAndReveal(offset, 5); // select the 'path1' id for the path element
		ITextSelection selection = (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "path1", selection.getText()); //$NON-NLS-1$ //$NON-NLS-2$
		XMLTextHover hover = new XMLTextHover(editor);
		IRegion region = hover.getHoverRegion(editor.getViewer(), offset);
		assertNotNull("The selected region for the augmented element cannot be null", region); //$NON-NLS-1$
		String text = hover.getHoverInfo(editor.getViewer(), region);
		assertNotNull("The hover text for the path element must not be null", text); //$NON-NLS-1$
		assertTrue("The hover text must contain the augmented element 'foo'", text.contains("foo")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests that the Ant editor is resilient to using an Augment task that references an unknown id
	 * 
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=396219
	 * @throws Exception
	 */
	public void testAugmentMissingId() throws Exception {
		IFile file = getIFile("bug396219.ent"); //$NON-NLS-1$
		IEditorPart part = EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		assertTrue("The opened editor must be the AntEditor", part instanceof AntEditor); //$NON-NLS-1$
	}

	public void testHoverRegionWithSpaces() throws PartInitException, BadLocationException {
		IFile file = getIFile("refid.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		XMLTextHover hover = new XMLTextHover(editor);
		// in the middle of the "compile" target of the depends attribute
		int offset = getOffsetWithinLine(editor, 22, 39);
		IRegion region = hover.getHoverRegion(editor.getViewer(), offset);

		assertNotNull(region);
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		String text = doc.get(region.getOffset(), region.getLength());
		assertTrue("Region incorrect. Expected length of 7 and text of \"compile\", length was " + region.getLength() + " and text was " //$NON-NLS-1$ //$NON-NLS-2$
				+ text, region.getLength() == 7 && "compile".equals(text)); //$NON-NLS-1$
	}

	public void testOpenImport() throws PartInitException {
		IFile file = getIFile("import.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$

		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());

		assertNotNull("Should have a document", doc); //$NON-NLS-1$
	}

	public void testOpenImportViaClasspath() throws PartInitException {
		IFile file = getIFile("importViaClasspath.xml"); //$NON-NLS-1$
		AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		IDocument doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		assertNotNull("Should have a document", doc); //$NON-NLS-1$

		file = getIFile("BaseBaseBuild.xml"); //$NON-NLS-1$
		editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		assertNotNull("Should have a document", doc); //$NON-NLS-1$

		file = getIFile("BaseBuild.xml"); //$NON-NLS-1$
		editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		assertNotNull("Should have a document", doc); //$NON-NLS-1$

		file = getIFile("Base.xml"); //$NON-NLS-1$
		editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
		doc = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		assertNotNull("Should have a document", doc); //$NON-NLS-1$
	}

	/**
	 * bug 195840 Import a XML file with BOM character in ant editor fails Runs on 1.5 vms or newer.
	 */
	public void testOpenImportWithByteOrderMark() throws PartInitException {
		if (ProjectCreationDecorator.isJ2SE15Compatible()) {
			IFile file = getIFile("importWithByteOrderMark.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
			assertNotNull("Should have imported target", editor.getAntModel().getTargetNode("build")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private int getOffsetWithinLine(AntEditor editor, int lineNumber, int offsetInLine) throws BadLocationException {
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		int offset = document.getLineOffset(lineNumber) + offsetInLine;
		return offset;
	}

	@Override
	protected void tearDown() throws Exception {
		EditorTestHelper.closeAllEditors();
		super.tearDown();
	}
}
