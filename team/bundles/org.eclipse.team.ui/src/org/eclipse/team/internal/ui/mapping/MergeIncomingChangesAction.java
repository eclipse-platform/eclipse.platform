/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.mapping;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.commands.IHandlerListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.team.core.diff.FastDiffFilter;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IThreeWayDiff;
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

	@Override
	protected boolean isEnabledForSelection(IStructuredSelection selection) {
		return handler.isEnabled();
	}

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
			PlatformUI.getWorkbench().getProgressService().run(true, true, monitor -> {
				try {
					handleTargetSaveableChange(getConfiguration().getSite().getShell(), null, currentBuffer, true,
							monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
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
