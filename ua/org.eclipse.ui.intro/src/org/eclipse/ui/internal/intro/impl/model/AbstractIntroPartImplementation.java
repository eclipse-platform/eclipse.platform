/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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

package org.eclipse.ui.internal.intro.impl.model;

import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.internal.intro.impl.IIntroConstants;
import org.eclipse.ui.internal.intro.impl.IntroPlugin;
import org.eclipse.ui.internal.intro.impl.Messages;
import org.eclipse.ui.internal.intro.impl.model.viewer.IntroModelContentProvider;
import org.eclipse.ui.internal.intro.impl.model.viewer.IntroModelLabelProvider;
import org.eclipse.ui.internal.intro.impl.util.ImageUtil;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.eclipse.ui.internal.intro.impl.util.Util;
import org.eclipse.ui.intro.IIntroPart;
import org.eclipse.ui.intro.config.CustomizableIntroPart;

/**
 * UI Implementation class that represents a Presentation Part. This class is
 * instantiated from plugin markup and so we need a default constructor,
 * including in subclasses. It has some base methods, including maintaining a
 * history of navigation locations. Also, dynamic awarness is honored here.
 *
 */
public abstract class AbstractIntroPartImplementation {

	// CustomizableIntroPart instance.
	private CustomizableIntroPart introPart = null;

	// IMemento for restoring state.
	private IMemento memento;

	protected History history = new History();

	// flag used to enable logging of perf data for full UI creation only once.
	// Since standbyStateChanged is called several times, flag is used in method
	// to filter out all subsequent calls.
	boolean logUIcreationTime = true;

	// Global actions
	protected Action backAction = new Action() {

		{
			setToolTipText(Messages.Browser_backwardButton_tooltip);
			setImageDescriptor(ImageUtil
				.createImageDescriptor("full/elcl16/backward_nav.png")); //$NON-NLS-1$
			setDisabledImageDescriptor(ImageUtil
				.createImageDescriptor("full/dlcl16/backward_nav.png")); //$NON-NLS-1$
		}

		@Override
		public void run() {
			navigateBackward();
		}
	};

	protected Action forwardAction = new Action() {

		{
			setToolTipText(Messages.Browser_forwardButton_tooltip);
			setImageDescriptor(ImageUtil
				.createImageDescriptor("full/elcl16/forward_nav.png")); //$NON-NLS-1$
			setDisabledImageDescriptor(ImageUtil
				.createImageDescriptor("full/dlcl16/forward_nav.png")); //$NON-NLS-1$
		}

		@Override
		public void run() {
			navigateForward();
		}
	};

	protected Action homeAction = new Action() {

		{
			setToolTipText(Messages.Browser_homeButton_tooltip);
			setImageDescriptor(ImageUtil
				.createImageDescriptor("full/elcl16/home_nav.png")); //$NON-NLS-1$
			setDisabledImageDescriptor(ImageUtil
				.createImageDescriptor("full/dlcl16/home_nav.png")); //$NON-NLS-1$
		}

		@Override
		public void run() {
			navigateHome();
		}
	};

	protected Action viewIntroModelAction = new Action() {

		{
			setToolTipText(Messages.IntroPart_showContentButton_tooltip);
			setImageDescriptor(ImageUtil
				.createImageDescriptor("contents_view.png")); //$NON-NLS-1$
		}

		@Override
		public void run() {
			ElementTreeSelectionDialog treeViewer = new ElementTreeSelectionDialog(
				getIntroPart().getIntroSite().getShell(),
				new IntroModelLabelProvider(), new IntroModelContentProvider());
			treeViewer.setInput(getModel());
			treeViewer.open();
		}
	};

	/**
	 * Creates the UI based on this implementation class. .
	 *
	 * @param parent
	 */
	public abstract void createPartControl(Composite parent);

	/**
	 * Called when the init method is called in the IIntroPart. Subclasses may
	 * extend, for example to get the passed Memento. When extending, make sure
	 * you include a call to super.
	 *
	 * @param introPart
	 */
	public void init(IIntroPart introPart, IMemento memento) {
		// we know the class type to cast to.
		this.introPart = (CustomizableIntroPart) introPart;
		this.memento = memento;
	}

	/**
	 * @return
	 */
	public IntroModelRoot getModel() {
		return IntroPlugin.getDefault().getIntroModelRoot();
	}

	/**
	 * @return Returns the introPart.
	 */
	public CustomizableIntroPart getIntroPart() {
		return introPart;
	}


	/**
	 * Updates the UI navigation history with either a real URL.
	 *
	 * @param location
	 */
	public void updateHistory(String location) {
		history.updateHistory(location);
		updateNavigationActionsState();
	}

	/**
	 * Updates the UI navigation history with a page ID.
	 *
	 * @param pageId
	 */
	public void updateHistory(AbstractIntroPage page) {
		history.updateHistory(page);
		updateNavigationActionsState();
	}


	/**
	 * Subclasses must implement to set the state of the navigation actions in
	 * the toolbar.
	 *
	 */
	public abstract void setFocus();


	/**
	 * Subclasses must implement to update the intro view actions when history
	 * is updated.
	 *
	 */
	protected abstract void updateNavigationActionsState();



	public abstract boolean navigateBackward();

	public abstract boolean navigateForward();

	public abstract boolean navigateHome();


	/**
	 * Called when the IntroPart is disposed. Subclasses should override to
	 * dispose of resources. By default, this implementation does nothing.
	 */
	public void dispose() {
		// no-op
	}


	/*
	 * Add the Intro Model Viewer as an action to all implementations.
	 */
	protected void addToolBarActions() {
		// Handle menus:
		IActionBars actionBars = getIntroPart().getIntroSite().getActionBars();
		IToolBarManager toolBarManager = actionBars.getToolBarManager();
		toolBarManager.add(viewIntroModelAction);
		toolBarManager.update(true);
		actionBars.updateActionBars();
	}

