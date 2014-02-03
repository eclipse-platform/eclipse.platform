/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.standalone;
import java.io.*;
import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.configurator.UpdateURLDecoder;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.search.*;
import org.eclipse.update.search.*;

/**
 * Command to search an update site and list its features.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 3.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class SearchCommand extends ScriptedCommand {

	private URL remoteSiteURL;
	private UpdateSearchRequest searchRequest;
	private IUpdateSearchResultCollector collector;

	public SearchCommand(String fromSite) {
		try {
			//PAL foundation
			this.remoteSiteURL = new URL(UpdateURLDecoder.decode(fromSite, "UTF-8")); //$NON-NLS-1$
			UpdateSearchScope searchScope = new UpdateSearchScope();
			searchScope.addSearchSite(
				"remoteSite", //$NON-NLS-1$
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
			monitor.beginTask(Messages.Standalone_searching + remoteSiteURL.toExternalForm(), 4); 
			searchRequest.performSearch(collector, monitor);
			return true;
		} catch (CoreException ce) {
			IStatus status = ce.getStatus();
			if (status != null
				&& status.getCode() == ISite.SITE_ACCESS_EXCEPTION) {
				// Just show this but do not throw exception
				// because there may be results anyway.
				System.out.println(Messages.Standalone_connection); 
			} else {
				StandaloneUpdateApplication.exceptionLogged();
				UpdateCore.log(ce);
			}
			return false;
		} catch (OperationCanceledException ce) {
			return true;
		} finally {
			monitor.done();
		}
	}


	class UpdateSearchResultCollector implements IUpdateSearchResultCollector {
		public void accept(IFeature feature) {
			System.out.println(
				"\"" //$NON-NLS-1$
					+ feature.getLabel()
					+ "\" " //$NON-NLS-1$
					+ feature.getVersionedIdentifier().getIdentifier()
					+ " " //$NON-NLS-1$
					+ feature.getVersionedIdentifier().getVersion());
		}
	}
}
