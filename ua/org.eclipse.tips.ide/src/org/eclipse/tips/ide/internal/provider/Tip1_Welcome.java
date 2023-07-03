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

public class Tip1_Welcome extends Tip implements IHtmlTip {

	private TipImage fImage;

	public Tip1_Welcome(String providerId) {
		super(providerId);
	}

	@Override
	public Date getCreationDate() {
		return TipsTipProvider.getDateFromYYMMDD(9, 1, 2019);
	}

	@Override
	public String getSubject() {
		return "Welcome to the tips framework";
	}

	@Override
	public String getHTML() {
		return "<h2>Welcome to the Tips Framework</h2>It can show tips from various tip providers which are listed in the bottom. This provider has tips about tips which will show you how to navigate this UI."
				+ "Tips appear here in various forms. They can come from Twitter, a Wiki, a Website, a file or even from Java and inline HTML, like this one."
				+ "<br><br>" + "Press <b><i>Next Tip</i></b> to see how to start tips manually.<br><br>";
	}

	@Override
	public TipImage getImage() {
		if (fImage == null) {
			Optional<TipImage> tipImage = TipsTipProvider.getTipImage("images/tips/welcome.png"); //$NON-NLS-1$
			fImage = tipImage.map(i -> i.setAspectRatio(560, 480, true)).orElse(null);
		}
		return fImage;
	}
}