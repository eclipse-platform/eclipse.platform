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

package org.eclipse.ui.intro.config;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.PerformanceStats;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbenchPreferenceConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.intro.impl.IIntroConstants;
import org.eclipse.ui.internal.intro.impl.IntroPlugin;
import org.eclipse.ui.internal.intro.impl.Messages;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.model.IntroPartPresentation;
import org.eclipse.ui.internal.intro.impl.model.loader.ContentProviderManager;
import org.eclipse.ui.internal.intro.impl.model.loader.ExtensionPointManager;
import org.eclipse.ui.internal.intro.impl.model.loader.ModelLoaderUtil;
import org.eclipse.ui.internal.intro.impl.parts.StandbyPart;
import org.eclipse.ui.internal.intro.impl.presentations.BrowserIntroPartImplementation;
import org.eclipse.ui.internal.intro.impl.util.DialogUtil;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.eclipse.ui.internal.intro.impl.util.ReopenUtil;
import org.eclipse.ui.intro.IIntroSite;
import org.eclipse.ui.part.IntroPart;

/**
 * A re-usable intro part that the Eclipse platform uses for its Out of the Box
 * Experience. It is a customizable intro part where both its presentation, and
 * its content can be customized based on a configuration. Both are contributed
 * using the org.eclipse.ui.intro.config extension point. There are two
 * presentations: an SWT browser based presentation, and a UI forms
 * presentation. Based on the configuration, one is chosen on startup. If a
 * Browser based presentation is selected, and the intro is being loaded on a
 * platform that does not support the SWT Browser control, the default behavior
 * is to degrade to UI forms presentation. Content displayed in this intro part
 * can be static or dynamic. Static is html files, dynamic is markup in content
 * files. Again, both of which can be specified using the above extension point.
 * <p>
 * Memento Support: This intro part tries to restore its previous state when
 * possible. The state of the intro page is remembered, along with which standby
 * content content part was opened. IStandbyContent parts are passed the Intro's
 * memento shortly after construction, and are expected to restore there own
 * state based on the memento. The customizable intro part handles there initial
 * creation on load, and leaves restoring state to content part. Same with
 * saving state. The memento is passed shortly before shutdown to enable storing
 * of part specific data.
 *
 * Note: This class was made public for re-use, as-is, as a valid class for the
 * <code>org.eclipse.ui.intro</code> extension point. It is not intended to be
 * subclassed or used otherwise.
 * </p>
 *
 * @since 3.0
 */

/*
 * Internal docs: This class wraps a presentation part and a standby part. The
 * standby part is only shown when an IntroURL asks to show standby or when we
 * are restarting an Intro with an old memento. An internal data object
 * "showStandbyPart" is set on the control by the intro URL to signal standby
 * part needed. This data is nulled when the close button on the standby part is
 * clicked, signaling that the standby part is no longer needed.
 */
