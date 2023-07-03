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
package org.eclipse.tips.core.internal;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.tips.core.IHtmlTip;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Special generic tip that tells the user that there are no more tips.
 *
 */
public class FinalTip extends Tip implements IHtmlTip {

	private static final String EH1 = "</h1>"; //$NON-NLS-1$
	private static final String H1 = "<h1>"; //$NON-NLS-1$

	/**
	 * Constructor.
	 */
	public FinalTip(String providerId) {
		super(providerId);
	}

	@Override
	public Date getCreationDate() {
		Calendar instance = Calendar.getInstance();
		return instance.getTime();
	}

	@Override
	public String getSubject() {
		return Messages.FinalTip_2;
	}

	@Override
	public String getHTML() {
		return H1 + Messages.FinalTip_3 + EH1 //
				+ Messages.FinalTip_4;
	}

	@Override
	public TipImage getImage() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		try {
			return new TipImage(bundle.getEntry("images/nomoretips.png")).setAspectRatio(417, 640, false); //$NON-NLS-1$
		} catch (IOException e) {
//			getManager().log(LogUtil.error(getClass(), e));
		}
		return null;
	}
}