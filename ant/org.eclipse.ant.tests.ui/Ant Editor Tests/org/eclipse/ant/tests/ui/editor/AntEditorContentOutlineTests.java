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

import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.ant.internal.ui.editor.outline.AntModel;
import org.eclipse.ant.internal.ui.editor.model.AntElementNode;
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.jface.text.BadLocationException;

/**
 * Tests the correct creation of the outline for an xml file.
 * 
 */
public class AntEditorContentOutlineTests extends AbstractAntUITest {
    
    public AntEditorContentOutlineTests(String name) {
        super(name);
    }
   
    /**
     * Tests the creation of the AntElementNode, that includes parsing a file
     * and determining the correct location of the tags.
     */
    public void testCreationOfOutlineTree() throws BadLocationException {
		AntModel model= getAntModel("buildtest1.xml");
        
		AntElementNode[] roots = model.getRootElements();
		assertNotNull(roots);
		AntElementNode rootProject= roots[0];
       
        assertNotNull(rootProject);
        
        // Get the content as string
        String wholeDocumentString = getCurrentDocument().get();
        
        // <project>
        assertEquals(2, getStartingRow(rootProject));
        assertEquals(2, getStartingColumn(rootProject));
        int offset = wholeDocumentString.indexOf("project");
	    assertEquals(offset, rootProject.getOffset());
        
        List children = rootProject.getChildNodes();
		
		// <property name="propD">
		AntElementNode element = (AntElementNode)children.get(0);
		assertEquals(3, getStartingRow(element));
		assertEquals(3, getStartingColumn(element)); // with tab in file
		assertEquals(3, getEndingRow(element));
		assertEquals(39, getEndingColumn(element));  // with tab in file

        offset = wholeDocumentString.indexOf("property");
        assertEquals(offset, element.getOffset());
        int length = "<property name=\"propD\" value=\"valD\" />".length();
        assertEquals(length- 1, element.getLength()); //we do not include the first '<'
		
		// <property file="buildtest1.properties">
		element = (AntElementNode)children.get(1);
		assertEquals(4, getStartingRow(element));
		assertEquals(6, getStartingColumn(element)); // no tab
		assertEquals(4, getEndingRow(element));
		assertEquals(45, getEndingColumn(element));

		// <property name="propV">
		element = (AntElementNode)children.get(2);
		assertEquals(5, getStartingRow(element));
		assertEquals(6, getStartingColumn(element));
		assertEquals(5, getEndingRow(element));
		assertEquals(42, getEndingColumn(element));
		
		// <target name="main">
		element = (AntElementNode)children.get(3);
		assertEquals(6, getStartingRow(element));
		assertEquals(6, getStartingColumn(element));
		assertEquals(9, getEndingRow(element));
		assertEquals(13, getEndingColumn(element));
		
		// <property name="property_in_target">
		element = (AntElementNode)element.getChildNodes().get(0);
		assertEquals(7, getStartingRow(element));
		assertEquals(10, getStartingColumn(element));
		assertEquals(7, getEndingRow(element));
		assertEquals(57, getEndingColumn(element));
        offset = wholeDocumentString.indexOf("property name=\"property_in_target\"");
        assertEquals(offset, element.getOffset());
		
        assertEquals(21, getEndingRow(rootProject));
        assertEquals(10, getEndingColumn(rootProject));
    }
    
    private int getColumn(int offset, int line) throws BadLocationException {
    	return offset - getCurrentDocument().getLineOffset(line - 1) + 1;
    }
    
    private int getStartingRow(AntElementNode element) throws BadLocationException {
    	return getCurrentDocument().getLineOfOffset(element.getOffset()) + 1;
    }
    
    private int getEndingRow(AntElementNode element) throws BadLocationException {
    	return getCurrentDocument().getLineOfOffset(element.getOffset() + element.getLength() - 1) + 1;
    }
    
    private int getStartingColumn(AntElementNode element) throws BadLocationException {
    	return getColumn(element.getOffset(), getStartingRow(element));
    }
    
    private int getEndingColumn(AntElementNode element) throws BadLocationException {
    	return getColumn(element.getOffset() + element.getLength() - 1, getEndingRow(element));
    }

    /**
     * Tests the creation of the AntElementNode, that includes parsing a non-valid 
     * file.
     */
    public void testParsingOfNonValidFile() throws BadLocationException {
		AntModel model= getAntModel("buildtest2.xml");
        
		AntElementNode[] roots = model.getRootElements();
		assertNotNull(roots);
		AntElementNode root= roots[0];
   		assertNotNull(root);

        List children = root.getChildNodes();

		// <target name="main">
		AntElementNode element = (AntElementNode)children.get(2);
		assertEquals(5, getStartingRow(element));
		assertEquals(3, getStartingColumn(element)); // with tab in file
		assertEquals(5, getEndingRow(element));
		//main has no ending column as the element is not closed
        int offset = getCurrentDocument().get().indexOf("target name=\"main\"");
        assertEquals(offset, element.getOffset()); 
    }

	
	/**
	 * Tests whether the outline can handle a build file with only the <project></project> tags.
	 */
	public void testWithProjectOnlyBuildFile() {
		AntModel model= getAntModel("projectOnly.xml");
		AntElementNode[] roots = model.getRootElements();
		assertNotNull(roots);
		AntElementNode root= roots[0];
		assertNotNull(root);
	}
	
	/**
	 * Tests whether the outline can handle an empty build file.
	 */
	public void testWithEmptyBuildFile() {
		AntModel model= getAntModel("empty.xml");
		AntElementNode[] roots = model.getRootElements();
		assertNotNull(roots);
		assertTrue(roots.length == 0);
	}		

	/**
	 * Some testing of getting the right location of tags.
	 */
	public void testAdvancedTaskLocation() throws BadLocationException {
		AntModel model= getAntModel("outline_select_test_build.xml");
        
		AntElementNode[] roots = model.getRootElements();
        assertNotNull(roots);
        AntElementNode rootProject= roots[0];
        // Get the content as string
        String wholeDocumentString = getCurrentDocument().get();
        
        // <project>
        assertNotNull(rootProject);
        assertEquals(2, getStartingRow(rootProject));
        assertEquals(2, getStartingColumn(rootProject));
        int offset = wholeDocumentString.indexOf("project");
        
	    assertEquals(offset, rootProject.getOffset());
        
		// <target name="properties">
        AntElementNode element = (AntElementNode)rootProject.getChildNodes().get(1);
        assertNotNull(element);
        assertEquals("properties", element.getLabel());
        assertEquals(16, getStartingRow(element));
        assertEquals(3, getStartingColumn(element));
        offset = wholeDocumentString.indexOf("target name=\"properties\"");
      
	    assertEquals(offset, element.getOffset());
	}

    public static Test suite() {
		return new TestSuite(AntEditorContentOutlineTests.class);
    }
}