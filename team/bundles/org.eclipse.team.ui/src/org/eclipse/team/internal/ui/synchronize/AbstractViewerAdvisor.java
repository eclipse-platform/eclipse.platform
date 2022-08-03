/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.IActionBars;

/**
 * Abstract superclass for viewer advisors
 */
public abstract class AbstractViewerAdvisor {

	private ISynchronizePageConfiguration configuration;
	private StructuredViewer viewer;

	public AbstractViewerAdvisor(ISynchronizePageConfiguration configuration) {
		this.configuration = configuration;
		configuration.setProperty(SynchronizePageConfiguration.P_ADVISOR, this);
	}

	public ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Install a viewer to be configured with this advisor. An advisor can only be installed with
	 * one viewer at a time. When this method completes the viewer is considered initialized and
	 * can be shown to the user.
	 * @param viewer the viewer being installed
	 */
	protected void initializeViewer(final StructuredViewer viewer) {
		Assert.isTrue(this.viewer == null, "Can only be initialized once."); //$NON-NLS-1$
		Assert.isTrue(validateViewer(viewer));
		this.viewer = viewer;
	}

	/**
	 * Subclasses can validate that the viewer being initialized with this advisor
	 * is of the correct type.
	 *
	 * @param viewer the viewer to validate
	 * @return <code>true</code> if the viewer is valid, <code>false</code> otherwise.
	 */
	protected boolean validateViewer(StructuredViewer viewer) {
		return true;
	}

	/**
	 * Returns the viewer configured by this advisor.
	 *
	 * @return the viewer configured by this advisor.
	 */
	public StructuredViewer getViewer() {
		return viewer;
	}

	public abstract void setActionBars(IActionBars actionBars);

	public abstract void setInitialInput();
}
