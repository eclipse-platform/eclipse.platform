/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.history;

import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.ui.history.IHistoryPageSource;
import org.eclipse.ui.*;

/**
 * Fake part to use in page book for the History view
 */
public class HistoryPageSourceWorkbenchPart implements IWorkbenchPart {

	private Object object;
	private IHistoryPageSource source;
	private IWorkbenchPartSite site;

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof HistoryPageSourceWorkbenchPart) &&
		    source.equals(((HistoryPageSourceWorkbenchPart)obj).getSource());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
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

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#addPropertyListener(org.eclipse.ui.IPropertyListener)
	 */
	@Override
	public void addPropertyListener(IPropertyListener listener) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#getSite()
	 */
	@Override
	public IWorkbenchPartSite getSite() {
		return site;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#getTitle()
	 */
	@Override
	public String getTitle() {
		return ""; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#getTitleImage()
	 */
	@Override
	public Image getTitleImage() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#getTitleToolTip()
	 */
	@Override
	public String getTitleToolTip() {
		return ""; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#removePropertyListener(org.eclipse.ui.IPropertyListener)
	 */
	@Override
	public void removePropertyListener(IPropertyListener listener) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	public IHistoryPageSource getSource() {
		return source;
	}

	public Object getObject() {
		return object;
	}
}
