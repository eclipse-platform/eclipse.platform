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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipAction;
import org.eclipse.tips.core.TipImage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class Tip6_ActionsTip extends Tip implements IHtmlTip {

	private TipImage fImage;

	@Override
	public TipImage getImage() {
		if (fImage == null) {
			Optional<TipImage> tipImage = TipsTipProvider.getTipImage("images/tips/actions.png"); //$NON-NLS-1$
			fImage = tipImage.map(i -> i.setAspectRatio(758, 480, true)).orElse(null);
		}
		return fImage;
	}

	public Tip6_ActionsTip(String providerId) {
		super(providerId);
	}

	@Override
	public List<TipAction> getActions() {
		TipAction tip1 = tip("Clock", "What is the time?", "icons/clock.png", () -> MessageDialog.openConfirm(null, //$NON-NLS-3$
				getSubject(), DateFormat.getTimeInstance().format(Calendar.getInstance().getTime())));
		TipAction tip2 = tip("Open Preferences", "Opens the preferences", null, () -> {
			PreferenceDialog pref = PreferencesUtil.createPreferenceDialogOn(
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), "myPreferencePage", null, null);
			if (pref != null) {
				pref.open();
			}
		});
		TipAction tip3 = tip("Open Dialog", "Opens a Dialog", "icons/asterisk.png", //$NON-NLS-3$
				() -> MessageDialog.openConfirm(null, getSubject(), "A dialog was opened."));
		return List.of(tip1, tip2, tip3);
	}

	private static TipAction tip(String text, String tooltip, String imagePath, Runnable runner) {
		TipImage image = imagePath != null
				? TipsTipProvider.getTipImage(imagePath).map(i -> i.setAspectRatio(1)).orElse(null)
				: null;
		return new TipAction(text, tooltip, () -> Display.getDefault().syncExec(runner), image);
	}

	@Override
	public Date getCreationDate() {
		return TipsTipProvider.getDateFromYYMMDD(9, 1, 2019);
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