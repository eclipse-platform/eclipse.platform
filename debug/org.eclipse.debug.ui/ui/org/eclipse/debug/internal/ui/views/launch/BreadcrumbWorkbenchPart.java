/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Pawel Piech - Wind River - adapted to use in Debug view
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.launch;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * Fake part to used to create the breadcrumb page.
 *
 * @since 3.5
 */
class BreadcrumbWorkbenchPart implements IWorkbenchPart {

	private IWorkbenchPartSite fSite = null;

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof BreadcrumbWorkbenchPart);
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	/**
	 * Constructs a part for the given console that binds to the given
	 * site
	 * @param site the backing site
	 */
	public BreadcrumbWorkbenchPart(IWorkbenchPartSite site) {
		fSite = site;
	}

	@Override
	public void addPropertyListener(IPropertyListener listener) {
	}

	@Override
	public void createPartControl(Composite parent) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public IWorkbenchPartSite getSite() {
		return fSite;
	}

	@Override
	public String getTitle() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public Image getTitleImage() {
		return null;
	}

	@Override
	public String getTitleToolTip() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public void removePropertyListener(IPropertyListener listener) {
	}

	@Override
	public void setFocus() {
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}
}

