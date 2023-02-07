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

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipAction;
import org.eclipse.tips.core.TipImage;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class Tip7_Extend extends Tip implements IHtmlTip {

	public Tip7_Extend(String providerId) {
		super(providerId);
	}

	@Override
	public List<TipAction> getActions() {
		Function<String, Boolean> openBrowser = input -> {
			Desktop d = Desktop.getDesktop();
			try {
				d.browse(new URI("https://wiki.eclipse.org/Tip_of_the_Day"));
			} catch (IOException | URISyntaxException e) {
				e.printStackTrace();
			}
			return false;
		};
		Runnable runner = () -> Display.getDefault().asyncExec(() -> {
			if (Platform.isRunning() && Platform.getWS().startsWith("gtk")) {
				boolean confirm = MessageDialog.openConfirm(null, "Action",
						"Can't open a browser in GTK. It crashes the JVM. Press Ok to try anyway.");
				if (confirm) {
					openBrowser.apply("go");
				}
			} else {
				openBrowser.apply("go");
			}
		});

		ArrayList<TipAction> actions = new ArrayList<>();
		actions.add(new TipAction("Open Browser", "Opens Eclipse Wiki.", runner, null));
		return actions;
	}

	@Override
	public Date getCreationDate() {
		return TipsTipProvider.getDateFromYYMMDD("09/01/2019");
	}

	@Override
	public String getSubject() {
		return "On GitHub";
	}

	@Override
	public String getHTML() {
		return "<h2>Extending Tips</h2>You can extend this framework and add your own tip provider for your project. Press the action button to open the Eclipse Wiki for more information."
				+ "<br><br>";
	}

	private TipImage fImage;

	@Override
	public TipImage getImage() {
		if (fImage == null) {
			try {
				Bundle bundle = FrameworkUtil.getBundle(getClass());
				fImage = new TipImage(bundle.getEntry("images/tips/photon.jpg")).setAspectRatio(720, 480, true);
			} catch (Exception e) {
			}
		}
		return fImage;
	}
}