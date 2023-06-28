/*******************************************************************************
 * Copyright (c) 2008, 2013 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.protocol;


/**
 * Retrieves data stack information
 *
 * <pre>
 *    C: children {thread_id} {frame_id} {variable_name}
 *    R: {child variable 1}|{child variable 2}|{child variable 3}|...|
 *
 * Errors:
 *    error: invalid thread
 * </pre>
 */
public class PDAChildrenCommand extends PDACommand {

	public PDAChildrenCommand(int threadId, int frameId, String name  ) {
		super("children " + threadId + " " + frameId + " " + name); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Override
	public PDACommandResult createResult(String resultText) {
		return new PDAListResult(resultText);
	}
}
