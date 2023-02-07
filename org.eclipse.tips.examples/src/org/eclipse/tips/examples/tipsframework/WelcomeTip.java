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

import java.util.Date;

import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.examples.DateUtil;

public class WelcomeTip extends Tip implements IHtmlTip {

	public WelcomeTip(String providerId) {
		super(providerId);
	}

	@Override
	public Date getCreationDate() {
		return DateUtil.getDateFromYYMMDD("09/01/2018");
	}

	@Override
	public String getSubject() {
		return "Welcome to the tips framework";
	}

	@Override
	public String getHTML() {
		return """
				<h2>Welcome to the Tips Framework</h2>
				It can show tips from various tip providers. This provider has tips about tips
				which will show you how to navigate this UI. The dialog is this Tip UI.
				Tips appear here in various forms. They can come from Twitter, a Wiki, a Website,
				a file or even from Java, like this one.<br><br>
				Press <b><i>Next Tip</i></b> to see how to start tips manually.""";
	}

	@Override
	public TipImage getImage() {
		return null;
	}
}