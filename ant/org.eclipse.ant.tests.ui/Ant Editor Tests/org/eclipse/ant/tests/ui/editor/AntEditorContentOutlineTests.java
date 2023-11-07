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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.ant.internal.ui.model.IAntElement;
import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.jface.text.BadLocationException;
import org.junit.Test;

/**
 * Tests the correct creation of the outline for an xml file.
 */
public class AntEditorContentOutlineTests extends AbstractAntUITest {

	/**
	 * Tests the creation of the AntElementNode, that includes parsing a file and determining the correct location of the tags.
	 */
	@Test
	public void testCreationOfOutlineTree() throws BadLocationException {
		AntModel model = getAntModel("buildtest1.xml"); //$NON-NLS-1$

		AntElementNode rootProject = model.getProjectNode();

		assertNotNull(rootProject);

		// Get the content as string
		String wholeDocumentString = getCurrentDocument().get();

		// <project>
		assertEquals(2, getStartingRow(rootProject));
		assertEquals(2, getStartingColumn(rootProject));
		int offset = wholeDocumentString.indexOf("project"); //$NON-NLS-1$
		assertEquals(offset, rootProject.getOffset());

		List<IAntElement> children = rootProject.getChildNodes();

		// <property name="propD">
		IAntElement element = children.get(0);
		assertEquals(3, getStartingRow(element));
		assertEquals(3, getStartingColumn(element)); // with tab in file
		assertEquals(3, getEndingRow(element));
		assertEquals(39, getEndingColumn(element)); // with tab in file

		offset = wholeDocumentString.indexOf("property"); //$NON-NLS-1$
		assertEquals(offset, element.getOffset());
		int length = "<property name=\"propD\" value=\"valD\" />".length(); //$NON-NLS-1$
		assertEquals(length - 1, element.getLength()); // we do not include the first '<'

		// <property file="buildtest1.properties">
		element = children.get(1);
		assertEquals(4, getStartingRow(element));
		assertEquals(6, getStartingColumn(element)); // no tab
		assertEquals(4, getEndingRow(element));
		assertEquals(45, getEndingColumn(element));

		// <property name="propV">
		element = children.get(2);
		assertEquals(5, getStartingRow(element));
		assertEquals(6, getStartingColumn(element));
		assertEquals(5, getEndingRow(element));
		assertEquals(42, getEndingColumn(element));

		// <target name="main">
		element = children.get(3);
		assertEquals(6, getStartingRow(element));
		assertEquals(6, getStartingColumn(element));
		assertEquals(9, getEndingRow(element));
		assertEquals(13, getEndingColumn(element));

		// <property name="property_in_target">
		element = element.getChildNodes().get(0);
		assertEquals(7, getStartingRow(element));
		assertEquals(10, getStartingColumn(element));
		assertEquals(7, getEndingRow(element));
		assertEquals(57, getEndingColumn(element));
		offset = wholeDocumentString.indexOf("property name=\"property_in_target\""); //$NON-NLS-1$
		assertEquals(offset, element.getOffset());

		assertEquals(21, getEndingRow(rootProject));
		assertEquals(10, getEndingColumn(rootProject));
	}

	private int getColumn(int offset, int line) throws BadLocationException {
		return offset - getCurrentDocument().getLineOffset(line - 1) + 1;
	}

	private int getStartingRow(IAntElement element) throws BadLocationException {
		return getCurrentDocument().getLineOfOffset(element.getOffset()) + 1;
	}

	private int getEndingRow(IAntElement element) throws BadLocationException {
		return getCurrentDocument().getLineOfOffset(element.getOffset() + element.getLength() - 1) + 1;
	}

	private int getStartingColumn(IAntElement element) throws BadLocationException {
		return getColumn(element.getOffset(), getStartingRow(element));
	}

	private int getEndingColumn(IAntElement element) throws BadLocationException {
		return getColumn(element.getOffset() + element.getLength() - 1, getEndingRow(element));
	}

	/**
	 * Tests the creation of the AntElementNode, that includes parsing a non-valid file.
	 */
	@Test
	public void testParsingOfNonValidFile() throws BadLocationException {
		AntModel model = getAntModel("buildtest2.xml"); //$NON-NLS-1$

		IAntElement root = model.getProjectNode();
		assertNotNull(root);

		List<IAntElement> children = root.getChildNodes();

		// <target name="main">
		IAntElement element = children.get(2);
		assertEquals(5, getStartingRow(element));
		assertEquals(3, getStartingColumn(element)); // with tab in file
		assertEquals(5, getEndingRow(element));
		// main has no ending column as the element is not closed
		int offset = getCurrentDocument().get().indexOf("target name=\"main\""); //$NON-NLS-1$
		assertEquals(offset, element.getOffset());
	}

	/**
	 * Tests whether the outline can handle a build file with only the {@literal <project></project>} tags.
	 */
	@Test
	public void testWithProjectOnlyBuildFile() {
		AntModel model = getAntModel("projectOnly.xml"); //$NON-NLS-1$
		AntElementNode rootProject = model.getProjectNode();
		assertNotNull(rootProject);
	}

	/**
	 * Tests whether the outline can handle an empty build file.
	 */
	@Test
	public void testWithEmptyBuildFile() {
		AntModel model = getAntModel("empty.xml"); //$NON-NLS-1$
		AntElementNode rootProject = model.getProjectNode();
		assertTrue(rootProject == null);
	}

	/**
	 * Some testing of getting the right location of tags.
	 */
	@Test
	public void testAdvancedTaskLocation() throws BadLocationException {
		AntModel model = getAntModel("outline_select_test_build.xml"); //$NON-NLS-1$

		AntElementNode rootProject = model.getProjectNode();
		// Get the content as string
		String wholeDocumentString = getCurrentDocument().get();

		// <project>
		assertNotNull(rootProject);
		assertEquals(2, getStartingRow(rootProject));
		assertEquals(2, getStartingColumn(rootProject));
		int offset = wholeDocumentString.indexOf("project"); //$NON-NLS-1$

		assertEquals(offset, rootProject.getOffset());

		// <target name="properties">
		IAntElement element = rootProject.getChildNodes().get(1);
		assertNotNull(element);
		assertEquals("properties", element.getLabel()); //$NON-NLS-1$
		assertEquals(16, getStartingRow(element));
		assertEquals(3, getStartingColumn(element));
		offset = wholeDocumentString.indexOf("target name=\"properties\""); //$NON-NLS-1$

		assertEquals(offset, element.getOffset());
	}

	/**
	 * Tests if target is internal or not
	 */
	@Test
	public void testInternalTargets() {
		AntModel model = getAntModel("internalTargets.xml"); //$NON-NLS-1$
		assertTrue("Target without description should be internal", model.getTargetNode("internal1").isInternal()); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Target with name starting with '-' should be internal", model.getTargetNode("-internal2").isInternal()); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse("Target with description attribute should not be internal", model.getTargetNode("non-internal").isInternal()); //$NON-NLS-1$ //$NON-NLS-2$
		assertFalse("Default target should not be internal", model.getTargetNode("-default").isInternal()); //$NON-NLS-1$ //$NON-NLS-2$
	}
}