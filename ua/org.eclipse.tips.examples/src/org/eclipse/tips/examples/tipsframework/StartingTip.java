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
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class StartingTip extends Tip implements IHtmlTip {

	public StartingTip(String providerId) {
		super(providerId);
	}

	@Override
	public Date getCreationDate() {
		return DateUtil.getDateFromYYMMDD("09/01/2018");
	}

	@Override
	public String getSubject() {
		return "Opening the Tips Dialog";
	}

	@Override
	public String getHTML() {
		return """
				<h2>Opening the Tips Dialog</h2>
				The tips are started automatically at startup but you can switch this off.
				In case the tips are not loaded at startup you can active the tips manually
				from the Help menu.<br><br>
				Press <b><i>Next Tip</i></b> to see how to navigate Tips.<br><br>""";
	}

	private TipImage fImage;

	@Override
	public TipImage getImage() {
		if (fImage == null) {
			try {
				Bundle bundle = FrameworkUtil.getBundle(getClass());
				fImage = new TipImage(bundle.getEntry("images/tips/starttip.gif")).setAspectRatio(780, 430, true);
			} catch (Exception e) {
//				getProvider().getManager().log(LogUtil.info(getClass(), e));
			}
		}
		return fImage;
	}

}