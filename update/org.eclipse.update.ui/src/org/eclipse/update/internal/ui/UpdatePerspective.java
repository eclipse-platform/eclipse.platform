package org.eclipse.update.internal.ui;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.swt.SWT;
import org.eclipse.ui.*;
import org.eclipse.jface.dialogs.MessageDialog;

public class UpdatePerspective implements IPerspectiveFactory {
	private static final String PREFIX = "org.eclipse.update.ui.";
	public static final String PERSPECTIVE_ID = PREFIX + "UpdatePerspective";
	public static final String ID_UPDATES = PREFIX + "UpdatesView";
	public static final String ID_CONFIGURATION = PREFIX + "ConfigurationView";
	public static final String ID_DETAILS = PREFIX + "DetailsView";
	public static final String ID_BROWSER = PREFIX + "WebBrowser";
	public static final String ID_SEARCH_RESULTS = PREFIX + "SearchResultsView";

	public UpdatePerspective() {
		super();
	}

	public void createInitialLayout(IPageLayout factory) {
		factory.setEditorAreaVisible(false);
		factory.addView(
			ID_CONFIGURATION,
			IPageLayout.LEFT,
			0.33f,
			factory.getEditorArea());
		factory.addView(ID_UPDATES, IPageLayout.BOTTOM, 0.5f, ID_CONFIGURATION);
		IFolderLayout detailsFolder = factory.createFolder("details", IPageLayout.RIGHT, 0.67f, factory.getEditorArea());
		detailsFolder.addView(ID_DETAILS);
		if (SWT.getPlatform().equals("win32"))
			detailsFolder.addPlaceholder(ID_BROWSER);
		factory.addPlaceholder(ID_SEARCH_RESULTS, IPageLayout.BOTTOM, 0.67f, ID_DETAILS);
	}
}