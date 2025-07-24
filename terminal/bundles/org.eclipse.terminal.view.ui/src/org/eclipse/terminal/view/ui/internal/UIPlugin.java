/*******************************************************************************
 * Copyright (c) 2011, 2025 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * Max Weninger (Wind River) - [361363] [TERMINALS] Implement "Pin&Clone" for the "Terminals" view
 * Alexander Fedorov (ArSysOp) - further evolution
 *******************************************************************************/
package org.eclipse.terminal.view.ui.internal;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.terminal.connector.TerminalState;
import org.eclipse.terminal.control.ITerminalViewControl;
import org.eclipse.terminal.view.core.ITerminalService;
import org.eclipse.terminal.view.core.utils.ScopedEclipsePreferences;
import org.eclipse.terminal.view.ui.internal.listeners.WorkbenchWindowListener;
import org.eclipse.terminal.view.ui.internal.view.TerminalsView;
import org.eclipse.terminal.view.ui.internal.view.TerminalsViewMementoHandler;
import org.eclipse.terminal.view.ui.launcher.ILauncherDelegateManager;
import org.eclipse.terminal.view.ui.launcher.ITerminalConsoleViewManager;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator class controls the plug-in life cycle
 */
public class UIPlugin extends AbstractUIPlugin {
	// The shared instance
	private static volatile UIPlugin plugin;
	// The scoped preferences instance
	private static volatile ScopedEclipsePreferences scopedPreferences;
	// The trace handler instance
	private static volatile TraceHandler traceHandler;
	// The workbench listener instance
	private IWorkbenchListener listener;
	// The global window listener instance
	private IWindowListener windowListener;

	private ServiceTracker<ITerminalService, ITerminalService> terminalServiceTracker;
	private ServiceTracker<ILauncherDelegateManager, ILauncherDelegateManager> launchDelegateServiceTracker;
	private ServiceTracker<ITerminalConsoleViewManager, ITerminalConsoleViewManager> consoleManagerTracker;

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static UIPlugin getDefault() {
		return plugin;
	}

	/**
	 * Convenience method which returns the unique identifier of this plug-in.
	 */
	public static String getUniqueIdentifier() {
		if (getDefault() != null && getDefault().getBundle() != null) {
			return getDefault().getBundle().getSymbolicName();
		}
		return "org.eclipse.terminal.view.ui"; //$NON-NLS-1$
	}

	/**
	 * Return the scoped preferences for this plug-in.
	 */
	public static ScopedEclipsePreferences getScopedPreferences() {
		if (scopedPreferences == null) {
			scopedPreferences = new ScopedEclipsePreferences(getUniqueIdentifier());
		}
		return scopedPreferences;
	}

	/**
	 * Returns the bundles trace handler.
	 *
	 * @return The bundles trace handler.
	 */
	public static TraceHandler getTraceHandler() {
		if (traceHandler == null) {
			traceHandler = new TraceHandler(getUniqueIdentifier());
		}
		return traceHandler;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		terminalServiceTracker = new ServiceTracker<>(context, ITerminalService.class, null);
		terminalServiceTracker.open();
		launchDelegateServiceTracker = new ServiceTracker<>(context, ILauncherDelegateManager.class, null);
		launchDelegateServiceTracker.open();
		consoleManagerTracker = new ServiceTracker<>(context, ITerminalConsoleViewManager.class, null);
		consoleManagerTracker.open();
		plugin = this;
		// Create and register the workbench listener instance
		listener = new IWorkbenchListener() {

			@Override
			public boolean preShutdown(IWorkbench workbench, boolean forced) {
				if (workbench != null && workbench.getActiveWorkbenchWindow() != null
						&& workbench.getActiveWorkbenchWindow().getActivePage() != null) {
					// Find all "Terminal" views
					IViewReference[] refs = workbench.getActiveWorkbenchWindow().getActivePage().getViewReferences();
					for (IViewReference ref : refs) {
						IViewPart part = ref.getView(false);
						if (part instanceof TerminalsView) {
							/*
							 * The terminal tabs to save to the views memento on shutdown can
							 * be determined only _before_ the saveState(memento) method of the
							 * view is called. Within saveState, it is already to late and the
							 * terminals might be in CLOSED state already. This depends on the
							 * terminal type and the corresponding connector implementation.
							 *
							 * To be safe, we determine the still opened terminals on shutdown
							 * separately here in the preShutdown.
							 */
							final List<CTabItem> saveables = new ArrayList<>();

							// Get the tab folder
							CTabFolder tabFolder = ((TerminalsView) part).getAdapter(CTabFolder.class);
							if (tabFolder != null && !tabFolder.isDisposed()) {
								// Get the list of tab items
								CTabItem[] items = tabFolder.getItems();
								// Loop the tab items and find the still connected ones
								for (CTabItem item : items) {
									// Ignore disposed items
									if (item.isDisposed()) {
										continue;
									}
									// Get the terminal view control
									ITerminalViewControl terminal = (ITerminalViewControl) item.getData();
									if (terminal == null || terminal.getState() != TerminalState.CONNECTED) {
										continue;
									}
									// Still connected -> Add to the list
									saveables.add(item);
								}
							}

							// Push the determined saveable items to the memento handler
							TerminalsViewMementoHandler mementoHandler = ((TerminalsView) part)
									.getAdapter(TerminalsViewMementoHandler.class);
							if (mementoHandler != null) {
								mementoHandler.setSaveables(saveables);
							}
						}
					}
				}

				return true;
			}

			@Override
			public void postShutdown(IWorkbench workbench) {
			}
		};
		PlatformUI.getWorkbench().addWorkbenchListener(listener);

		if (windowListener == null && PlatformUI.getWorkbench() != null) {
			windowListener = new WorkbenchWindowListener();
			PlatformUI.getWorkbench().addWindowListener(windowListener);
			activateContexts();
		}

	}

