/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.intro.impl.presentations;

import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.intro.impl.IntroPlugin;
import org.eclipse.ui.internal.intro.impl.model.AbstractIntroPartImplementation;
import org.eclipse.ui.internal.intro.impl.model.IntroModelRoot;
import org.eclipse.ui.internal.intro.impl.util.IntroModelSerializer;
import org.eclipse.ui.intro.config.IIntroContentProvider;

/**
 * This is an Text based implementation of an Intro Part. It simply walks the
 * model and prints the content of pages. It is used for debugging.
 */
public class TextIntroPartImplementation extends
		AbstractIntroPartImplementation {


	@Override
	public void doStandbyStateChanged(boolean standby,
			boolean isStandbyPartNeeded) {
		// no-op
	}

	@Override
	public void createPartControl(Composite container) {
		Text text = new Text(container, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		IntroModelRoot model = IntroPlugin.getDefault().getIntroModelRoot();
		IntroModelSerializer serializer = new IntroModelSerializer(model);
		text.setText(serializer.toString());
		addToolBarActions();
	}

	@Override
	protected void updateNavigationActionsState() {
		// no-op
	}

	@Override
	public void setFocus() {
		// no-op
	}

	@Override
	public boolean navigateBackward() {
		return false;
	}

	@Override
	public boolean navigateForward() {
		return false;
	}

	@Override
	protected void handleRegistryChanged(IRegistryChangeEvent event) {
		// no-op
	}

	@Override
	public boolean navigateHome() {
		return false;
	}

	public void reflow(IIntroContentProvider provider, boolean incremental) {
		// no-op
	}
}