	/**
	 * Called when the Intro changes state. This method should not be
	 * subclassed. It adds performance logging calls. Subclasses must implement
	 * doStandbyStateChanged instead.
	 *
	 * @param standby
	 */
	public void standbyStateChanged(boolean standby, boolean isStandbyPartNeeded) {
		PerformanceStats setStandbyStateStats = null;
		long start = 0;
		if (Log.logPerformance) {
			if (logUIcreationTime && PerformanceStats.ENABLED) {
				PerformanceStats stats = PerformanceStats.getStats(
					IIntroConstants.PERF_UI_ZOOM, IIntroConstants.INTRO);
				stats.endRun();
				Util
					.logPerformanceMessage(
						"(perf stats) time spent in UI code before content is displayed (standbyStateChanged event is fired) ", //$NON-NLS-1$
						stats.getRunningTime());
				stats.reset();
			}

			// standby time.
			setStandbyStateStats = PerformanceStats.getStats(
				IIntroConstants.PERF_SET_STANDBY_STATE, IIntroConstants.INTRO);
			setStandbyStateStats.startRun();
			start = System.currentTimeMillis();
		}


		doStandbyStateChanged(standby, isStandbyPartNeeded);

		// now log performance
		if (Log.logPerformance) {
			if (PerformanceStats.ENABLED) {
				setStandbyStateStats.endRun();
				Util
					.logPerformanceMessage(
						"(perf stats) setting standby state (zooming, displaying content) took:", //$NON-NLS-1$
						+setStandbyStateStats.getRunningTime());
				setStandbyStateStats.reset();
			} else
				Util
					.logPerformanceTime(
						"setting standby state (zooming, generating content, setText() ) took:", //$NON-NLS-1$
						+start);

			if (logUIcreationTime) {
				if (PerformanceStats.ENABLED) {
					PerformanceStats stats = PerformanceStats.getStats(
						IIntroConstants.PERF_VIEW_CREATION_TIME,
						IIntroConstants.INTRO);
					stats.endRun();
					Util
						.logPerformanceMessage(
							"END - (perf stats): creating CustomizableIntroPart view took:", //$NON-NLS-1$
							+stats.getRunningTime());
					stats.reset();
				} else
					Util.logPerformanceTime(
						"END: creating CustomizableIntroPart view took:", //$NON-NLS-1$
						+IntroPlugin.getDefault().gettUICreationStartTime());


				// prevent further logging of UI creation time.
				logUIcreationTime = false;
			}

		}
	}



	/*
	 * Subclasses must implement the actual logic for the method.
	 */
	protected abstract void doStandbyStateChanged(boolean standby,
			boolean isStandbyPartNeeded);


	/**
	 * Save the current state of the intro. Currently, we only store information
	 * about the most recently visited intro page. In static case, the last HTML
	 * page is remembered. In dynamic case, the last UI page or HTML page is
	 * remembered.
	 *
	 * Note: This method saves the last visited intro page in a dynamic case.
	 * Subclasses need to extend to get the desired behavior relavent to the
	 * specific implementation. Broswer implementation needs to cache an http
	 * web page, if it happens to be the last page visited.
	 *
	 * @param memento
	 */
	public void saveState(IMemento memento) {
		saveCurrentPage(memento);
	}


	/**
	 * This method saves the most recently visited dynamic intro page in the
	 * memento. If a given implementation requires saving alternative
	 * information (e.g., information about the most recently visited static
	 * page) it should override this method.
	 *
	 * @param memento
	 */
	protected void saveCurrentPage(IMemento memento) {
		IntroModelRoot model = getModel();

		if (memento == null || model == null)
			return;
		String currentPage = model.getCurrentPageId();
		if (currentPage != null && currentPage.length() > 0) {
			memento.putString(IIntroConstants.MEMENTO_CURRENT_PAGE_ATT,
				currentPage);
		}
	}


	/**
	 * get the last page if it was stored in memento. This page is the last
	 * visited intro page. It can be a intro page id, in the case of dynamic
	 * intro. Or it can be an http in the case of static intro. It can also be
	 * an http in the case of dynamic intro where the last visited page is a
	 * url.
	 */
	protected String getCachedCurrentPage() {
		// Check to see if the start page has been overridden because
		// content
		String newContentPage = ExtensionMap.getInstance().getStartPage();
		if (newContentPage != null) {
			return newContentPage;
		}
		IMemento memento = getMemento();
		if (memento == null) {
			String startPageId = getModel().getStartPageId();
			if (startPageId.length() > 0) {
				return startPageId;
			} else {
				return null;
			}
		}
		return memento.getString(IIntroConstants.MEMENTO_CURRENT_PAGE_ATT);
	}


	/**
	 * @return Returns the memento passed on creation.
	 */
	public IMemento getMemento() {
		return memento;
	}

	/**
	 * Support dynamic awarness. Clear cached models first, then update UI by
	 * delegating to implementation.
	 *
	 * @see org.eclipse.core.runtime.IRegistryChangeListener#registryChanged(org.eclipse.core.runtime.IRegistryChangeEvent)
	 */
	public void registryChanged(IRegistryChangeEvent event) {
		history.clear();
		// give implementation a chance to react to change.
		handleRegistryChanged(event);
	}

	/*
	 * Handle reacting to plugin registry changes. This method will only be
	 * called when regitry changes pertaining to Intro extension points is
	 * detected.
	 */
	protected abstract void handleRegistryChanged(IRegistryChangeEvent event);


	public History getHistory() {
		return history;
	}


}
