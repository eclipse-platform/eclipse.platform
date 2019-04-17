/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.mappings;

import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.*;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class CommitAction extends AbstractCommitAction implements IPropertyChangeListener {

	public CommitAction(final ISynchronizePageConfiguration configuration) {
		super(configuration);
		configuration.addPropertyChangeListener(this);
		setId(ICVSUIConstants.CMD_COMMIT);
		setActionDefinitionId(ICVSUIConstants.CMD_COMMIT);
	}
	
	@Override
	protected String getBundleKeyPrefix() {
		return "WorkspaceCommitAction."; //$NON-NLS-1$
	}

	@Override
	protected boolean isEnabledForSelection(IStructuredSelection selection) {
		return internalIsEnabled(selection);
	}

	private boolean internalIsEnabled(IStructuredSelection selection) {
		// Only enable commit in outgoing or both modes
		int mode = getConfiguration().getMode();
		if (mode == ISynchronizePageConfiguration.OUTGOING_MODE || mode == ISynchronizePageConfiguration.BOTH_MODE) {
			return getResourceMappings(selection).length > 0;
		}
		return false;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getSource() == getConfiguration() && event.getProperty() == ISynchronizePageConfiguration.P_MODE) {
			setEnabled(internalIsEnabled(getStructuredSelection()));
		}
	}

	@Override
	protected ResourceTraversal[] getCommitTraversals(IStructuredSelection selection, IProgressMonitor monitor)
			throws CoreException {
		return getResourceTraversals(selection, monitor);
	}

	@Override
	protected IStructuredSelection getActualSelection() throws CVSException {
		IStructuredSelection selection = getStructuredSelection();
		IStructuredSelection actualSelection = internalGetActualSelection();
		if (!equal(selection, actualSelection)) {
			throw new CVSException(CVSUIMessages.CommitAction_3);
		}
		return selection;
	}

	private boolean equal(IStructuredSelection selection,
			IStructuredSelection actualSelection) {
		return selection.equals(actualSelection);
	}

	private IStructuredSelection internalGetActualSelection() {
		ISelection s = getConfiguration().getSite().getSelectionProvider().getSelection();
		if (s instanceof IStructuredSelection) {
			return (IStructuredSelection) s;	
		}
		return StructuredSelection.EMPTY;
	}

}
