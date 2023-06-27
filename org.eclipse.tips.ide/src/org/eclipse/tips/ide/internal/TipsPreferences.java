/*******************************************************************************
 * Copyright (c) 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.ide.internal;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.tips.core.internal.LogUtil;
import org.eclipse.tips.core.internal.TipManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Internal class to store preferences.
 *
 * The preferences will first process command line options before accessing the
 * default scope which can be loaded with a plug-in Customization file.
 *
 * <h2>Control Startup</h2> Without any option, the tips will be started in the
 * background.
 * <p>
 * Command line option "org.eclipse.tips.startup.default" can be set in the
 * following ways:
 *
 * <ul>
 * <li><b>-Dorg.eclipse.tips.startup.default=dialog</b> (Show the dialog)</li>
 * <li><b>-Dorg.eclipse.tips.startup.default=background</b> (Start in the
 * background)</li>
 * <li><b>-Dorg.eclipse.tips.startup.default=disable</b> (Do not start
 * tips)</li>
 * </ul>
 *
 * The -pluginCustomization program option can be used to provide startup
 * defaults in a file:
 *
 * <pre>
 * <b>-pluginCustomization /some/path/plugin_customization.ini</b>
 *
 * Inside the file:
 * org.eclipse.tips.ide/activate_at_startup=&lt;dialog|background|disable&gt;
 * </pre>
 *
 * <h2>Disable specific providers</h2> Without any option, all providers will be
 * loaded.
 *
 * The -pluginCustomization program option can be used to disable specific
 * providers:
 *
 * <pre>
 * <b>-pluginCustomization /some/path/plugin_customization.ini</b>
 *
 * Inside the file:
 * org.eclipse.tips.ide/disabled_providers=org.eclipse.tips.ide.internal.provider.TipsTipProvider,another.provider.id
 * </pre>
 *
 */
@SuppressWarnings("restriction")
public class TipsPreferences extends AbstractPreferenceInitializer {

	private static final String FALSE = "false"; //$NON-NLS-1$

	/**
	 * Preference store key to indicate showing tips at startup.
	 */
	public static final String PREF_STARTUP_BEHAVIOR = "activate_at_startup"; //$NON-NLS-1$

	private static final String PREF_STARTUP_BEHAVIOR_PROPERTY = "org.eclipse.tips.startup.default"; //$NON-NLS-1$
	private static final String PREF_STARTUP_BEHAVIOR_DIALOG = "dialog"; //$NON-NLS-1$
	private static final String PREF_STARTUP_BEHAVIOR_BACKGROUND = "background"; //$NON-NLS-1$
	private static final String PREF_STARTUP_BEHAVIOR_DISABLE = "disable"; //$NON-NLS-1$

	/**
	 * Preference store key to indicate serving tips that the user as already seen.
	 */
	public static final String PREF_SERVE_READ_TIPS = "serve_read_tips"; //$NON-NLS-1$

	/**
	 * Preference store key to indicate providers which are disabled. I.e. no tips
	 * are shown from these providers. The providers are indicated by their
	 * identifiers, the identifiers are strings separated by commas.
	 */
	public static final String PREF_DISABLED_PROVIDERS = "disabled_providers"; //$NON-NLS-1$

	public TipsPreferences() {
	}

	@Override
	public void initializeDefaultPreferences() {
		// nothing needs to be done right now
	}

	/**
	 * Loads the read tips from disk.
	 *
	 * @return a map that stores the read tip hashes per provider.
	 */
	public static Map<String, List<Integer>> getReadState() {
		HashMap<String, List<Integer>> result = new HashMap<>();

		try {
			File stateLocation = getStateLocation();
			for (String key : stateLocation.list(getStateFileNameFilter(stateLocation))) {
				PreferenceStore store = new PreferenceStore(new File(stateLocation, key).getAbsolutePath());
				store.load();
				ArrayList<Integer> tips = new ArrayList<>();
				for (String tipKey : store.preferenceNames()) {
					if (!"provider".equals(tipKey)) { //$NON-NLS-1$
						tips.add(Integer.valueOf(store.getInt(tipKey)));
					}
				}
				result.put(store.getString("provider"), tips); //$NON-NLS-1$
			}
		} catch (Exception e) {
			Status status = new Status(IStatus.ERROR, "org.eclipse.tips.ide", e.getMessage(), e); //$NON-NLS-1$
			log(status);
		}
		return result;
	}

