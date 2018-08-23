/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.mapping;

import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.internal.core.TeamPlugin;

public class ChangeSetModelProvider extends ModelProvider {

	public static final String ID = "org.eclipse.team.cvs.core.changeSetModel"; //$NON-NLS-1$
	private static ChangeSetModelProvider provider;
	
	public ChangeSetModelProvider() {
		super();
	}

	public static ChangeSetModelProvider getProvider() {
		if (provider == null) {
			try {
				provider = (ChangeSetModelProvider)ModelProvider.getModelProviderDescriptor(ChangeSetModelProvider.ID).getModelProvider();
			} catch (CoreException e) {
				TeamPlugin.log(e);
			}
		}
		return provider;
	}

}