	void activateContexts() {
		if (Display.getCurrent() != null) {
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window != null && windowListener != null) {
				windowListener.windowOpened(window);
			}
		} else {
			PlatformUI.getWorkbench().getDisplay().asyncExec(() -> activateContexts());
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		terminalServiceTracker.close();
		launchDelegateServiceTracker.close();
		consoleManagerTracker.close();
		if (windowListener != null && PlatformUI.getWorkbench() != null) {
			PlatformUI.getWorkbench().removeWindowListener(windowListener);
			windowListener = null;
		}

		scopedPreferences = null;
		traceHandler = null;
		if (listener != null) {
			PlatformUI.getWorkbench().removeWorkbenchListener(listener);
			listener = null;
		}
		super.stop(context);
		plugin = null;
	}

	@Override
	protected void initializeImageRegistry(ImageRegistry registry) {
		Bundle bundle = getBundle();
		URL consoleViewIconUrl = bundle
				.getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_EVIEW + "console_view.svg"); //$NON-NLS-1$
		registry.put(ImageConsts.VIEW_Terminals, ImageDescriptor.createFromURL(consoleViewIconUrl));
		putActionImages(registry, bundle, "lock_co.svg", //$NON-NLS-1$
				ImageConsts.ACTION_ScrollLock_Enabled, ImageConsts.ACTION_ScrollLock_Disabled);
		putActionImages(registry, bundle, "command_input_field.svg", //$NON-NLS-1$
				ImageConsts.ACTION_ToggleCommandField_Enabled, ImageConsts.ACTION_ToggleCommandField_Disabled);
		putActionImages(registry, bundle, "new_terminal_view.svg", //$NON-NLS-1$
				ImageConsts.ACTION_NewTerminalView_Enabled, ImageConsts.ACTION_NewTerminalView_Disabled);
		putActionImages(registry, bundle, "clear_co.svg", //$NON-NLS-1$
				ImageConsts.ACTION_ClearAll_enabled, ImageConsts.ACTION_ClearAll_disabled);
	}

	private void putActionImages(ImageRegistry registry, Bundle bundle, String file, String ekey, String dkey) {
		URL url = bundle.getEntry(ImageConsts.IMAGE_DIR_ROOT + ImageConsts.IMAGE_DIR_ELCL + file);
		ImageDescriptor base = ImageDescriptor.createFromURL(url);
		registry.put(ekey, base);
		registry.put(dkey, ImageDescriptor.createWithFlags(base, SWT.IMAGE_DISABLE));
	}

	/**
	 * Loads the image registered under the specified key from the image
	 * registry and returns the <code>Image</code> object instance.
	 *
	 * @param key The key the image is registered with.
	 * @return The <code>Image</code> object instance or <code>null</code>.
	 */
	public static Image getImage(String key) {
		return getDefault().getImageRegistry().get(key);
	}

	/**
	 * Loads the image registered under the specified key from the image
	 * registry and returns the <code>ImageDescriptor</code> object instance.
	 *
	 * @param key The key the image is registered with.
	 * @return The <code>ImageDescriptor</code> object instance or <code>null</code>.
	 */
	public static ImageDescriptor getImageDescriptor(String key) {
		return getDefault().getImageRegistry().getDescriptor(key);
	}

	public static boolean isOptionEnabled(String strOption) {
		String strEnabled = Platform.getDebugOption(strOption);
		if (strEnabled == null) {
			return false;
		}

		return Boolean.parseBoolean(strEnabled);
	}

	public static ITerminalService getTerminalService() {
		UIPlugin plugin = getDefault();
		if (plugin == null) {
			return null;
		}
		return plugin.terminalServiceTracker.getService();
	}

	public static synchronized ILauncherDelegateManager getLaunchDelegateManager() {
		UIPlugin plugin = getDefault();
		if (plugin == null) {
			return null;
		}
		return plugin.launchDelegateServiceTracker.getService();
	}

	public static synchronized ITerminalConsoleViewManager getConsoleManager() {
		UIPlugin plugin = getDefault();
		if (plugin == null) {
			return null;
		}
		return plugin.consoleManagerTracker.getService();
	}

}
