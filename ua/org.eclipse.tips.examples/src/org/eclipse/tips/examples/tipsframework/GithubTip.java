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

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipAction;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.examples.DateUtil;

public class GithubTip extends Tip implements IHtmlTip {

	public GithubTip(String providerId) {
		super(providerId);
	}

	@Override
	public List<TipAction> getActions() {
		Runnable runner = () -> Display.getDefault().asyncExec(() -> {
			if (Platform.isRunning() && Platform.getWS().startsWith("gtk")) {
				MessageDialog.openInformation(null, "Action", "Can't open a browser in GTK. It crashes the JVM.");
			} else {
				Desktop d = Desktop.getDesktop();
				try {
					d.browse(new URI("https://github.com/wimjongman/tips"));
				} catch (IOException | URISyntaxException e) {
					e.printStackTrace();
				}
			}
		});

		ArrayList<TipAction> actions = new ArrayList<>();
		actions.add(new TipAction("Show in Github", "Opens a browser.", runner, null));
		return actions;
	}

	@Override
	public Date getCreationDate() {
		return DateUtil.getDateFromYYMMDD("09/01/2018");
	}

	@Override
	public String getSubject() {
		return "On GitHub";
	}

	@Override
	public String getHTML() {
		return """
				<h2>Incubating on GitHub</h2>
				We are incubating this project on Github and we could use your help.
				Press the <b>More...</b> button to open the GitHub repository.<br><br>
				We are looking forward to your pull requests.<br>""";
	}

	private TipImage fImage;

	@Override
	public TipImage getImage() {
		if (fImage == null) {
			try {
				fImage = new TipImage(new URL("https://assets-cdn.github.com/images/modules/logos_page/Octocat.png"))
						.setAspectRatio(800, 665, true);
			} catch (Exception e) {
//				getProvider().getManager().log(LogUtil.error(getClass(), e));
			}
		}
		return fImage;
	}
}