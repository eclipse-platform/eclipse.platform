/*******************************************************************************
 * Copyright (c) 2002, 2003 GEBIT Gesellschaft fuer EDV-Beratung
 * und Informatik-Technologien mbH, 
 * Berlin, Duesseldorf, Frankfurt (Germany) and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     GEBIT Gesellschaft fuer EDV-Beratung und Informatik-Technologien mbH - initial implementation
 * 	   IBM Corporation - additional tests
 *******************************************************************************/

package org.eclipse.ant.tests.ui.editor;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.ant.tests.ui.editor.support.TestTextCompletionProcessor;
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Tests everything about code completion and code assistance.
 * 
 */
public class CodeCompletionTest extends AbstractAntUITest {

    /**
     * Constructor for CodeCompletionTest.
     * @param name
     */
    public CodeCompletionTest(String name) {
        super(name);
    }
    
	public static Test suite() {
		return new TestSuite(CodeCompletionTest.class);
	}

    /**
     * Tests the code completion for attributes of tasks.
     */
    public void testAttributeProposals() {
        TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

        ICompletionProposal[] proposals = processor.getAttributeProposals("contains", "ca");
        assertEquals(1, proposals.length);
        assertEquals("casesensitive - (true | false | on | off | yes | no)", proposals[0].getDisplayString());

        proposals = processor.getAttributeProposals("move", "");
        assertEquals(16, proposals.length);
        ICompletionProposal proposal = proposals[0];
        String displayString = proposal.getDisplayString();
        assertTrue(displayString.equals("id") 
        || displayString.equals("taskname")
        || displayString.equals("description")
        || displayString.equals("file")
        || displayString.equals("preservelastmodified - (true | false | on | off | yes | no)")
        || displayString.equals("tofile")
        || displayString.equals("todir")
        || displayString.equals("overwrite - (true | false | on | off | yes | no)")
        || displayString.equals("filtering - (true | false | on | off | yes | no)")
        || displayString.equals("flatten - (true | false | on | off | yes | no)")
        || displayString.equals("includeemptydirs")
        || displayString.equals("failonerror - (true | false | on | off | yes | no)")
        || displayString.equals("verbose - (true | false | on | off | yes | no)")
        || displayString.equals("encoding")
        || displayString.equals("outputencoding")
        || displayString.equals("enablemultiplemapping - (true | false | on | off | yes | no)"));
        
        proposals = processor.getAttributeProposals("move", "to");
        assertEquals(2, proposals.length);

        proposals = processor.getAttributeProposals("reference", "idl");
        assertEquals(0, proposals.length);

        proposals = processor.getAttributeProposals("reference", "id");
        assertEquals(1, proposals.length);
        assertEquals("id", proposals[0].getDisplayString());

        proposals = processor.getAttributeProposals("reference", "i");
        //id includesfile includes
        assertEquals(3, proposals.length);
        displayString= proposals[0].getDisplayString();
        assertTrue(displayString.equals("id") 
        		||  displayString.equals("includeFile")
				|| displayString.equals("includes"));

        proposals = processor.getAttributeProposals("project", "de");
        assertEquals(1, proposals.length);
    }
    
    /**
     * Test the code completion for properties, including unquoted (bug 40871)
     */
    public void testPropertyProposals() throws BadLocationException {
    	TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest1.xml"));

    	int lineNumber= 7;
    	int columnNumber= 17;
    	int lineOffset= getCurrentDocument().getLineOffset(lineNumber);
    	processor.setLineNumber(lineNumber);
    	processor.setColumnNumber(columnNumber);
    	processor.setCursorPosition(lineOffset + columnNumber);
    	ICompletionProposal[] proposals = processor.getPropertyProposals(getCurrentDocument(), "", lineOffset + columnNumber);
    	assertTrue(proposals.length >= 1);
    	assertContains("prop1", proposals);
    	
    	lineNumber= 18;
    	columnNumber= 25;
    	lineOffset= getCurrentDocument().getLineOffset(lineNumber);
    	processor.setLineNumber(lineNumber);
    	processor.setColumnNumber(columnNumber);
    	processor.setCursorPosition(lineOffset + columnNumber);
    	proposals = processor.getPropertyProposals(getCurrentDocument(), "", lineOffset + columnNumber);
    	assertTrue(proposals.length >= 1);
    	assertContains("prop2", proposals);
    }
    
