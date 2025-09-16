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
 * Alexander Fedorov (ArSysOp) - further evolution
 *******************************************************************************/
package org.eclipse.terminal.view.ui.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.terminal.connector.ITerminalConnector;
import org.eclipse.terminal.view.core.ITerminalService;
import org.eclipse.terminal.view.core.ITerminalTabListener;
import org.eclipse.terminal.view.core.ITerminalsConnectorConstants;
import org.eclipse.terminal.view.ui.IUIConstants;
import org.eclipse.terminal.view.ui.TerminalViewId;
import org.eclipse.terminal.view.ui.launcher.ILauncherDelegate;
import org.eclipse.terminal.view.ui.launcher.ILauncherDelegateManager;
import org.eclipse.terminal.view.ui.launcher.ITerminalConsoleViewManager;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Terminal service implementation.
 */
@Component(service = ITerminalService.class)
public class TerminalService implements ITerminalService {
	/**
	 * The registered terminal tab dispose listeners.
	 */
	private final ListenerList<ITerminalTabListener> terminalTabListeners = new ListenerList<>();

	// Flag to remember if the terminal view has been restored or not.
	private boolean fRestoringView;

	// Terminal tab events

	/**
	 * A terminal tab got disposed.
	 */
	public static final int TAB_DISPOSED = 1;

	private final ITerminalConsoleViewManager consoleViewManager;

	private final ILauncherDelegateManager launchDelegateManager;

	/**
	 * Common terminal service runnable implementation.
	 */
	protected static abstract class TerminalServiceRunnable {

		/**
		 * Invoked to execute the terminal service runnable.
		 *
		 * @param tvid The terminals view id or <code>null</code>.
		 * @param title The terminal tab title. Must not be <code>null</code>.
		 * @param connector The terminal connector. Must not be <code>null</code>.
		 * @param data The custom terminal data node or <code>null</code>.
		 * @return the result {@link IStatus}
		 */
		public abstract IStatus run(TerminalViewId tvid, String title, ITerminalConnector connector, Object data);

		/**
		 * Returns if or if not to execute the runnable asynchronously.
		 * <p>
		 * The method returns per default <code>true</code>. Overwrite to
		 * modify the behavior.
		 *
		 * @return <code>True</code> to execute the runnable asynchronously, <code>false</code> otherwise.
		 */
		public boolean isExecuteAsync() {
			return true;
		}
	}

	@Activate
	public TerminalService(@Reference ITerminalConsoleViewManager consoleViewManager,
			@Reference ILauncherDelegateManager launchDelegateManager) {
		this.consoleViewManager = consoleViewManager;
		this.launchDelegateManager = launchDelegateManager;
	}

	@Override
	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	public final void addTerminalTabListener(ITerminalTabListener listener) {
		Assert.isNotNull(listener);
		terminalTabListeners.add(listener);
	}

	@Override
	public final void removeTerminalTabListener(ITerminalTabListener listener) {
		Assert.isNotNull(listener);
		terminalTabListeners.remove(listener);
	}

