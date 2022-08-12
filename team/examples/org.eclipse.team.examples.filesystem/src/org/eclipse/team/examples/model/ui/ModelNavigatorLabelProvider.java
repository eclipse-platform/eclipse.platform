/*******************************************************************************
 * Copyright (c) 2006, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.model.ui;

import org.eclipse.team.examples.model.ModelObject;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

/**
 * Model content provider for use with the Common Navigator framework.
 * It makes use of an <code>IWorkbenchAdapter</code> to get the label and image
 * of model objects.
 */
public class ModelNavigatorLabelProvider extends WorkbenchLabelProvider implements
		ICommonLabelProvider {

	private ICommonContentExtensionSite extensionSite;

	@Override
	public void init(ICommonContentExtensionSite aConfig) {
		extensionSite = aConfig;
	}

	@Override
	public void restoreState(IMemento aMemento) {
		// Nothing to do
	}

	@Override
	public void saveState(IMemento aMemento) {
		// Nothing to do
	}

	@Override
	public String getDescription(Object anElement) {
		if (anElement instanceof ModelObject) {
			return ((ModelObject) anElement).getPath();
		}
		return null;
	}

	/**
	 * Return the extension site for this label provider.
	 * @return the extension site for this label provider
	 */
	public ICommonContentExtensionSite getExtensionSite() {
		return extensionSite;
	}

}
