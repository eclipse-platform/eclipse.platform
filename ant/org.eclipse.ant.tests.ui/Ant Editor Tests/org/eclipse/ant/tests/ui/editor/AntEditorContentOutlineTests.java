/*******************************************************************************
 * Copyright (c) 2002, 2013 GEBIT Gesellschaft fuer EDV-Beratung
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
 *******************************************************************************/

package org.eclipse.ant.tests.ui.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.ant.internal.ui.model.IAntElement;
import org.eclipse.ant.tests.ui.testplugin.AntModelForDocument;
import org.eclipse.ant.tests.ui.testplugin.AntUITest;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.junit.jupiter.api.Test;

/**
 * Tests the correct creation of the outline for an xml file.
 */
@AntUITest
public class AntEditorContentOutlineTests {

	/**
	 * Tests the creation of the AntElementNode, that includes parsing a file and determining the correct location of the tags.
	 */
	@Test
	public void testCreationOfOutlineTree() throws BadLocationException {
		AntModelForDocument model = new AntModelForDocument("buildtest1.xml"); //$NON-NLS-1$

		AntElementNode rootProject = model.getAntModel().getProjectNode();

		assertNotNull(rootProject);

		// Get the content as string
		String wholeDocumentString = model.getDocument().get();

		IDocument document = model.getDocument();
		// <project>
		assertEquals(2, getStartingRow(document, rootProject));
		assertEquals(2, getStartingColumn(document, rootProject));
		int offset = wholeDocumentString.indexOf("project"); //$NON-NLS-1$
		assertEquals(offset, rootProject.getOffset());

		List<IAntElement> children = rootProject.getChildNodes();

		// <property name="propD">
		IAntElement element = children.get(0);
		assertEquals(3, getStartingRow(document, element));
		assertEquals(3, getStartingColumn(document, element)); // with tab in file
		assertEquals(3, getEndingRow(document, element));
		assertEquals(39, getEndingColumn(document, element)); // with tab in file

		offset = wholeDocumentString.indexOf("property"); //$NON-NLS-1$
		assertEquals(offset, element.getOffset());
		int length = "<property name=\"propD\" value=\"valD\" />".length(); //$NON-NLS-1$
		assertEquals(length - 1, element.getLength()); // we do not include the first '<'

		// <property file="buildtest1.properties">
		element = children.get(1);
		assertEquals(4, getStartingRow(document, element));
		assertEquals(6, getStartingColumn(document, element)); // no tab
		assertEquals(4, getEndingRow(document, element));
		assertEquals(45, getEndingColumn(document, element));

		// <property name="propV">
		element = children.get(2);
		assertEquals(5, getStartingRow(document, element));
		assertEquals(6, getStartingColumn(document, element));
		assertEquals(5, getEndingRow(document, element));
		assertEquals(42, getEndingColumn(document, element));

		// <target name="main">
		element = children.get(3);
		assertEquals(6, getStartingRow(document, element));
		assertEquals(6, getStartingColumn(document, element));
		assertEquals(9, getEndingRow(document, element));
		assertEquals(13, getEndingColumn(document, element));

		// <property name="property_in_target">
		element = element.getChildNodes().get(0);
		assertEquals(7, getStartingRow(document, element));
		assertEquals(10, getStartingColumn(document, element));
		assertEquals(7, getEndingRow(document, element));
		assertEquals(57, getEndingColumn(document, element));
		offset = wholeDocumentString.indexOf("property name=\"property_in_target\""); //$NON-NLS-1$
		assertEquals(offset, element.getOffset());

		assertEquals(21, getEndingRow(document, rootProject));
		assertEquals(10, getEndingColumn(document, rootProject));
	}

	private int getColumn(IDocument document, int offset, int line) throws BadLocationException {
		return offset - document.getLineOffset(line - 1) + 1;
	}

	private int getStartingRow(IDocument document, IAntElement element) throws BadLocationException {
		return document.getLineOfOffset(element.getOffset()) + 1;
	}

	private int getEndingRow(IDocument document, IAntElement element) throws BadLocationException {
		return document.getLineOfOffset(element.getOffset() + element.getLength() - 1) + 1;
	}

