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
 *     Nikifor Fedorov (ArSysOp) - externalize tips text
 *******************************************************************************/
package org.eclipse.tips.ide.internal.provider;

import java.util.Date;
import java.util.Optional;

import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.ide.internal.Messages;

public class Tip3_StartingTips extends Tip implements IHtmlTip {

	public Tip3_StartingTips(String providerId) {
		super(providerId);
	}

	@Override
	public Date getCreationDate() {
		return TipsTipProvider.getDateFromYYMMDD(9, 1, 2019);
	}

	@Override
	public String getSubject() {
		return Messages.Tip3_StartingTips_subject;
	}

	@Override
	public String getHTML() {
		return new TipHtml(Messages.Tip3_StartingTips_text_header, Messages.Tip3_StartingTips_text_body,
				Messages.Tip3_StartingTips_text_footer).get();
	}

	private TipImage fImage;

	@Override
	public TipImage getImage() {
		if (fImage == null) {
			Optional<TipImage> tipImage = TipsTipProvider.getTipImage("images/tips/starttip2.png"); //$NON-NLS-1$
			fImage = tipImage.map(i -> i.setAspectRatio(968, 741, true)).orElse(null);
		}
		return fImage;
	}
}