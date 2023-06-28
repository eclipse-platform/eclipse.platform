/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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
 *     Patrick Chuong (Texas Instruments) - Improve usability of the breakpoint view (Bug 238956)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.update;

import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointContainer;
import org.eclipse.debug.internal.ui.elements.adapters.DefaultBreakpointsViewInput;
import org.eclipse.debug.internal.ui.memory.provisional.AbstractAsyncTableRendering;
import org.eclipse.debug.internal.ui.memory.provisional.MemoryViewPresentationContext;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelProxyFactory;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.ui.IDebugUIConstants;

public class DefaultModelProxyFactory implements IModelProxyFactory {

	@Override
	public IModelProxy createModelProxy(Object element, IPresentationContext context) {
		String id = context.getId();
		if (IDebugUIConstants.ID_DEBUG_VIEW.equals(id)) {
			if (element instanceof IDebugTarget) {
				return new DebugTargetProxy((IDebugTarget)element);
			}
			if (element instanceof ILaunchManager) {
				return new LaunchManagerProxy();
			}
			if (element instanceof ILaunch) {
				return new LaunchProxy((ILaunch) element);
			}
			if (element instanceof IProcess) {
				return new ProcessProxy((IProcess)element);
			}
		}
		if (IDebugUIConstants.ID_VARIABLE_VIEW.equals(id)) {
			if (element instanceof IStackFrame) {
				return new DefaultVariableViewModelProxy((IStackFrame)element);
			}
		}
		if (IDebugUIConstants.ID_EXPRESSION_VIEW.equals(id)) {
			if (element instanceof IExpressionManager) {
				return new ExpressionManagerModelProxy();
			}
			if (element instanceof IWatchExpression) {
				return new DefaultWatchExpressionModelProxy((IWatchExpression)element);
			}
			if (element instanceof IExpression) {
				return new DefaultExpressionModelProxy((IExpression)element);
			}
		}
		if (IDebugUIConstants.ID_REGISTER_VIEW.equals(id)) {
			if (element instanceof IStackFrame) {
				return new DefaultVariableViewModelProxy((IStackFrame)element);
			}
		}
		if (IDebugUIConstants.ID_MEMORY_VIEW.equals(id)) {
			if (element instanceof IMemoryBlockRetrieval)
				return new MemoryRetrievalProxy((IMemoryBlockRetrieval)element);
		}
		if (IDebugUIConstants.ID_BREAKPOINT_VIEW.equals(id)) {
			if (element instanceof DefaultBreakpointsViewInput) {
				return new BreakpointManagerProxy(element, context);
			} else if (element instanceof IBreakpoint) {
				return new BreakpointProxy((IBreakpoint)element);
			} else if (element instanceof IBreakpointContainer) {
				return new BreakpointContainerProxy((IBreakpointContainer)element);
			}
		}

		if (context instanceof MemoryViewPresentationContext)
		{
			if (((MemoryViewPresentationContext)context).getRendering() instanceof AbstractAsyncTableRendering)
			{
				if (element instanceof IMemoryBlock)
					return new MemoryBlockProxy((IMemoryBlock)element);
			}
		}

		return null;
	}

}
