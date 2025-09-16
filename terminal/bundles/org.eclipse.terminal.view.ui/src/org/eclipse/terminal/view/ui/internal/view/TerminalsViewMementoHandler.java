/*******************************************************************************
 * Copyright (c) 2012, 2025 Wind River Systems, Inc. and others. All rights reserved.
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
package org.eclipse.terminal.view.ui.internal.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ILog;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.terminal.control.ITerminalViewControl;
import org.eclipse.terminal.view.core.ITerminalsConnectorConstants;
import org.eclipse.terminal.view.ui.IMementoHandler;
import org.eclipse.terminal.view.ui.internal.UIPlugin;
import org.eclipse.terminal.view.ui.launcher.ILauncherDelegate;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.PlatformUI;

/**
 * Take care of the persisted state handling of the "Terminal" view.
 */
public class TerminalsViewMementoHandler {
	// The list of items to save. See the workbench listener implementation
	// in o.e.tm.terminal.view.ui.activator.UIPlugin.
	private final List<CTabItem> saveables = new ArrayList<>();

	/**
	 * Sets the list of saveable items.
	 *
	 * @param saveables The list of saveable items. Must not be <code>null</code>.
	 */
	public void setSaveables(List<CTabItem> saveables) {
		Assert.isNotNull(saveables);
		this.saveables.clear();
		this.saveables.addAll(saveables);
	}

	/**
	 * Saves the view state in the given memento.
	 *
	 * @param view The terminals view. Must not be <code>null</code>.
	 * @param memento The memento. Must not be <code>null</code>.
	 */
	@SuppressWarnings("unchecked")
	public void saveState(TerminalsView view, IMemento memento) {
		Assert.isNotNull(view);
		Assert.isNotNull(memento);

		// Create a child element within the memento holding the
		// connection info of the open, non-terminated tab items
		memento = memento.createChild("terminalConnections"); //$NON-NLS-1$
		Assert.isNotNull(memento);

		// Write the view id and secondary id
		memento.putString("id", view.getViewSite().getId()); //$NON-NLS-1$
		memento.putString("secondaryId", view.getViewSite().getSecondaryId()); //$NON-NLS-1$

		// Loop the saveable items and store the connection data of each
		// item to the memento
		for (CTabItem item : saveables) {
			// Ignore disposed items
			if (item.isDisposed()) {
				continue;
			}

			// Get the original terminal properties associated with the tab item
			Map<String, Object> properties = (Map<String, Object>) item.getData("properties"); //$NON-NLS-1$
			if (properties == null) {
				continue;
			}

			// Get the terminal launcher delegate
			Optional<IMementoHandler> mementoHandler = findDelegate(properties).flatMap(this::mementoHandler);
			if (mementoHandler.isPresent()) {
				// Create terminal connection child memento
				IMemento connectionMemento = memento.createChild("connection"); //$NON-NLS-1$
				Assert.isNotNull(connectionMemento);
				// Store the common attributes
				String delegateId = (String) properties.get(ITerminalsConnectorConstants.PROP_DELEGATE_ID);
				connectionMemento.putString(ITerminalsConnectorConstants.PROP_DELEGATE_ID, delegateId);

				String terminalConnectorId = (String) properties
						.get(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID);
				if (terminalConnectorId != null) {
					connectionMemento.putString(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID,
							terminalConnectorId);
				}

				if (properties.get(ITerminalsConnectorConstants.PROP_FORCE_NEW) instanceof Boolean) {
					connectionMemento.putBoolean(ITerminalsConnectorConstants.PROP_FORCE_NEW,
							((Boolean) properties.get(ITerminalsConnectorConstants.PROP_FORCE_NEW)).booleanValue());
				}

				// Store the current encoding
				ITerminalViewControl terminal = (ITerminalViewControl) item.getData();
				String encoding = terminal != null ? terminal.getCharset().name() : null;
				if (encoding == null || "".equals(encoding)) { //$NON-NLS-1$
					encoding = (String) properties.get(ITerminalsConnectorConstants.PROP_ENCODING);
				}
				if (encoding != null && !"".equals(encoding)) { //$NON-NLS-1$
					connectionMemento.putString(ITerminalsConnectorConstants.PROP_ENCODING, encoding);
				}

				// Store the current working directory, or if not available, the initial working directory
				if (terminal != null) {
					encoding = terminal.getCharset().name();
					Optional<String> workingDirectory = terminal.getTerminalConnector().getWorkingDirectory();
					String cwd = workingDirectory
							.orElse((String) properties.get(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR));
					if (cwd != null) {
						connectionMemento.putString(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR, cwd);
					}
				}

				// Pass on to the memento handler
				mementoHandler.get().saveState(connectionMemento, properties);
			}
		}
	}

