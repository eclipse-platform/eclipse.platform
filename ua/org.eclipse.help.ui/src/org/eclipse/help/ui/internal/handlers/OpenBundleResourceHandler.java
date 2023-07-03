/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.handlers;

import java.io.File;
import java.net.URL;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;

public class OpenBundleResourceHandler extends AbstractHandler {

	private static final String PARAM_ID_PLUGIN = "plugin"; //$NON-NLS-1$

	private static final String PARAM_ID_PATH = "path"; //$NON-NLS-1$

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		String pluginId = event.getParameter(PARAM_ID_PLUGIN);
		String pluginPath = event.getParameter(PARAM_ID_PATH);
		URL url;
		File workspaceFile;
		String errorMessage=""; //$NON-NLS-1$
		if (pluginId == null || pluginPath==null) {
			url = null;
		} else {
			try {
				if(pluginPath.startsWith("/")) //$NON-NLS-1$
					pluginPath = pluginPath.substring(1);
				url = new URL(Platform.getInstanceLocation().getURL().toString()+pluginId+"/"+pluginPath); //$NON-NLS-1$
				workspaceFile = new File(url.getFile());
				if(!workspaceFile.exists())
				{
					url = BaseHelpSystem.resolve("/" + pluginId + '/' + pluginPath , true); //$NON-NLS-1$
					if (url == null)
					{
						errorMessage="file not found:" + pluginId+"/"+pluginPath; //$NON-NLS-1$ //$NON-NLS-2$
						throw new ExecutionException(errorMessage);
					}
				}
			}
			catch (Exception ex) {
				throw new ExecutionException(errorMessage, ex);
			}
		}

		String browserId = pluginId+"."+pluginPath; //$NON-NLS-1$
		try {
			IWorkbenchBrowserSupport browserSupport = PlatformUI.getWorkbench()
					.getBrowserSupport();

			IWebBrowser browser = browserSupport.createBrowser(browserId);
			browser.openURL(url);
		} catch (PartInitException ex) {
			throw new ExecutionException("error opening browser", ex); //$NON-NLS-1$
		}
		return null;
	}
}
