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
package org.eclipse.tips.ide.internal.provider;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipAction;
import org.eclipse.tips.core.TipImage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class Tip6_ActionsTip extends Tip implements IHtmlTip {

	private TipImage fImage;

	@Override
	public TipImage getImage() {
		if (fImage == null) {
			try {
				Bundle bundle = FrameworkUtil.getBundle(getClass());
				fImage = new TipImage(bundle.getEntry("images/tips/actions.png")).setAspectRatio(758, 480, true);
			} catch (Exception e) {
			}
		}
		return fImage;
	}

	public Tip6_ActionsTip(String providerId) {
		super(providerId);
	}

	@Override
	public List<TipAction> getActions() {
		Runnable runnable = () -> Display.getDefault()
				.syncExec(() -> MessageDialog.openConfirm(null, getSubject(), "A dialog was opened."));
		Runnable clock = () -> Display.getDefault().syncExec(() -> MessageDialog.openConfirm(null, getSubject(),
				DateFormat.getTimeInstance().format(Calendar.getInstance().getTime())));
		Runnable runner2 = () -> Display.getDefault().syncExec(() -> {
			PreferenceDialog pref = PreferencesUtil.createPreferenceDialogOn(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "myPreferencePage", null, null);
			if (pref != null) {
				pref.open();
			}
		});
		ArrayList<TipAction> actions = new ArrayList<>();
		actions.add(new TipAction("Clock", "What is the time?", clock, getImage("icons/clock.png")));
		actions.add(
				new TipAction("Open Preferences", "Opens the preferences", runner2, null));
		actions.add(new TipAction("Open Dialog", "Opens a Dialog", runnable, getImage("icons/asterisk.png")));
		return actions;
	}

	private TipImage getImage(String pIcon) {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		try {
			return new TipImage(bundle.getEntry(pIcon)).setAspectRatio(1);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Date getCreationDate() {
		return TipsTipProvider.getDateFromYYMMDD("09/01/2019");
	}

	@Override
	public String getSubject() {
		return "Actions";
	}

	@Override
	public String getHTML() {
		return "<h2>ActionTips</h2>Some tips enable you to start one or more actions. " //
				+ "If this is the case then an additional button will be displayed " //
				+ "like in this tip. Go ahead and press the button, or choose another "
				+ "action from the drop down menu next to the button. <br><br><br>";
	}
}