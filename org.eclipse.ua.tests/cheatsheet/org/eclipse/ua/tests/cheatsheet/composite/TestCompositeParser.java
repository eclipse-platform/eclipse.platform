/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.cheatsheet.composite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ua.tests.cheatsheet.util.StatusCheck;
import org.eclipse.ua.tests.util.ResourceFinder;
import org.eclipse.ui.internal.cheatsheets.composite.model.AbstractTask;
import org.eclipse.ui.internal.cheatsheets.composite.model.CompositeCheatSheetModel;
import org.eclipse.ui.internal.cheatsheets.composite.model.EditableTask;
import org.eclipse.ui.internal.cheatsheets.composite.parser.CompositeCheatSheetParser;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class TestCompositeParser {

	private static final String COMPOSITES_FOLDER = "data/cheatsheet/composite/";
	private CompositeCheatSheetParser parser;

	@Before
	public void setUp() throws Exception {
		parser = new CompositeCheatSheetParser();
	}

	private CompositeCheatSheetModel parseTestFile(String path) {
		URL testURL = ResourceFinder.findFile(FrameworkUtil.getBundle(TestCompositeParser.class),
				COMPOSITES_FOLDER + path);
		return parser.parseGuide(testURL);
	}

	@Test
	public void testNullInput() {
		assertNull(parser.parseGuide(null));
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "Could not open");
	}

	@Test
	public void testBadURL() {
		try {
			assertNull(parser.parseGuide(new URL("file:/nonexistent")));
		} catch (MalformedURLException e) {
			fail("Exception thrown");
		}
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "Could not open");
	}

	@Test
	public void testSimpleGuide() {
		CompositeCheatSheetModel model = parseTestFile("SingleTask.xml");
		assertNotNull(model);
		assertTrue(parser.getStatus().isOK());
		assertFalse(((EditableTask)model.getRootTask()).isSkippable());
	}

	@Test
	public void testNoTasks() {
		CompositeCheatSheetModel model = parseTestFile("GuideWithoutTasks.xml");
		assertNull(model);
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "Missing root task");
	}

	@Test
	public void testInvalidRoot() {
		assertNull(parseTestFile("InvalidRoot.xml"));
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "Root node is not");
	}

	@Test
	public void testTwoRootTasksGuide() {
		assertNull(parseTestFile("TwoRootTasks.xml"));
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "more than one root task");
	}

	@Test
	public void testParamNoName() {
		assertNotNull(parseTestFile("ParamNoName.xml"));
		assertEquals(IStatus.WARNING, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "Parameter has no name");
	}

	@Test
	public void testParamNoValue() {
		assertNotNull(parseTestFile("ParamNoValue.xml"));
		assertEquals(IStatus.WARNING, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "Parameter has no value");
	}

	@Test
	public void testValidParameters() {
		CompositeCheatSheetModel model = parseTestFile("ValidParameter.xml");
		assertNotNull(model);
		assertTrue(parser.getStatus().isOK());
		Dictionary<String, String> params = model.getRootTask().getParameters();
		assertEquals(1, params.size());
		assertEquals("b", params.get("a"));
	}

	@Test
	public void testDependency() {
		CompositeCheatSheetModel model = parseTestFile("TaskDependency.xml");
		assertNotNull(model);
		assertTrue(parser.getStatus().isOK());
		AbstractTask task1 = model.getDependencies().getTask("task1");
		AbstractTask task2 = model.getDependencies().getTask("task2");
		assertTrue(task1.getRequiredTasks().length == 0);
		assertTrue(task1.getSuccessorTasks().length == 1);
		assertEquals(task2, task1.getSuccessorTasks()[0]);
		assertTrue(task2.getSuccessorTasks().length == 0);
		assertTrue(task2.getRequiredTasks().length == 1);
		assertEquals(task1, task2.getRequiredTasks()[0]);
		assertTrue(task1.isSkippable());
		assertFalse(task2.isSkippable());
	}

	@Test
	public void testBackwardDependency() {
		CompositeCheatSheetModel model = parseTestFile("BackwardDependency.xml");
		assertNotNull(model);
		assertTrue(parser.getStatus().isOK());
		AbstractTask task1 = model.getDependencies().getTask("task1");
		AbstractTask task2 = model.getDependencies().getTask("task2");
		assertTrue(task1.getRequiredTasks().length == 0);
		assertTrue(task1.getSuccessorTasks().length == 1);
		assertEquals(task2, task1.getSuccessorTasks()[0]);
		assertTrue(task2.getSuccessorTasks().length == 0);
		assertTrue(task2.getRequiredTasks().length == 1);
		assertEquals(task1, task2.getRequiredTasks()[0]);
	}

	@Test
	public void testDependencyWithoutId() {
		assertNull(parseTestFile("DependencyWithoutId.xml"));
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "Missing task id");
	}

	@Test
	public void testDependencyWithInvalidId() {
		assertNull(parseTestFile("DependencyInvalidId.xml"));
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "Invalid id");
	}

	@Test
	public void testCircularDependency() {
		assertNull(parseTestFile("CircularDependency.xml"));
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertMultiStatusContains(parser.getStatus(), "Cycle detected");
	}

	@Test
	public void testSelfDependency() {
		assertNull(parseTestFile("SelfDependency.xml"));
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertMultiStatusContains(parser.getStatus(), "Cycle detected");
	}

	@Test
	public void testDuplicateId() {
		assertNull(parseTestFile("DuplicateTaskId.xml"));
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "Duplicate task id");
	}

	@Test
	public void testNoTaskKind() {
		assertNull(parseTestFile("LeafTaskWithoutKind.xml"));
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "Missing kind attribute in task");
	}

	@Test
	public void testLeafTaskInvalidKind() {
		assertNull(parseTestFile("LeafTaskInvalidKind.xml"));
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "Invalid kind");
	}

	@Test
	public void testLeafTaskNoName() {
		assertNull(parseTestFile("TaskNoName.xml"));
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "Missing name attribute in task");
	}

	@Test
	public void testCompositeNoName() {
		assertNull(parseTestFile("CompositeNoName.xml"));
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "Missing name attribute in composite");
	}

	@Test
	public void testTaskGroupInvalidKind() {
		assertNull(parseTestFile("InvalidTaskGroupKind.xml"));
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "Invalid kind");
	}

	@Test
	public void testSetNoChild() {
		assertNull(parseTestFile("SetWithNoChildren.xml"));
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "Task group");
		StatusCheck.assertStatusContains(parser.getStatus(), "has no children");
	}

	@Test
	public void testChoiceNoChild() {
		assertNull(parseTestFile("EmptyChoice.xml"));
		assertEquals(IStatus.ERROR, parser.getStatus().getSeverity());
		StatusCheck.assertStatusContains(parser.getStatus(), "has no children");
	}



}
