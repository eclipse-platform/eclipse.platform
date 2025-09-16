/*******************************************************************************
 * Copyright (c) 2014, 2025 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * Alexander Fedorov (ArSysOp) - further evolution
 *******************************************************************************/
package org.eclipse.terminal.connector.local.launcher;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.terminal.connector.local.activator.UIPlugin;
import org.eclipse.terminal.view.core.ITerminalsConnectorConstants;
import org.eclipse.terminal.view.ui.launcher.ILauncherDelegate;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Local terminal launcher handler implementation.
 */
public class LocalLauncherHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = selection(event);
		Optional<ILauncherDelegate> delegate = UIPlugin.getLaunchDelegateManager()
				.getApplicableLauncherDelegates(selection)
				.filter(d -> "org.eclipse.terminal.connector.local.launcher.local".equals(d.getId())).findFirst(); //$NON-NLS-1$
		if (delegate.isPresent()) {
			executeDelegate(selection, delegate.get());
		}
		return null;
	}

	private ISelection selection(ExecutionEvent event) {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		// If the selection is not a structured selection, check if there is an active
		// editor and get the path from the editor input
		if (!(selection instanceof IStructuredSelection)) {
			IEditorInput input = HandlerUtil.getActiveEditorInput(event);
			if (input instanceof IPathEditorInput) {
				IPath path = ((IPathEditorInput) input).getPath();
				if (path != null) {
					if (path.toFile().isFile()) {
						path = path.removeLastSegments(1);
					}
					if (path.toFile().isDirectory() && path.toFile().canRead()) {
						selection = new StructuredSelection(path);
					}
				}
			}
		}
		return selection;
	}

	private void executeDelegate(ISelection selection, ILauncherDelegate delegate) throws ExecutionException {
		Map<String, Object> properties = new HashMap<>();
		properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID, delegate.getId());
		properties.put(ITerminalsConnectorConstants.PROP_SELECTION, selection);
		delegate.execute(properties).whenComplete((r, e) -> {
			if (e != null) {
				ILog.get().error("Error occurred while running delegate to open console", e); //$NON-NLS-1$
			}
		});
	}

}
