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
package org.eclipse.update.internal.standalone;
import java.io.*;
import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.search.*;
import org.eclipse.update.internal.api.search.*;

public class SearchCommand extends ScriptedCommand {

	private URL remoteSiteURL;
	private UpdateSearchRequest searchRequest;
	private IUpdateSearchResultCollector collector;

	public SearchCommand(String fromSite) {
		try {
			this.remoteSiteURL = new URL(URLDecoder.decode(fromSite, "UTF-8"));
			UpdateSearchScope searchScope = new UpdateSearchScope();
			searchScope.addSearchSite(
				"remoteSite",
				remoteSiteURL,
				new String[0]);
			searchRequest =
				new UpdateSearchRequest(new SiteSearchCategory(), searchScope);
			collector = new UpdateSearchResultCollector();
		} catch (MalformedURLException e) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(e);
		} catch (UnsupportedEncodingException e) {
		}
	}

	/**
	 */
	public boolean run(IProgressMonitor monitor) {
		try {
			System.out.println("Searching on " + remoteSiteURL.toString());
			searchRequest.performSearch(collector, monitor);
			System.out.println("Done.");
			return true;
		} catch (CoreException ce) {
			IStatus status = ce.getStatus();
			if (status != null
				&& status.getCode() == ISite.SITE_ACCESS_EXCEPTION) {
				// Just show this but do not throw exception
				// because there may be results anyway.
				System.out.println("Connection Error");
			} else {
				StandaloneUpdateApplication.exceptionLogged();
				UpdateCore.log(ce);
			}
			return false;
		}
	}


	class UpdateSearchResultCollector implements IUpdateSearchResultCollector {
		public void accept(IFeature feature) {
			System.out.println(
				"\""
					+ feature.getLabel()
					+ "\" "
					+ feature.getVersionedIdentifier().getIdentifier()
					+ " "
					+ feature.getVersionedIdentifier().getVersion());
		}
	}
}