	private int getStartingColumn(IDocument document, IAntElement element) throws BadLocationException {
		return getColumn(document, element.getOffset(), getStartingRow(document, element));
	}

	private int getEndingColumn(IDocument document, IAntElement element) throws BadLocationException {
		return getColumn(document, element.getOffset() + element.getLength() - 1, getEndingRow(document, element));
	}

	/**
	 * Tests the creation of the AntElementNode, that includes parsing a non-valid file.
	 */
	@Test
	public void testParsingOfNonValidFile() throws BadLocationException {
		AntModelForDocument model = new AntModelForDocument("buildtest2.xml"); //$NON-NLS-1$

		IAntElement root = model.getAntModel().getProjectNode();
		assertNotNull(root);

		List<IAntElement> children = root.getChildNodes();

		IDocument document = model.getDocument();
		// <target name="main">
		IAntElement element = children.get(2);
		assertEquals(5, getStartingRow(document, element));
		assertEquals(3, getStartingColumn(document, element)); // with tab in file
		assertEquals(5, getEndingRow(document, element));
		// main has no ending column as the element is not closed
		int offset = model.getDocument().get().indexOf("target name=\"main\""); //$NON-NLS-1$
		assertEquals(offset, element.getOffset());
	}

	/**
	 * Tests whether the outline can handle a build file with only the {@literal <project></project>} tags.
	 */
	@Test
	public void testWithProjectOnlyBuildFile() {
		AntModel model = new AntModelForDocument("projectOnly.xml").getAntModel(); //$NON-NLS-1$
		AntElementNode rootProject = model.getProjectNode();
		assertNotNull(rootProject);
	}

	/**
	 * Tests whether the outline can handle an empty build file.
	 */
	@Test
	public void testWithEmptyBuildFile() {
		AntModel model = new AntModelForDocument("empty.xml").getAntModel(); //$NON-NLS-1$
		AntElementNode rootProject = model.getProjectNode();
		assertNull(rootProject);
	}

	/**
	 * Some testing of getting the right location of tags.
	 */
	@Test
	public void testAdvancedTaskLocation() throws BadLocationException {
		AntModelForDocument model = new AntModelForDocument("outline_select_test_build.xml"); //$NON-NLS-1$

		AntElementNode rootProject = model.getAntModel().getProjectNode();
		// Get the content as string
		String wholeDocumentString = model.getDocument().get();

		IDocument document = model.getDocument();
		// <project>
		assertNotNull(rootProject);
		assertEquals(2, getStartingRow(document, rootProject));
		assertEquals(2, getStartingColumn(document, rootProject));
		int offset = wholeDocumentString.indexOf("project"); //$NON-NLS-1$

		assertEquals(offset, rootProject.getOffset());

		// <target name="properties">
		IAntElement element = rootProject.getChildNodes().get(1);
		assertNotNull(element);
		assertEquals("properties", element.getLabel()); //$NON-NLS-1$
		assertEquals(16, getStartingRow(document, element));
		assertEquals(3, getStartingColumn(document, element));
		offset = wholeDocumentString.indexOf("target name=\"properties\""); //$NON-NLS-1$

		assertEquals(offset, element.getOffset());
	}

	/**
	 * Tests if target is internal or not
	 */
	@Test
	public void testInternalTargets() {
		AntModel model = new AntModelForDocument("internalTargets.xml").getAntModel(); //$NON-NLS-1$
		assertTrue(model.getTargetNode("internal1").isInternal(), "Target without description should be internal"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(model.getTargetNode("-internal2").isInternal(), //$NON-NLS-1$
				"Target with name starting with '-' should be internal\""); //$NON-NLS-1$
		assertFalse(model.getTargetNode("non-internal").isInternal(), //$NON-NLS-1$
				"Target with description attribute should not be internal"); //$NON-NLS-1$
		assertFalse(model.getTargetNode("-default").isInternal(), "Default target should not be internal"); //$NON-NLS-1$ //$NON-NLS-2$
	}
}