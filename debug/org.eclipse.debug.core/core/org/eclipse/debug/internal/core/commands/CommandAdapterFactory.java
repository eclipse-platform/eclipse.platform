/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.debug.internal.core.commands;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.commands.IDisconnectHandler;
import org.eclipse.debug.core.commands.IDropToFrameHandler;
import org.eclipse.debug.core.commands.IResumeHandler;
import org.eclipse.debug.core.commands.IStepFiltersHandler;
import org.eclipse.debug.core.commands.IStepIntoHandler;
import org.eclipse.debug.core.commands.IStepOverHandler;
import org.eclipse.debug.core.commands.IStepReturnHandler;
import org.eclipse.debug.core.commands.ISuspendHandler;
import org.eclipse.debug.core.commands.ITerminateHandler;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDisconnect;
import org.eclipse.debug.core.model.IDropToFrame;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStep;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.core.model.ITerminate;

/**
 * Adapter factory for debug commands.
 *
 * @since 3.3
 *
 */
public class CommandAdapterFactory implements IAdapterFactory {


	private static ITerminateHandler fgTerminateCommand = new TerminateCommand();
	private static IStepOverHandler fgStepOverCommand = new StepOverCommand();
	private static IStepIntoHandler fgStepIntoCommand = new StepIntoCommand();
	private static IStepReturnHandler fgStepReturnCommand = new StepReturnCommand();
	private static IDropToFrameHandler fgDropToFrameCommand = new DropToFrameCommand();
	private static IDisconnectHandler fgDisconnectCommand = new DisconnectCommand();
	private static ISuspendHandler fgSuspendCommand = new SuspendCommand();
	private static IResumeHandler fgResumeCommand = new ResumeCommand();
	private static IStepFiltersHandler fgStepFiltersCommand = new StepFiltersCommand();

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (IStepFiltersHandler.class.equals(adapterType)) {
			if (adaptableObject instanceof IDebugElement ||
				adaptableObject instanceof ILaunch ||
				adaptableObject instanceof IProcess) {
				return (T) fgStepFiltersCommand;
			}
		}

		if (ITerminateHandler.class.equals(adapterType)) {
			if (adaptableObject instanceof ITerminate) {
				return (T) fgTerminateCommand;
			}
		}
		if (IStepOverHandler.class.equals(adapterType)) {
			if (adaptableObject instanceof IStep) {
				return (T) fgStepOverCommand;
			}
		}
		if (IStepIntoHandler.class.equals(adapterType)) {
			if (adaptableObject instanceof IStep) {
				return (T) fgStepIntoCommand;
			}
		}
		if (IStepReturnHandler.class.equals(adapterType)) {
			if (adaptableObject instanceof IStep) {
				return (T) fgStepReturnCommand;
			}
		}
		if (ISuspendHandler.class.equals(adapterType)) {
			if (adaptableObject instanceof ISuspendResume) {
				return (T) fgSuspendCommand;
			}
		}
		if (IResumeHandler.class.equals(adapterType)) {
			if (adaptableObject instanceof ISuspendResume) {
				return (T) fgResumeCommand;
			}
		}
		if (IDisconnectHandler.class.equals(adapterType)) {
			if (adaptableObject instanceof IDisconnect) {
				return (T) fgDisconnectCommand;
			}
		}
		if (IDropToFrameHandler.class.equals(adapterType)) {
			if (adaptableObject instanceof IDropToFrame) {
				return (T) fgDropToFrameCommand;
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[]{
				ITerminateHandler.class,
				IStepOverHandler.class,
				IStepIntoHandler.class,
				IStepReturnHandler.class,
				ISuspendHandler.class,
				IResumeHandler.class,
				IDropToFrameHandler.class,
				IDisconnectHandler.class,
				IStepFiltersHandler.class};
	}

}
