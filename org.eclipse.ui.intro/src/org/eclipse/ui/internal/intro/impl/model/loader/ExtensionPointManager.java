/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
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

package org.eclipse.ui.internal.intro.impl.model.loader;

import org.eclipse.ui.internal.intro.impl.model.ExtensionMap;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.util.Log;

/**
 * Manages all Intro plugin extension points. Currently, there are two:
 * org.eclipse.ui.intro.config &amp; org.eclipse.ui.intro.configExtension. <br>
 * The model is lazily loaded on per need basis. This happens when a page is
 * asked for its children, or when the model is trying to resolve includes or
 * extensions. <br>
 */

public class ExtensionPointManager extends BaseExtensionPointManager {

	// singleton instance. Can be retrieved from here or from the Intro Plugin.
	private static ExtensionPointManager inst = new ExtensionPointManager();

	// The root model class that represents a full/combined OOBBE config. This
	// model is loaded based on an introId when the customizableIntroPart tries
	// to load a model based on introId. This is different when includes and
	// extension aer resolved because in tnose cases models are being loaded
	// given an id and not an introId.
	private IntroModelRoot currentModel;

	// the id of the intro part contribution who's model (config) we are trying
	// to load. The customizableIntroPart loads this id and loads the model that
	// is bound to this intro id (ie: has this id as an introId).
	private String introId;

	/*
	 * Prevent creation.
	 */
	private ExtensionPointManager() {
		super();
	}

	/**
	 * @return Returns the inst.
	 */
	public static ExtensionPointManager getInst() {
		return inst;
	}

	/**
	 * Load the intro model given the current intro id.
	 */
	private void loadCurrentModel() {
		currentModel = loadModel(ATT_CONFIG_INTRO_ID, this.introId);
	}

	/**
	 * @return Returns the Intro Model root. Note: Prefereed way of getting to
	 *         the intro model root is throught the intro plugin.
	 */
	public IntroModelRoot getCurrentModel() {
		if (currentModel == null)
			// we never loaded this model before, or we tried before and we
			// failed. Load it. Get the correct config element based on
			// config introId, and log any extra contributions.
			loadCurrentModel();
		return currentModel;
	}

	/**
	 * Load an intro model given a config id.
	 *
	 * @param configId
	 * @return
	 */
	public IntroModelRoot getModel(String configId) {
		IntroModelRoot model = getCachedModel(configId);
		if (model == null) {
			// we never loaded this model before, or we tried before and we
			// failed. Load it. Get the correct config element based on
			// config id, and log any extra contributions.
			model = loadModel(ATT_ID, configId);
		}
		return model;
	}

	/**
	 * @param introPartId
	 *            The introPartId to set.
	 */
	public void setIntroId(String introId) {
		this.introId = introId;
		// we do not have to clean model here. remove cached model, if it
		// exists.
		this.currentModel = null;
	}

	public void clear() {
		currentModel = null;
		sharedConfigExtensionsManager = null;
		introModels.clear();
		ExtensionMap.getInstance().clear();
		if (Log.logInfo)
			Log.info("Cleared Intro model"); //$NON-NLS-1$
	}


}
