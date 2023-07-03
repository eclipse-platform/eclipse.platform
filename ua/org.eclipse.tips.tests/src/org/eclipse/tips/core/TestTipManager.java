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
package org.eclipse.tips.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.tips.core.internal.TipManager;

@SuppressWarnings("restriction")
public class TestTipManager extends TipManager {

	private List<Integer> fReadList = new ArrayList<>();
	private int fStartupBehavior = TipManager.START_DIALOG;

	@Override
	public TipManager setStartupBehavior(int pStartupBehavior) {
		fStartupBehavior = pStartupBehavior;
		return this;
	}

	@Override
	public int getStartupBehavior() {
		return fStartupBehavior;
	}

	@Override
	public ITipManager register(TipProvider provider) {
		super.register(provider);
		load(provider);
		return this;
	}

	private void load(TipProvider provider) {
		provider.loadNewTips(new NullProgressMonitor());
	}

	@Override
	public boolean isRead(Tip tip) {
		return fReadList.contains(Integer.valueOf(tip.hashCode()));
	}

	@Override
	public TipManager setAsRead(Tip tip) {
		fReadList.remove(Integer.valueOf(tip.hashCode()));
		fReadList.add(Integer.valueOf(tip.hashCode()));
		return this;
	}

	@Override
	public ITipManager log(IStatus status) {
		System.out.println(status.toString());
		return this;
	}

	@Override
	public TipManager open(boolean startUp) {
		setOpen(true);
		return this;
	}

	@Override
	public int getPriority(TipProvider provider) {
		return 20;
	}
}