	/**
	 * Convenience method for notifying the registered terminal tab listeners.
	 *
	 * @param event The terminal tab event.
	 * @param source The disposed tab item. Must not be <code>null</code>.
	 * @param data The custom data object associated with the disposed tab item or <code>null</code>.
	 */
	public final void fireTerminalTabEvent(final int event, final Object source, final Object data) {
		Assert.isNotNull(source);

		// If no listener is registered, we are done here
		if (terminalTabListeners.isEmpty()) {
			return;
		}

		// Get the list or currently registered listeners
		// Loop the registered terminal tab listeners and invoke the proper method
		for (final ITerminalTabListener listener : terminalTabListeners) {
			ISafeRunnable job = new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					// already logged in Platform#run()
				}

				@Override
				public void run() throws Exception {
					switch (event) {
					case TAB_DISPOSED:
						listener.terminalTabDisposed(source, data);
						break;
					default:
					}
				}
			};
			SafeRunner.run(job);
		}
	}

	/**
	 * Executes the given runnable operation and invokes the given callback, if any,
	 * after the operation finished.
	 *
	 * @param properties The terminal properties. Must not be <code>null</code>.
	 * @param runnable The terminal service runnable. Must not be <code>null</code>.
	 * @return the {@link CompletableFuture} with result
	 */
	protected final CompletableFuture<?> executeServiceOperation(final Map<String, Object> properties,
			final TerminalServiceRunnable runnable) {
		Assert.isNotNull(properties);
		Assert.isNotNull(runnable);

		// Extract the properties
		String id = (String) properties.get(ITerminalsConnectorConstants.PROP_ID);
		String secondaryId = (String) properties.get(ITerminalsConnectorConstants.PROP_SECONDARY_ID);
		if (!properties.containsKey(ITerminalsConnectorConstants.PROP_SECONDARY_ID)) {
			secondaryId = ITerminalsConnectorConstants.LAST_ACTIVE_SECONDARY_ID;
		}
		String title = (String) properties.get(ITerminalsConnectorConstants.PROP_TITLE);
		Object data = properties.get(ITerminalsConnectorConstants.PROP_DATA);

		// Normalize the terminals console view id
		id = normalizeId(id, data);
		// Normalize the terminal console tab title
		title = normalizeTitle(title, data);

		// Create the terminal connector instance
		ITerminalConnector connector;
		try {
			connector = createTerminalConnector(properties);
		} catch (CoreException e) {
			// Properties contain invalid connector arguments
			return CompletableFuture.failedFuture(e);
		}
		return executeServiceOperation(runnable, new TerminalViewId(id, secondaryId), title, connector, data);
	}

	private CompletableFuture<?> executeServiceOperation(final TerminalServiceRunnable runnable, TerminalViewId tvid,
			final String title, final ITerminalConnector connector, final Object data) {
		try { // Execute the operation
			if (runnable.isExecuteAsync()) {
				return CompletableFuture.supplyAsync(() -> runnable.run(tvid, title, connector, data),
						/*
						 * A special executor is used here because the default Display executor will
						 * do a syncExec here if called from the UI thread, but isExecuteAsync is true so we
						 * must run async on the UI thread.
						 */
						Display.getDefault()::asyncExec);

			} else {
				return CompletableFuture.completedFuture(runnable.run(tvid, title, connector, data));
			}
		} catch (RuntimeException e) {
			return CompletableFuture.failedFuture(e);
		}
	}

	/**
	 * Normalize the terminals view id.
	 *
	 * @param id The terminals view id or <code>null</code>.
	 * @param data The custom data object or <code>null</code>.
	 *
	 * @return The normalized terminals console view id.
	 */
	protected String normalizeId(String id, Object data) {
		return id != null ? id : IUIConstants.ID;
	}

	/**
	 * Normalize the terminal tab title.
	 *
	 * @param title The terminal tab title or <code>null</code>.
	 * @param data The custom data object or <code>null</code>.
	 *
	 * @return The normalized terminal tab title.
	 */
	protected String normalizeTitle(String title, Object data) {
		// If the title is explicitly specified, return as is
		if (title != null) {
			return title;
		}

		// Return the default console title in all other cases
		return Messages.TerminalService_defaultTitle;
	}

	/**
	 * Creates the terminal connector configured within the given properties.
	 *
	 * @param properties The terminal console properties. Must not be <code>null</code>.
	 * @return The created terminal connector
	 * @throws CoreException if connector cannot be created for provided input
	 */
	protected ITerminalConnector createTerminalConnector(Map<String, Object> properties) throws CoreException {
		Assert.isNotNull(properties);
		ILauncherDelegate delegate = Optional.of(properties)
				.map(map -> map.get(ITerminalsConnectorConstants.PROP_DELEGATE_ID)).filter(String.class::isInstance)
				.map(String.class::cast).flatMap(id -> launchDelegateManager.findLauncherDelegate(id, false))
				.orElseThrow(
						() -> new CoreException(Status.error(Messages.TerminalService_error_cannotCreateConnector)));

		return delegate.createTerminalConnector(properties);
	}

	@Override
	public CompletableFuture<?> openConsole(final Map<String, Object> properties) {
		Assert.isNotNull(properties);
		final boolean restoringView = fRestoringView;
		return executeServiceOperation(properties, new TerminalServiceRunnable() {
			@Override
			public IStatus run(TerminalViewId tvid, String title, ITerminalConnector connector, Object data) {
				if (restoringView) {
					return doRun(tvid, title, connector, data);
				} else {
					// First, restore the view. This opens consoles from the memento
					fRestoringView = true;
					try {
						consoleViewManager.showConsoleView(tvid);
					} catch (CoreException e) {
						ILog.get().log(e.getStatus());
					}
					fRestoringView = false;
					return doRun(tvid, title, connector, data);
				}
			}

			private IStatus doRun(TerminalViewId tvid, String title, ITerminalConnector connector, Object data) {
				// Determine the terminal encoding
				String encoding = (String) properties.get(ITerminalsConnectorConstants.PROP_ENCODING);
				// Create the flags to pass on to openConsole
				Map<String, Boolean> flags = new HashMap<>();
				flags.put("activate", Boolean.TRUE); //$NON-NLS-1$
				if (properties.get(ITerminalsConnectorConstants.PROP_FORCE_NEW) instanceof Boolean) {
					flags.put(ITerminalsConnectorConstants.PROP_FORCE_NEW,
							(Boolean) properties.get(ITerminalsConnectorConstants.PROP_FORCE_NEW));
				}
				if (properties.get(ITerminalsConnectorConstants.PROP_DATA_NO_RECONNECT) instanceof Boolean) {
					flags.put(ITerminalsConnectorConstants.PROP_DATA_NO_RECONNECT,
							(Boolean) properties.get(ITerminalsConnectorConstants.PROP_DATA_NO_RECONNECT));
				}
				if (properties.get(ITerminalsConnectorConstants.PROP_TITLE_DISABLE_ANSI_TITLE) instanceof Boolean) {
					flags.put(ITerminalsConnectorConstants.PROP_TITLE_DISABLE_ANSI_TITLE,
							(Boolean) properties.get(ITerminalsConnectorConstants.PROP_TITLE_DISABLE_ANSI_TITLE));
				} else {
					flags.put(ITerminalsConnectorConstants.PROP_TITLE_DISABLE_ANSI_TITLE, false);
				}
				// Open the new console
				try {
					Widget console = consoleViewManager.openConsole(tvid, title, encoding, connector, data, flags);
					// Associate the original terminal properties with the tab item.
					// This makes it easier to persist the connection data within the memento handler
					if (!console.isDisposed()) {
						console.setData("properties", properties); //$NON-NLS-1$
					}
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return e.getStatus();
				}
			}
		});
	}

	@Override
	public CompletableFuture<?> closeConsole(Map<String, Object> properties) {
		Assert.isNotNull(properties);
		return executeServiceOperation(properties, new TerminalServiceRunnable() {
			@Override
			public IStatus run(TerminalViewId tvid, String title, ITerminalConnector connector, Object data) {
				consoleViewManager.closeConsole(tvid, title, connector, data);
				return Status.OK_STATUS;
			}
		});
	}

	@Override
	public CompletableFuture<?> terminateConsole(Map<String, Object> properties) {
		Assert.isNotNull(properties);

		return executeServiceOperation(properties, new TerminalServiceRunnable() {
			@Override
			public IStatus run(TerminalViewId tvid, String title, ITerminalConnector connector, Object data) {
				consoleViewManager.terminateConsole(tvid, title, connector, data);
				return Status.OK_STATUS;
			}
		});
	}
}
