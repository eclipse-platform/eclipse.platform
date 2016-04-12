/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import org.eclipse.core.commands.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.team.core.diff.*;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.mapping.SaveableComparison;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelParticipantAction;
import org.eclipse.ui.PlatformUI;

/**
 * Action that performs an optimistic merge
 */
public class MergeIncomingChangesAction extends ModelParticipantAction implements IHandlerListener {

	IHandler handler;
	
	public MergeIncomingChangesAction(ISynchronizePageConfiguration configuration) {
		super(null, configuration);
		// TODO: We're past the API freeze so we need to access the property by string
		handler = (IHandler)configuration.getProperty("org.eclipse.team.ui.mergeAll"); //$NON-NLS-1$
		if (handler == null)
			handler = new MergeAllActionHandler(configuration);
		handler.addHandlerListener(this);
	}
	
	@Override
	public void runWithEvent(Event event) {
		if (handler == null || !handler.isEnabled())
			return;
		try {
			handleTargetSaveableChange();
		} catch (InvocationTargetException e) {
			handle(e);
			return;
		} catch (InterruptedException e) {
			// Canceled so return
			return;
		}
		try {
			handler.execute(new ExecutionEvent(null, Collections.EMPTY_MAP, event, null));
		} catch (ExecutionException e) {
			handle(e);
		}
	}
	
	private void handle(Throwable throwable) {
		if (throwable instanceof ExecutionException) {
			ExecutionException ee = (ExecutionException) throwable;
			if (ee.getCause() != null) {
				throwable = ee.getCause();
			}
		}
		Utils.handle(throwable);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.mapping.ModelProviderAction#isEnabledForSelection(org.eclipse.jface.viewers.IStructuredSelection)
	 */
	@Override
	protected boolean isEnabledForSelection(IStructuredSelection selection) {
		return handler.isEnabled();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.operations.ModelProviderAction#getDiffFilter()
	 */
	protected FastDiffFilter getDiffFilter() {
		return new FastDiffFilter() {
			@Override
			public boolean select(IDiff node) {
				if (node instanceof IThreeWayDiff) {
					IThreeWayDiff twd = (IThreeWayDiff) node;
					if (twd.getDirection() == IThreeWayDiff.CONFLICTING || twd.getDirection() == IThreeWayDiff.INCOMING) {
						return true;
					}
				}
				return false;
			}
		};
	}
	
	@Override
	protected void handleTargetSaveableChange() throws InvocationTargetException, InterruptedException {
		final SaveableComparison currentBuffer = getActiveSaveable();
		if (currentBuffer != null && currentBuffer.isDirty()) {
			PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {	
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try {
						handleTargetSaveableChange(getConfiguration().getSite().getShell(), null, currentBuffer, true, monitor);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
		}
		setActiveSaveable(null);
	}

	public void dispose() {
		handler.dispose();
	}

	/**
	 * @param handlerEvent
	 */
	@Override
	public void handlerChanged(HandlerEvent handlerEvent) {
		setEnabled(handler.isEnabled());
	}
	
}
