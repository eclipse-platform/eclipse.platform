/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import org.eclipse.core.commands.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Event;
import org.eclipse.team.internal.ui.*;
import org.eclipse.team.ui.mapping.*;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.PlatformUI;

/**
 * An action that delegates to an appropriate handler when performing 
 * a merge opereration.
 * 
 * @since 3.2
 */
public class MergeAction extends Action {
	
	private final String handlerId;
	private final CommonMenuManager manager;
	private final ISynchronizePageConfiguration configuration;
	private IHandler defaultHandler;
	
	public MergeAction(String handlerId, CommonMenuManager manager, ISynchronizePageConfiguration configuration) {
		Assert.isNotNull(handlerId);
		Assert.isNotNull(manager);
		Assert.isNotNull(configuration);
		this.handlerId = handlerId;
		this.manager = manager;
		this.configuration = configuration;
	}
	
	@Override
	public void runWithEvent(Event event) {
		IHandler handler = getHandler();
		if (handler != null && handler.isEnabled()) {
			final SaveableComparison currentBuffer = ((ModelSynchronizeParticipant)configuration.getParticipant()).getActiveSaveable();
			if (currentBuffer != null && currentBuffer.isDirty()) {
				SaveableComparison targetBuffer = null;
				if (handler instanceof MergeActionHandler) {
					MergeActionHandler mah = (MergeActionHandler) handler;
					targetBuffer = mah.getSaveable();
				}
				final SaveableComparison target = targetBuffer;
				try {
					PlatformUI.getWorkbench().getProgressService().run(true, true, new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException,
								InterruptedException {
							try {
								ModelParticipantAction.handleTargetSaveableChange(configuration.getSite().getShell(), target, currentBuffer, true, monitor);
							} catch (CoreException e) {
								throw new InvocationTargetException(e);
							}
						}			
					});
				} catch (InvocationTargetException e) {
					Utils.handle(e);
					return;
				} catch (InterruptedException e) {
					return;
				}
				((ModelSynchronizeParticipant)configuration.getParticipant()).setActiveSaveable(targetBuffer);
			}
			try {
				handler.execute(new ExecutionEvent(null, Collections.EMPTY_MAP, event, null));
			} catch (ExecutionException e) {
				handle(e);
			}
		}
	}

	/**
	 * Handle the given exception. By default, the user is prompted
	 * @param exception the exception
	 */
	protected void handle(Throwable exception) {
		if (exception instanceof ExecutionException) {
			ExecutionException ee = (ExecutionException) exception;
			if (ee.getCause() != null) {
				handle(exception.getCause());
			}
		}
		IStatus status;
		if (exception instanceof CoreException) {
			CoreException ce = (CoreException) exception;
			status = ce.getStatus();
		} else {
			status = new Status(IStatus.ERROR, TeamUIPlugin.ID, 0, TeamUIMessages.exception, exception);
		}
		ErrorDialog.openError(configuration.getSite().getShell(), null, null, status);
	}

	private IHandler getHandler() {
		IHandler handler = manager.getHandler(handlerId);
		if (handler == null) {
			if (defaultHandler == null)
				defaultHandler = getDefaultHandler();
			return defaultHandler;
		}
		return handler;
	}
	
	private IHandler getDefaultHandler() {
		return MergeActionHandler.getDefaultHandler(handlerId, configuration);
	}

	private boolean calculateEnablement() {
		IHandler handler = getHandler();
		return handler != null && handler.isEnabled();
	}

	public void dispose() {
		if (defaultHandler != null)
			defaultHandler.dispose();
	}
	
	public void update() {
		setEnabled(calculateEnablement());
	}

}
