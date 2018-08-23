/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.actions;
 
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation;
import org.eclipse.ui.IWorkbenchPart;

public class IgnoreAction extends WorkspaceTraversalAction {
    
    /**
     * Define an operation that can be run in the background.
     * We divide the ignores by provider to obtain project
     * locks while modifying the .cvsignore files
     */
    class IgnoreOperation extends RepositoryProviderOperation {

        private final IgnoreResourcesDialog dialog;

        public IgnoreOperation(IWorkbenchPart part, IResource[] resources, IgnoreResourcesDialog dialog) {
            super(part, resources);
            this.dialog = dialog;
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#getTaskName(org.eclipse.team.internal.ccvs.core.CVSTeamProvider)
         */
        protected String getTaskName(CVSTeamProvider provider) {
            return NLS.bind(CVSUIMessages.IgnoreAction_0, new String[] { provider.getProject().getName() }); 
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#execute(org.eclipse.team.internal.ccvs.core.CVSTeamProvider, org.eclipse.core.resources.IResource[], org.eclipse.core.runtime.IProgressMonitor)
         */
        protected void execute(CVSTeamProvider provider, IResource[] resources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException {
			try {
			    monitor.beginTask(null, resources.length);
				for (int i = 0; i < resources.length; i++) {
					IResource resource = resources[i];
					String pattern = dialog.getIgnorePatternFor(resource);
					ICVSResource cvsResource = getCVSResourceFor(resource);
					cvsResource.setIgnoredAs(pattern);
					monitor.worked(1);
				}
			} catch (TeamException e) {
				collectStatus(e.getStatus());
				return;
			} finally {
			    monitor.done();
			}
			collectStatus(Status.OK_STATUS);
        }

        /* (non-Javadoc)
         * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#getTaskName()
         */
        protected String getTaskName() {
            return CVSUIMessages.IgnoreAction_1; 
        }
        
    	/* (non-Javadoc)
    	 * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#consultModelsForMappings()
    	 */
    	public boolean consultModelsForMappings() {
    		return false;
    	}
        
    }
	
	protected void execute(final IAction action) throws InvocationTargetException, InterruptedException {
		run(new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				IResource[] resources = getSelectedResources();
				IgnoreResourcesDialog dialog = new IgnoreResourcesDialog(getShell(), resources);
				if (dialog.open() != Window.OK) return;
				new IgnoreOperation(getTargetPart(), resources, dialog).run();
				
				//if (action != null) action.setEnabled(isEnabled());
			}
		}, false /* cancelable */, PROGRESS_BUSYCURSOR);
	}
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return CVSUIMessages.IgnoreAction_ignore; 
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForManagedResources()
	 */
	protected boolean isEnabledForManagedResources() {
		return false;
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForUnmanagedResources()
	 */
	protected boolean isEnabledForUnmanagedResources() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForCVSResource(org.eclipse.team.internal.ccvs.core.ICVSResource)
	 */
	protected boolean isEnabledForCVSResource(ICVSResource cvsResource) throws CVSException {
		if (super.isEnabledForCVSResource(cvsResource)) {
			// Perform an extra check against the subscriber to ensure there is no conflict
			CVSWorkspaceSubscriber subscriber = CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber();
			IResource resource = cvsResource.getIResource();
			if (resource == null) return false;
			try {
				SyncInfo info = subscriber.getSyncInfo(resource);
				return ((info.getKind() & SyncInfo.DIRECTION_MASK) == SyncInfo.OUTGOING);
			} catch (TeamException e) {
				// Let the enablement happen
				return true;
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getId()
	 */
	public String getId() {
		return ICVSUIConstants.CMD_IGNORE;
	}
}
