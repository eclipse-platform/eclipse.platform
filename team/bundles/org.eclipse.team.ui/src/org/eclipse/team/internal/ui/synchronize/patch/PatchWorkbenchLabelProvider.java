/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonLabelProvider;

public class PatchWorkbenchLabelProvider extends WorkbenchLabelProvider
		implements ICommonLabelProvider {

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
		if (anElement instanceof DiffNode) {
			return ((DiffNode) anElement).getName();
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
