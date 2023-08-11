/*******************************************************************************
 * Copyright (c) 2018, 2023 Remain Software and others
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
 *     Nikifor Fedorov (ArSysOp) - externalize tips text
 *******************************************************************************/
package org.eclipse.tips.ide.internal.provider;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipAction;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.ide.internal.Messages;

public class Tip7_Extend extends Tip implements IHtmlTip {

	public Tip7_Extend(String providerId) {
		super(providerId);
	}

	@Override
	public List<TipAction> getActions() {
		Runnable action = () -> Display.getDefault().asyncExec(() -> {
			if (Platform.isRunning() && Platform.getWS().startsWith("gtk")) { //$NON-NLS-1$
				boolean confirm = MessageDialog.openConfirm(null, Messages.Tip7_Extend_gtk_browser_failure_title,
						Messages.Tip7_Extend_gtk_browser_failure_message);
				if (!confirm) {
					return;
				}
			}
			try {
				Desktop.getDesktop().browse(new URI("https://wiki.eclipse.org/Tip_of_the_Day")); //$NON-NLS-1$
			} catch (IOException | URISyntaxException e) {
				e.printStackTrace();
			}
		});
		return List.of(new TipAction(Messages.Tip7_Extend_action_title, Messages.Tip7_Extend_action_description, action, null));
	}

	@Override
	public Date getCreationDate() {
		return TipsTipProvider.getDateFromYYMMDD(9, 1, 2019);
	}

	@Override
	public String getSubject() {
		return Messages.Tip7_Extend_subject;
	}

	@Override
	public String getHTML() {
		return new TipHtml(Messages.Tip7_Extend_text_header, Messages.Tip7_Extend_text_body).get();
	}

	private TipImage fImage;

	@Override
	public TipImage getImage() {
		if (fImage == null) {
			Optional<TipImage> tipImage = TipsTipProvider.getTipImage("images/tips/photon.jpg"); //$NON-NLS-1$
			fImage = tipImage.map(i -> i.setAspectRatio(720, 480, true)).orElse(null);
		}
		return fImage;
	}
}