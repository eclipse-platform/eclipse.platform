/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui;

import org.eclipse.swt.SWT;
import org.eclipse.ui.*;

public class UpdatePerspective implements IPerspectiveFactory {
	private static final String PREFIX = "org.eclipse.update.ui.";
	public static final String PERSPECTIVE_ID = PREFIX + "UpdatePerspective";
	public static final String ID_UPDATES = PREFIX + "UpdatesView";
	public static final String ID_CONFIGURATION = PREFIX + "ConfigurationView";
	public static final String ID_DETAILS = PREFIX + "DetailsView";
	public static final String ID_BROWSER = PREFIX + "WebBrowser";
	public static final String ID_SEARCH_RESULTS = PREFIX + "SearchResultsView";
	public static final String ID_ITEMS = PREFIX + "ItemsView";

	public UpdatePerspective() {
		super();
	}

	public void createInitialLayout(IPageLayout factory) {
		factory.setEditorAreaVisible(false);
		IFolderLayout upperFolder = factory.createFolder("upper",
			IPageLayout.LEFT,
			0.33f,
			factory.getEditorArea());
		upperFolder.addView(ID_CONFIGURATION);
		upperFolder.addView(ID_ITEMS);
		
		factory.addView(ID_UPDATES, IPageLayout.BOTTOM, 0.5f, "upper");
		IFolderLayout detailsFolder = factory.createFolder("details", IPageLayout.RIGHT, 0.67f, factory.getEditorArea());
		detailsFolder.addView(ID_DETAILS);
		if (SWT.getPlatform().equals("win32"))
			detailsFolder.addPlaceholder(ID_BROWSER);
		factory.addPlaceholder(ID_SEARCH_RESULTS, IPageLayout.BOTTOM, 0.67f, ID_DETAILS);
	}
}
