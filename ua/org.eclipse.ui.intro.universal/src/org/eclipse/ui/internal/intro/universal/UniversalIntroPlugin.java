/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.universal;

import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.intro.universal.util.Log;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * Intro main plugin.
 */
public class UniversalIntroPlugin extends AbstractUIPlugin {
	public static final String PLUGIN_ID = "org.eclipse.ui.intro.universal"; //$NON-NLS-1$

	// The static shared instance.
	private static UniversalIntroPlugin inst;

	// image registry that can be disposed while the
	// plug-in is still active. This is important for
	// switching themes after the plug-in has been loaded.
	private ImageRegistry volatileImageRegistry;

	/**
	 * The constructor.
	 */
	public UniversalIntroPlugin() {
		super();
	}

	/**
	 * Returns the shared plugin instance.
	 */
	public static UniversalIntroPlugin getDefault() {
		return inst;
	}

	/**
	 * Returns the Intro Part.
	 */
	public static IIntroPart getIntro() {
		IIntroPart introPart = PlatformUI.getWorkbench().getIntroManager()
			.getIntro();
		return introPart;
	}

	public ImageRegistry getVolatileImageRegistry() {
		if (volatileImageRegistry==null) {
			volatileImageRegistry = createImageRegistry();
			initializeImageRegistry(volatileImageRegistry);
		}
		return volatileImageRegistry;
	}

	public void resetVolatileImageRegistry() {
		if (volatileImageRegistry!=null) {
			volatileImageRegistry.dispose();
			volatileImageRegistry = null;
		}
	}


	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		inst = this;
		if (Log.logInfo)
			Log.info("IntroPlugin - calling start on Intro bundle"); //$NON-NLS-1$

	}

	@Override
	public void stop(BundleContext context) throws Exception {
		resetVolatileImageRegistry();
		super.stop(context);
	}


}
