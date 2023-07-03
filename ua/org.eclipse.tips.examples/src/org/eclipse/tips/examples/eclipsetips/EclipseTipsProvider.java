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
package org.eclipse.tips.examples.eclipsetips;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.core.internal.LogUtil;
import org.eclipse.tips.examples.DateUtil;
import org.eclipse.tips.examples.browserfunction.BrowserFunctionTip;
import org.eclipse.tips.examples.tips.MediaWikiTip;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

@SuppressWarnings("restriction")
public class EclipseTipsProvider extends org.eclipse.tips.core.TipProvider {

	public int fCurrentTip;

	private Tip createTip1() {
		return new org.eclipse.tips.examples.tips.TwitterTip(getID(),
				"https://twitter.com/EclipseJavaIDE/status/919915440041840641", DateUtil.getDateFromYYMMDD("08/12/017"),
				"Twitter Tip");
	}

	private Tip createTip2() {
		return new org.eclipse.tips.examples.tips.MediaWikiTip(getID(),
				"https://wiki.eclipse.org/Tip_of_the_Day/Eclipse_Tips/Show_In_System_Explorer",
				DateUtil.getDateFromYYMMDD("08/01/2017"), "Show in Systems Explorer");
	}

	private MediaWikiTip createTip3() {
		return new org.eclipse.tips.examples.tips.MediaWikiTip(getID(),
				"https://wiki.eclipse.org/Tip_of_the_Day/Eclipse_Tips/Now_where_was_I",
				DateUtil.getDateFromYYMMDD("08/01/2017"), "Where was I?");
	}

	private Tip createTip4() {
		return new org.eclipse.tips.examples.tips.MediaWikiTip(getID(),
				"https://twitter.com/EclipseJavaIDE/status/949238007051235328",
				DateUtil.getDateFromYYMMDD("08/01/2017"), "Extract class");
	}

	private MediaWikiTip createTip5() {
		return new org.eclipse.tips.examples.tips.MediaWikiTip(getID(),
				"https://twitter.com/EclipseJavaIDE/status/919915440041840641",
				DateUtil.getDateFromYYMMDD("08/01/2017"), "Junit Jupiter Test");
	}

	@Override
	public String getDescription() {
		return "General Eclipse IDE Tips";
	}

	@Override
	public String getID() {
		return getClass().getName();
	}

	private TipImage fImage48;

	@Override
	public TipImage getImage() {
		if (fImage48 == null) {
			Bundle bundle = FrameworkUtil.getBundle(getClass());
			try {
				fImage48 = new TipImage(bundle.getEntry("icons/48/eclipse.png")).setAspectRatio(1);
			} catch (IOException e) {
				getManager().log(LogUtil.error(getClass(), e));
			}
		}
		return fImage48;
	}

	@Override
	public synchronized IStatus loadNewTips(IProgressMonitor pMonitor) {
		SubMonitor subMonitor = SubMonitor.convert(pMonitor);
		subMonitor.beginTask("Loading Tips", -1);
		List<Tip> tips = new ArrayList<>();
		tips.add(new Tip1(getID()));
		tips.add(new Tip2(getID()));
		tips.add(new Tip3(getID()));
		tips.add(createTip1());
		tips.add(createTip2());
		tips.add(createTip3());
		tips.add(createTip4());
		tips.add(createTip5());
		tips.add(new BrowserFunctionTip(getID()));
		setTips(tips);
		subMonitor.done();
		return Status.OK_STATUS;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}
}