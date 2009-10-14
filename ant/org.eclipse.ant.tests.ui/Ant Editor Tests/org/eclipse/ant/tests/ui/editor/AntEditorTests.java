/*******************************************************************************
 * Copyright (c) 2004, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.editor;

import java.io.File;

import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.ant.internal.ui.editor.text.XMLTextHover;
import org.eclipse.ant.tests.ui.editor.performance.EditorTestHelper;
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ui.PartInitException;

public class AntEditorTests extends AbstractAntUITest {

    public AntEditorTests(String name) {
        super(name);
    }
    
    public void testHoverForPath() throws PartInitException, BadLocationException {
        IFile file= getIFile("refid.xml");
		AntEditor editor= (AntEditor)EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true);
		XMLTextHover hover= new XMLTextHover(editor);
		int offset= getOffsetWithinLine(editor, 9, 20);
		IRegion region= hover.getHoverRegion(editor.getViewer(), offset);
		String hoverText= hover.getHoverInfo(editor.getViewer(), region);
		String correctResultRegEx= "<html><body text=\"#.*\" bgcolor=\"#.*\"><h5>Path Elements:</h5><ul><li>.*</html>";
		assertTrue("Expected the following hover text to match regex: " + correctResultRegEx, hoverText.matches(correctResultRegEx));
    }
    
    public void testHoverForProperty() throws PartInitException, BadLocationException {
        IFile file= getIFile("refid.xml");
		AntEditor editor= (AntEditor)EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true);
		XMLTextHover hover= new XMLTextHover(editor);
		int offset= getOffsetWithinLine(editor, 42, 13);
		IRegion region= hover.getHoverRegion(editor.getViewer(), offset);
		String hoverText= hover.getHoverInfo(editor.getViewer(), region);
		String correctResult= "<p>value with spaces</font></body></html>";
		assertTrue("Expected the following hover text to end with: " + correctResult, hoverText.endsWith(correctResult));
    }
    
    public void testPropertyOpenDeclaration() throws PartInitException, BadLocationException {
        IFile file= getIFile("refid.xml");
		AntEditor editor= (AntEditor)EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true);
		int offset = getOffsetWithinLine(editor, 42, 12);
		editor.selectAndReveal(offset, 2);
		
		editor.openReferenceElement();
		ITextSelection selection= (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "property", selection.getText());
    }
    
    public void testPatternSetHover() throws PartInitException, BadLocationException {
        IFile file= getIFile("refid.xml");
		AntEditor editor= (AntEditor)EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true);
		XMLTextHover hover= new XMLTextHover(editor);
		int offset= getOffsetWithinLine(editor, 45, 25);
		IRegion region= hover.getHoverRegion(editor.getViewer(), offset);
		String hoverText= hover.getHoverInfo(editor.getViewer(), region);
		String correctResult= "<h5>Includes:</h5><li>*.xml</li><p><p><h5>Excludes:</h5><li>**/*Test*</li></font></body></html>";
		assertTrue("Expected the following hover text to end with: " + correctResult + "was: " + hoverText, hoverText.endsWith(correctResult));
    }
    
    public void testBadPatternSetHover() throws PartInitException, BadLocationException {
        IFile file= getIFile("refid.xml");
		AntEditor editor= (AntEditor)EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true);
		XMLTextHover hover= new XMLTextHover(editor);
		int offset= getOffsetWithinLine(editor, 46, 25);
		IRegion region= hover.getHoverRegion(editor.getViewer(), offset);
		String hoverText= hover.getHoverInfo(editor.getViewer(), region);
		String correctResult= "Ant UI Tests" + File.separatorChar + "buildfiles" + File.separatorChar + "nothere not found.";
		assertTrue("Expected the following hover text to ends with: " + correctResult, hoverText.endsWith(correctResult));
    }
    
    public void testFileSetHover() throws PartInitException, BadLocationException {
        IFile file= getIFile("refid.xml");
		AntEditor editor= (AntEditor)EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true);
		XMLTextHover hover= new XMLTextHover(editor);
		int offset= getOffsetWithinLine(editor, 44, 20);
		IRegion region= hover.getHoverRegion(editor.getViewer(), offset);
		String hoverText= hover.getHoverInfo(editor.getViewer(), region);
		
		String correctResult= "<h5>Includes:</h5><li>include</li><p><p><h5>Excludes:</h5><li>exclude</li><li>**" + 
			File.separatorChar + "*~</li><li>**" 
			+ File.separatorChar
			+ "#*#</li><li>**" + 
			File.separatorChar + 
			".#*</li><li>**" +
			File.separatorChar + 
			"%*%</li><li>**" + 
			File.separatorChar +
			"._*</li><li>**" + 
			File.separatorChar + 
			"CVS</li><li>**" +
			File.separatorChar + 
			"CVS" + File.separatorChar + "**</li><li>**" + 
			File.separatorChar + ".cvsignore</li><li>**" + 
			File.separatorChar + "SCCS</li><li>**" + File.separatorChar + 
			"SCCS" + File.separatorChar + "**</li><li>**" + File.separatorChar + 
			"vssver.scc</li><li>**" + File.separatorChar + 
			".svn</li><li>**" + File.separatorChar + ".svn" + File.separatorChar + 
			"**</li><li>**" + File.separatorChar + ".DS_Store</li></font></body></html>";
		assertTrue("Expected the following hover text to be: " + correctResult + " Was " + hoverText, hoverText.endsWith(correctResult));
    }
    
    
    public void testTaskdefOpenDeclaration() throws PartInitException, BadLocationException {
        IFile file= getIFile("taskdef.xml");
		AntEditor editor= (AntEditor)EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true);
		int offset = getOffsetWithinLine(editor, 9, 3);
		editor.selectAndReveal(offset, 0);
		
		editor.openReferenceElement();
		ITextSelection selection= (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "taskdef", selection.getText());
		
		offset = getOffsetWithinLine(editor, 9, 10);
		editor.selectAndReveal(offset, 3);
		
		editor.openReferenceElement();
		selection= (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "taskdef", selection.getText());
    }
    
    public void testMacroDefOpenDeclaration() throws PartInitException, BadLocationException {
    	IFile file= getIFile("macrodef.xml");
		AntEditor editor= (AntEditor)EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true);
		int offset = getOffsetWithinLine(editor, 12, 9);
		editor.selectAndReveal(offset, 0);
		
		editor.openReferenceElement();
		ITextSelection selection= (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "macrodef", selection.getText());
		
		offset = getOffsetWithinLine(editor, 14, 9);
		editor.selectAndReveal(offset, 1);
		
		editor.openReferenceElement();
		selection= (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "macrodef", selection.getText());
    }
    
    /**
     * from bug 98853
     */
    public void testMacroDefOpenDeclarationWithURI() throws PartInitException, BadLocationException {
        IFile file= getIFile("98853.xml");
		AntEditor editor= (AntEditor)EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true);
		int offset = getOffsetWithinLine(editor, 17, 9);
		editor.selectAndReveal(offset, 0);
		
		editor.openReferenceElement();
		ITextSelection selection= (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct:", "macrodef", selection.getText());
    }
    
    /**
     * Bug 95061
     */
    public void testSelfClosingTagOpenDeclaration() throws PartInitException, BadLocationException {
        IFile file= getIFile("macrodef.xml");
		AntEditor editor= (AntEditor)EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true);
		int offset = getOffsetWithinLine(editor, 18, 10);
		editor.selectAndReveal(offset, 0);
		
		editor.openReferenceElement();
		ITextSelection selection= (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "macrodef", selection.getText());
    }
    
    public void testMacroDefAttributeOpenDeclaration() throws PartInitException, BadLocationException {
    	IFile file= getIFile("macrodef.xml");
		AntEditor editor= (AntEditor)EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true);
		int offset = getOffsetWithinLine(editor, 12, 16);
		editor.selectAndReveal(offset, 1);
		
		editor.openReferenceElement();
		ITextSelection selection= (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "attribute", selection.getText());
    }
    
    public void testRefOpenDeclaration() throws PartInitException, BadLocationException {
    	IFile file= getIFile("refid.xml");
		AntEditor editor= (AntEditor)EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true);
		int offset = getOffsetWithinLine(editor, 9, 20);
		editor.selectAndReveal(offset, 2);
		
		editor.openReferenceElement();
		ITextSelection selection= (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "path", selection.getText());
    }
    
    public void testTargetOpenDeclaration() throws PartInitException, BadLocationException {
    	IFile file= getIFile("refid.xml");
		AntEditor editor= (AntEditor)EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true);
		int offset = getOffsetWithinLine(editor, 0, 18);
		editor.selectAndReveal(offset, 1);
		
		editor.openReferenceElement();
		ITextSelection selection= (ITextSelection) editor.getSelectionProvider().getSelection();
		assertEquals("Selection is not correct", "target", selection.getText());
    }
    
    public void testHoverRegionWithSpaces() throws PartInitException, BadLocationException {
    	IFile file= getIFile("refid.xml");
    	AntEditor editor= (AntEditor)EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true);
    	XMLTextHover hover= new XMLTextHover(editor);
    	//in the middle of the "compile" target of the depends attribute
    	int offset= getOffsetWithinLine(editor, 22, 39);
    	IRegion region= hover.getHoverRegion(editor.getViewer(), offset);

    	assertNotNull(region);
    	IDocument doc= editor.getDocumentProvider().getDocument(editor.getEditorInput());
    	String text= doc.get(region.getOffset(), region.getLength());
    	assertTrue("Region incorrect. Expected length of 7 and text of \"compile\", length was " + region.getLength() + " and text was " + text, region.getLength() == 7 && "compile".equals(text));            
    }

    public void testOpenImport() throws PartInitException {
    	IFile file= getIFile("import.xml");
    	AntEditor editor= (AntEditor)EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true);
    	
    	IDocument doc= editor.getDocumentProvider().getDocument(editor.getEditorInput());
    	
    	assertNotNull("Should have a document", doc);            
    }

    
	private int getOffsetWithinLine(AntEditor editor, int lineNumber, int offsetInLine) throws BadLocationException {
		IDocument document= editor.getDocumentProvider().getDocument(editor.getEditorInput());
		int offset= document.getLineOffset(lineNumber) + offsetInLine;
		return offset;
	}
	
	/* (non-Javadoc)
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		EditorTestHelper.closeAllEditors();
		super.tearDown();
	}
}
