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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.SerializationException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.cheatsheets.CommandRunner;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheetCommand;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests which exercise the CommandRunner class
 */

public class TestCommandExecution {

	private static final String PARAM2_VALUE = "bar"; //$NON-NLS-1$
	private static final String PARAM1_VALUE = "foo"; //$NON-NLS-1$

	private static final String PARAM1_ID = "param1_id"; //$NON-NLS-1$
	private static final String PARAM2_ID = "param2_id"; //$NON-NLS-1$
	private static final String COMMAND_ID =
		"org.eclipse.ui.cheatsheets.tests.command1"; //$NON-NLS-1$
	private static final String SERIALIZED_COMMAND = COMMAND_ID + '('
		+ PARAM1_ID + '=' + PARAM1_VALUE + ','
		+ PARAM2_ID + '=' + PARAM2_VALUE + ')';
	private static final String RETURN_STORE = "retData";
	private static final String PARENT_RETURN_STORE = "parent.retData";

	@Before
	public void setUp() throws Exception {
		CommandHandler.reset();
	}

	public CheatSheetManager createManager() {
		CheatSheetElement element = new CheatSheetElement("Name");
		element.setID("TestCommandExecutionId");
		return new CheatSheetManager(element);
	}

	private ICommandService getService() {
		IWorkbench wb = PlatformUI.getWorkbench();
		Object serviceObject = wb.getAdapter(ICommandService.class);
		if (serviceObject != null) {
			ICommandService service = (ICommandService)serviceObject;
			return service;
		}
		return null;
	}

	private IHandlerService getHandlerService() {
		IWorkbench wb =	PlatformUI.getWorkbench();
		if (wb != null) {
			Object serviceObject = wb.getAdapter(IHandlerService.class);
			if (serviceObject != null) {
				IHandlerService service = (IHandlerService)serviceObject;
				return service;
			}
		}
		return null;
	}

	/**
	 * Execute a command without using the command runner class
	 */
	@Test
	public void testExecuteCommand() {
		ParameterizedCommand selectedCommand;
		try {
			selectedCommand = getService().deserialize(SERIALIZED_COMMAND);
			getHandlerService().executeCommand(selectedCommand, null);
		} catch (NotDefinedException e) {
			fail("Command not defined");
		} catch (SerializationException e) {
			fail("Bad serialization");
		} catch (ExecutionException e) {
			fail("Execution exception");
		} catch (NotEnabledException e) {
			fail("Not enabled exception");
		} catch (NotHandledException e) {
			fail("Not handled exception");
		}

		checkCommandExecution();
	}

	private void checkCommandExecution() {
		assertTrue(CommandHandler.getTimesCompleted() == 1);
		Map<?, ?> params = CommandHandler.getParams();
		assertEquals(2, params.size());
		assertTrue(params.containsKey(PARAM1_ID));
		assertEquals(PARAM1_VALUE, params.get(PARAM1_ID));
		assertTrue(params.containsKey(PARAM2_ID));
		assertEquals(PARAM2_VALUE, params.get(PARAM2_ID));
	}

	@Test
	public void testCommandRunner() {
		CheatSheetCommand command = new CheatSheetCommand();
		CheatSheetManager csm = createManager();
		command.setSerialization(SERIALIZED_COMMAND);

		IStatus status = new CommandRunner().executeCommand(command, csm);
		assertTrue(status.isOK());

		checkCommandExecution();
	}

	@Test
	public void testCommandWithResult() {
		CheatSheetCommand command = new CheatSheetCommand();
		CheatSheetManager csm = createManager();
		command.setSerialization(SERIALIZED_COMMAND);
		command.setReturns(RETURN_STORE);

		IStatus status = new CommandRunner().executeCommand(command, csm);
		assertTrue(status.isOK());
		String result = csm.getData(RETURN_STORE);
		assertNotNull(result);
		assertEquals(CommandHandler.RESULT_TO_STRING, result);
		checkCommandExecution();
	}

	/**
	 * Test that if the return is set to parent.retData the
	 * return value is written to the parent cheat sheet manager.
	 */
	@Test
	public void testCommandWithQualifiedResult() {
		CheatSheetCommand command = new CheatSheetCommand();
		CheatSheetManager csm = createManager();
		CheatSheetManager parentManager = createManager();
		csm.setParent(parentManager);
		command.setSerialization(SERIALIZED_COMMAND);
		command.setReturns(PARENT_RETURN_STORE);

		IStatus status = new CommandRunner().executeCommand(command, csm);
		assertTrue(status.isOK());
		assertNull(csm.getData(RETURN_STORE));
		assertNotNull(parentManager.getData(RETURN_STORE));
	}

	@Test
	public void testInvalidCommandId() {
		CheatSheetCommand command = new CheatSheetCommand();
		CheatSheetManager csm = createManager();
		command.setSerialization(COMMAND_ID + ".invalid");	 //$NON-NLS-1$
		IStatus status = new CommandRunner().executeCommand(command, csm);
		assertFalse(status.isOK());
	}

	@Test
	public void testCommandException() {
		CheatSheetCommand command = new CheatSheetCommand();
		CheatSheetManager csm = createManager();
		command.setSerialization(SERIALIZED_COMMAND);
		CommandHandler.setThrowException(true);

		IStatus status = new CommandRunner().executeCommand(command, csm);
		assertFalse(status.isOK());
	}

	private static final String NEGATE_INTEGER_COMMAND_ID = "org.eclipse.ui.cheatsheets.tests.NegateIntegerCommand(number=123)";
	private static final String INT_RETURN_STORE = "intData";

	@Test
	public void testCommandWithIntegerValues() {
		CheatSheetCommand command = new CheatSheetCommand();
		CheatSheetManager csm = createManager();
		command.setSerialization(NEGATE_INTEGER_COMMAND_ID);
		command.setReturns(INT_RETURN_STORE);

		IStatus status = new CommandRunner().executeCommand(command, csm);
		assertTrue(status.isOK());
		String result = csm.getData(INT_RETURN_STORE);
		assertNotNull(result);
		assertEquals("-123", result);
	}
}