	/**
	 * Restore the view state from the given memento.
	 *
	 * @param view The terminals view. Must not be <code>null</code>.
	 * @param memento The memento. Must not be <code>null</code>.
	 */
	protected void restoreState(final TerminalsView view, IMemento memento) {
		Assert.isNotNull(view);
		Assert.isNotNull(memento);

		// Get the "terminalConnections" memento
		memento = memento.getChild("terminalConnections"); //$NON-NLS-1$
		if (memento != null) {
			// Read view id and secondary id
			String id = memento.getString("id"); //$NON-NLS-1$
			String secondaryId = memento.getString("secondaryId"); //$NON-NLS-1$
			if ("null".equals(secondaryId)) { //$NON-NLS-1$
				secondaryId = null;
			}

			// Get all the "connection" memento's.
			IMemento[] connections = memento.getChildren("connection"); //$NON-NLS-1$
			for (IMemento connection : connections) {
				// Create the properties container that holds the terminal properties
				Map<String, Object> properties = new HashMap<>();

				// Set the view id attributes
				properties.put(ITerminalsConnectorConstants.PROP_ID, id);
				properties.put(ITerminalsConnectorConstants.PROP_SECONDARY_ID, secondaryId);

				// Restore the common attributes
				properties.put(ITerminalsConnectorConstants.PROP_DELEGATE_ID,
						connection.getString(ITerminalsConnectorConstants.PROP_DELEGATE_ID));
				properties.put(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID,
						connection.getString(ITerminalsConnectorConstants.PROP_TERMINAL_CONNECTOR_ID));
				if (connection.getBoolean(ITerminalsConnectorConstants.PROP_FORCE_NEW) != null) {
					properties.put(ITerminalsConnectorConstants.PROP_FORCE_NEW,
							connection.getBoolean(ITerminalsConnectorConstants.PROP_FORCE_NEW));
				}

				// Restore the encoding
				if (connection.getString(ITerminalsConnectorConstants.PROP_ENCODING) != null) {
					properties.put(ITerminalsConnectorConstants.PROP_ENCODING,
							connection.getString(ITerminalsConnectorConstants.PROP_ENCODING));
				}

				// Restore the working directory
				if (connection.getString(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR) != null) {
					properties.put(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR,
							connection.getString(ITerminalsConnectorConstants.PROP_PROCESS_WORKING_DIR));
				}
				Optional<ILauncherDelegate> delegate = findDelegate(properties);
				// Pass on to the memento handler
				delegate.map(d -> d.getAdapter(IMementoHandler.class))
						.ifPresent(mh -> mh.restoreState(connection, properties));
				// Restore the terminal connection
				delegate.ifPresent(d -> executeDelegate(properties, d));

			}
		}
	}

	private Optional<ILauncherDelegate> findDelegate(Map<String, Object> properties) {
		return Optional.of(properties).map(map -> map.get(ITerminalsConnectorConstants.PROP_DELEGATE_ID))
				.filter(String.class::isInstance).map(String.class::cast)
				.flatMap(id -> UIPlugin.getLaunchDelegateManager().findLauncherDelegate(id, false));
	}

	private void executeDelegate(Map<String, Object> properties, ILauncherDelegate delegate) {
		delegate.execute(properties).whenComplete((r, e) -> {
			if (e != null) {
				ILog.get().error("Error occurred while running delegate to open console", e); //$NON-NLS-1$
			}
		});
	}

	private Optional<IMementoHandler> mementoHandler(ILauncherDelegate delegate) {
		return Optional.ofNullable(delegate.getAdapter(IMementoHandler.class));
	}

	/**
	 * Executes the given runnable asynchronously in the display thread.
	 *
	 * @param runnable The runnable. Must not be <code>null</code>.
	 */
	/* default */ void asyncExec(Runnable runnable) {
		Assert.isNotNull(runnable);
		if (PlatformUI.getWorkbench() != null && PlatformUI.getWorkbench().getDisplay() != null
				&& !PlatformUI.getWorkbench().getDisplay().isDisposed()) {
			PlatformUI.getWorkbench().getDisplay().asyncExec(runnable);
		}
	}
}
