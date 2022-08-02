/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.history;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.ui.history.IHistoryPageSource;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

/**
 * Fake part to use in page book for the History view
 */
public class HistoryPageSourceWorkbenchPart implements IWorkbenchPart {
	private Object object;
	private IHistoryPageSource source;
	private IWorkbenchPartSite site;

	@Override
	public boolean equals(Object obj) {
		return (obj instanceof HistoryPageSourceWorkbenchPart) &&
			source.equals(((HistoryPageSourceWorkbenchPart)obj).getSource());
	}

	@Override
	public int hashCode() {
		return source.hashCode();
	}

	/**
	 * Constructs a part that binds the object and its history page source to the given site.
	 *
	 * @param object
	 *            the object the input whose history is to be displayed
	 * @param source
	 *            the history page source
	 * @param site
	 *            the part site
	 */
	public HistoryPageSourceWorkbenchPart(Object object, IHistoryPageSource source, IWorkbenchPartSite site) {
		this.object= object;
		this.source = source;
		this.site = site;
	}

	@Override
	public void addPropertyListener(IPropertyListener listener) {
	}

	@Override
	public void createPartControl(Composite parent) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public IWorkbenchPartSite getSite() {
		return site;
	}

	@Override
	public String getTitle() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public Image getTitleImage() {
		return null;
	}

	@Override
	public String getTitleToolTip() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public void removePropertyListener(IPropertyListener listener) {
	}

	@Override
	public void setFocus() {
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	public IHistoryPageSource getSource() {
		return source;
	}

	public Object getObject() {
		return object;
	}
}
