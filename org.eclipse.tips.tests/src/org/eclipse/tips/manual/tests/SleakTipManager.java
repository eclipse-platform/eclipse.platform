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
package org.eclipse.tips.manual.tests;

import java.net.MalformedURLException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tips.core.ITipManager;
import org.eclipse.tips.core.JsonTestProvider;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipProvider;
import org.eclipse.tips.core.internal.TipManager;
import org.eclipse.tips.ui.internal.TipDialog;

/**
 * Class to manage the tip providers and start the tip of the day UI.
 */
@SuppressWarnings("restriction")
public class SleakTipManager extends TipManager {

	private static SleakTipManager instance = new SleakTipManager();

	public static void main(String[] args) throws MalformedURLException {
		instance.register(new JsonTestProvider());
		if (!instance.getProviders().isEmpty()) {
			instance.open(true);
		}
	}

	/**
	 * @return the tip manager instance.
	 */
	public static SleakTipManager getInstance() {
		return instance;
	}

	private SleakTipManager() {
	}

	/**
	 * For resource leak detection rename this method to open and run the IDE. Won't
	 * work on Linux because GTK cannot handle multiple displays.
	 */
	@Override
	public TipManager open(boolean pStart) {

		Thread t = new Thread(() -> {

			DeviceData data = new DeviceData();
			data.tracking = true;
			Display display = new Display(data);
			new Sleak().open();
			TipDialog tipDialog = new TipDialog(null, SleakTipManager.this, TipDialog.DEFAULT_STYLE, null);
			tipDialog.open();
			Shell shell = tipDialog.getShell();
			shell.addDisposeListener(pE -> dispose());
			shell.pack();
			shell.open();
			while (!display.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
			display.dispose();
		});

		t.start();
		return this;

	}

	@Override
	public ITipManager register(TipProvider provider) {
		super.register(provider);
		load(provider);
		return this;
	}

	private void load(TipProvider pProvider) {
		pProvider.loadNewTips(new NullProgressMonitor());
	}

	@Override
	public boolean isRead(Tip pTip) {
		return false;
	}

	@Override
	public TipManager setAsRead(Tip pTip) {
		return this;
	}

	protected synchronized SleakTipManager setNewTips(boolean pNewTips) {
		return this;
	}

	@Override
	public TipManager setStartupBehavior(int pStartupBehavior) {
		return this;
	}

	@Override
	public ITipManager log(IStatus pStatus) {
		return this;
	}

	@Override
	public int getPriority(TipProvider pProvider) {
		return 0;
	}
}