/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Unit tests for the classes which execute commands and actions from a cheatsheet
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	TestVariableSubstitution.class,
	TestActionExecution.class,
	TestCommandExecution.class
})
public class AllExecutionTests {
}
