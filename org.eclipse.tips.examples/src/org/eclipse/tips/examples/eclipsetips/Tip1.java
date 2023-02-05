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

public class Tip1 extends Tip implements IHtmlTip {

	public Tip1(String providerId) {
		super(providerId);
	}

	@Override
	public Date getCreationDate() {
		return DateUtil.getDateFromYYMMDD("10/01/2018");
	}

	@Override
	public String getSubject() {
		return "This is SwtTipImpl";
	}

	@Override
	public String getHTML() {
		return """
				<h1>Iterate with Iterator</h1>
				Workbench editors keep a navigation history. If  you open a second editor
				while you're editing, you can press <b>Navigate > Backward (Alt+Left Arrow,
				or the Left arrow icon back arrow icon on the workbench toolbar)</b> to
				go back to the last editor. This makes working with several open editors
				a whole lot easier.<br/><br/>""";
	}

	@Override
	public TipImage getImage() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		try {
			return new TipImage(bundle.getEntry("images/eclipsetips/tip1.gif")).setAspectRatio(560.0 / 266.0);
		} catch (IOException e) {
//			getProvider().getManager().log(LogUtil.error(getClass(), e));
		}
		return null;

	}
}