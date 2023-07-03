/*******************************************************************************
 * Copyright (c) 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.ide.internal;

import java.util.List;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipProvider;

/**
 *
 * Internal class to listen to async provider load completions.
 *
 */
public class ProviderLoadJobChangeListener extends JobChangeAdapter {

	private IDETipManager fManager;
	private TipProvider fProvider;

	public ProviderLoadJobChangeListener(IDETipManager manager, TipProvider provider) {
		fManager = manager;
		fProvider = provider;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * If this provider has new tips then the {@link IDETipManager} gets a callback
	 * to update the UI.
	 *
	 * @see IDETipManager#setNewTips(boolean)
	 */
	@Override
	public void done(IJobChangeEvent event) {
		List<Tip> unreadTips = fProvider.getTips(tip -> !fManager.isRead(tip));
		fManager.setNewTips(!unreadTips.isEmpty());
	}
}