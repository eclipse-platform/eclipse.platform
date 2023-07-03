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
package org.eclipse.tips.examples.swttip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.core.TipProvider;
import org.eclipse.tips.core.internal.LogUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

@SuppressWarnings("restriction")
public class SwtTipsProvider extends TipProvider {

	private TipImage fImage;
	private int fCounter;
	private boolean fFetching;

	@Override
	public String getDescription() {
		return "Never ending list of SWT Tips";
	}

	@Override
	public String getID() {
		return getClass().getName();
	}

	@Override
	public synchronized List<Tip> getTips() {
		List<Tip> tips = super.getTips();
		if (tips.size() <= 1) {
			Job job = new Job("Load tips for " + getDescription()) {

				@Override
				protected IStatus run(IProgressMonitor pMonitor) {
					return loadNewTips(pMonitor);
				}
			};
			job.setUser(true);
			job.schedule();
		}
		return tips;
	}

	@Override
	public TipImage getImage() {
		if (fImage == null) {
			Bundle bundle = FrameworkUtil.getBundle(getClass());
			try {
				fImage = new TipImage(bundle.getEntry("icons/48/swt.png")).setAspectRatio(1);
			} catch (IOException e) {
				getManager().log(LogUtil.error(getClass(), e));
			}
		}
		return fImage;
	}

	@Override
	public synchronized IStatus loadNewTips(IProgressMonitor pMonitor) {
		SubMonitor subMonitor = SubMonitor.convert(pMonitor);
		if (fFetching) {
			return Status.CANCEL_STATUS;
		}
		try {
			subMonitor.beginTask("Loading Tips for " + getDescription(), -1);
			List<Tip> tips = new ArrayList<>();
			tips.add(new SwtTipImpl(getID(), System.currentTimeMillis() + 100));
			tips.add(new SwtTipImpl(getID(), System.currentTimeMillis() + 200));
			tips.add(new SwtTipImpl(getID(), System.currentTimeMillis() + 300));
			tips.add(new SwtTipImpl(getID(), System.currentTimeMillis() + 400));
			tips.add(new SwtTipImpl(getID(), System.currentTimeMillis() + 500));
			addTips(tips);
			return Status.OK_STATUS;
		} finally {
			fFetching = false;
			subMonitor.done();
		}
	}

	@Override
	public void dispose() {
	}

	public int getCounter() {
		return ++fCounter;
	}
}