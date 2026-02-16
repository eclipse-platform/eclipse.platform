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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.terminal.connector.ITerminalConnector;
import org.eclipse.terminal.connector.ITerminalControl;
import org.eclipse.terminal.control.ITerminalViewControl;
import org.eclipse.terminal.view.core.ITerminalsConnectorConstants;
import org.eclipse.terminal.view.ui.IPreferenceKeys;
import org.eclipse.terminal.view.ui.ITerminalsView;
import org.eclipse.terminal.view.ui.TerminalViewId;
import org.eclipse.terminal.view.ui.internal.tabs.TabFolderManager;
import org.eclipse.terminal.view.ui.internal.view.TerminalsView;
import org.eclipse.terminal.view.ui.launcher.ITerminalConsoleViewManager;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.component.annotations.Component;

/**
 * Terminal console manager.
 */
@Component(service = ITerminalConsoleViewManager.class)
public final class ConsoleManager implements ITerminalConsoleViewManager {

	// Reference to the perspective listener instance
	private final IPerspectiveListener perspectiveListener;

	// Internal perspective listener implementation
	static final class ConsoleManagerPerspectiveListener extends PerspectiveAdapter {
		private final List<IViewReference> references = new ArrayList<>();

		@Override
		public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
			// If the old references list is empty, just return
			if (references.isEmpty()) {
				return;
			}
			// Create a copy of the old view references list
			List<IViewReference> oldReferences = new ArrayList<>(references);

			// Get the current list of view references
			List<IViewReference> references = new ArrayList<>(Arrays.asList(page.getViewReferences()));
			for (IViewReference reference : oldReferences) {
				if (references.contains(reference)) {
					continue;
				}
				// Previous visible terminals console view reference, make visible again
				try {
					page.showView(reference.getId(), reference.getSecondaryId(), IWorkbenchPage.VIEW_VISIBLE);
				} catch (PartInitException e) {
					/* Failure on part instantiation is ignored */ }
			}

		}

