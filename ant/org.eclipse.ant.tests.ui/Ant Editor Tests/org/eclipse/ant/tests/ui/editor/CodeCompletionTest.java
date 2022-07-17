/*******************************************************************************
 * Copyright (c) 2002, 2014 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH, 
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 *
 * This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial implementation
 * 	   IBM Corporation - additional tests
 *     Remy Chi Jian Suen - bug 277587
 *******************************************************************************/

package org.eclipse.ant.tests.ui.editor;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.Property;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.AntUIImages;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.ant.tests.ui.editor.performance.EditorTestHelper;
import org.eclipse.ant.tests.ui.editor.support.TestTextCompletionProcessor;
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ui.PartInitException;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests everything about code completion and code assistance.
 * 
 */
@SuppressWarnings("restriction")
public class CodeCompletionTest extends AbstractAntUITest {

	/**
	 * Constructor for CodeCompletionTest.
	 * 
	 * @param name
	 */
	public CodeCompletionTest(String name) {
		super(name);
	}

	/**
	 * Tests the code completion for attributes of tasks.
	 */
	public void testAttributeProposals() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

		ICompletionProposal[] proposals = processor.getAttributeProposals("contains", "ca"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(1, proposals.length);
		assertEquals("casesensitive - (true | false | on | off | yes | no)", proposals[0].getDisplayString()); //$NON-NLS-1$

		proposals = processor.getAttributeProposals("move", ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(17, proposals.length);
		ICompletionProposal proposal = proposals[0];
		String displayString = proposal.getDisplayString();
		assertTrue(displayString.equals("id") //$NON-NLS-1$
				|| displayString.equals("taskname") //$NON-NLS-1$
				|| displayString.equals(IAntCoreConstants.DESCRIPTION) || displayString.equals(IAntCoreConstants.FILE)
				|| displayString.equals("preservelastmodified - (true | false | on | off | yes | no)") //$NON-NLS-1$
				|| displayString.equals("tofile") //$NON-NLS-1$
				|| displayString.equals("todir") //$NON-NLS-1$
				|| displayString.equals("overwrite - (true | false | on | off | yes | no)") //$NON-NLS-1$
				|| displayString.equals("filtering - (true | false | on | off | yes | no)") //$NON-NLS-1$
				|| displayString.equals("flatten - (true | false | on | off | yes | no)") //$NON-NLS-1$
				|| displayString.equals("includeemptydirs - (true | false | on | off | yes | no)") //$NON-NLS-1$
				|| displayString.equals("failonerror - (true | false | on | off | yes | no)") //$NON-NLS-1$
				|| displayString.equals("verbose - (true | false | on | off | yes | no)") //$NON-NLS-1$
				|| displayString.equals("encoding") //$NON-NLS-1$
				|| displayString.equals("outputencoding") //$NON-NLS-1$
				|| displayString.equals("enablemultiplemapping - (true | false | on | off | yes | no)") //$NON-NLS-1$
				|| displayString.equals("granularity")); //$NON-NLS-1$

		proposals = processor.getAttributeProposals("move", "to"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(2, proposals.length);

		proposals = processor.getAttributeProposals("reference", "idl"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(0, proposals.length);

		proposals = processor.getAttributeProposals("reference", "id"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(1, proposals.length);
		assertEquals("id", proposals[0].getDisplayString()); //$NON-NLS-1$

		proposals = processor.getAttributeProposals("reference", "i"); //$NON-NLS-1$ //$NON-NLS-2$
		// id includesfile includes
		assertEquals(3, proposals.length);
		displayString = proposals[0].getDisplayString();
		assertTrue(displayString.equals("id") //$NON-NLS-1$
				|| displayString.equals("includesfile") //$NON-NLS-1$
				|| displayString.equals("includes")); //$NON-NLS-1$

		proposals = processor.getAttributeProposals("project", "de"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(1, proposals.length);
	}

	/**
	 * Test the code completion for properties, including unquoted (bug 40871)
	 */
	public void testPropertyProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest1.xml")); //$NON-NLS-1$

		int lineNumber = 7;
		int columnNumber = 16;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getPropertyProposals(getCurrentDocument(), "", lineOffset + columnNumber); //$NON-NLS-1$
		assertTrue(proposals.length >= 1);
		assertContains("prop1", proposals); //$NON-NLS-1$

		lineNumber = 18;
		columnNumber = 25;
		lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		proposals = processor.getPropertyProposals(getCurrentDocument(), "", lineOffset + columnNumber); //$NON-NLS-1$
		assertTrue(proposals.length >= 1);
		assertContains("prop2", proposals); //$NON-NLS-1$
	}

	/**
	 * Tests the code completion for nested elements that no templates are presented Bug 76414
	 */
	public void testPropertyTemplateProposals() throws BadLocationException, PartInitException {
		try {
			IFile file = getIFile("buildtest1.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, ANT_EDITOR_ID, true);
			TestTextCompletionProcessor processor = new TestTextCompletionProcessor(editor);
			int lineNumber = 7;
			int columnNumber = 16;
			int lineOffset = editor.getDocumentProvider().getDocument(editor.getEditorInput()).getLineOffset(lineNumber);
			processor.setLineNumber(lineNumber);
			processor.setColumnNumber(columnNumber);
			processor.setCursorPosition(lineOffset + columnNumber);

			ICompletionProposal[] proposals = processor.determineTemplateProposals();
			assertTrue("No templates are relevant at the current position. Found: " + proposals.length, proposals.length == 0); //$NON-NLS-1$
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}

	/**
	 * Test the code completion for "system" properties
	 */
	public void testSystemPropertyProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest1.xml")); //$NON-NLS-1$

		int lineNumber = 18;
		int columnNumber = 25;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getPropertyProposals(getCurrentDocument(), "", lineOffset + columnNumber); //$NON-NLS-1$
		assertTrue(proposals.length >= 1);
		assertContains("java.home", proposals); //$NON-NLS-1$
	}

	/**
	 * Test the code completion for "built-in" properties
	 */
	public void testBuiltInPropertyProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest1.xml")); //$NON-NLS-1$

		int lineNumber = 18;
		int columnNumber = 25;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getPropertyProposals(getCurrentDocument(), "", lineOffset + columnNumber); //$NON-NLS-1$
		assertTrue(proposals.length >= 1);
		assertContains("ant.file", proposals); //$NON-NLS-1$
		assertContains("ant.version", proposals); //$NON-NLS-1$
		assertContains("ant.project.name", proposals); //$NON-NLS-1$
		assertContains("basedir", proposals); //$NON-NLS-1$
		assertContains("ant.home", proposals); //$NON-NLS-1$
		assertContains("ant.library.dir", proposals); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Test the code completion for extension point / preference properties
	 */
	public void testPreferencePropertyProposals() throws BadLocationException {
		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		try {

			prefs.setCustomProperties(new Property[] { new Property("test", "result") }); //$NON-NLS-1$ //$NON-NLS-2$
			prefs.updatePluginPreferences();

			TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest1.xml")); //$NON-NLS-1$

			int lineNumber = 18;
			int columnNumber = 25;
			int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
			processor.setLineNumber(lineNumber);
			processor.setColumnNumber(columnNumber);
			processor.setCursorPosition(lineOffset + columnNumber);
			ICompletionProposal[] proposals = processor.getPropertyProposals(getCurrentDocument(), "", lineOffset + columnNumber); //$NON-NLS-1$
			assertTrue(proposals.length >= 3);
			assertContains("eclipse.home", proposals); // contributed //$NON-NLS-1$
			assertContains("property.ui.testing2", proposals); // contributed //$NON-NLS-1$
			assertContains("test", proposals); //$NON-NLS-1$
		}
		finally {
			prefs.setCustomProperties(new Property[] {});
			prefs.updatePluginPreferences();
		}
	}

	/**
	 * Test the code completion for the depend attribute of a target.
	 */
	public void testTargetDependProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest1.xml")); //$NON-NLS-1$
		// simple depends
		int lineNumber = 10;
		int columnNumber = 34;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getTargetAttributeValueProposals(getCurrentDocument(), getCurrentDocument().get(0, lineOffset
				+ columnNumber), "", "depends"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(proposals.length == 7);
		assertContains("pretest", proposals); //$NON-NLS-1$
		assertContains("testMoreDepends", proposals); //$NON-NLS-1$
		// comma separated depends
		lineNumber = 17;
		columnNumber = 53;
		lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		proposals = processor.getTargetAttributeValueProposals(getCurrentDocument(), getCurrentDocument().get(0, lineOffset
				+ columnNumber), "", "depends"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(proposals.length == 7);
		assertContains("main", proposals); //$NON-NLS-1$
		// XXX why do we not want a valid proposal?
		/* assertDoesNotContain("pretest", proposals); */
	}

	/**
	 * Test the image for a code completion proposal for the depend attribute of a target.
	 */
	public void testTargetDependProposalImages() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest3.xml")); //$NON-NLS-1$
		// simple depends
		int lineNumber = 5;
		int columnNumber = 34;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		assertEquals(3, proposals.length);
		assertContains("main", proposals); //$NON-NLS-1$
		assertContains("pretest", proposals); //$NON-NLS-1$
		assertContains("test2", proposals); //$NON-NLS-1$

		for (ICompletionProposal proposal : proposals) {
			String displayString = proposal.getDisplayString();
			if (displayString.equals("main")) { //$NON-NLS-1$
				assertEquals(AntUIImages.getImage(IAntUIConstants.IMG_ANT_DEFAULT_TARGET), proposal.getImage());
			} else if (displayString.equals("pretest")) { //$NON-NLS-1$
				assertEquals(AntUIImages.getImage(IAntUIConstants.IMG_ANT_TARGET), proposal.getImage());
			} else if (displayString.equals("test2")) { //$NON-NLS-1$
				assertEquals(AntUIImages.getImage(IAntUIConstants.IMG_ANT_TARGET_INTERNAL), proposal.getImage());
			} else {
				fail("Unknown completion proposal detected: " + displayString); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Test the image for a code completion proposal for the default attribute of a project.
	 */
	public void testProjectDefaultProposalImages() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest4.xml")); //$NON-NLS-1$
		// simple depends
		int lineNumber = 1;
		int columnNumber = 18;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		assertEquals(3, proposals.length);
		assertContains("task", proposals); //$NON-NLS-1$
		assertContains("task2", proposals); //$NON-NLS-1$
		assertContains("task3", proposals); //$NON-NLS-1$

		for (ICompletionProposal proposal : proposals) {
			String displayString = proposal.getDisplayString();
			if (displayString.equals("task3")) { //$NON-NLS-1$
				assertEquals(AntUIImages.getImage(IAntUIConstants.IMG_ANT_DEFAULT_TARGET), proposal.getImage());
			} else if (displayString.equals("task")) { //$NON-NLS-1$
				assertEquals(AntUIImages.getImage(IAntUIConstants.IMG_ANT_TARGET), proposal.getImage());
			} else if (displayString.equals("task2")) { //$NON-NLS-1$
				assertEquals(AntUIImages.getImage(IAntUIConstants.IMG_ANT_TARGET_INTERNAL), proposal.getImage());
			} else {
				fail("Unknown completion proposal detected: " + displayString); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Test the image for a code completion proposal for the target attribute of an antcall task.
	 */
	public void testAntcallTargetProposalImages() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest4.xml")); //$NON-NLS-1$
		int lineNumber = 4;
		int columnNumber = 25;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		assertEquals(2, proposals.length);
		assertContains("task", proposals); //$NON-NLS-1$
		assertContains("task3", proposals); //$NON-NLS-1$

		for (ICompletionProposal proposal : proposals) {
			String displayString = proposal.getDisplayString();
			if (displayString.equals("task3")) { //$NON-NLS-1$
				assertEquals(AntUIImages.getImage(IAntUIConstants.IMG_ANT_DEFAULT_TARGET), proposal.getImage());
			} else if (displayString.equals("task")) { //$NON-NLS-1$
				assertEquals(AntUIImages.getImage(IAntUIConstants.IMG_ANT_TARGET), proposal.getImage());
			} else {
				fail("Unknown completion proposal detected: " + displayString); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Test the code completion for the if attribute of a target.
	 */
	public void testTargetIfProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest1.xml")); //$NON-NLS-1$

		int lineNumber = 15;
		int columnNumber = 31;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getTargetAttributeValueProposals(getCurrentDocument(), getCurrentDocument().get(0, lineOffset
				+ columnNumber), "", "if"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(proposals.length >= 1);
		assertContains("prop1", proposals); //$NON-NLS-1$
	}

	/**
	 * Test the code completion for the unless attribute of a target.
	 */
	public void testTargetUnlessProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest1.xml")); //$NON-NLS-1$

		int lineNumber = 16;
		int columnNumber = 43;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getTargetAttributeValueProposals(getCurrentDocument(), getCurrentDocument().get(0, lineOffset
				+ columnNumber), "prop", "unless"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(proposals.length >= 1);
		assertContains("prop1", proposals); //$NON-NLS-1$

	}

	/**
	 * Test the code completion for the target attribute of antcall.
	 */
	public void testAntCallTargetProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("debugAntCall.xml")); //$NON-NLS-1$
		int lineNumber = 4;
		int columnNumber = 25;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getAntCallAttributeValueProposals(getCurrentDocument(), "", "target"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(proposals.length == 2);
		assertContains("call", proposals); //$NON-NLS-1$
		assertContains("pre-call", proposals); //$NON-NLS-1$
	}

	/**
	 * Asserts that <code>displayString</code> is in one of the completion proposals.
	 */
	private void assertContains(String displayString, ICompletionProposal[] proposalArray) {
		boolean found = false;
		for (ICompletionProposal proposal : proposalArray) {
			String proposalDisplayString = proposal.getDisplayString();
			if (displayString.equals(proposalDisplayString)) {
				found = true;
				break;
			}
		}
		assertTrue("Did not find displayString: " + displayString, found); //$NON-NLS-1$
	}

	/**
	 * Asserts that <code>displayString</code> is not in one of the completion proposals.
	 */
	private void assertDoesNotContain(String displayString, ICompletionProposal[] proposalArray) {
		boolean found = false;
		for (ICompletionProposal proposal : proposalArray) {
			String proposalDisplayString = proposal.getDisplayString();
			if (displayString.equals(proposalDisplayString)) {
				found = true;
				break;
			}
		}
		assertFalse("Found displayString: " + displayString, found); //$NON-NLS-1$
	}

	/**
	 * Tests the property proposals for the case that they are defined in a dependent targets.
	 */
	public void testPropertyProposalDefinedInDependantTargets() throws FileNotFoundException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("dependencytest.xml")); //$NON-NLS-1$

		File file = getBuildFile("dependencytest.xml"); //$NON-NLS-1$
		processor.setEditedFile(file);
		String documentText = getFileContentAsString(file);

		processor.setLineNumber(35);
		processor.setColumnNumber(41);
		int cursorPosition = documentText.lastIndexOf("${"); //$NON-NLS-1$
		assertTrue(cursorPosition != -1);
		ICompletionProposal[] proposals = processor.getPropertyProposals(new org.eclipse.jface.text.Document(documentText), "", cursorPosition + 2); //$NON-NLS-1$
		assertContains("init_prop", proposals); //$NON-NLS-1$
		assertContains("main_prop", proposals); //$NON-NLS-1$
		assertContains("prop_prop", proposals); //$NON-NLS-1$
		assertContains("do_not_compile", proposals); //$NON-NLS-1$
		assertContains("adit_prop", proposals); //$NON-NLS-1$
		assertContains("compile_prop", proposals); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Tests the code completion for tasks that have been defined in the buildfile
	 */
	public void testCustomTaskProposals() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("taskdef.xml")); //$NON-NLS-1$

		ICompletionProposal[] proposals = processor.getTaskProposals(getCurrentDocument(), "target", "min"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(1, proposals.length);
		ICompletionProposal proposal = proposals[0];
		assertEquals("mine", proposal.getDisplayString()); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Tests the code completion for tasks that have been defined via the task extension point
	 */
	public void testExtensionPointTaskProposals() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("taskdef.xml")); //$NON-NLS-1$
		ICompletionProposal[] proposals = processor.getTaskProposals(getCurrentDocument(), "target", "cool"); //$NON-NLS-1$ //$NON-NLS-2$
		assertContains("coolUITask", proposals); //$NON-NLS-1$
		assertContains("coolUIType", proposals); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Tests the code completion for tasks that have been defined via macrodef in the buildfile
	 */
	public void testMacrodefProposals() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("macrodef.xml")); //$NON-NLS-1$

		ICompletionProposal[] proposals = processor.getTaskProposals(getCurrentDocument(), "target", "eclipsema"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(1, proposals.length);
		ICompletionProposal proposal = proposals[0];
		assertEquals("eclipseMacro", proposal.getDisplayString()); //$NON-NLS-1$

	}

	/**
	 * Tests the code completion for tasks that have been defined via macrodef with uri in the buildfile
	 */
	public void testNamespacedMacrodefProposals() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("98853.xml")); //$NON-NLS-1$

		ICompletionProposal[] proposals = processor.getTaskProposals(getCurrentDocument(), "target", "xyz"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(2, proposals.length);
		ICompletionProposal proposal = proposals[0];
		assertEquals("xyz:echo-macro", proposal.getDisplayString()); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Tests the code completion for nested element attributes
	 */
	public void testMacrodefNestedElementAttributeProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("macrodef.xml")); //$NON-NLS-1$
		int lineNumber = 5;
		int columnNumber = 11;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		assertEquals(proposals.length, 5);
		assertContains(IAntCoreConstants.DESCRIPTION, proposals);
		assertContains("implicit - (true | false | on | off | yes | no)", proposals); //$NON-NLS-1$
		assertContains(IAntCoreConstants.NAME, proposals);
		processor.dispose();
	}

	/**
	 * Tests the code completion for tasks that have been defined via macrodef in the buildfile
	 */
	public void testMacrodefAttributeProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("macrodef.xml")); //$NON-NLS-1$
		int lineNumber = 12;
		int columnNumber = 16;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		assertTrue(proposals.length == 2);
		assertContains("v", proposals); //$NON-NLS-1$
		assertContains("eclipse", proposals); //$NON-NLS-1$
		assertTrue("Additional proposal information not correct", proposals[1].getAdditionalProposalInfo().startsWith("Testing Eclipse")); //$NON-NLS-1$ //$NON-NLS-2$
		processor.dispose();
	}

	/**
	 * Tests the code completion for tasks that have been defined via macrodef in the buildfile
	 */
	public void testNamespacedMacrodefAttributeProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("98853.xml")); //$NON-NLS-1$
		int lineNumber = 16;
		int columnNumber = 18;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		assertEquals("There should be one completion proposal", proposals.length, 1); //$NON-NLS-1$
		assertContains("str", proposals); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Tests the code completion for elements that have been defined via macrodef in the buildfile
	 */
	public void testMacrodefElementProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("macrodef.xml")); //$NON-NLS-1$
		int lineNumber = 13;
		int columnNumber = 3;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		assertEquals("There should be 1 completion proposal", proposals.length, 1); //$NON-NLS-1$
		assertTrue("Proposal not correct", proposals[0].getDisplayString().equals("some-tasks")); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Additional proposal information not correct", proposals[0].getAdditionalProposalInfo().endsWith("Not required")); //$NON-NLS-1$ //$NON-NLS-2$
		processor.dispose();
	}

	/**
	 * Tests the code completion for tasks having parent tasks.
	 */
	public void testTaskProposals() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest1.xml")); //$NON-NLS-1$

		ICompletionProposal[] proposals = processor.getTaskProposals("         <", "rename", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals(0, proposals.length);

		proposals = processor.getTaskProposals("       <cl", "property", "cl"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals(1, proposals.length);
		ICompletionProposal proposal = proposals[0];
		assertEquals("classpath", proposal.getDisplayString()); //$NON-NLS-1$

		proposals = processor.getTaskProposals("       <pr", "property", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals(1, proposals.length);
		proposal = proposals[0];
		assertEquals("classpath", proposal.getDisplayString()); //$NON-NLS-1$

		// "<project><target><mk"
		proposals = processor.getTaskProposals("<project><target><mk", "target", "mk"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals(1, proposals.length);
		proposal = proposals[0];
		assertEquals("mkdir", proposal.getDisplayString()); //$NON-NLS-1$
		processor.dispose();
	}

	public void testTargetTemplateProposals() throws BadLocationException, PartInitException {
		try {
			IFile file = getIFile("buildtest1.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, ANT_EDITOR_ID, true);
			TestTextCompletionProcessor processor = new TestTextCompletionProcessor(editor);
			int lineNumber = 7;
			int columnNumber = 6;
			int lineOffset = editor.getDocumentProvider().getDocument(editor.getEditorInput()).getLineOffset(lineNumber);
			processor.setLineNumber(lineNumber);
			processor.setColumnNumber(columnNumber);
			processor.setCursorPosition(lineOffset + columnNumber);

			// complete inside a target
			ICompletionProposal[] proposals = processor.determineTemplateProposals();
			assertDoesNotContain("target - public target", proposals); //$NON-NLS-1$

			// complete outside of a target
			lineNumber = 8;
			columnNumber = 13;
			lineOffset = editor.getDocumentProvider().getDocument(editor.getEditorInput()).getLineOffset(lineNumber);
			processor.setLineNumber(lineNumber);
			processor.setColumnNumber(columnNumber);
			processor.setCursorPosition(lineOffset + columnNumber);

			proposals = processor.determineTemplateProposals();
			assertContains("target - public target", proposals); //$NON-NLS-1$
			// ensure all the tasks are still there
			proposals = processor.getProposalsFromDocument(editor.getDocumentProvider().getDocument(editor.getEditorInput()), ""); //$NON-NLS-1$
			assertContains("ant", proposals); //$NON-NLS-1$
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}

	}

	/**
	 * Tests the code completion for the fail task bug 73637
	 */
	public void testFailProposals() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest1.xml")); //$NON-NLS-1$

		ICompletionProposal[] proposals = processor.getAttributeProposals("fail", ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(6, proposals.length);

		assertContains("message", proposals); //$NON-NLS-1$
		assertContains("if", proposals); //$NON-NLS-1$
		assertContains("unless", proposals); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Test for bug 40951
	 */
	public void testMixedElements() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("mixed.xml")); //$NON-NLS-1$
		// String string = "<project><target><sql driver=\"\" password=\"\" url=\"\" userid=\"\"></sql><concat></concat>";
		ICompletionProposal[] proposals = processor.getTaskProposals(getCurrentDocument(), processor.getParentName(getCurrentDocument(), 0, 62), "t"); //$NON-NLS-1$
		assertEquals(1, proposals.length);
		ICompletionProposal proposal = proposals[0];
		assertEquals("transaction", proposal.getDisplayString()); //$NON-NLS-1$

		proposals = processor.getTaskProposals(getCurrentDocument(), processor.getParentName(getCurrentDocument(), 0, 76), ""); //$NON-NLS-1$
		// filelist fileset filterchain footer header path
		assertEquals(6, proposals.length);
		proposal = proposals[0];
		assertEquals("filelist", proposal.getDisplayString()); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Tests the algorithm for finding a child as used by the processor.
	 */
	public void testFindChildElement() throws ParserConfigurationException {

		// Create the test data
		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc = docBuilder.newDocument();
		Element parentElement = doc.createElement("parent"); //$NON-NLS-1$
		Attr attribute = doc.createAttribute("att1"); //$NON-NLS-1$
		parentElement.setAttributeNode(attribute);
		Comment comment = doc.createComment("lakjjflsakdfj"); //$NON-NLS-1$
		parentElement.appendChild(comment);
		Element childElement = doc.createElement("child"); //$NON-NLS-1$
		parentElement.appendChild(childElement);
		childElement = doc.createElement("secondchild"); //$NON-NLS-1$
		parentElement.appendChild(childElement);

		// Create the processor
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

		// Test it!
		childElement = processor.findChildElementNamedOf(parentElement, "jkl"); //$NON-NLS-1$
		assertNull(childElement);
		childElement = processor.findChildElementNamedOf(parentElement, "secondchild"); //$NON-NLS-1$
		assertNotNull(childElement);
		assertEquals("secondchild", childElement.getTagName()); //$NON-NLS-1$
	}

	/**
	 * Tests that the processor correctly determines the attribute proposal mode
	 */
	public void testDeterminingAttributeProposalMode() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor();
		int mode = processor.determineProposalMode("<project><property ta", 21, "ta"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_ATTRIBUTE_PROPOSAL, mode);
		mode = processor.determineProposalMode("<project><property ", 19, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_ATTRIBUTE_PROPOSAL, mode);
		mode = processor.determineProposalMode("<project><property   ", 21, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_ATTRIBUTE_PROPOSAL, mode);
		mode = processor.determineProposalMode("<property id=\"hu\" ", 18, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_ATTRIBUTE_PROPOSAL, mode);
		mode = processor.determineProposalMode("<property id=\"hu\" \r\n ", 21, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_ATTRIBUTE_PROPOSAL, mode);
		mode = processor.determineProposalMode("<property\n", 10, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_ATTRIBUTE_PROPOSAL, mode);
	}

	/**
	 * Tests that the processor correctly determines the attribute value proposal mode
	 */
	public void testDeterminingAttributeValueProposalMode() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor();
		int mode = processor.determineProposalMode("<project><property take=\"", 25, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL, mode);
		mode = processor.determineProposalMode("<property id=\"hu\" ", 14, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL, mode);
		mode = processor.determineProposalMode("<property id=\"hu\" \r\n ", 16, "hu"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL, mode);
		mode = processor.determineProposalMode("<property \n\t\tid=\"hu\" \r\n ", 19, "hu"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_ATTRIBUTE_VALUE_PROPOSAL, mode);
	}

	/**
	 * Tests how the processor determines the proposal mode.
	 */
	public void testDeterminingPropertyProposalMode() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor();
		int mode = processor.determineProposalMode("<project><target name=\"$\"", 24, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_PROPERTY_PROPOSAL, mode);
		mode = processor.determineProposalMode("<project><target name=\"${\"", 25, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_PROPERTY_PROPOSAL, mode);
		mode = processor.determineProposalMode("<project><target name=\"${ja.bl\"", 30, "ja.bl"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_PROPERTY_PROPOSAL, mode);

		mode = processor.determineProposalMode("<project><target><echo>${", 25, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_PROPERTY_PROPOSAL, mode);
	}

	/**
	 * Tests how the processor determines the proposal mode.
	 */
	public void testDeterminingTaskProposalMode() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

		int mode = processor.determineProposalMode("<project><prop", 14, "prop"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_TASK_PROPOSAL, mode);
		mode = processor.determineProposalMode("<project> hjk", 13, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_TASK_PROPOSAL, mode);
		mode = processor.determineProposalMode("<project> hjk<", 14, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_TASK_PROPOSAL, mode); // allow this case though it is not valid with Ant
		mode = processor.determineProposalMode("<project>", 9, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_TASK_PROPOSAL, mode);
		mode = processor.determineProposalMode("<project> ", 10, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_TASK_PROPOSAL, mode);
		mode = processor.determineProposalMode("<project></", 11, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_TASK_PROPOSAL_CLOSING, mode);
		mode = processor.determineProposalMode("<project>< </project>", 10, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_TASK_PROPOSAL, mode);
		mode = processor.determineProposalMode("<project default=\"hey\"><target name=\"hey\">a</target></project>", 44, "a"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_TASK_PROPOSAL, mode);
		mode = processor.determineProposalMode("<project default=\"hey\"><target name=\"hey\"></target></project>", 43, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_TASK_PROPOSAL, mode);
		mode = processor.determineProposalMode("<project default=\"hey\"><target name=\"hey\"><a</target></project>", 45, "<a"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_TASK_PROPOSAL, mode);
		mode = processor.determineProposalMode("<target name=\"main\"><zip><size></size></zip></", 46, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_TASK_PROPOSAL_CLOSING, mode);
		mode = processor.determineProposalMode("", 0, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_BUILDFILE, mode);
		mode = processor.determineProposalMode("<project default=\"hey\"><target name=\"hey\"><javac>a</javac></target></project>", 51, "a"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_TASK_PROPOSAL, mode);
		mode = processor.determineProposalMode("<project> hjk", 13, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_TASK_PROPOSAL, mode);
	}

	/**
	 * Tests how the processor determines the proposal mode.
	 */
	public void testDeterminingTaskClosingProposalMode() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

		int mode = processor.determineProposalMode("<target name=\"main\"><zip><size></size></zip></", 46, ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_TASK_PROPOSAL_CLOSING, mode);
	}

