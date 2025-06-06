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
 *     John-Mason P. Shackelford (john-mason.shackelford@pearson.com) - bug 53547
 *******************************************************************************/
package org.eclipse.ant.internal.ui;

import java.util.Locale;

import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.ui.editor.DecayCodeCompletionDataStructuresThread;
import org.eclipse.ant.internal.ui.editor.text.AntEditorDocumentProvider;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.osgi.framework.BundleContext;

/**
 * The plug-in runtime class for the Ant UI plug-in.
 */
public class AntUIPlugin extends AbstractUIPlugin {

	/**
	 * Status code indicating an unexpected internal error.
	 *
	 * @since 2.1
	 */
	public static final int INTERNAL_ERROR = 120;

	/**
	 * The single instance of this plug-in runtime class.
	 */
	private static AntUIPlugin plugin;

	/**
	 * Unique identifier constant (value <code>"org.eclipse.ant.ui"</code>) for the Ant UI plug-in.
	 */
	public static final String PI_ANTUI = "org.eclipse.ant.ui"; //$NON-NLS-1$

	/**
	 * The combined preference store.
	 *
	 * @since 3.1
	 */
	private IPreferenceStore fCombinedPreferenceStore;

	private IDocumentProvider fDocumentProvider;

	/**
	 * Constructs an instance of this plug-in runtime class.
	 * <p>
	 * An instance of this plug-in runtime class is automatically created when the facilities provided by the Ant Core plug-in are required.
	 * <b>Clients must never explicitly instantiate a plug-in runtime class.</b>
	 * </p>
	 */
	public AntUIPlugin() {
		super();
		plugin = this;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		try {
			AntUIImages.disposeImageDescriptorRegistry();
			DecayCodeCompletionDataStructuresThread.cancel();
			ColorManager.getDefault().dispose();
		}
		finally {
			super.stop(context);
		}
	}

	/**
	 * Returns this plug-in instance.
	 *
	 * @return the single instance of this plug-in runtime class
	 */
	public static AntUIPlugin getDefault() {
		return plugin;
	}

	/**
	 * Convenience method which returns the unique identifier of this plugin.
	 */
	public static String getUniqueIdentifier() {
		return PI_ANTUI;
	}

	/**
	 * Logs the specified throwable with this plug-in's log.
	 *
	 * @param t
	 *            throwable to log
	 */
	public static void log(Throwable t) {
		IStatus status = new Status(IStatus.ERROR, PI_ANTUI, INTERNAL_ERROR, "Error logged from Ant UI: ", t); //$NON-NLS-1$
		log(status);
	}

	/**
	 * Logs the specified status with this plug-in's log.
	 *
	 * @param status
	 *            status
	 */
	public static void log(IStatus status) {
		getDefault().getLog().log(status);
	}

	/**
	 * Writes the message to the plug-in's log
	 *
	 * @param message
	 *            the text to write to the log
	 */
	public static void log(String message, Throwable exception) {
		IStatus status = newErrorStatus(message, exception);
		log(status);
	}

	/**
	 * Returns a new <code>IStatus</code> for this plug-in
	 */
	public static IStatus newErrorStatus(String message, Throwable exception) {
		if (message == null) {
			return new Status(IStatus.ERROR, IAntUIConstants.PLUGIN_ID, 0, IAntCoreConstants.EMPTY_STRING, exception);
		}
		return new Status(IStatus.ERROR, IAntUIConstants.PLUGIN_ID, 0, message, exception);
	}

	/**
	 * Returns the standard display to be used. The method first checks, if the thread calling this method has an associated display. If so, this
	 * display is returned. Otherwise the method returns the default display.
	 */
	public static Display getStandardDisplay() {
		Display display = Display.getCurrent();
		if (display == null) {
			display = Display.getDefault();
		}
		return display;
	}

	@Override
	protected ImageRegistry createImageRegistry() {
		return AntUIImages.initializeImageRegistry();
	}

	/**
	 * Returns the preference color, identified by the given preference.
	 */
	public static Color getPreferenceColor(String pref) {
		return ColorManager.getDefault().getColor(PreferenceConverter.getColor(getDefault().getCombinedPreferenceStore(), pref));
	}

	/**
	 * Returns the active workbench page or <code>null</code> if none.
	 */
	public static IWorkbenchPage getActivePage() {
		IWorkbenchWindow window = getActiveWorkbenchWindow();
		if (window != null) {
			return window.getActivePage();
		}
		return null;
	}

	/**
	 * Returns the active workbench window or <code>null</code> if none
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow();
	}

	/**
	 * Returns whether the current OS claims to be Mac
	 */
	public static boolean isMacOS() {
		String osname = System.getProperty("os.name").toLowerCase(Locale.US); //$NON-NLS-1$
		return osname.contains("mac"); //$NON-NLS-1$
	}

	/**
	 * Returns a combined preference store, this store is read-only.
	 *
	 * @return the combined preference store
	 *
	 * @since 3.1
	 */
	public IPreferenceStore getCombinedPreferenceStore() {
		if (fCombinedPreferenceStore == null) {
			IPreferenceStore generalTextStore = EditorsUI.getPreferenceStore();
			fCombinedPreferenceStore = new ChainedPreferenceStore(new IPreferenceStore[] { getPreferenceStore(), generalTextStore });
		}
		return fCombinedPreferenceStore;
	}

	/**
	 * Returns the document provider for use in the Ant editor.
	 *
	 * @return the Ant editor document provider
	 *
	 * @since 3.1
	 */
	public synchronized IDocumentProvider getDocumentProvider() {
		if (fDocumentProvider == null) {
			fDocumentProvider = new AntEditorDocumentProvider();
		}
		return fDocumentProvider;
	}
}
