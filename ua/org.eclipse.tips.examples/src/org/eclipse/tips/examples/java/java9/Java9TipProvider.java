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
package org.eclipse.tips.examples.java.java9;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.core.TipImage;
import org.eclipse.tips.core.TipProvider;
import org.eclipse.tips.core.internal.LogUtil;
import org.eclipse.tips.examples.tipsframework.Navigate2Tip;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 *
 *
 */
@SuppressWarnings("restriction")
public class Java9TipProvider extends TipProvider {

	@Override
	public String getID() {
		return getClass().getName();
	}

	@Override
	public TipImage getImage() {
		Bundle bundle = FrameworkUtil.getBundle(getClass());
		try {
			return new TipImage(bundle.getEntry("icons/48/java.png")).setAspectRatio(1);
		} catch (IOException e) {
			getManager().log(LogUtil.error(getClass(), e));
		}
		return null;

	}

	@Override
	public synchronized IStatus loadNewTips(IProgressMonitor pMonitor) {
		SubMonitor subMonitor = SubMonitor.convert(pMonitor);
		subMonitor.beginTask("Loading Tips", -1);
		List<Tip> tips = new ArrayList<>();
		tips.add(new Tip1(getID()));
		tips.add(new Navigate2Tip(getID()));
		setTips(tips);
		subMonitor.done();
		return Status.OK_STATUS;
	}

	@Override
	public String getDescription() {
		return "Java and Java Dev Tools Tips";
	}

	@Override
	public void dispose() {
	}
}
