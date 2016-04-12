/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.history;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.team.internal.ui.PropertyChangeHandler;
import org.eclipse.ui.part.Page;

/**
 * Abstract HistoryPage class that keeps track of the history page site.
 * <p>
 * Clients may subclass this class.
 * @see IHistoryPage
 * @since 3.2
 */
public abstract class HistoryPage extends Page implements IHistoryPage, IAdaptable {

	private IHistoryPageSite site;
	private Object input;
	private IHistoryView historyView;
	private PropertyChangeHandler fChangeHandler;

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.history.IHistoryPage#setSite(org.eclipse.team.ui.history.IHistoryPageSite)
	 */
	@Override
	public void setSite(IHistoryPageSite site) {
		this.site = site;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.history.IHistoryPage#getHistoryPageSite()
	 */
	@Override
	public IHistoryPageSite getHistoryPageSite() {
		return site;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.history.IHistoryPage#getInput()
	 */
	@Override
	public Object getInput() {
		return input;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.history.IHistoryPage#setInput(java.lang.Object, boolean)
	 */
	@Override
	public boolean setInput(Object object) {
		this.input = object;
		return inputSet();
	}

	/**
	 * Called by HistoryPage after {@link #setInput(Object)}. Clients can
	 * gain access to the input by using {@link #getInput()}.
	 *
	 * @return <code>true</code> if the page was able to display the contents, <code>false</code> otherwise
	 */
	public abstract boolean inputSet();


	public void setHistoryView(IHistoryView historyView) {
		this.historyView = historyView;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.history.IHistoryPage#getHistoryView()
	 */
	@Override
	public IHistoryView getHistoryView() {
		if (historyView != null)
			return historyView;

		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.history.IHistoryPage#addPropertyChangeListener(org.eclipse.jface.util.IPropertyChangeListener)
	 */
	@Override
	public synchronized void addPropertyChangeListener(IPropertyChangeListener listener) {
		if (fChangeHandler == null) {
			fChangeHandler = new PropertyChangeHandler();
		}
		fChangeHandler.addPropertyChangeListener(listener);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.history.IHistoryPage#removePropertyChangeListener(org.eclipse.jface.util.IPropertyChangeListener)
	 */
	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		if (fChangeHandler != null) {
			fChangeHandler.removePropertyChangeListener(listener);
		}
	}

	/**
	 * Notify all listeners that the given property has changed.
	 *
	 * @param source the object on which a property has changed
	 * @param property identifier of the property that has changed
	 * @param oldValue the old value of the property, or <code>null</code>
	 * @param newValue the new value of the property, or <code>null</code>
	 * @since 3.3
	 */
	protected void firePropertyChange(Object source, String property, Object oldValue, Object newValue) {
		if (fChangeHandler == null) {
			return;
		}
		fChangeHandler.firePropertyChange(source, property, oldValue, newValue);
	}
}