	private static FilenameFilter getStateFileNameFilter(File stateLocation) {
		return (pDir, pName) -> {
			if (pDir.equals(stateLocation) && pName.endsWith(".state")) { //$NON-NLS-1$
				return true;
			}
			return false;
		};
	}

	private static File getStateLocation() throws Exception {
		File file = new File(IDETipManager.getStateLocation(), "org.eclipse.tips.ide.state"); //$NON-NLS-1$
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}

	/**
	 * Saves the list with read tips to disk.
	 *
	 * @param pReadTips the list with read tips
	 * @return the status of the call
	 */
	public static IStatus saveReadState(Map<String, List<Integer>> pReadTips) {
		try {
			File stateLocation = getStateLocation();
			for (Map.Entry<String, List<Integer>> entry : pReadTips.entrySet()) {
				String child = entry.getKey();
				PreferenceStore store = new PreferenceStore(
						new File(stateLocation, child.trim() + ".state").getAbsolutePath()); //$NON-NLS-1$
				entry.getValue().forEach(value -> store.setValue(value.toString(), value.intValue()));
				store.setValue("provider", child); //$NON-NLS-1$
				store.save();
			}
			return Status.OK_STATUS;
		} catch (Exception e) {
			e.printStackTrace();
			return new Status(IStatus.ERROR, "org.eclipse.tips.ide", e.getMessage(), e); //$NON-NLS-1$
		}
	}

	/**
	 * @return the preferences for this bundle
	 */
	private static IEclipsePreferences getPreferences() {
		return ConfigurationScope.INSTANCE.getNode(Constants.BUNDLE_ID);

	}

	public static void log(IStatus status) {
		if (status.matches(IStatus.ERROR | IStatus.WARNING) || isDebug()) {
			Bundle bundle = FrameworkUtil.getBundle(TipsPreferences.class);
			ILog.of(bundle).log(status);
		}
		if (isConsoleLog()) {
			System.out.println(
					String.format("%1$tR:%1$tS:%1$tN - %2$s", Calendar.getInstance().getTime(), format(status))); //$NON-NLS-1$
		}
	}

	/**
	 * Converts the passed status object to a readable string and tries to get the
	 * location where the log method was called.
	 *
	 * @param pStatus the status to format
	 * @return the formatted status to string.
	 */
	private static Object format(IStatus pStatus) {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		String origin = "<unknown>"; //$NON-NLS-1$
		if (stackTrace.length > 3) {
			StackTraceElement ste = stackTrace[4];
			String[] fqcn = ste.getClassName().split("\\."); //$NON-NLS-1$
			String clazz = fqcn[fqcn.length - 1];
			origin = clazz + "#" + ste.getMethodName() + "(" + ste.getLineNumber() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}

		String statusLine = pStatus.toString();
		if (statusLine.endsWith(" null")) { //$NON-NLS-1$
			statusLine = statusLine.substring(0, statusLine.length() - " null".length()); //$NON-NLS-1$
		}
		return statusLine + " : " + origin; //$NON-NLS-1$
	}

	/**
	 * Preferences are stored as strings but returned in TipManager form (int).
	 *
	 * @return the startup behavior
	 *
	 * @see TipManager#START_DIALOG
	 * @see TipManager#START_BACKGROUND
	 * @see TipManager#START_DISABLE
	 */
	public static int getStartupBehavior() {
		String behavior = getPreferences().get(PREF_STARTUP_BEHAVIOR, null);
		if (behavior == null) {
			behavior = getDefaultStartupBehavior();
		}
		switch (behavior) {
		case PREF_STARTUP_BEHAVIOR_DIALOG:
			return TipManager.START_DIALOG;
		case PREF_STARTUP_BEHAVIOR_BACKGROUND:
			return TipManager.START_BACKGROUND;
		case PREF_STARTUP_BEHAVIOR_DISABLE:
			return TipManager.START_DISABLE;
		default:
			return TipManager.START_BACKGROUND;
		}
	}

