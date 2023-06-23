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
		return "Opening the Tips Dialog";
	}

	@Override
	public String getHTML() {
		return "<h2>Tips Available?</h2>In case you do not want to start tips at startup, you can still see if there are tips available. "
				+ "Check the bottom right corner to see a little lightbulb. This lightbulb only appears if there is something to read."
				+ "<br><br>" + "Press <b><i>Next Tip</i></b> to see how to navigate tips.<br><br>";
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