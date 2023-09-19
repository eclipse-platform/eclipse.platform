/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.help.internal.browser;

import java.io.*;
import java.util.Hashtable;
import java.util.Locale;

import org.eclipse.core.runtime.*;
import org.eclipse.help.browser.*;
import org.eclipse.help.internal.base.*;


/**
 * Browser factory for Linux-based operating systems.
 */
public class MozillaFactory implements IBrowserFactory, IExecutableExtension {
	private String executable;
	private String executableName;
	private String osList;
	private MozillaBrowserAdapter browserInstance = null;
	/**
	 * Constructor.
	 */
	public MozillaFactory() {
		super();
	}

	@SuppressWarnings("resource")
	@Override
	public boolean isAvailable() {
		if (!isSupportedOS(System.getProperty("os.name"))) { //$NON-NLS-1$
			return false;
		}
		try {
			Process pr = Runtime.getRuntime().exec(new String[] { "which", executable }); //$NON-NLS-1$
			StreamConsumer outputs = new StreamConsumer(pr.getInputStream());
			(outputs).start();
			StreamConsumer errors = new StreamConsumer(pr.getErrorStream());
			(errors).start();
			pr.waitFor();
			int ret = pr.exitValue();
			if (ret == 0) {
				return !errorsInOutput(outputs, errors);
			}
			return false;
		} catch (InterruptedException e) {
			return false;
		} catch (IOException e) {
			// launching which failed, assume browser executable is present
			return true;
		}
	}
	/**
	 * On some OSes 0 is always returned by "which" command it is necessary to
	 * examine output to find out failure.
	 *
	 * @param outputs
	 * @param errors
	 * @return
	 */
	private boolean errorsInOutput(StreamConsumer outputs, StreamConsumer errors) {
		try {
			outputs.join(1000);
			if (outputs.getLastLine() != null
					&& outputs.getLastLine().contains("no " + executable + " in")) {//$NON-NLS-1$ //$NON-NLS-2$

				return true;
			}
			errors.join(1000);
			if (errors.getLastLine() != null
					&& errors.getLastLine().contains("no " + executable + " in")) { //$NON-NLS-1$ //$NON-NLS-2$
				return true;
			}
		} catch (InterruptedException ie) {
			// ignore
		}
		return false;
	}

	@Override
	public IBrowser createBrowser() {
		// Create single browser for all clients
		if (browserInstance == null) {
			browserInstance = new MozillaBrowserAdapter(executable,
					executableName);
		}
		return browserInstance;
	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {
		try {
			Hashtable<?, ?> params = (Hashtable<?, ?>) data;
			executable = (String) params.get("executable"); //$NON-NLS-1$
			executableName = (String) params.get("executableName"); //$NON-NLS-1$
			osList = (String) params.get("os"); //$NON-NLS-1$
		} catch (Exception e) {
			throw new CoreException(new Status(IStatus.ERROR,
					HelpBasePlugin.PLUGIN_ID, IStatus.OK, HelpBaseResources.MozillaFactory_dataMissing,
					e));
		}
	}
	private boolean isSupportedOS(String os) {
		if (osList == null || osList.length() <= 0) {
			// parameter missing
			return false;
		}
		String[] OSes = osList.split(",\\s*"); //$NON-NLS-1$
		for (int i = 0; i < OSes.length; i++) {
			if (os.toLowerCase(Locale.ENGLISH).startsWith(OSes[i].toLowerCase(Locale.ENGLISH))) {
				return true;
			}
		}
		return false;
	}
}
