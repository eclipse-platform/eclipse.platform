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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

/**
 * Handler used for testing which keeps a count of how many times it was called
 * and remembers the parameters from the last invocation
 */
public class CommandHandler extends AbstractHandler {

	public static final String RESULT_TO_STRING = "RESULT_TO_STRING";

	private static Map<String, String> params;
	private static int timesCompleted;
	private static boolean throwException;

	public static void reset() {
		params = null;
		timesCompleted = 0;
		throwException = false;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		if (throwException) {
			throw new RuntimeException();
		}
		// Copy all the parameters
		params = new HashMap<>();
		params.putAll(event.getParameters());

		timesCompleted++;

		return RESULT_TO_STRING;
	}

	public static Map<String, String> getParams() {
		return params;
	}

	public static int getTimesCompleted() {
		return timesCompleted;
	}

	public static void setThrowException(boolean doThrowException) {
		throwException = doThrowException;
	}

}
