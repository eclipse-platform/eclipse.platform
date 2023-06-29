/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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
 *     Bjorn Freeman-Benson - initial API and implementation
 *     Pawel Piech (Wind River) - ported PDA Virtual Machine to Java (Bug 261400)
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.examples.core.pda.DebugCorePlugin;
import org.eclipse.debug.examples.core.pda.protocol.PDACommand;
import org.eclipse.debug.examples.core.pda.protocol.PDACommandResult;


/**
 * Common function for PDA debug elements.
 */
public class PDADebugElement extends DebugElement {

	/**
	 * Constructs a new debug element in the given target.
	 *
	 * @param target debug target
	 */
	public PDADebugElement(IDebugTarget target) {
		super(target);
	}

	@Override
	public String getModelIdentifier() {
		return DebugCorePlugin.ID_PDA_DEBUG_MODEL;
	}

	/**
	 * Sends a request to the PDA interpreter, waits for and returns the reply.
	 *
	 * @param request command
	 * @return reply
	 * @throws DebugException if the request fails
	 *
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDATerminateCommand
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDAVMSuspendCommand
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDAVMResumeCommand
	 *
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDASuspendCommand
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDAResumeCommand
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDAStepCommand
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDADropFrameCommand
	 *
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDASetBreakpointCommand
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDAClearBreakpointCommand
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDAWatchCommand
	 *
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDADataCommand
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDASetDataCommand
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDAPopDataCommand
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDAPushDataCommand
	 *
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDAEvalCommand
	 *
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDAEventStopCommand
	 *
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDAStackCommand
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDAStackDepthCommand
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDAFrameCommand
	 *
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDASetVarCommand
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDAVarCommand
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDAChildrenCommand
	 *
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDAGroupsCommand
	 * @see org.eclipse.debug.examples.core.pda.protocol.PDARegistersCommand
	 *
	 * @since 3.5
	 */
	public PDACommandResult sendCommand(PDACommand command) throws DebugException {
		return getPDADebugTarget().sendCommand(command);
	}

	/**
	 * Returns the debug target as a PDA target.
	 *
	 * @return PDA debug target
	 */
	protected PDADebugTarget getPDADebugTarget() {
		return (PDADebugTarget) getDebugTarget();
	}

	/**
	 * Returns the breakpoint manager
	 *
	 * @return the breakpoint manager
	 */
	protected IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();
	}
}
