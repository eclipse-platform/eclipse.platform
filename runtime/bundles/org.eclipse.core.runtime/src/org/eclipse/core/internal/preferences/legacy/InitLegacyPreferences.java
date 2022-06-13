/*******************************************************************************
 *  Copyright (c) 2005, 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Christoph Läubrich - remove reference to InternalPlatform.getDefault().log
 *******************************************************************************/
package org.eclipse.core.internal.preferences.legacy;

import java.lang.reflect.Field;
import org.eclipse.core.internal.preferences.exchange.ILegacyPreferences;
import org.eclipse.core.internal.runtime.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Provides initialization of the legacy preferences as described in
 * the Plugin class.
 */
public class InitLegacyPreferences implements ILegacyPreferences {
	/**
	 * The method tries to initialize the preferences using the legacy Plugin method.
	 *
	 * @param object - plugin to initialize
	 * @param name - ID of the plugin to be initialized
	 *
	 * @see Plugin#initializeDefaultPluginPreferences
	 *
	 * @since org.eclipse.core.runtime 3.2
	 *
	 * @deprecated Marked deprecated to suppress warnings. This class is added to support
	 * backward compatibility only and should not be used in any new code.
	 */
	@Override
	@Deprecated
	public Object init(Object object, String name) {
		Plugin plugin = null;
		if (object instanceof Plugin)
			plugin = (Plugin) object;
		else {
			plugin = getActivator(name);
			if (plugin == null) {
				if (InternalPlatform.DEBUG_PLUGIN_PREFERENCES)
					InternalPlatform.message("No plug-in object available to set plug-in default preference overrides for:" + name); //$NON-NLS-1$
				return null;
			}
		}

		if (InternalPlatform.DEBUG_PLUGIN_PREFERENCES)
			InternalPlatform.message("Applying plug-in default preference overrides for plug-in: " + plugin.getBundle().getBundleId()); //$NON-NLS-1$

		plugin.internalInitializeDefaultPluginPreferences();
		return plugin;
	}

	private Plugin getActivator(String name) {
		Bundle bundle = InternalPlatform.getDefault().getBundle(name);
		if (bundle == null)
			return null;

		BundleContext context = bundle.getBundleContext();
		if (context == null)
			return null;

		/*
		 * Reflection is required since there is no OSGi API to retrieve the
		 * activator. Originally Platform.getPlugin(String) was used, but that
		 * is no longer available after removing the runtine.compatiblity layer.
		 */
		try {
			Field field = context.getClass().getDeclaredField("activator"); //$NON-NLS-1$
			field.setAccessible(true);
			Object activator = field.get(context);
			if (activator instanceof Plugin)
				return (Plugin) activator;
		} catch (SecurityException | NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
			log(ex, name);
		}

		return null;
	}

	private static void log(Exception ex, String name) {
		IStatus status = new Status(IStatus.ERROR, Platform.PI_RUNTIME, IStatus.ERROR, NLS.bind(Messages.plugin_unableToGetActivator, name), ex);
		RuntimeLog.log(status);
	}

}