    /**
     * Test the code completion for the depend attribute of a target.
     */
    public void testTargetDependProposals() throws BadLocationException {
    	TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest1.xml"));
    	//simple depends
    	int lineNumber= 10;
    	int columnNumber= 34;
    	int lineOffset= getCurrentDocument().getLineOffset(lineNumber);
    	processor.setLineNumber(lineNumber);
    	processor.setColumnNumber(columnNumber);
    	processor.setCursorPosition(lineOffset + columnNumber);
    	ICompletionProposal[] proposals = processor.getTargetAttributeValueProposals(getCurrentDocument(), getCurrentDocument().get(0, lineOffset + columnNumber), "", "depends");
    	assertTrue(proposals.length == 7);
    	assertContains("pretest", proposals);
    	assertContains("testMoreDepends", proposals);
    	//comma separated depends
    	lineNumber= 17;
    	columnNumber= 53;
    	lineOffset= getCurrentDocument().getLineOffset(lineNumber);
    	processor.setLineNumber(lineNumber);
    	processor.setColumnNumber(columnNumber);
    	processor.setCursorPosition(lineOffset + columnNumber);
    	proposals = processor.getTargetAttributeValueProposals(getCurrentDocument(), getCurrentDocument().get(0, lineOffset + columnNumber), "", "depends");
    	assertTrue(proposals.length == 6);
    	assertContains("main", proposals);
    	assertDoesNotContain("pretest", proposals);
    	
    }
    
    /**
     * Test the code completion for the if attribute of a target.
     */
    public void testTargetIfProposals() throws BadLocationException {
    	TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest1.xml"));

    	int lineNumber= 15;
    	int columnNumber= 31;
    	int lineOffset= getCurrentDocument().getLineOffset(lineNumber);
    	processor.setLineNumber(lineNumber);
    	processor.setColumnNumber(columnNumber);
    	processor.setCursorPosition(lineOffset + columnNumber);
    	ICompletionProposal[] proposals = processor.getTargetAttributeValueProposals(getCurrentDocument(), getCurrentDocument().get(0, lineOffset + columnNumber), "", "if");
    	assertTrue(proposals.length >= 1);
    	assertContains("prop1", proposals);
    }
    
    /**
     * Test the code completion for the unless attribute of a target.
     */
    public void testTargetUnlessProposals() throws BadLocationException {
    	TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest1.xml"));

    	int lineNumber= 16;
    	int columnNumber= 43;
    	int lineOffset= getCurrentDocument().getLineOffset(lineNumber);
    	processor.setLineNumber(lineNumber);
    	processor.setColumnNumber(columnNumber);
    	processor.setCursorPosition(lineOffset + columnNumber);
    	ICompletionProposal[] proposals = processor.getTargetAttributeValueProposals(getCurrentDocument(), getCurrentDocument().get(0, lineOffset + columnNumber), "prop", "unless");
    	assertTrue(proposals.length >= 1);
    	assertContains("prop1", proposals);

    }
    
 	/**
 	 * Asserts that <code>displayString</code> is in one of the 
 	 * completion proposals.
 	 */
    private void assertContains(String displayString, ICompletionProposal[] proposalArray) {
        boolean found = false;
        for (int i = 0; i < proposalArray.length; i++) {
            ICompletionProposal proposal = proposalArray[i];
            String proposalDisplayString = proposal.getDisplayString();
            if(displayString.equals(proposalDisplayString)) {
                found = true;
                break;
            }
        }
        assertEquals(true, found);
    }        
    
    /**
 	 * Asserts that <code>displayString</code> is not in one of the 
 	 * completion proposals.
 	 */
    private void assertDoesNotContain(String displayString, ICompletionProposal[] proposalArray) {
        boolean found = false;
        for (int i = 0; i < proposalArray.length; i++) {
            ICompletionProposal proposal = proposalArray[i];
            String proposalDisplayString = proposal.getDisplayString();
            if(displayString.equals(proposalDisplayString)) {
                found = true;
                break;
            }
        }
        assertEquals(false, found);
    }        

