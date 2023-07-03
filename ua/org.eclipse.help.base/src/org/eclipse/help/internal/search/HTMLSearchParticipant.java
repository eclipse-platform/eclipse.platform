/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
package org.eclipse.help.internal.search;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.util.ProxyUtil;
import org.eclipse.help.internal.xhtml.XHTMLContentDescriber;
import org.eclipse.help.search.IHelpSearchIndex;
import org.eclipse.help.search.ISearchDocument;
import org.eclipse.help.search.SearchParticipant;


public class HTMLSearchParticipant extends SearchParticipant {

	private static final String HELP_BASE_XHTML = "org.eclipse.help.base.xhtml"; //$NON-NLS-1$
	private HTMLDocParser parser;
	private String indexPath;
	private IContentDescriber xhtmlDescriber;
	private XHTMLSearchParticipant xhtmlParticipant;

	public HTMLSearchParticipant(String indexPath) {
		parser = new HTMLDocParser();
		this.indexPath = indexPath;
	}


	@Override
	@SuppressWarnings("resource")
	public IStatus addDocument(IHelpSearchIndex index, String pluginId, String name, URL url, String id,
			ISearchDocument doc) {
		// if it's XHTML, forward it on to the proper search participant
		if (isXHTML(pluginId, url)) {
			LocalSearchManager manager = BaseHelpSystem.getLocalSearchManager();
			SearchParticipant participant  = manager.getParticipant(HELP_BASE_XHTML);
			if (participant == null) {
				participant = getXhtmlParticipant();
			}
			return participant.addDocument(index, pluginId, name, url, id, doc);
		}
		// otherwise, treat it as HTML
		else {
			try {
				try {
					try {
						parser.openDocument(url);
					} catch (IOException ioe) {
						return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.ERROR,
								"Help document " //$NON-NLS-1$
										+ name + " cannot be opened.", //$NON-NLS-1$
								null);
					}
					doc.addContents(parser.getContentReader(), parser.getContentReader());
					String title = parser.getTitle();
					doc.setTitle(title);
					doc.setSummary(parser.getSummary(title));
					if (parser.getException() != null) {
						return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.ERROR,
								"Parse error occurred while adding document " + name //$NON-NLS-1$
										+ " to search index " + indexPath + ".", //$NON-NLS-1$ //$NON-NLS-2$
								parser.getException());
					}
				} finally {
					parser.closeDocument();
				}
			} catch (IOException e) {
				return new Status(IStatus.ERROR, HelpBasePlugin.PLUGIN_ID, IStatus.ERROR,
						"IO exception occurred while adding document " + name //$NON-NLS-1$
								+ " to search index " + indexPath + ".", //$NON-NLS-1$ //$NON-NLS-2$
						e);
			}
			return Status.OK_STATUS;
		}
	}

	private SearchParticipant getXhtmlParticipant() {
		if (xhtmlParticipant == null) {
			xhtmlParticipant = new XHTMLSearchParticipant();
		}
		return xhtmlParticipant;
	}

	/**
	 * Returns whether or not the given content should be treated as XHTML.
	 *
	 * @param pluginId the plugin id containing the content
	 * @param url the URL to the content
	 * @return whether the content should be treated as XHTML
	 */
	private boolean isXHTML(String pluginId, URL url) {
		if (xhtmlDescriber == null) {
			xhtmlDescriber = new XHTMLContentDescriber();
		}
		try (InputStream in = ProxyUtil.getStream(url)) {
			return (xhtmlDescriber.describe(in, null) == IContentDescriber.VALID);
		} catch (Exception e) {
			// if anything goes wrong, treat it as not xhtml
		}

		return false;
	}

}
