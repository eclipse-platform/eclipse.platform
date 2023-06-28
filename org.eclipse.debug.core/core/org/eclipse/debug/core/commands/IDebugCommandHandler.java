/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
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
package org.eclipse.debug.core.commands;



/**
 * Handles a command for a debugger. Specific command handlers extend this interface.
 * <p>
 * The debug platform provides actions for common debug commands that operate against
 * these handler interfaces. For example, the platform provides a terminate action that
 * operates on the active debug context (selected element in the debug view). The action
 * delegates to the active context's {@link ITerminateHandler} implementation to update
 * its enabled state and execute the command. Debug model elements may implement supported
 * command handler interfaces directly or provide them as adapters. The debug platform
 * provides implementations of handlers for standard debug models.
 * </p>
 * @see org.eclipse.core.runtime.IAdaptable
 * @see IDisconnectHandler
 * @see IDropToFrameHandler
 * @see IResumeHandler
 * @see IStepFiltersHandler
 * @see IStepIntoHandler
 * @see IStepOverHandler
 * @see IStepReturnHandler
 * @see ISuspendHandler
 * @see ITerminateHandler
 * @since 3.3
 */
public interface IDebugCommandHandler {

	/**
	 * Determines whether this handler can execute on the elements specified
	 * in the given request by reporting enabled state to the request.
	 * <p>
	 * Implementations must be non-blocking and may respond asynchronously to the
	 * given request. Errors can reported by setting an appropriate status
	 * on the given request. A request can be canceled by this handler or caller.
	 * A <code>null</code> status is equivalent to an OK status.
	 * When a request succeeds, fails, or is canceled, implementations must call
	 * <code>done()</code> on the given request.
	 * </p>
	 * <p>
	 * Clients are expected to poll the request (using <code>isCanceled</code>)
	 * periodically and abort at their earliest convenience calling <code>done()</code>
	 * on the request.
	 * </p>
	 * @param request specifies elements to operate on and collects enabled state
	 */
	void canExecute(IEnabledStateRequest request);

	/**
	 * Executes this command on the elements specified in the given request
	 * reporting status to the given request and returns whether this handler should
	 * remain enabled while the command is executing.
	 * <p>
	 * Implementations must be non-blocking and may respond asynchronously to the
	 * given request. Errors can reported by setting an appropriate status
	 * on the given request. A request can be canceled by this handler or the caller.
	 * A <code>null</code> status is equivalent to an OK status. When a request is
	 * complete, has encountered an error, or canceled, implementations must call
	 * <code>done()</code> on the given collector.
	 * </p>
	 * <p>
	 * Handlers are expected to poll the request (using <code>isCanceled</code>)
	 * periodically and abort at their earliest convenience calling <code>done()</code>
	 * on the request.
	 * </p>
	 * @param request specifies elements to operate on and collects execution status
	 * @return whether this handler remains enabled while command is executing
	 */
	boolean execute(IDebugCommandRequest request);

}
