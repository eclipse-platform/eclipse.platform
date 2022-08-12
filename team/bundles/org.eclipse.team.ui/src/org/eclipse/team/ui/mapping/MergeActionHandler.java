/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.mapping;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.ResourceMarkAsMergedHandler;
import org.eclipse.team.internal.ui.mapping.ResourceMergeHandler;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

/**
 * An abstract superclass that enables models to create handlers
 * for the basic merge operations (merge, overwrite and mark-as-merged).
 * This class makes use of a {@link SynchronizationOperation} to determine its
 * enablement state and execute the handler. Enablement is determined
 * using {@link SynchronizationOperation#shouldRun()} and the handler will
 * invoke {@link SynchronizationOperation#run()} when executed.
 *
 * @since 3.2
 * @see SynchronizationActionProvider
 */
public abstract class MergeActionHandler extends AbstractHandler {

	private final ISynchronizePageConfiguration configuration;
	private boolean enabled = false;
	private IStructuredSelection selection;
	private ISelectionChangedListener listener = this::updatedEnablement;

	/**
	 * Return an instance of the default handler for the given merge action id.
	 * Note that this handler must be disposed by the caller when it is no longer
	 * needed.
	 * @param mergeActionId the merge action id
	 * @param configuration the synchronization page configuration
	 * @return the default handler for the given merge action or <code>null</code>
	 */
	public static IHandler getDefaultHandler(String mergeActionId, ISynchronizePageConfiguration configuration) {
		if (mergeActionId == SynchronizationActionProvider.MERGE_ACTION_ID) {
			ResourceMergeHandler resourceMergeHandler = new ResourceMergeHandler(configuration, false /* no overwrite */);
			resourceMergeHandler.updateEnablement((IStructuredSelection)configuration.getSite().getSelectionProvider().getSelection());
			return resourceMergeHandler;
		} else if (mergeActionId == SynchronizationActionProvider.OVERWRITE_ACTION_ID) {
			ResourceMergeHandler resourceMergeHandler = new ResourceMergeHandler(configuration, true /* overwrite */);
			resourceMergeHandler.updateEnablement((IStructuredSelection)configuration.getSite().getSelectionProvider().getSelection());
			return resourceMergeHandler;
		} else if (mergeActionId == SynchronizationActionProvider.MARK_AS_MERGE_ACTION_ID) {
			ResourceMarkAsMergedHandler resourceMarkAsMergedHandler = new ResourceMarkAsMergedHandler(configuration);
			resourceMarkAsMergedHandler.updateEnablement((IStructuredSelection)configuration.getSite().getSelectionProvider().getSelection());
			return resourceMarkAsMergedHandler;
		}
		return null;
	}

	/**
	 * Create the handler.
	 * @param configuration the configuration for the synchronize page displaying the model.
	 */
	public MergeActionHandler(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
		ISelectionProvider selectionProvider = getConfiguration().getSite().getSelectionProvider();
		selectionProvider.addSelectionChangedListener(listener);
		updateEnablement((IStructuredSelection)selectionProvider.getSelection());
	}

	/**
	 * Deregister this handler from selection change events.
	 */
	@Override
	public void dispose() {
		getConfiguration().getSite().getSelectionProvider().removeSelectionChangedListener(listener);
	}

	/* private */ void updatedEnablement(SelectionChangedEvent event) {
		updateEnablement(event.getStructuredSelection());
	}

	/**
	 * Update the enablement of this handler for the new selection.
	 * By default, this method uses the <code>shouldRun</code>
	 * method of the handler's operation to determine the enablement
	 * of this handler. Subclasses may override but should
	 * either still invoke this method or call {@link #setEnabled(boolean)}
	 * to set the enablement.
	 * @param selection the selection
	 */
	protected void updateEnablement(IStructuredSelection selection) {
		this.selection = selection;
		boolean isEnabled = getOperation().shouldRun();
		setEnabled(isEnabled);
	}

	/**
	 * Return the configuration of the synchronize page that is surfacing
	 * the merge action to which this handler is registered.
	 * @return the synchronize page configuration
	 */
	protected final ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Return the current selection.
	 * @return the current selection.
	 */
	protected final IStructuredSelection getStructuredSelection() {
		return selection;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set the enablement of this handler.
	 * @param isEnabled whether the handler is enabled
	 */
	protected void setEnabled(boolean isEnabled) {
		if (enabled != isEnabled) {
			enabled = isEnabled;
			fireHandlerChanged(new HandlerEvent(this, true, false));
		}
	}

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		try {
			SynchronizationOperation operation = getOperation();
			IRunnableContext context = getConfiguration().getRunnableContext();
			if (context != null) {
				context.run(true, true, operation);
			} else {
				operation.run();
			}
		} catch (InvocationTargetException e) {
			Utils.handle(e);
		} catch (InterruptedException e) {
			// Ignore
		}
		return null;
	}

	/**
	 * Return the synchronization operation that performs
	 * the merge operation.
	 * @return a synchronization operation
	 */
	protected abstract SynchronizationOperation getOperation();

	/**
	 * Return the saveable that is the target of this handler.
	 * By default, the saveable of this handlers operation is returned.
	 * @return the saveable that is the target of this operation
	 */
	public SaveableComparison getSaveable() {
		return getOperation().getSaveable();
	}
}
