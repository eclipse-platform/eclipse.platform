/*******************************************************************************
 * Copyright (c) 2008, 2009 Wind River Systems and others.
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
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.protocol;



/**
 * @see PDAFrameCommand
 */

public class PDAFrameCommandResult extends PDACommandResult {

	/**
	 * Frame data return by the frame command.
	 */
	final public PDAFrameData fFrame;

	PDAFrameCommandResult(String response) {
		super(response);
		fFrame = new PDAFrameData(response);
	}
}
