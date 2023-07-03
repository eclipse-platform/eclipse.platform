/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.ui.browser;

import java.util.Hashtable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.ILog;
import org.eclipse.help.browser.IBrowser;
import org.eclipse.help.internal.browser.BrowserManager;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.help.ui.internal.util.ErrorUtil;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Action that launches a URL in a browser.
 * <p>
 * This class is intended to be specified as a value of a class attribute of an
 * action element in plugin.xml for extensions of org.eclipse.ui.actionSets
 * extension point. The URL to launch must be specified in the markup in one of
 * two ways.
 * </p>
 * <p>
 * The action element can have an attribute named url, in addition to markup
 * required by org.eclipse.ui.actionSets extension point specification. The
 * value of the url attribute should specify a URL to be opened in a browser.
 * </p>
 * <p>
 * Alternatively, since 3.1, instead of a class attribute on the action element,
 * the extension can specify a nested class element with a class attribute and
 * URL specified in a parameter sub-element. For example:
 * </p>
 *
 * <pre>
 *          &lt;class class="org.eclipse.help.ui.browser.LaunchURL"&gt;
 *              &lt;parameter name="url" value="http://eclipse.org/" /&gt;
 *          &lt;/class&gt;
 * </pre>
 */
public class LaunchURL implements IWorkbenchWindowActionDelegate,
		IExecutableExtension {
	private String url;

	@Override
	public void dispose() {
	}

	@Override
	public void init(IWorkbenchWindow window) {
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		if (data != null && data instanceof Hashtable) {
			url = ((Hashtable<String, String>) data).get("url"); //$NON-NLS-1$
		}
		if (url == null || url.length() == 0)
			url = config.getAttribute("url"); //$NON-NLS-1$
	}

	@Override
	public void run(IAction action) {
		if (url == null || "".equals(url)) { //$NON-NLS-1$
			return;
		}
		IBrowser browser = BrowserManager.getInstance().createBrowser(true);
		try {
			browser.displayURL(url);
		} catch (Exception e) {
			ILog.of(getClass()).error("Exception occurred when opening URL: " //$NON-NLS-1$
					+ url + ".", e); //$NON-NLS-1$
			ErrorUtil.displayErrorDialog(Messages.LaunchURL_exception);
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}

}