	private static String getDefaultStartupBehavior() {
		String startupBehavior = System.getProperty(PREF_STARTUP_BEHAVIOR_PROPERTY);
		if (startupBehavior == null) {
			return startupBehavior = DefaultScope.INSTANCE.getNode(Constants.BUNDLE_ID).get(PREF_STARTUP_BEHAVIOR,
					PREF_STARTUP_BEHAVIOR_BACKGROUND);
		}
		return startupBehavior;
	}

	public static boolean isServeReadTips() {
		return getPreferences().getBoolean(PREF_SERVE_READ_TIPS, false);
	}

	/**
	 * Sets the startup behavior of the tips framework which is one of the
	 * TipManager#START* constants. If an invalid value is passed,
	 * {@link TipManager#START_BACKGROUND} is assumed.
	 *
	 * @param startupBehavior {@link TipManager#START_BACKGROUND},
	 *                        {@link TipManager#START_DIALOG} or
	 *                        {@link TipManager#START_DISABLE}
	 */
	public static void setStartupBehavior(int startupBehavior) {
		log(LogUtil.info("setStartupBehavior = '" + startupBehavior + "' 0=dialog, 1=background, 2=disable")); //$NON-NLS-1$ //$NON-NLS-2$
		IEclipsePreferences node = getPreferences();
		if (startupBehavior == TipManager.START_DIALOG) {
			node.put(PREF_STARTUP_BEHAVIOR, PREF_STARTUP_BEHAVIOR_DIALOG);
		} else if (startupBehavior == TipManager.START_DISABLE) {
			node.put(PREF_STARTUP_BEHAVIOR, PREF_STARTUP_BEHAVIOR_DISABLE);
		} else {
			node.put(PREF_STARTUP_BEHAVIOR, PREF_STARTUP_BEHAVIOR_BACKGROUND);
		}
		try {
			node.flush();
		} catch (BackingStoreException e) {
			log(LogUtil.error(e));
		}
	}

	public static void setServeReadTips(boolean serveReadTips) {
		log(LogUtil.info("Entering method setServeReadTips with boolean parameter = '" + serveReadTips + "'")); //$NON-NLS-1$ //$NON-NLS-2$
		IEclipsePreferences node = getPreferences();
		node.putBoolean(PREF_SERVE_READ_TIPS, serveReadTips);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			log(LogUtil.error(e));
		}
	}

	/**
	 * @return true if tips are in debug mode.
	 */
	public static boolean isDebug() {
		return !System.getProperty("org.eclipse.tips.debug", FALSE).equals(FALSE); //$NON-NLS-1$
	}

	/**
	 * @return true if console logging is required
	 */
	public static boolean isConsoleLog() {
		return !System.getProperty("org.eclipse.tips.consolelog", FALSE).equals(FALSE); //$NON-NLS-1$
	}

	/**
	 * @return A list of tip provider identifiers, which are disabled with a
	 *         preference on the configuration scope.
	 */
	public static List<String> getDisabledProviderIds() {
		String disabledProviderIds = getPreferences().get(PREF_DISABLED_PROVIDERS, null);
		if (disabledProviderIds == null) {
			disabledProviderIds = getDefaultDisabledProviderIds();
		}
		log(LogUtil.info("Disabled provider ids = '" + disabledProviderIds + "'")); //$NON-NLS-1$ //$NON-NLS-2$
		String[] result = disabledProviderIds.split(","); //$NON-NLS-1$
		return Arrays.asList(result);
	}

	/**
	 * @return A list of default tip provider identifiers, which can be provided by
	 *         a pluginCustomization file, or an empty string. Never null.
	 */
	private static String getDefaultDisabledProviderIds() {
		String result = Platform.getPreferencesService().getString(Constants.BUNDLE_ID, PREF_DISABLED_PROVIDERS, "", //$NON-NLS-1$
				new IScopeContext[] { DefaultScope.INSTANCE });
		log(LogUtil.info("Default disabled provider ids = '" + result + "'")); //$NON-NLS-1$ //$NON-NLS-2$
		return result;
	}
}