		@Override
		public void perspectivePreDeactivate(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
			references.clear();
			for (IViewReference reference : page.getViewReferences()) {
				IViewPart part = reference.getView(false);
				if (part instanceof TerminalsView && !references.contains(reference)) {
					references.add(reference);
				}
			}
		}
	}

	// Reference to the part listener instance
	private final IPartListener2 partListener;

	// The ids of the last activated terminals view
	/* default */ String lastActiveViewId = null;
	/* default */ String lastActiveSecondaryViewId = null;

	// Internal part listener implementation
	class ConsoleManagerPartListener implements IPartListener2 {

		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
			IWorkbenchPart part = partRef.getPart(false);
			if (part instanceof ITerminalsView) {
				lastActiveViewId = ((ITerminalsView) part).getViewSite().getId();
				lastActiveSecondaryViewId = ((ITerminalsView) part).getViewSite().getSecondaryId();
				//System.out.println("Terminals view activated: id = " + lastActiveViewId + ", secondary id = " + lastActiveSecondaryViewId); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partClosed(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partHidden(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
		}

		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {
		}
	}

	/**
	 * Constructor.
	 */
	public ConsoleManager() {
		perspectiveListener = new ConsoleManagerPerspectiveListener();
		partListener = new ConsoleManagerPartListener();
	}

	public void addWindowAndPerspectiveListeners(IWorkbenchWindow window) {
		if (PlatformUI.isWorkbenchRunning() && window != null) {
			window.addPerspectiveListener(perspectiveListener);
			IPartService service = window.getPartService();
			service.addPartListener(partListener);
		}
	}

	public void removeWindowAndPerspectiveListeners(IWorkbenchWindow window) {
		if (PlatformUI.isWorkbenchRunning() && window != null) {
			window.removePerspectiveListener(perspectiveListener);
			IPartService service = window.getPartService();
			service.removePartListener(partListener);
		}
	}

	/**
	 * Returns the active workbench window page if the workbench is still running.
	 *
	 * @return The active workbench window page or <code>null</code>
	 */
	private final IWorkbenchPage getActiveWorkbenchPage() {
		// To lookup the console view, the workbench must be still running
		if (PlatformUI.isWorkbenchRunning() && PlatformUI.getWorkbench() != null
				&& PlatformUI.getWorkbench().getActiveWorkbenchWindow() != null) {
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		}
		return null;
	}

	@Override
	public Optional<ITerminalsView> findConsoleView(TerminalViewId tvid) {
		Assert.isNotNull(Display.findDisplay(Thread.currentThread()));
		return findTerminalsViewWithSecondaryId(tvid, true);
	}

	/**
	 * Search and return a terminal view with a specific secondary id
	 *
	 * @param tvid The terminals console view id. To specify reuse of most recent terminal view use special value of
	 *        {@link ITerminalsConnectorConstants#LAST_ACTIVE_SECONDARY_ID} for its secondary part.
	 * @param restore <code>True</code> if to try to restore the view, <code>false</code> otherwise.
	 *
	 * @return The terminals console view instance or <code>null</code> if not found.
	 */
	private Optional<ITerminalsView> findTerminalsViewWithSecondaryId(TerminalViewId tvid, boolean restore) {
		IWorkbenchPage page = getActiveWorkbenchPage();
		if (page == null) {
			return Optional.empty();
		}
		for (IViewReference ref : page.getViewReferences()) {
			if (ref.getId().equals(tvid.primary())) {
				String refSecondaryId = ref.getSecondaryId();
				String secondaryId = tvid.secondary().orElse(null);
				if (ITerminalsConnectorConstants.ANY_ACTIVE_SECONDARY_ID.equals(secondaryId)
						|| Objects.equals(secondaryId, refSecondaryId)) {
					return Optional.ofNullable(ref.getView(restore)).filter(ITerminalsView.class::isInstance)
							.map(ITerminalsView.class::cast);
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * Search and return the active terminals view.
	 *
	 * @param tvid The terminals console view id. To specify reuse of most recent terminal view use special value of
	 *        {@link ITerminalsConnectorConstants#LAST_ACTIVE_SECONDARY_ID} for its secondary part.
	 * @return The terminals console view instance or <code>null</code> if not found.
	 */
	private Optional<ITerminalsView> findActiveTerminalsView(TerminalViewId tvid) {
		Optional<ITerminalsView> part = Optional.empty();
		String id = tvid.primary();
		String secondaryId = tvid.secondary().orElse(null);
		if (id.equals(lastActiveViewId)) {
			if (ITerminalsConnectorConstants.LAST_ACTIVE_SECONDARY_ID.equals(secondaryId)
					|| Objects.equals(secondaryId, lastActiveSecondaryViewId)) {
				part = findTerminalsViewWithSecondaryId(new TerminalViewId(lastActiveViewId, lastActiveSecondaryViewId),
						false);
			}
		}

		if (part.isEmpty()) {
			String finalSecondaryId;
			if (ITerminalsConnectorConstants.LAST_ACTIVE_SECONDARY_ID.equals(secondaryId)) {
				// There is no last available, so get any available instead
				finalSecondaryId = ITerminalsConnectorConstants.ANY_ACTIVE_SECONDARY_ID;
			} else {
				finalSecondaryId = secondaryId;
			}
			part = findTerminalsViewWithSecondaryId(new TerminalViewId(id, finalSecondaryId), true);
			if (part.isPresent()) {
				IViewSite site = part.get().getViewSite();
				lastActiveViewId = site.getId();
				lastActiveSecondaryViewId = site.getSecondaryId();
			}
		}
		return part;
	}

	/**
	 * Show the terminals console view specified by the given id.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 *
	 * @param tvid The terminals console view id.
	 * @throws CoreException if the requested console cannot be opened
	 * @return opened terminal console view part
	 */
	@Override
	public ITerminalsView showConsoleView(TerminalViewId tvid) throws CoreException {
		Assert.isNotNull(Display.findDisplay(Thread.currentThread()));
		IWorkbenchPage page = getActiveWorkbenchPage();
		if (page == null) {
			throw noActivePage();
		}
		// show the view
		Optional<ITerminalsView> found = findActiveTerminalsView(tvid);
		if (found.isEmpty()) {
			String secondaryId = tvid.secondary().orElse(null);
			String finalSecondaryId;
			if (ITerminalsConnectorConstants.LAST_ACTIVE_SECONDARY_ID.equals(secondaryId)
					|| ITerminalsConnectorConstants.ANY_ACTIVE_SECONDARY_ID.equals(secondaryId)) {
				// We have already checked all open views, so since none of the special flags work
				// we are opening the first view, which means no secondary id.
				finalSecondaryId = null;
			} else {
				finalSecondaryId = secondaryId;
			}
			found = Optional.of(page.showView(tvid.primary(), finalSecondaryId, IWorkbenchPage.VIEW_ACTIVATE))
					.filter(ITerminalsView.class::isInstance).map(ITerminalsView.class::cast);
		}
		// and force the view to the foreground
		found.ifPresent(page::bringToTop);
		return found.orElseThrow(this::cannotCreateConsole);
	}

	private CoreException noActivePage() {
		return new CoreException(Status.error(Messages.ConsoleManager_e_no_active_page));
	}

	private CoreException cannotCreateConsole() {
		return new CoreException(Status.error(Messages.ConsoleManager_e_cannot_create_console));
	}

	/**
	 * Bring the terminals console view, specified by the given id, to the top of the view stack.
	 *
	 * @param tvid The terminals console view id.
	 * @param activate If <code>true</code> activate the console view.
	 * @throws CoreException if the requested console cannot be opened
	 * @return opened terminal console view part
	 */
	private ITerminalsView bringToTop(TerminalViewId tvid, boolean activate) throws CoreException {
		// Get the active workbench page
		IWorkbenchPage page = getActiveWorkbenchPage();
		if (page == null) {
			throw noActivePage();
		}
		// get (last) active terminal view
		Optional<ITerminalsView> found = findActiveTerminalsView(tvid);
		if (found.isEmpty()) {
			// Create a new one
			return showConsoleView(new TerminalViewId(tvid.primary(), new TerminalViewId().next().secondary()));
		}
		ITerminalsView tv = found.get();
		if (activate) {
			page.activate(tv);
		} else {
			page.bringToTop(tv);
		}
		return tv;
	}

	/**
	 * Opens the console with the given title and connector.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 *
	 * @param tvid The terminals console view id. To specify reuse of most recent terminal view use special value of
	 *        {@link ITerminalsConnectorConstants#LAST_ACTIVE_SECONDARY_ID} for its secondary part.
	 * @param title The console title. Must not be <code>null</code>.
	 * @param encoding The terminal encoding or <code>null</code>.
	 * @param connector The terminal connector. Must not be <code>null</code>.
	 * @param data The custom terminal data node or <code>null</code>.
	 * @param flags The flags controlling how the console is opened or <code>null</code> to use defaults.
	 * @throws CoreException if the requested console cannot be opened
	 * @return opened terminal console widget
	 */
	@Override
	public Widget openConsole(TerminalViewId tvid, String title, String encoding, ITerminalConnector connector,
			Object data, Map<String, Boolean> flags) throws CoreException {
		Assert.isNotNull(title);
		Assert.isNotNull(connector);
		Assert.isNotNull(Display.findDisplay(Thread.currentThread()));

		// Get the flags handled by the openConsole method itself
		boolean activate = flags != null && flags.containsKey("activate") ? flags.get("activate").booleanValue() //$NON-NLS-1$//$NON-NLS-2$
				: false;
		boolean forceNew = flags != null && flags.containsKey(ITerminalsConnectorConstants.PROP_FORCE_NEW)
				? flags.get(ITerminalsConnectorConstants.PROP_FORCE_NEW).booleanValue()
				: false;
		// Make the consoles view visible
		ITerminalsView view = bringToTop(tvid, activate);
		// Cast to the correct type
		// Get the tab folder manager associated with the view
		TabFolderManager manager = view.getAdapter(TabFolderManager.class);
		if (manager == null) {
			throw cannotCreateConsole();
		}

		// Lookup an existing console first
		String secId = ((IViewSite) view.getSite()).getSecondaryId();
		Optional<Widget> item = findConsole(new TerminalViewId(tvid.primary(), secId), title, connector, data);

		// Switch to the tab folder page _before_ calling TabFolderManager#createItem(...).
		// The createItem(...) method invokes the corresponding connect and this may take
		// a while if connecting to a remote host. To allow a "Connecting..." decoration,
		// the tab folder page needs to be visible.
		view.switchToTabFolderControl();

		// If no existing console exist or forced -> Create the tab item
		if (item.isEmpty() || forceNew) {
			// If configured, check all existing tab items if they are associated
			// with terminated consoles
			if (UIPlugin.getScopedPreferences().getBoolean(IPreferenceKeys.PREF_REMOVE_TERMINATED_TERMINALS)) {
				// Remote all terminated tab items. This will invoke the
				// tab's dispose listener.
				manager.removeTerminatedItems();
				// Switch back to the tab folder control as removeTerminatedItems()
				// may have triggered the switch to the empty space control.
				view.switchToTabFolderControl();
			}

			// Create a new tab item
			item = Optional.ofNullable(manager.createTabItem(title, encoding, connector, data, flags));
		}
		CTabItem tab = toTabItem(item);
		// Make the item the active console
		manager.bringToTop(tab);
		// Make sure the terminals view has the focus after opening a new terminal
		view.setFocus();
		// Return the tab item of the opened console
		return tab;
	}

	private CTabItem toTabItem(Optional<Widget> item) throws CoreException {
		return item.filter(CTabItem.class::isInstance).map(CTabItem.class::cast).orElseThrow(this::cannotCreateConsole);
	}

	/**
	 * Lookup a console with the given title and the given terminal connector.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 * <b>Note:</b> The method will handle unified console titles itself.
	 *
	 * @param tvid The terminals console view id.
	 * @param title The console title. Must not be <code>null</code>.
	 * @param connector The terminal connector. Must not be <code>null</code>.
	 * @param data The custom terminal data node or <code>null</code>.
	 *
	 * @return The corresponding console tab item or <code>null</code>.
	 */
	@Override
	public Optional<Widget> findConsole(TerminalViewId tvid, String title, ITerminalConnector connector, Object data) {
		Assert.isNotNull(title);
		Assert.isNotNull(connector);
		Assert.isNotNull(Display.findDisplay(Thread.currentThread()));

		// Get the console view
		ITerminalsView view = findConsoleView(tvid).orElse(null);
		if (view == null) {
			return Optional.empty();
		}

		// Get the tab folder manager associated with the view
		TabFolderManager manager = view.getAdapter(TabFolderManager.class);
		if (manager == null) {
			return Optional.empty();
		}

		return Optional.ofNullable(manager.findTabItem(title, connector, data)).map(Widget.class::cast);
	}

	/**
	 * Lookup a console which is assigned with the given terminal control.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 *
	 * @param control The terminal control. Must not be <code>null</code>.
	 * @return The corresponding console tab item or <code>null</code>.
	 */
	@Override
	public Optional<Widget> findConsole(ITerminalControl control) {
		Assert.isNotNull(control);

		CTabItem item = null;

		IWorkbenchPage page = getActiveWorkbenchPage();
		if (page != null) {
			for (IViewReference ref : page.getViewReferences()) {
				IViewPart part = ref != null ? ref.getView(false) : null;
				if (part instanceof ITerminalsView) {
					CTabFolder tabFolder = part.getAdapter(CTabFolder.class);
					if (tabFolder == null) {
						continue;
					}
					CTabItem[] candidates = tabFolder.getItems();
					for (CTabItem candidate : candidates) {
						Object data = candidate.getData();
						if (data instanceof ITerminalControl && control.equals(data)) {
							item = candidate;
							break;
						}
					}
				}
				if (item != null) {
					break;
				}
			}
		}

		return Optional.ofNullable(item);
	}

	/**
	 * Search all console views for the one that contains a specific connector.
	 * <p>
	 * <b>Note:</b> The method will handle unified console titles itself.
	 *
	 * @param tvid The terminals console view id.
	 * @param title The console title. Must not be <code>null</code>.
	 * @param connector The terminal connector. Must not be <code>null</code>.
	 * @param data The custom terminal data node or <code>null</code>.
	 *
	 * @return The corresponding console tab item or <code>null</code>.
	 */
	private CTabItem findConsoleForTerminalConnector(TerminalViewId tvid, String title, ITerminalConnector connector,
			Object data) {
		Assert.isNotNull(title);
		Assert.isNotNull(connector);

		IWorkbenchPage page = getActiveWorkbenchPage();
		if (page != null) {
			IViewReference[] refs = page.getViewReferences();
			for (IViewReference ref : refs) {
				if (ref.getId().equals(tvid.primary())) {
					IViewPart part = ref.getView(true);
					if (part instanceof ITerminalsView) {
						// Get the tab folder manager associated with the view
						TabFolderManager manager = part.getAdapter(TabFolderManager.class);
						if (manager == null) {
							continue;
						}
						CTabItem item = manager.findTabItem(title, connector, data);
						if (item != null) {
							return item;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Close the console with the given title and the given terminal connector.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 * <b>Note:</b> The method will handle unified console titles itself.
	 *
	 * @param tvid The terminals console view id.
	 * @param title The console title. Must not be <code>null</code>.
	 * @param connector The terminal connector. Must not be <code>null</code>.
	 * @param data The custom terminal data node or <code>null</code>.
	 */
	@Override
	public void closeConsole(TerminalViewId tvid, String title, ITerminalConnector connector, Object data) {
		Assert.isNotNull(title);
		Assert.isNotNull(connector);
		Assert.isNotNull(Display.findDisplay(Thread.currentThread()));

		// Lookup the console with this connector
		CTabItem console = findConsoleForTerminalConnector(tvid, title, connector, data);
		// If found, dispose the console
		if (console != null) {
			console.dispose();
		}
	}

	/**
	 * Terminate (disconnect) the console with the given title and the given terminal connector.
	 * <p>
	 * <b>Note:</b> The method must be called within the UI thread.
	 * <b>Note:</b> The method will handle unified console titles itself.
	 *
	 * @param tvid The terminals console view id.
	 * @param title The console title. Must not be <code>null</code>.
	 * @param connector The terminal connector. Must not be <code>null</code>.
	 * @param data The custom terminal data node or <code>null</code>.
	 */
	@Override
	public void terminateConsole(TerminalViewId tvid, String title, ITerminalConnector connector, Object data) {
		Assert.isNotNull(title);
		Assert.isNotNull(connector);
		Assert.isNotNull(Display.findDisplay(Thread.currentThread()));

		// Lookup the console
		CTabItem console = findConsoleForTerminalConnector(tvid, title, connector, data);
		// If found, disconnect the console
		if (console != null && !console.isDisposed()) {
			ITerminalViewControl terminal = (ITerminalViewControl) console.getData();
			if (terminal != null && !terminal.isDisposed()) {
				terminal.disconnectTerminal();
			}
		}
	}
}