public final class CustomizableIntroPart extends IntroPart implements
		 IRegistryChangeListener {

	private IntroPartPresentation presentation;
	private StandbyPart standbyPart;
	private Composite container;
	private IMemento memento;
	IntroModelRoot model;
	// this flag is used to recreate a cached standby part. It is used once and
	// the set to false when the standby part is first created.
	private boolean restoreStandby;


	// Adapter factory to abstract out the StandbyPart implementation from APIs.
	IAdapterFactory factory = new IAdapterFactory() {

		@Override
		public Class<?>[] getAdapterList() {
			return new Class[] { StandbyPart.class, IntroPartPresentation.class };
		}

		@Override
		public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
			if (!(adaptableObject instanceof CustomizableIntroPart))
				return null;

			if (adapterType.equals(StandbyPart.class)) {
				return adapterType.cast(getStandbyPart());
			} else if (adapterType.equals(IntroPartPresentation.class)) {
				return adapterType.cast(getPresentation());
			} else
				return null;
		}
	};

	public CustomizableIntroPart() {
		// register adapter to hide standbypart.
		Platform.getAdapterManager().registerAdapters(factory,
			CustomizableIntroPart.class);
		// model can not be loaded here because the configElement of this part
		// is still not loaded here.

		// if we are logging performance, start the UI creation start time.
		// Clock stops at the end of the standbyStateChanged event.
		if (Log.logPerformance) {
			if (PerformanceStats.ENABLED)
				PerformanceStats.getStats(
					IIntroConstants.PERF_VIEW_CREATION_TIME,
					IIntroConstants.INTRO).startRun();
			else
				// capture start time to be used when only Intro performance
				// trace
				// is turned on.
				IntroPlugin.getDefault().setUICreationStartTime(
					System.currentTimeMillis());
		}
	}

	@Override
	public void init(IIntroSite site, IMemento memento)
			throws PartInitException {
		super.init(site, memento);
		IntroPlugin.getDefault().closeLaunchBar();
		// load the correct model based in the current Intro Part id. Set the
		// IntroPartId in the manager class.
		String introId = getConfigurationElement().getAttribute("id"); //$NON-NLS-1$
		ExtensionPointManager extensionPointManager = IntroPlugin.getDefault()
			.getExtensionPointManager();
		extensionPointManager.setIntroId(introId);
		model = extensionPointManager.getCurrentModel();

		if (model != null && model.hasValidConfig()) {

			boolean startAtHomePage = ReopenUtil.isReopenPreference();
			if (startAtHomePage) {
				PlatformUI.getPreferenceStore().setValue(
						IWorkbenchPreferenceConstants.SHOW_INTRO, true);
				memento = null;
			}
			// we have a valid config contribution, get presentation. Make sure
			// you pass correct memento.
			presentation = model.getPresentation();
			if (presentation != null)
				presentation.init(this, getMemento(memento,
					IIntroConstants.MEMENTO_PRESENTATION_TAG));

			// standby part is not created here for performance.

			// cache memento, and detemine if we have a cached standby part.
			this.memento = memento;
			restoreStandby = needToRestoreStandby(memento);

			// add the registry listerner for dynamic awarness.
			Platform.getExtensionRegistry().addRegistryChangeListener(this,
				IIntroConstants.PLUGIN_ID);
		}

		if (model == null || !model.hasValidConfig())
			DialogUtil.displayErrorMessage(site.getShell(),
				Messages.CustomizableIntroPart_configNotFound,
				new Object[] { ModelLoaderUtil.getLogString(
					getConfigurationElement(), null) }, null);

	}

	/**
	 * Creates the UI based on how the InroPart has been configured.
	 *
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		container = new Composite(parent, SWT.NULL);
		StackLayout layout = new StackLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);

		if (model != null && model.hasValidConfig()) {
			presentation.createPartControl(container);
			// do not create the standby part here for performance.
		}

		if (Log.logPerformance) {
			PerformanceStats stats = PerformanceStats.getStats(
				IIntroConstants.PERF_UI_ZOOM, IIntroConstants.INTRO);
			stats.startRun();
		}

	}




	/**
	 * Determine if we need to recreate a standby part. Return true if we have a
	 * standby part memento that is not for the empty part AND stangby memento
	 * has been tagged for restore, ie: it was open when workbench closed.
	 *
	 * @param memento
	 * @return <code>true</code> if we need to recreate a standby part
	 */
	private boolean needToRestoreStandby(IMemento memento) {
		// If we have a standby memento, it means we closed with standby open,
		// and so recreate it.
		IMemento standbyMemento = getMemento(memento, IIntroConstants.MEMENTO_STANDBY_PART_TAG);
		if (standbyMemento == null)
			return false;
		String restore = standbyMemento.getString(IIntroConstants.MEMENTO_RESTORE_ATT);
		if (restore == null)
			return false;
		String cachedStandbyPart = standbyMemento
			.getString(IIntroConstants.MEMENTO_STANDBY_CONTENT_PART_ID_ATT);
		if (cachedStandbyPart != null
				&& cachedStandbyPart.equals(IIntroConstants.EMPTY_STANDBY_CONTENT_PART))
			return false;

		return cachedStandbyPart != null ? true : false;
	}

	/*
	 * Handled state changes. Recreates the standby part if workbench was shut
	 * down with one.
	 *
	 * @see org.eclipse.ui.IIntroPart#standbyStateChanged(boolean)
	 */
	@Override
	public void standbyStateChanged(boolean standby) {

		// do this only if there is a valid config.
		if (model == null || !model.hasValidConfig())
			return;

		if (!standby)
			// we started of not in standby, no need to restore standby.
			restoreStandby = false;

		boolean isStandbyPartNeeded = isStandbyPartNeeded();
		isStandbyPartNeeded = isStandbyPartNeeded || restoreStandby;

		try {
			if (standbyPart == null && standby && isStandbyPartNeeded)
				// if standby part is not created yet, create it only if in
				// standby, and we need to.
				createStandbyPart();

			handleSetFocus(isStandbyPartNeeded);
			setTopControl(isStandbyPartNeeded ? getStandbyControl()
					: getPresentationControl());
			// triger state change in presentation to enable/disable toobar
			// actions. For this, we need to disable actions as long as we are in
			// standby, or we need to show standby part.
			presentation.standbyStateChanged(standby, isStandbyPartNeeded);
		} catch (RuntimeException e) {
			Log.error("Exception thrown in intro", e); //$NON-NLS-1$
		}
	}

	/**
	 * Returns true if we need to show the standby part. False in all other
	 * cases. This basically overrides the workbench behavior of Standby/normal
	 * states. The design here is that if the showStandbyPart flag is set, then
	 * we always need to show the standby part.
	 *
	 * @return <code>true</code> if we need to show the standby part
	 */
	private boolean isStandbyPartNeeded() {
		return container.getData(IIntroConstants.SHOW_STANDBY_PART) == null ? false : true;
	}

	/*
	 * Create standby part. Called only when really needed. We reset the restore
	 * falg, but we need to tag the intro part as needing standby.
	 */
	private void createStandbyPart() {
		standbyPart = new StandbyPart(model);
		standbyPart.init(this, getMemento(memento, IIntroConstants.MEMENTO_STANDBY_PART_TAG));
		standbyPart.createPartControl((Composite) getControl());
		restoreStandby = false;
		container.setData(IIntroConstants.SHOW_STANDBY_PART, "true"); //$NON-NLS-1$
	}

	private void handleSetFocus(boolean standby) {
		if (standby) {
			// standby part is null when Intro has not gone into standby state
			// yet.
			if (standbyPart != null)
				standbyPart.setFocus();
		} else
			presentation.setFocus();
	}

	@Override
	public void setFocus() {
		handleSetFocus(IntroPlugin.isIntroStandby());
	}

	private void setTopControl(Control c) {
		// container has stack layout. safe to cast.
		StackLayout layout = (StackLayout) container.getLayout();
		layout.topControl = c;
		container.layout();
	}

	private Control getPresentationControl() {
		return container.getChildren()[0];
	}

	private Control getStandbyControl() {
		// the Container top control may have only one child if the standby
		// part is not created yet. This happens if the intro never goes into
		// standby. Doing this for performance.
		if (standbyPart != null)
			return container.getChildren()[1];
		return null;
	}

	IntroPartPresentation getPresentation() {
		return presentation;
	}

	@Override
	public void dispose() {
		super.dispose();
		// call dispose on both parts.
		if (presentation != null)
			presentation.dispose();
		if (standbyPart != null)
			standbyPart.dispose();
		// clear all loaded models since we are disposing of the Intro Part.
		IntroPlugin.getDefault().getExtensionPointManager().clear();
		ContentProviderManager.getInst().clear();
		// clean platform adapter.
		Platform.getAdapterManager().unregisterAdapters(factory,
			CustomizableIntroPart.class);
		if (model != null && model.hasValidConfig())
			Platform.getExtensionRegistry().removeRegistryChangeListener(this);

	}

	/**
	 * @return Returns the standbyPart.
	 */
	StandbyPart getStandbyPart() {
		return standbyPart;
	}

	/**
	 * Returns the primary control associated with this Intro part.
	 *
	 * @return the SWT control which displays this Intro part's content, or
	 *         <code>null</code> if this standby part's controls have not yet
	 *         been created.
	 */
	public Control getControl() {
		return container;
	}

	@Override
	public void saveState(IMemento memento) {
		// give presentation and standby part there own children to create a
		// name space for each. But save either the presentation or the standby
		// part as needing to be restored. This way if we close with a standby
		// mode, we dont get the cached standby part.

		// Find out if presentation or standby is at the top and restore
		// them. Container has stack layout. safe to cast.
		boolean restorePresentation = false;
		StackLayout layout = (StackLayout) container.getLayout();
		if (getPresentationControl().equals(layout.topControl))
			restorePresentation = true;

		IMemento presentationMemento = memento
			.createChild(IIntroConstants.MEMENTO_PRESENTATION_TAG);
		IMemento standbyPartMemento = memento
			.createChild(IIntroConstants.MEMENTO_STANDBY_PART_TAG);
		if (restorePresentation)
			presentationMemento.putString(IIntroConstants.MEMENTO_RESTORE_ATT, "true"); //$NON-NLS-1$
		else
			standbyPartMemento.putString(IIntroConstants.MEMENTO_RESTORE_ATT, "true"); //$NON-NLS-1$
		if (presentation != null)
			presentation.saveState(presentationMemento);
		if (standbyPart != null)
			standbyPart.saveState(standbyPartMemento);
	}

	private IMemento getMemento(IMemento memento, String key) {
		if (memento == null)
			return null;
		return memento.getChild(key);
	}

	/**
	 * Support dynamic awarness.
	 *
	 * @see org.eclipse.core.runtime.IRegistryChangeListener#registryChanged(org.eclipse.core.runtime.IRegistryChangeEvent)
	 */
	@Override
	public void registryChanged(final IRegistryChangeEvent event) {
		// Clear cached models first, then update UI by delegating to
		// implementation. wrap in synchExec because notification is
		// asynchronous. The design here is that the notification is centralized
		// here, then this event propagates and each receiving class reacts
		// accordingly.
		Display.getDefault().syncExec(() -> {
			String currentPageId = model.getCurrentPageId();
			// clear model, including content providers.
			ExtensionPointManager.getInst().clear();
			ContentProviderManager.getInst().clear();
			// refresh to new model.
			model = ExtensionPointManager.getInst().getCurrentModel();
			// reuse existing presentation, since we just nulled it.
			model.setPresentation(getPresentation());
			// keep same page on refresh. No need for notification here.
			model.setCurrentPageId(currentPageId, false);
			if (getPresentation() != null)
				getPresentation().registryChanged(event);

		});

	}

	/*
	 * Internal test hook (Non-API).
	 */
	public boolean internal_isFinishedLoading() {
		BrowserIntroPartImplementation impl = (BrowserIntroPartImplementation)presentation.getIntroPartImplementation();
		return impl.isFinishedLoading();
	}
}
