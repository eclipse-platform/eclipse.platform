/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.diff.IDiffChangeEvent;
import org.eclipse.team.core.diff.IDiffChangeListener;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.core.diff.IThreeWayDiff;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.SynchronizeView;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.mapping.MergeActionHandler;
import org.eclipse.team.ui.mapping.SynchronizationOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.ide.IDE;

public class MergeAllActionHandler extends MergeActionHandler implements IDiffChangeListener {

	private MergeAllOperation operation;

	public MergeAllActionHandler(ISynchronizePageConfiguration configuration) {
		super(configuration);
		getContext().getDiffTree().addDiffChangeListener(this);
	}

	@Override
	protected synchronized SynchronizationOperation getOperation() {
		if (operation == null) {
			operation = createOperation();
		}
		return operation;
	}

	protected MergeAllOperation createOperation() {
		return new MergeAllOperation(getJobName(), getConfiguration(), getMappings(), getContext());
	}

	private IMergeContext getContext() {
		return ((IMergeContext)getConfiguration().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_CONTEXT));
	}

	private ResourceMapping[] getMappings() {
		return ((ISynchronizationScope)getConfiguration().getProperty(ITeamContentProviderManager.P_SYNCHRONIZATION_SCOPE)).getMappings();
	}

	@Override
	public void diffsChanged(IDiffChangeEvent event, IProgressMonitor monitor) {
		synchronized (this) {
			operation = null;
		}
		setEnabled(event.getTree().countFor(IThreeWayDiff.INCOMING, IThreeWayDiff.DIRECTION_MASK) > 0
				|| event.getTree().countFor(IThreeWayDiff.CONFLICTING, IThreeWayDiff.DIRECTION_MASK) > 0);
	}

	@Override
	public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
		// Nothing to do
	}

	@Override
	public void dispose() {
		getContext().getDiffTree().removeDiffChangeListener(this);
		super.dispose();
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (saveDirtyEditors() && promptToUpdate())
			return super.execute(event);
		return null;
	}

	/**
	 * Prompt to save all dirty editors and return whether to proceed
	 * or not.
	 * @return whether to proceed
	 * or not
	 */
	public final boolean saveDirtyEditors() {
		if(needsToSaveDirtyEditors()) {
			if(!saveAllEditors(getTargetResources(), confirmSaveOfDirtyEditor())) {
				return false;
			}
		}
		return true;
	}

	private IResource[] getTargetResources() {
		return getContext().getDiffTree().getAffectedResources();
	}

	/**
	 * Save all dirty editors in the workbench that are open on files that may
	 * be affected by this operation. Opens a dialog to prompt the user if
	 * <code>confirm</code> is true. Return true if successful. Return false
	 * if the user has canceled the command. Must be called from the UI thread.
	 * @param resources the root resources being operated on
	 * @param confirm prompt the user if true
	 * @return boolean false if the operation was canceled.
	 */
	public final boolean saveAllEditors(IResource[] resources, boolean confirm) {
		return IDE.saveAllEditors(resources, confirm);
	}

	/**
	 * Return whether dirty editor should be saved before this action is run.
	 * Default is <code>true</code>.
	 *
	 * @return whether dirty editor should be saved before this action is run
	 */
	protected boolean needsToSaveDirtyEditors() {
		return true;
	}

	/**
	 * Returns whether the user should be prompted to save dirty editors. The
	 * default is <code>true</code>.
	 *
	 * @return whether the user should be prompted to save dirty editors
	 */
	protected boolean confirmSaveOfDirtyEditor() {
		return true;
	}

	protected String getJobName() {
		String name = getConfiguration().getParticipant().getName();
		return NLS.bind(TeamUIMessages.MergeAllActionHandler_0, Utils.shortenText(SynchronizeView.MAX_NAME_LENGTH, name));
	}

	protected boolean promptToUpdate() {
		final IResourceDiffTree tree = getContext().getDiffTree();
		if (tree.isEmpty()) {
			return false;
		}
		final long count = tree.countFor(IThreeWayDiff.INCOMING, IThreeWayDiff.DIRECTION_MASK) + tree.countFor(IThreeWayDiff.CONFLICTING, IThreeWayDiff.DIRECTION_MASK);
		if (count == 0)
			return false;
		final boolean[] result = new boolean[] {true};
		TeamUIPlugin.getStandardDisplay().syncExec(() -> {
			String sizeString = Long.toString(count);
			String message = tree.size() > 1 ? NLS.bind(TeamUIMessages.MergeAllActionHandler_1, new String[] { sizeString }) :
				NLS.bind(TeamUIMessages.MergeAllActionHandler_2, new String[] { sizeString });
			result[0] = MessageDialog.openQuestion(getConfiguration().getSite().getShell(),
					NLS.bind(TeamUIMessages.MergeAllActionHandler_3, new String[] { sizeString }), message);
		});
		return result[0];
	}
}
