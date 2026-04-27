/*******************************************************************************
 * Copyright (c) 2004, 2026 IBM Corporation and others.
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
import org.eclipse.swt.layout.FillLayout;
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
 * possible. The state of the intro page is remembered. The memento is passed
 * shortly before shutdown to enable storing of part specific data.
 * </p>
 * <p>
 * Note: This class was made public for re-use, as-is, as a valid class for the
 * <code>org.eclipse.ui.intro</code> extension point. It is not intended to be
 * subclassed or used otherwise.
 * </p>
 *
 * @since 3.0
 */
public final class CustomizableIntroPart extends IntroPart implements
		 IRegistryChangeListener {

	private IntroPartPresentation presentation;
	private Composite container;
	IntroModelRoot model;

	// Adapter factory to expose IntroPartPresentation.
	IAdapterFactory factory = new IAdapterFactory() {

		@Override
		public Class<?>[] getAdapterList() {
			return new Class[] { IntroPartPresentation.class };
		}

		@Override
		public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
			if (!(adaptableObject instanceof CustomizableIntroPart)) {
				return null;
			}

			if (adapterType.equals(IntroPartPresentation.class)) {
				return adapterType.cast(getPresentation());
			}
			return null;
		}
	};

	public CustomizableIntroPart() {
		Platform.getAdapterManager().registerAdapters(factory,
			CustomizableIntroPart.class);
		// model can not be loaded here because the configElement of this part
		// is still not loaded here.

		// if we are logging performance, start the UI creation start time.
		// Clock stops at the end of the standbyStateChanged event.
		if (Log.logPerformance) {
			if (PerformanceStats.ENABLED) {
				PerformanceStats.getStats(
					IIntroConstants.PERF_VIEW_CREATION_TIME,
					IIntroConstants.INTRO).startRun();
			} else {
				IntroPlugin.getDefault().setUICreationStartTime(
					System.currentTimeMillis());
			}
		}
	}

	@SuppressWarnings("removal")
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
			if (presentation != null) {
				presentation.init(this, getMemento(memento,
					IIntroConstants.MEMENTO_PRESENTATION_TAG));
			}

			// add the registry listener for dynamic awareness.
			Platform.getExtensionRegistry().addRegistryChangeListener(this,
				IIntroConstants.PLUGIN_ID);
		}

		if (model == null || !model.hasValidConfig()) {
			DialogUtil.displayErrorMessage(site.getShell(),
				Messages.CustomizableIntroPart_configNotFound,
				new Object[] { ModelLoaderUtil.getLogString(
					getConfigurationElement(), null) }, null);
		}

	}

	/**
	 * Creates the UI based on how the IntroPart has been configured.
	 *
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		container = new Composite(parent, SWT.NULL);
		FillLayout layout = new FillLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);

		if (model != null && model.hasValidConfig()) {
			presentation.createPartControl(container);
		}

		if (Log.logPerformance) {
			PerformanceStats stats = PerformanceStats.getStats(
				IIntroConstants.PERF_UI_ZOOM, IIntroConstants.INTRO);
			stats.startRun();
		}

	}

	@Override
	public void standbyStateChanged(boolean standby) {
		// do this only if there is a valid config.
		if (model == null || !model.hasValidConfig()) {
			return;
		}

		try {
			presentation.setFocus();
			presentation.standbyStateChanged(standby, false);
		} catch (RuntimeException e) {
			Log.error("Exception thrown in intro", e); //$NON-NLS-1$
		}
	}

	@Override
	public void setFocus() {
		if (presentation != null) {
			presentation.setFocus();
		}
	}

	IntroPartPresentation getPresentation() {
		return presentation;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (presentation != null) {
			presentation.dispose();
		}
		// clear all loaded models since we are disposing of the Intro Part.
		IntroPlugin.getDefault().getExtensionPointManager().clear();
		ContentProviderManager.getInst().clear();
		// clean platform adapter.
		Platform.getAdapterManager().unregisterAdapters(factory,
			CustomizableIntroPart.class);
		if (model != null && model.hasValidConfig()) {
			Platform.getExtensionRegistry().removeRegistryChangeListener(this);
		}

	}

	/**
	 * Returns the primary control associated with this Intro part.
	 *
	 * @return the SWT control which displays this Intro part's content, or
	 *         <code>null</code> if this part's controls have not yet been
	 *         created.
	 */
	public Control getControl() {
		return container;
	}

	@Override
	public void saveState(IMemento memento) {
		IMemento presentationMemento = memento
			.createChild(IIntroConstants.MEMENTO_PRESENTATION_TAG);
		presentationMemento.putString(IIntroConstants.MEMENTO_RESTORE_ATT, "true"); //$NON-NLS-1$
		if (presentation != null) {
			presentation.saveState(presentationMemento);
		}
	}

	private IMemento getMemento(IMemento memento, String key) {
		if (memento == null) {
			return null;
		}
		return memento.getChild(key);
	}

	/**
	 * Support dynamic awareness.
	 *
	 * @see org.eclipse.core.runtime.IRegistryChangeListener#registryChanged(org.eclipse.core.runtime.IRegistryChangeEvent)
	 */
	@Override
	public void registryChanged(final IRegistryChangeEvent event) {
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
			if (getPresentation() != null) {
				getPresentation().registryChanged(event);
			}

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
