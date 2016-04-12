/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.registry;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.team.internal.ui.TeamUIPlugin;

public class SynchronizeWizardRegistry extends RegistryReader {

	public static final String PT_SYNCHRONIZE_WIZARDS = "synchronizeWizards"; //$NON-NLS-1$
	private static final String TAG_SYNCHRONIZE_WIZARD = "wizard"; //$NON-NLS-1$
	private Map wizards = new HashMap();

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ui.registry.RegistryReader#readElement(org.eclipse.core.runtime.IConfigurationElement)
	 */
	@Override
	protected boolean readElement(IConfigurationElement element) {
		if (element.getName().equals(TAG_SYNCHRONIZE_WIZARD)) {
			String descText = getDescription(element);
			SynchronizeWizardDescription desc;
			try {
				desc = new SynchronizeWizardDescription(element, descText);
				wizards.put(desc.getId(), desc);
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
			return true;
		}
		return false;
	}

	public SynchronizeWizardDescription[] getSynchronizeWizards() {
		return (SynchronizeWizardDescription[])wizards.values().toArray(new SynchronizeWizardDescription[wizards.size()]);
	}

}