	/**
	 * Tests how the prefix will be determined.
	 */
	public void testDeterminingPrefix() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

		// cursor after ${
		String prefix = processor.getPrefixFromDocument("<project><target name=\"${}\"", 25); //$NON-NLS-1$
		assertEquals("", prefix); //$NON-NLS-1$

		// cursor after $
		prefix = processor.getPrefixFromDocument("<project><target name=\"${\"", 24); //$NON-NLS-1$
		assertEquals("", prefix); //$NON-NLS-1$

		// cursor after ${ja.
		prefix = processor.getPrefixFromDocument("<project><target name=\"${ja.\"", 28); //$NON-NLS-1$
		assertEquals("ja.", prefix); //$NON-NLS-1$

		// cursor after <
		prefix = processor.getPrefixFromDocument("<project><", 10); //$NON-NLS-1$
		assertEquals("", prefix); //$NON-NLS-1$

		prefix = processor.getPrefixFromDocument("<project name= \"test\"><tar", 26); //$NON-NLS-1$
		assertEquals("tar", prefix); //$NON-NLS-1$

		prefix = processor.getPrefixFromDocument("pro", 3); //$NON-NLS-1$
		assertEquals("pro", prefix); //$NON-NLS-1$
	}

	/**
	 * Tests how the processor determines the proposal mode.
	 */
	public void testDeterminingNoneProposalMode() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

		int mode = processor.determineProposalMode("<project><prop bla", 18, "bla"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_NONE, mode);
		mode = processor.determineProposalMode("<project default=\"hey\"><target name=", 37, "name="); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(TestTextCompletionProcessor.TEST_PROPOSAL_MODE_NONE, mode);
	}

	/**
	 * Tests the code completion for tasks in an empty build file (no parent).
	 */
	public void testTaskProposalsForEmptyBuildFile() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("empty.xml")); //$NON-NLS-1$

		ICompletionProposal[] proposals = processor.getBuildFileProposals("", ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(1, proposals.length);
		assertEquals("project", proposals[0].getDisplayString()); //$NON-NLS-1$

		proposals = processor.getBuildFileProposals("            jl", "jl"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(0, proposals.length);

		proposals = processor.getBuildFileProposals("    \n<project></project>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(1, proposals.length);
		processor.dispose();
	}

	/**
	 * Tests the code completion for refids (Bug 49830)
	 */
	public void testRefidProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("refid.xml")); //$NON-NLS-1$

		int lineNumber = 9;
		int columnNumber = 16;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		// for sure should have project.class.path and project.class.path2 but project.class.path2
		// should not present itself as a possible reference
		assertTrue(proposals.length >= 2);
		assertContains("project.class.path", proposals); //$NON-NLS-1$
		assertDoesNotContain("project.class.path2", proposals); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Tests the code completion for custom task that has a boolean attribute
	 */
	public void testCustomBooleanProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("customBoolean.xml")); //$NON-NLS-1$

		int lineNumber = 2;
		int columnNumber = 44;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		// true false yes no on off
		System.out.println("Proposals length:" + proposals.length); //$NON-NLS-1$
		assertTrue(proposals.length == 6);
		assertContains("true", proposals); //$NON-NLS-1$
		assertContains("no", proposals); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Tests the code completion for custom task that has an enumerated attribute
	 */
	public void testCustomEnumeratedProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("extensionPointTaskSepVM.xml")); //$NON-NLS-1$
		int lineNumber = 2;
		int columnNumber = 24;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), "c"); //$NON-NLS-1$
		assertEquals("Incorrect number of proposals", 2, proposals.length); //$NON-NLS-1$
		assertContains("cool", proposals); //$NON-NLS-1$
		assertContains("chillin", proposals); //$NON-NLS-1$
		assertDoesNotContain("awesome", proposals); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Tests the code completion for custom task that have a reference attribute
	 */
	public void testCustomReferenceProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("extensionPointTaskSepVM.xml")); //$NON-NLS-1$
		int lineNumber = 2;
		int columnNumber = 41;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), "e"); //$NON-NLS-1$
		assertEquals("Incorrect number of proposals", 1, proposals.length); //$NON-NLS-1$
		// the reference to the project by name
		assertContains("Extension Point Task", proposals); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Tests the code completion for nested element attributes of custom tasks
	 */
	public void testNestedElementAttributeProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("nestedElementAttributes.xml")); //$NON-NLS-1$
		int lineNumber = 4;
		int columnNumber = 18;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		assertTrue(proposals.length == 1);
		assertContains("works", proposals); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Tests the code completion for nested elements
	 */
	public void testNestedElementProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("nestedElementAttributes.xml")); //$NON-NLS-1$
		int lineNumber = 4;
		int columnNumber = 3;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		assertTrue(proposals.length == 1);
		assertContains("nestedelement", proposals); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Tests the code completion for nested elements that no templates are presented Bug 76414
	 */
	public void testNestedElementTemplateProposals() throws BadLocationException, PartInitException {
		try {
			IFile file = getIFile("nestedElementAttributes.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, ANT_EDITOR_ID, true);
			TestTextCompletionProcessor processor = new TestTextCompletionProcessor(editor);
			int lineNumber = 4;
			int lineOffset = editor.getDocumentProvider().getDocument(editor.getEditorInput()).getLineOffset(lineNumber);

			editor.getSelectionProvider().setSelection(new TextSelection(lineOffset, 0));

			ICompletionProposal[] proposals = processor.computeCompletionProposals(lineOffset);

			assertTrue("No templates are relevant at the current position. Found: " + proposals.length, proposals.length == 1); //$NON-NLS-1$
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}

	/**
	 * Tests the code completion for nested element attribute values of custom tasks
	 */
	public void testNestedElementAttributeValueProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("nestedElementAttributes.xml")); //$NON-NLS-1$
		int lineNumber = 4;
		int columnNumber = 25;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		assertEquals("There should be 6 completion proposals", proposals.length, 6); //$NON-NLS-1$
		assertContains("true", proposals); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Tests the code completion when a parse error occurs in the project definition bug 63151
	 */
	public void testBadProjectProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("badproject.xml")); //$NON-NLS-1$
		int lineNumber = 0;
		int columnNumber = 10;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), "n"); //$NON-NLS-1$
		assertTrue(proposals.length == 1);
		assertContains(IAntCoreConstants.NAME, proposals);
		processor.dispose();
	}

	/**
	 * Tests the code completion for attribute value proposals both with and without leading whitespace
	 */
	public void testAttributeValueProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("javac.xml")); //$NON-NLS-1$
		int lineNumber = 2;
		int columnNumber = 29;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		assertTrue(proposals.length == 6); // boolean proposals
		assertContains("false", proposals); //$NON-NLS-1$

		lineNumber = 3;
		columnNumber = 19;
		lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		assertTrue(proposals.length == 6); // boolean proposals
		assertContains("true", proposals); //$NON-NLS-1$

		lineNumber = 4;
		columnNumber = 22;
		lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		assertTrue(proposals.length == 6); // boolean proposals
		assertContains("no", proposals); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Tests the code completion for an empty buildfile
	 */
	public void testEmptyBuildfileProposals() throws PartInitException {
		try {
			IFile file = getIFile("empty.xml"); //$NON-NLS-1$
			AntEditor editor = (AntEditor) EditorTestHelper.openInEditor(file, ANT_EDITOR_ID, true);
			TestTextCompletionProcessor processor = new TestTextCompletionProcessor(editor);

			editor.getSelectionProvider().setSelection(TextSelection.emptySelection());

			ICompletionProposal[] proposals = processor.computeCompletionProposals(0);
			assertTrue("Four proposals are relevant at the current position. Found: " + proposals.length, proposals.length == 4); //$NON-NLS-1$
			assertContains("project", proposals); //$NON-NLS-1$
			assertContains("Buildfile template - simple buildfile with two targets", proposals); //$NON-NLS-1$
			processor.dispose();
		}
		finally {
			EditorTestHelper.closeAllEditors();
		}
	}

	/**
	 * Tests the code completion for refids (Bug 65480)
	 */
	public void testJavacReferencesProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("refid.xml")); //$NON-NLS-1$

		int lineNumber = 16;
		int columnNumber = 24;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		// for sure should have project.class.path and project.class.path2
		assertTrue(proposals.length >= 2);
		assertContains("project.class.path", proposals); //$NON-NLS-1$
		assertContains("project.class.path2", proposals); //$NON-NLS-1$

		lineNumber = 17;
		columnNumber = 25;
		lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		// for sure should have project.class.path and project.class.path2
		assertTrue(proposals.length >= 2);
		assertContains("project.class.path", proposals); //$NON-NLS-1$
		assertContains("project.class.path2", proposals); //$NON-NLS-1$

		lineNumber = 18;
		columnNumber = 26;
		lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		// for sure should have project.class.path and project.class.path2
		assertTrue(proposals.length >= 2);
		assertContains("project.class.path", proposals); //$NON-NLS-1$
		assertContains("project.class.path2", proposals); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Tests the code completion for the default target of a project (Bug 78030)
	 */
	public void testProjectDefaultProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest1.xml")); //$NON-NLS-1$

		int lineNumber = 1;
		int columnNumber = 49;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		// includes all the public targets
		assertTrue(proposals.length == 8);
		assertContains("main", proposals); //$NON-NLS-1$
		assertContains("testUnless", proposals); //$NON-NLS-1$
		processor.dispose();
	}

	/**
	 * Tests the code completion for project attributes (bug 82031)
	 */
	public void testProjectAttributeProposals() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest1.xml")); //$NON-NLS-1$

		int lineNumber = 1;
		int columnNumber = 9;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		// includes all the project attributes
		assertTrue(proposals.length == 3);
		assertContains(IAntCoreConstants.NAME, proposals);
		assertContains("default", proposals); //$NON-NLS-1$
		assertContains("basedir", proposals); //$NON-NLS-1$

		columnNumber = 10;
		lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		proposals = processor.getProposalsFromDocument(getCurrentDocument(), "n"); //$NON-NLS-1$
		assertTrue(proposals.length == 1);
		assertContains(IAntCoreConstants.NAME, proposals);
		processor.dispose();
	}

	public void testExtensionPoint() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("antextpoint1.xml")); //$NON-NLS-1$

		int lineNumber = 2;
		int columnNumber = 3;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), "ext"); //$NON-NLS-1$
		assertTrue(proposals.length == 3);
		assertContains("extension-point", proposals); //$NON-NLS-1$
		processor.dispose();
	}

	public void testExtensionOf() throws BadLocationException {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("antextpoint2.xml")); //$NON-NLS-1$

		int lineNumber = 3;
		int columnNumber = 38;
		int lineOffset = getCurrentDocument().getLineOffset(lineNumber);
		processor.setLineNumber(lineNumber);
		processor.setColumnNumber(columnNumber);
		processor.setCursorPosition(lineOffset + columnNumber);
		ICompletionProposal[] proposals = processor.getProposalsFromDocument(getCurrentDocument(), ""); //$NON-NLS-1$
		assertTrue(proposals.length == 1);
		assertContains("ep-B", proposals); //$NON-NLS-1$
		processor.dispose();
	}
}