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
package org.eclipse.tips.examples.eclipsetips;

import java.io.IOException;
import java.util.Date;

import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.examples.DateUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class Tip2 extends Tip implements IHtmlTip {


	public Tip2(String providerId) {
		super(providerId);
	}

	@Override
	public Date getCreationDate() {
		return DateUtil.getDateFromYYMMDD("10/01/2018");
	}

	@Override
	public String getSubject() {
		return "Quick Access";
	}

	@Override
	public String getHTML() {
		return """
				<h1>Quick access</h1>
				You can quickly find all manner of user interface elements with the
				<b>Quick Access</b> search bar at the top of the workbench window.
				Click in the field or use the <b>Ctrl+3</b> binding to switch focus to it.
				Matching elements include (but are not limited to) open editors, available
				perspectives, views, preferences, wizards, and commands.
				Simply start typing the name of the item you wish to invoke and we will
				attempt to find something in the Workbench that matches the provided string.""";
	}

	@Override
	public TipImage getImage() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		try {
			return new TipImage(bundle.getEntry("images/eclipsetips/tip2.png"));
		} catch (IOException e) {
//			getProvider().getManager().log(LogUtil.error(getClass(), e));
		}
		return null;

	}
}