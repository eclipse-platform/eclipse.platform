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
package org.eclipse.help.ui.internal;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.HelpEvaluationContext;
import org.eclipse.help.internal.dynamic.FilterResolver;
import org.eclipse.help.internal.search.federated.IndexerJob;
import org.eclipse.help.ui.internal.dynamic.FilterResolverExtension;
import org.eclipse.help.ui.internal.util.ErrorUtil;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * This class is Help UI plugin.
 */
public class HelpUIPlugin extends AbstractUIPlugin {

	public final static String PLUGIN_ID = "org.eclipse.help.ui"; //$NON-NLS-1$
	// debug options
	public static boolean DEBUG = false;
	public static boolean DEBUG_INFOPOP = false;

	private static HelpUIPlugin plugin;

	/**
	 * Provides access to singleton
	 *
	 * @return HelpUIPlugin
	 */
	public static HelpUIPlugin getDefault() {
		return plugin;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		// Make sure we cancel indexer if it is currently running
		Job.getJobManager().cancel(IndexerJob.FAMILY);
		super.stop(context);
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		FilterResolver.setExtension(new FilterResolverExtension());
		HelpEvaluationContext.setContext(HelpUIEvaluationContext.getContext());

		// Setup debugging options
		DEBUG = isDebugging();
		if (DEBUG) {
			DEBUG_INFOPOP = "true".equalsIgnoreCase(Platform.getDebugOption(PLUGIN_ID + "/debug/infopop")); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if (BaseHelpSystem.getMode() == BaseHelpSystem.MODE_WORKBENCH)
			// UI may get activated during standalone
			BaseHelpSystem.setDefaultErrorUtil(new ErrorUtil());

		if (BaseHelpSystem.getMode() == BaseHelpSystem.MODE_WORKBENCH) {
			// This is workbench scenario. Set activity support of base help to
			// use workbench activity support
			IWorkbench workbench = PlatformUI.getWorkbench();
			if (workbench != null) {
				HelpBasePlugin.setActivitySupport(new HelpActivitySupport(workbench));
			}
		}
	}
}