	/**
	 * Tests the property proposals for the case that they are defined in
	 * a dependent targets.
	 */
    public void testPropertyProposalDefinedInDependantTargets() throws FileNotFoundException {
        TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("dependencytest.xml"));

        File file= getBuildFile("dependencytest.xml");
        processor.setEditedFile(file);
		String documentText = getFileContentAsString(file);

		processor.setLineNumber(35);
		processor.setColumnNumber(41);
		int cursorPosition = documentText.lastIndexOf("${");
		assertTrue(cursorPosition != -1);
        ICompletionProposal[] proposals = processor.getPropertyProposals(new org.eclipse.jface.text.Document(documentText), "", cursorPosition+2);
		assertContains("init_prop", proposals);
		assertContains("main_prop", proposals);
		assertContains("prop_prop", proposals);
		assertContains("do_not_compile", proposals);
		assertContains("adit_prop", proposals);
		assertContains("compile_prop", proposals);
    }
    
    /**
     * Tests the code completion for tasks that have been defined in the buildfile
     */
  //  public void testCustomTaskProposals() {
//		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("taskdef.xml"));

  //      ICompletionProposal[] proposals = processor.getTaskProposals(getCurrentDocument(), "target", "min");
    //    assertEquals(1, proposals.length);
     //   ICompletionProposal proposal = proposals[0];
    //    assertEquals("mine", proposal.getDisplayString());
        
//	}
    
    /**
     * Tests the code completion for tasks that have been defined in the buildfile
     */
//  //  public void testExtensionPointTaskProposals() {
////		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("taskdef.xml"));
//
//  //      ICompletionProposal[] proposals = processor.getTaskProposals(getCurrentDocument(), "target", "eclipse");
//   //     assertEquals(11, proposals.length);
//   //     assertContains("eclipse.refreshLocal", proposals);
        
//}
    
    /**
     * Tests the code completion for tasks that have been defined via macrodef in the buildfile
     */
  //  public void testMacrodefProposals() {
//		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("macrodef.xml"));
//
  //      ICompletionProposal[] proposals = processor.getTaskProposals(getCurrentDocument(), "target", "eclipsema");
   //     assertEquals(1, proposals.length);
    //    ICompletionProposal proposal = proposals[0];
    //    assertEquals("eclipseMacro", proposal.getDisplayString());
        
//	}

    /**
     * Tests the code completion for tasks having parent tasks.
     */
    public void testTaskProposals() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("buildtest1.xml"));

       
        ICompletionProposal[] proposals = processor.getTaskProposals("         <", "rename", "");
        assertEquals(0, proposals.length);

//        proposals = processor.getTaskProposals("       <cl", createTestPropertyElement(doc), "cl");
//        assertEquals(1, proposals.length);
//        ICompletionProposal proposal = proposals[0];
//        assertEquals("classpath", proposal.getDisplayString());
//        
//        //case insensitivity
//		proposals = processor.getTaskProposals("       <CL", createTestPropertyElement(doc), "cl");
//	   	assertEquals(1, proposals.length);
//	   	proposal = proposals[0];
//	   	assertEquals("classpath", proposal.getDisplayString());

        proposals = processor.getTaskProposals("       <cl", "property", "cl");
        assertEquals(1, proposals.length);
        ICompletionProposal proposal = proposals[0];
        assertEquals("classpath", proposal.getDisplayString());

        proposals = processor.getTaskProposals("       <pr", "property", "");
        assertEquals(1, proposals.length);
        proposal = proposals[0];
        assertEquals("classpath", proposal.getDisplayString());

