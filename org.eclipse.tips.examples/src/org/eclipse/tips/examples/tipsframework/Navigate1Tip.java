/*******************************************************************************
 * Copyright (c) 2018, 2023 Remain Software
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
package org.eclipse.tips.examples.tipsframework;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipAction;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.examples.DateUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class Navigate1Tip extends Tip implements IHtmlTip {

	private TipImage fImage;

	@Override
	public TipImage getImage() {
		if (fImage == null) {
			try {
				Bundle bundle = FrameworkUtil.getBundle(getClass());
				fImage = new TipImage(bundle.getEntry("images/tips/navigate1.png")).setAspectRatio(600, 200, true);
			} catch (Exception e) {
//				getProvider().getManager().log(LogUtil.info(getClass(), e));
			}
		}
		return fImage;
	}

	public Navigate1Tip(String providerId) {
		super(providerId);
	}

	@Override
	public List<TipAction> getActions() {
		Runnable runnable = () -> Display.getDefault()
				.syncExec(() -> MessageDialog.openConfirm(null, getSubject(), "We can do anything we want."));
		Runnable clock = () -> Display.getDefault().syncExec(() -> MessageDialog.openConfirm(null, getSubject(),
				DateFormat.getTimeInstance().format(Calendar.getInstance().getTime())));
		Runnable runner2 = () -> Display.getDefault()
				.syncExec(() -> MessageDialog.openConfirm(null, getSubject(), "Like open preferences..."));
		ArrayList<TipAction> actions = new ArrayList<>();
		actions.add(new TipAction("Clock", "Some sort of clock action", clock, getImage("icons/clock.png")));
		actions.add(
				new TipAction("Open Preferences", "Opens the preferences", runner2, getImage("icons/bug_link.png")));
		actions.add(
				new TipAction("Cut or Paste", "Just another silly action", runnable, getImage("icons/lightbulb.png")));
		actions.add(new TipAction("Eclipse Rocks, Idea Scissors", "Paper", runnable, getImage("icons/cut.png")));
		actions.add(new TipAction("Totally Bonkers", "The quick brown fox", runnable, getImage("icons/notfound.png")));
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
		return DateUtil.getDateFromYYMMDD("09/01/2018");
	}

	@Override
	public String getSubject() {
		return "Navigate Tip 1";
	}

	@Override
	public String getHTML() {
		return """
				<h2>Navigating Tips</h2>
				You can navigate tips by using the button bar.
				<br><b>Next Tip</b>
				<br>Navigates to the next tip.
				<br><b>Previous Tip</b>
				<br>Navigates to the previous tip.
				<br><b>Close</b>
				<br>Closes the Dialog (<b>Escape</b> does the same).
				<br><b>Show tips at startup</b>
				<br>A toggle to show this dialog when you start Eclipse.
				<br><br>If a tip can do something special then the <b>More...</b> button
				is activated, like with this tip.
				<br>Go on, press it!""";
	}
}