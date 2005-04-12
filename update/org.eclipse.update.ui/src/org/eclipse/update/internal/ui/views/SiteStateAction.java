/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.views;

import java.lang.reflect.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.internal.ui.*;
import org.eclipse.update.operations.*;

/**
 * Toggles a site's enabled state
 */

public class SiteStateAction extends Action {
	private IConfiguredSite site;
    private Shell shell;

	public SiteStateAction(Shell shell) {
        this.shell = shell;
	}

	public void setSite(IConfiguredSite site) {
		this.site = site;
		boolean state = site.isEnabled();
		setText(state ? UpdateUIMessages.SiteStateAction_disableLabel : UpdateUIMessages.SiteStateAction_enableLabel); 
	}

	public void run() {
		try {
			if (site == null)
				return;
			
			IStatus status = OperationsManager.getValidator().validatePlatformConfigValid();
			if (status != null) {
				ErrorDialog.openError(shell, null, null, status);
				return;
			}
			
			boolean oldValue = site.isEnabled();
			if (!confirm(!oldValue))
				return;
			
			IOperation toggleSiteOperation = OperationsManager.getOperationFactory().createToggleSiteOperation(site);
			boolean restartNeeded = toggleSiteOperation.execute(null, null);
					
			UpdateUI.requestRestart(restartNeeded);

		} catch (CoreException e) {
            ErrorDialog.openError(shell, null, null, e.getStatus());
		} catch (InvocationTargetException e) {
			UpdateUI.logException(e);
		}
	}

	private boolean confirm(boolean newState) {
		String name = site.getSite().getURL().toString();
		String enableMessage = NLS.bind("SiteStateAction.enableMessage", name); //$NON-NLS-1$ //$NON-NLS-2$
		String disableMessage = NLS.bind("SiteStateAction.disableMessage", name); //$NON-NLS-1$ //$NON-NLS-2$

		String message = newState ? enableMessage : disableMessage;
		return MessageDialog.openConfirm(shell, UpdateUIMessages.SiteStateAction_dialogTitle, message); 
	}
}
