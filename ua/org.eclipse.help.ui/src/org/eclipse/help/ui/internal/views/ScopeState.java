/*******************************************************************************
 * Copyright (c) 2010, 2011 IBM Corporation and others.
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

package org.eclipse.help.ui.internal.views;

public class ScopeState  {

	private static ScopeState instance;
	private ScopeSetManager scopeSetManager;
	private EngineDescriptorManager engineManager;

	public static synchronized ScopeState getInstance() {
		if ( instance == null )
		{
			instance = new ScopeState();
		}
		return instance;
	}

	private ScopeState() {
		scopeSetManager = new ScopeSetManager();
	}

	public ScopeSetManager getScopeSetManager() {
		return scopeSetManager;
	}

	public EngineDescriptorManager getEngineManager() {
		return engineManager;
	}

	public void setEngineManager(EngineDescriptorManager engineManager) {
		this.engineManager = engineManager;
	}

}