//        proposals = processor.getTaskProposals("       <pr", createTestProjectElement(doc), "pr");
//        assertEquals(1, proposals.length); // is choice and already used with classpath
//        proposal = proposals[0];
//        assertEquals("property", proposal.getDisplayString());
//        
//        proposals = processor.getTaskProposals("       <fi", createTestProjectElement(doc), "fi");
//        assertEquals(5, proposals.length); // is choice and already used with classpath

        proposals = processor.getTaskProposals("          ", null, "");
        assertEquals(1, proposals.length);
        proposal = proposals[0];
        assertEquals("project", proposal.getDisplayString());

        proposals = processor.getTaskProposals("            jl", null, "jl");
        assertEquals(0, proposals.length);

        proposals = processor.getTaskProposals("             ", "projexxx", "");
        assertEquals(0, proposals.length);

        proposals = processor.getTaskProposals("              ", "filelist", "");
        assertEquals(0, proposals.length);

        // "<project><target><mk"
        proposals = processor.getTaskProposals("<project><target><mk", "target", "mk");
        assertEquals(1, proposals.length);
        proposal = proposals[0];
        assertEquals("mkdir", proposal.getDisplayString());
        
        proposals = processor.getTaskProposals("", null, "");
        assertEquals(1, proposals.length);
        proposal = proposals[0];
        assertEquals("project", proposal.getDisplayString());

        proposals = processor.getTaskProposals("    \n<project></project>", null, "");
        assertEquals(1, proposals.length);
    }
    
	/**
	 * Test for bug 40951
	 */
	public void testMixedElements() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor(getAntModel("mixed.xml"));
		//String string = "<project><target><sql driver=\"\" password=\"\" url=\"\" userid=\"\"></sql><concat></concat>";
		ICompletionProposal[] proposals = processor.getTaskProposals(getCurrentDocument(), processor.getParentName(getCurrentDocument(), 0, 62), "t");
		assertEquals(1, proposals.length);
		ICompletionProposal proposal = proposals[0];
		assertEquals("transaction", proposal.getDisplayString());
		
		proposals = processor.getTaskProposals(getCurrentDocument(), processor.getParentName(getCurrentDocument(), 0, 76), "");
		//filelist fileset filterchain footer header path
		assertEquals(6, proposals.length);
		proposal = proposals[0];
		assertEquals("filelist", proposal.getDisplayString());
	}
	
    /**
     * Tests the algorithm for finding a child as used by the processor.
     */
    public void testFindChildElement() throws ParserConfigurationException {
        
        // Create the test data
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = docBuilder.newDocument();
        Element parentElement = doc.createElement("parent");
        Attr attribute = doc.createAttribute("att1");
        parentElement.setAttributeNode(attribute);
        Comment comment = doc.createComment("lakjjflsakdfj");
        parentElement.appendChild(comment);
        Element childElement = doc.createElement("child");
        parentElement.appendChild(childElement);
        childElement = doc.createElement("secondchild");
        parentElement.appendChild(childElement);
        
        // Create the processor
        TestTextCompletionProcessor processor = new TestTextCompletionProcessor();
        
        // Test it!
        childElement = processor.findChildElementNamedOf(parentElement, "jkl");
        assertNull(childElement);
        childElement = processor.findChildElementNamedOf(parentElement, "secondchild");
        assertNotNull(childElement);
        assertEquals("secondchild", childElement.getTagName());
    }
    
    /**
     * Tests how the processor determines the proposal mode.
     */
    public void testDeterminingAttributeProposalMode() {
        TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

        // Modes:
        // 0 None
        // 1 Task Proposal
        // 2 Attribute Proposal
        // 3 Task Closing Proposal
        // 4 Attribute Value Proposal
        // 5 Property Proposal
        
        int mode = processor.determineProposalMode("<project><property ta", 21, "ta");
        assertEquals(2, mode);
        mode = processor.determineProposalMode("<project><property ", 19, "");
        assertEquals(2, mode);
        mode = processor.determineProposalMode("<project><property   ", 21, "");
        assertEquals(2, mode);
        mode = processor.determineProposalMode("<property id=\"hu\" ", 18, "");
        assertEquals(2, mode);
        mode = processor.determineProposalMode("<property id=\"hu\" \r\n ", 21, "");
        assertEquals(2, mode);
        mode = processor.determineProposalMode("<property\n", 10, "");
        assertEquals(2, mode);
    }
    
	/**
	* Tests how the processor determines the proposal mode.
	*/
   public void testDeterminingPropertyProposalMode() {
	   TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

	   // Modes:
	   // 0 None
	   // 1 Task Proposal
	   // 2 Attribute Proposal
	   // 3 Task Closing Proposal
	   // 4 Attribute Value Proposal
	   // 5 Property Proposal
    
	   int mode =processor.determineProposalMode("<project><target name=\"$\"", 24, "");
	   assertEquals(5, mode);
	   mode = processor.determineProposalMode("<project><target name=\"${\"", 25, "");
	   assertEquals(5, mode);
	   mode = processor.determineProposalMode("<project><target name=\"${ja.bl\"", 30, "ja.bl");
	   assertEquals(5, mode);
	  
		mode = processor.determineProposalMode("<project><target><echo>${", 25, "");
		assertEquals(5, mode);
   }
    
	/**
	 * Tests how the processor determines the proposal mode.
	 */
	public void testDeterminingTaskProposalMode() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

		// Modes:
		// 0 None
		// 1 Task Proposal
		// 2 Attribute Proposal
		// 3 Task Closing Proposal
		// 4 Attribute Value Proposal
		// 5 Property Proposal
    
		int mode = processor.determineProposalMode("<project><prop", 14, "prop");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<project> hjk", 13, "");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<project> hjk<", 14, "");
		assertEquals(1, mode); // allow this case though it is not valid with Ant
		mode = processor.determineProposalMode("<project>", 9, "");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<project> ", 10, "");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<project></", 11, "");
		assertEquals(3, mode);
		mode = processor.determineProposalMode("<project>< </project>", 10, "");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<project default=\"hey\"><target name=\"hey\">a</target></project>", 44, "a");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<project default=\"hey\"><target name=\"hey\"></target></project>", 43, "");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<project default=\"hey\"><target name=\"hey\"><a</target></project>", 45, "<a");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<target name=\"main\"><zip><size></size></zip></", 46, "");
		assertEquals(3, mode);
		mode = processor.determineProposalMode("", 0, "");
		assertEquals(1, mode);
		mode= processor.determineProposalMode("<project default=\"hey\"><target name=\"hey\"><javac>a</javac></target></project>", 51, "a");
		assertEquals(1, mode);
		mode = processor.determineProposalMode("<project> hjk", 13, "");
		assertEquals(1, mode);
	}
	
	/**
	 * Tests how the processor determines the proposal mode.
	 */
	public void testDeterminingTaskClosingProposalMode() {
		TestTextCompletionProcessor processor = new TestTextCompletionProcessor();
	
		// Modes:
		// 0 None
		// 1 Task Proposal
		// 2 Attribute Proposal
		// 3 Task Closing Proposal
		// 4 Attribute Value Proposal
		// 5 Property Proposal
	
		int mode = processor.determineProposalMode("<target name=\"main\"><zip><size></size></zip></", 46, "");
		assertEquals(3, mode);
	}

    /**
     * Tests how the prefix will be determined.
     */
    public void testDeterminingPrefix() {
        TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

        // cursor after ${
        String prefix = processor.getPrefixFromDocument("<project><target name=\"${}\"", 25);
        assertEquals("", prefix);

        // cursor after $
        prefix = processor.getPrefixFromDocument("<project><target name=\"${\"", 24);
        assertEquals("", prefix);

        // cursor after ${ja.
        prefix = processor.getPrefixFromDocument("<project><target name=\"${ja.\"", 28);
        assertEquals("ja.", prefix);
        
        // cursor after <
		prefix = processor.getPrefixFromDocument("<project><", 10);
		assertEquals("", prefix);
		
		prefix = processor.getPrefixFromDocument("<project name= \"test\"><tar", 26);
		assertEquals("tar", prefix);
		
		prefix = processor.getPrefixFromDocument("pro", 3);
		assertEquals("pro", prefix);
    }    
    
    /**
     * Tests how the processor determines the proposal mode.
     */
    public void testDeterminingNoneProposalMode() {
        TestTextCompletionProcessor processor = new TestTextCompletionProcessor();

        // Modes:
        // 0 None
        // 1 Task Proposal
        // 2 Attribute Proposal
        // 3 Task Closing Proposal
        // 4 Attribute Value Proposal
        // 5 Property Proposal
        
        int mode = processor.determineProposalMode("<project><prop bla", 18, "bla");
        assertEquals(0, mode);
		mode= processor.determineProposalMode("<project default=\"hey\"><target name=", 37, "name=");
		assertEquals(0, mode);
	}
}