/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.wizards;

/**
 * Stores URL history for importing project sets window.
 * 
 */
public class PsfUrlStore extends PsfStore {

	private static final String URLS = "urls"; //$NON-NLS-1$
	private static final String PREVIOUS = "previous_url"; //$NON-NLS-1$

	private static PsfUrlStore instance;

	public static PsfUrlStore getInstance() {
		if (instance == null) {
			instance = new PsfUrlStore();
		}
		return instance;
	}

	private PsfUrlStore() {
		// Singleton
	}

	@Override
	protected String getPreviousTag() {
		return PREVIOUS;
	}

	@Override
	protected String getListTag() {
		return URLS;
	}

	@Override
	public String getSuggestedDefault() {
		return getPrevious();
	}

}
