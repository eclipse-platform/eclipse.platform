/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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
package org.eclipse.ant.tests.ui.editor;

import java.util.List;

import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.ant.internal.ui.editor.OccurrencesFinder;
import org.eclipse.ant.tests.ui.editor.performance.EditorTestHelper;
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.ui.PartInitException;

@SuppressWarnings("restriction")
public class OccurrencesFinderTests extends AbstractAntUITest {

	public OccurrencesFinderTests(String name) {
		super(name);
	}

	public void testFromPropertyName() throws PartInitException, BadLocationException {
		try {
			IFile file = getIFile("occurrencesTest.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
			int offset = getOffsetWithinLine(editor, 4, 18);
			editor.selectAndReveal(offset, 0);

			propertyOccurences(editor, offset);
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}

	private void propertyOccurences(AntEditor editor, int offset) throws BadLocationException {
		OccurrencesFinder finder = getOccurrencesFinder(editor, offset);
		List<Position> positions = finder.perform();
		assertNotNull("Expecting a position listing", positions); //$NON-NLS-1$
		assertTrue("7 positions should have been found; found: " + positions.size(), positions.size() == 7); //$NON-NLS-1$
		assertContainsPosition(positions, offset, 8);
		int newoffset = getOffsetWithinLine(editor, 34, 21);
		assertContainsPosition(positions, newoffset, 8);
	}

	public void testFromPropertyLocation() throws PartInitException, BadLocationException {
		try {
			IFile file = getIFile("occurrencesTest.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
			int offset = getOffsetWithinLine(editor, 6, 55);
			editor.selectAndReveal(offset, 0);

			propertyOccurences(editor, offset);
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}

	public void testFromPropertyValue() throws PartInitException, BadLocationException {
		try {
			IFile file = getIFile("occurrencesTest.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
			int offset = getOffsetWithinLine(editor, 39, 44);
			editor.selectAndReveal(offset, 0);

			propertyOccurences(editor, offset);
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}

	public void testPropertyRefFromTaskText() throws PartInitException, BadLocationException {
		try {
			IFile file = getIFile("occurrencesTest.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
			// from property name
			int offset = getOffsetWithinLine(editor, 39, 20);
			editor.selectAndReveal(offset, 50);
			OccurrencesFinder finder = getOccurrencesFinder(editor, offset);
			List<Position> positions = finder.perform();
			assertNotNull("Expecting a position listing", positions); //$NON-NLS-1$
			assertTrue("3 positions should have been found; found: " + positions.size(), positions.size() == 3); //$NON-NLS-1$
			assertContainsPosition(positions, offset, 15);
			offset = getOffsetWithinLine(editor, 40, 20);
			assertContainsPosition(positions, offset, 15);

			// from echo text
			offset = getOffsetWithinLine(editor, 40, 20);
			editor.selectAndReveal(offset, 10);
			finder = getOccurrencesFinder(editor, offset);
			positions = finder.perform();
			assertNotNull("Expecting a position listing", positions); //$NON-NLS-1$
			assertTrue("3 positions should have been found; found: " + positions.size(), positions.size() == 3); //$NON-NLS-1$
			assertContainsPosition(positions, offset, 15);
			offset = getOffsetWithinLine(editor, 39, 20);
			assertContainsPosition(positions, offset, 15);
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}

	public void testFromMacrodefAttributeDecl() throws PartInitException, BadLocationException {
		try {
			IFile file = getIFile("occurrencesTest.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
			int offset = getOffsetWithinLine(editor, 13, 25);
			editor.selectAndReveal(offset, 0);

			macrodefAttributeOccurences(editor, offset);

		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}

	private void macrodefAttributeOccurences(AntEditor editor, int offset) throws BadLocationException {
		OccurrencesFinder finder = getOccurrencesFinder(editor, offset);
		List<Position> positions = finder.perform();
		assertTrue("6 positions should have been found; found: " + positions.size(), positions.size() == 6); //$NON-NLS-1$
		assertContainsPosition(positions, offset, 7);
		int newoffset = getOffsetWithinLine(editor, 19, 32);
		assertContainsPosition(positions, newoffset, 7);
	}

	public void testFromMacrodefAttributeRef() throws PartInitException, BadLocationException {
		try {
			IFile file = getIFile("occurrencesTest.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
			int offset = getOffsetWithinLine(editor, 17, 23);
			editor.selectAndReveal(offset, 0);

			macrodefAttributeOccurences(editor, offset);
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}

	public void testTargetFromAnt() throws PartInitException, BadLocationException {
		try {
			IFile file = getIFile("occurrencesTest.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
			int offset = getOffsetWithinLine(editor, 42, 16);
			editor.selectAndReveal(offset, 0);

			OccurrencesFinder finder = getOccurrencesFinder(editor, offset);
			List<Position> positions = finder.perform();
			assertTrue("4 positions should have been found; found: " + positions.size(), positions.size() == 4); //$NON-NLS-1$
			assertContainsPosition(positions, offset, 7);
			offset = getOffsetWithinLine(editor, 10, 16);
			assertContainsPosition(positions, offset, 7);
			offset = getOffsetWithinLine(editor, 0, 34);
			assertContainsPosition(positions, offset, 7);
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}

	public void testTargetFromAntCall() throws PartInitException, BadLocationException {
		try {
			IFile file = getIFile("occurrencesTest.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
			int offset = getOffsetWithinLine(editor, 43, 19);
			editor.selectAndReveal(offset, 0);

			OccurrencesFinder finder = getOccurrencesFinder(editor, offset);
			List<Position> positions = finder.perform();
			assertTrue("4 positions should have been found; found: " + positions.size(), positions.size() == 4); //$NON-NLS-1$
			assertContainsPosition(positions, offset, 7);
			offset = getOffsetWithinLine(editor, 10, 16);
			assertContainsPosition(positions, offset, 7);
			offset = getOffsetWithinLine(editor, 0, 34);
			assertContainsPosition(positions, offset, 7);
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}

	/**
	 * bug 89115
	 */
	public void testTargetFromProjectDefault() throws PartInitException, BadLocationException {
		try {
			IFile file = getIFile("89115.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
			int offset = getOffsetWithinLine(editor, 0, 40);
			editor.selectAndReveal(offset, 0);

			OccurrencesFinder finder = getOccurrencesFinder(editor, offset);
			List<Position> positions = finder.perform();
			assertTrue("2 positions should have been found; found: " + positions.size(), positions.size() == 2); //$NON-NLS-1$
			assertContainsPosition(positions, offset, 7);
			offset = getOffsetWithinLine(editor, 1, 45);
			assertContainsPosition(positions, offset, 7);
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}

	/**
	 * bug 89115
	 * 
	 * @throws PartInitException
	 * @throws BadLocationException
	 */
	public void testTargetFromTargetDepends() throws PartInitException, BadLocationException {
		try {
			IFile file = getIFile("89115.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
			int offset = getOffsetWithinLine(editor, 1, 61);
			editor.selectAndReveal(offset, 0);

			OccurrencesFinder finder = getOccurrencesFinder(editor, offset);
			// from the declaration
			List<Position> positions = finder.perform();
			assertTrue("2 positions should have been found; found: " + positions.size(), positions.size() == 2); //$NON-NLS-1$
			assertContainsPosition(positions, offset, 7);
			offset = getOffsetWithinLine(editor, 5, 20);
			assertContainsPosition(positions, offset, 7);

			// check from the reference
			offset = getOffsetWithinLine(editor, 5, 20);
			finder = getOccurrencesFinder(editor, offset);
			positions = finder.perform();
			assertTrue("2 positions should have been found; found: " + positions.size(), positions.size() == 2); //$NON-NLS-1$

			// from the description
			offset = getOffsetWithinLine(editor, 1, 30);
			editor.selectAndReveal(offset, 0);
			finder = getOccurrencesFinder(editor, offset);
			positions = finder.perform();
			assertTrue("No positions should have been found", positions == null); //$NON-NLS-1$
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}

	/**
	 * bug 94128
	 * 
	 * @throws PartInitException
	 * @throws BadLocationException
	 */
	public void testTargetFromTargetIf() throws PartInitException, BadLocationException {
		try {
			IFile file = getIFile("occurrencesTest.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
			int offset = getOffsetWithinLine(editor, 46, 65);
			editor.selectAndReveal(offset, 0);

			propertyOccurences(editor, offset);
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}

	public void testNoRefFromProjectDefault() throws PartInitException, BadLocationException {
		try {
			IFile file = getIFile("89115.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
			int offset = getOffsetWithinLine(editor, 0, 30);
			editor.selectAndReveal(offset, 0);

			OccurrencesFinder finder = getOccurrencesFinder(editor, offset);
			List<Position> positions = finder.perform();
			assertTrue("No positions should have been found", positions == null); //$NON-NLS-1$
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}

	/**
	 * Current limitation With properties and targets with the same name, referencing from echo text will find the target if it occurs first in the
	 * buildfile. Logged as bug 94123
	 */
	// public void testPropertyRefFromTaskText2() throws PartInitException, BadLocationException {
	// try {
	// IFile file= getIFile("89901.xml");
	// AntEditor editor= (AntEditor)EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true);
	// int offset = getOffsetWithinLine(editor, 7, 12);
	// editor.selectAndReveal(offset, 0);
	//
	// OccurrencesFinder finder= getOccurrencesFinder(editor, offset);
	// List positions= finder.perform();
	// assertTrue("2 positions should have been found; found: " + positions.size(), positions.size() == 2);
	// assertContainsPosition(positions, offset, 4);
	// offset = getOffsetWithinLine(editor, 3, 26);
	// assertContainsPosition(positions, offset, 4);
	// } finally {
	// EditorTestHelper.closeAllEditors();
	// }
	// }

	/**
	 * Bug 89901
	 */
	public void testPropertyAndTargetWithSameName() throws PartInitException, BadLocationException {
		try {
			IFile file = getIFile("89901.xml"); //$NON-NLS-1$

			// from the test target
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, "org.eclipse.ant.ui.internal.editor.AntEditor", true); //$NON-NLS-1$
			int offset = getOffsetWithinLine(editor, 2, 38);
			editor.selectAndReveal(offset, 0);

			OccurrencesFinder finder = getOccurrencesFinder(editor, offset);
			List<Position> positions = finder.perform();
			assertTrue("2 positions should have been found; found: " + positions.size(), positions.size() == 2); //$NON-NLS-1$
			assertContainsPosition(positions, offset, 4);
			offset = getOffsetWithinLine(editor, 6, 17);
			assertContainsPosition(positions, offset, 4);

			// from the test property
			offset = getOffsetWithinLine(editor, 3, 26);
			editor.selectAndReveal(offset, 0);
			finder = getOccurrencesFinder(editor, offset);
			positions = finder.perform();
			assertTrue("2 positions should have been found; found: " + positions.size(), positions.size() == 2); //$NON-NLS-1$
			assertContainsPosition(positions, offset, 4);
			offset = getOffsetWithinLine(editor, 7, 12);
			assertContainsPosition(positions, offset, 4);
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}

	private void assertContainsPosition(List<Position> positions, int offset, int length) {
		boolean found = false;
		for (Position position : positions) {
			if (position.getLength() == length && position.getOffset() <= offset && (position.getOffset() + length) > offset) {
				found = true;
				break;
			}
		}
		assertTrue("Did not find position at offset: " + offset + " length: " + length, found); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private OccurrencesFinder getOccurrencesFinder(AntEditor editor, int offset) {
		return new OccurrencesFinder(editor, editor.getAntModel(), editor.getDocumentProvider().getDocument(editor.getEditorInput()), offset);
	}

	/**
	 * 
	 * @param editor
	 * @param lineNumber
	 *            zero based
	 * @param offsetInLine
	 *            zero based
	 * @return the offset within the document of the editor based on the line number and offset within line
	 * @throws BadLocationException
	 */
	private int getOffsetWithinLine(AntEditor editor, int lineNumber, int offsetInLine) throws BadLocationException {
		IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		int offset = document.getLineOffset(lineNumber) + offsetInLine;
		return offset;
	}
}
