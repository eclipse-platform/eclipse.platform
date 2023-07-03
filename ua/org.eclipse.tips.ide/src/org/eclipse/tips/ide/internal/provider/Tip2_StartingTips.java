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

import java.util.Date;
import java.util.Optional;

import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;

public class Tip2_StartingTips extends Tip implements IHtmlTip {

	public Tip2_StartingTips(String providerId) {
		super(providerId);
	}

	@Override
	public Date getCreationDate() {
		return TipsTipProvider.getDateFromYYMMDD(9, 1, 2019);
	}

	@Override
	public String getSubject() {
		return "Opening the Tips Dialog";
	}

	@Override
	public String getHTML() {
		return "<h2>Opening the Tips Dialog</h2>The tips are shown automatically at startup if there are tips available."
				+ " In case the tips are not loaded at startup you can activate the tips manually from the Help menu."
				+ "<br><br>" + "Press <b><i>Next Tip</i></b> to learn more.<br><br>";
	}

	private TipImage fImage;

	@Override
	public TipImage getImage() {
		if (fImage == null) {
			Optional<TipImage> tipImage = TipsTipProvider.getTipImage("images/tips/starttip.gif"); //$NON-NLS-1$
			fImage = tipImage.map(i -> i.setAspectRatio(780, 430, true)).orElse(null);
		}
		return fImage;
	}

}