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

package org.eclipse.ua.tests.cheatsheet.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.internal.cheatsheets.ActionRunner;
import org.eclipse.ui.internal.cheatsheets.data.Action;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.FrameworkUtil;

public class TestActionExecution {

	private static final String ACTION_PACKAGE =
		"org.eclipse.ua.tests.cheatsheet.execution"; //$NON-NLS-1$
	private static final String SIMPLE_ACTION_CLASS =
		ACTION_PACKAGE + ".SimpleAction"; //$NON-NLS-1$
	private static final String FAILING_ACTION_CLASS =
		ACTION_PACKAGE + ".FailingAction"; //$NON-NLS-1$
	private static final String ACTION_WITH_PARAMETERS_CLASS =
		ACTION_PACKAGE + ".ActionWithParameters"; //$NON-NLS-1$

	@BeforeEach
	public void setUp() throws Exception {
		ActionEnvironment.reset();
	}

	@AfterAll
	public static void tearDown() throws Exception {
		ActionEnvironment.reset();
	}

	@Test
	public void testSimpleAction() {
		Action action = new Action();
		action.setClass(SIMPLE_ACTION_CLASS);
		action.setParams(new String[0]);
		action.setPluginID(getPluginId());
		IStatus status = new ActionRunner().runAction(action, null);
		assertTrue(status.isOK());
		assertEquals(1, ActionEnvironment.getTimesCompleted());
	}

	@Test
	public void testInvalidAction() {
		Action action = new Action();
		action.setClass(SIMPLE_ACTION_CLASS + "invalid"); //$NON-NLS-1$
		action.setParams(new String[0]);
		action.setPluginID(getPluginId());
		IStatus status = new ActionRunner().runAction(action, null);
		assertEquals(IStatus.ERROR, status.getSeverity());
		assertEquals(0, ActionEnvironment.getTimesCompleted());
	}

	@Test
	public void testSimpleActionWithException() {
		Action action = new Action();
		action.setClass(SIMPLE_ACTION_CLASS);
		action.setParams(new String[0]);
		action.setPluginID(getPluginId());
		ActionEnvironment.setThrowException(true);
		IStatus status = new ActionRunner().runAction(action, null);
		assertEquals(IStatus.ERROR, status.getSeverity());
		assertEquals(RuntimeException.class, status.getException().getClass());
	}

	@Test
	public void testFailingAction() {
		Action action = new Action();
		action.setClass(FAILING_ACTION_CLASS);
		action.setParams(new String[0]);
		action.setPluginID(getPluginId());
		IStatus status = new ActionRunner().runAction(action, null);
		assertEquals(IStatus.WARNING, status.getSeverity());
	}

	@Test
	public void testActionWithParameters() {
		Action action = new Action();
		action.setClass(ACTION_WITH_PARAMETERS_CLASS);
		String value0 = "abc"; //$NON-NLS-1$
		String value1 = "defg"; //$NON-NLS-1$
		final String[] params = {value0, value1};
		action.setParams(params);
		action.setPluginID(getPluginId());
		IStatus status = new ActionRunner().runAction(action, null);
		assertTrue(status.isOK());
		assertEquals(1, ActionEnvironment.getTimesCompleted());
		String[] actuals = ActionEnvironment.getParams();
		assertThat(actuals).containsExactly(value0, value1);
	}

	private String getPluginId() {
		return FrameworkUtil.getBundle(getClass()).getSymbolicName();
	}

}
