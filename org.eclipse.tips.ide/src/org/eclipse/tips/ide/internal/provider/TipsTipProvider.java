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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.core.internal.LogUtil;
import org.eclipse.tips.ide.internal.TipsPreferences;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

@SuppressWarnings("restriction")
public class TipsTipProvider extends org.eclipse.tips.core.TipProvider {

	private TipImage fImage48;

	@Override
	public TipImage getImage() {
		if (fImage48 == null) {
			fImage48 = getTipImage("icons/tips.png").map(i -> i.setAspectRatio(1)).orElse(null); //$NON-NLS-1$
		}
		return fImage48;
	}

	@Override
	public synchronized IStatus loadNewTips(IProgressMonitor pMonitor) {
		SubMonitor subMonitor = SubMonitor.convert(pMonitor);
		subMonitor.beginTask("Loading Tips", -1);
		List<Tip> tips = new ArrayList<>();
		tips.add(new Tip1_Welcome(getID()));
		tips.add(new Tip2_StartingTips(getID()));
		tips.add(new Tip3_StartingTips(getID()));
		tips.add(new Tip6_ActionsTip(getID()));
		if (Platform.getBundle("org.eclipse.pde.ui") != null) { //$NON-NLS-1$
			tips.add(new Tip7_Extend(getID()));
		}
		setTips(tips);
		subMonitor.done();
		return Status.OK_STATUS;
	}

	@Override
	public String getDescription() {
		return "Tips about Tips"; //$NON-NLS-1$
	}

	@Override
	public String getID() {
		return getClass().getName();
	}

	@Override
	public void dispose() {
	}

	/**
	 * Convenience method that creates a date from a day, month year int values.
	 */
	static Date getDateFromYYMMDD(int day, int month, int year) {
		return new GregorianCalendar(year, month + 1, day).getTime();
	}

	static Optional<TipImage> getTipImage(String path) {
		Bundle bundle = FrameworkUtil.getBundle(TipsTipProvider.class);
		try {
			return Optional.of(new TipImage(bundle.getEntry(path)));
		} catch (IOException e) {
			TipsPreferences.log(LogUtil.info(TipsTipProvider.class, e));
		}
		return Optional.empty();
	}

}