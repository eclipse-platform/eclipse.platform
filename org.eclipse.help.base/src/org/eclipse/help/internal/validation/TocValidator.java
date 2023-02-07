package org.eclipse.help.internal.validation;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.help.IHelpResource;
import org.eclipse.help.ILink;
import org.eclipse.help.IToc;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.protocols.HelpURLConnection;
import org.eclipse.help.internal.toc.TocContribution;
import org.eclipse.help.internal.toc.TocFile;
import org.eclipse.help.internal.toc.TocFileParser;
import org.xml.sax.SAXException;

/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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

public class TocValidator {

	private static final boolean DEBUG = false;

	private HashMap<String, Object> processedTocs;
	private TocFileParser parser;

	public static class BrokenLink {
		private String tocID;
		private String href;
		private BrokenLink(String tocID, String href) {
			this.tocID = tocID;
			this.href = href;
		}
		public String getTocID() {
			return tocID; }
		public String getHref() {
			return href; }
	}

	public static abstract class Filter {
		abstract public boolean isIncluded(String href);
	}

	public static class PassThroughFilter extends Filter {
		@Override
		public boolean isIncluded(String href) {
			return true;
		}
	}

	/**
	 * Checks the validity of all <code>href</code> attributes on <code>topic</code> elements in the toc and the
	 * <code>topic</code> attribute on the <code>toc</code> element if there is one. Also checks validity of any
	 * nested tocs.
	 * @param hrefs gives the list of paths to toc files to validate including the plug-in id
	 * (i.e. "/&lt;plug-in id&gt;/&lt;path&gt;/&lt;file&gt;")
	 * @return An ArrayList of BrokenLink objects in the toc. If no broken links are found, an empty ArrayList
	 * is returned.
	 * @throws IOException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	public static ArrayList<BrokenLink> validate(String[] hrefs) throws IOException, SAXException, ParserConfigurationException{
		return filteredValidate(hrefs, new PassThroughFilter());
	}

	public static ArrayList<BrokenLink> filteredValidate (String[] hrefs, Filter filter) throws IOException, SAXException, ParserConfigurationException{
		TocValidator v = new TocValidator();
		ArrayList<BrokenLink> result = new ArrayList<>();
		for (String href : hrefs)
			v.processToc(href, null, result, filter);
		return result;
	}

	private TocValidator() {
		processedTocs = new HashMap<>();
		parser = new TocFileParser();
	}

	/* Checks validity of all links in a given toc. If all links are valid, an empty ArrayList is returned.
	 * Otherwise an ArrayList of BrokenLink objects is returned.
	 */
	private void processToc(String href, String plugin, ArrayList<BrokenLink> result, Filter filter)
	               throws IOException, SAXException, ParserConfigurationException {
		String path;
		if (href.startsWith("/")) { //$NON-NLS-1$
			href = href.substring(1);
			int index = href.indexOf('/');
			if (index == -1)
				throw new IOException("Invalid parameters supplied to the validate method."); //$NON-NLS-1$
			plugin = href.substring(0, index);
			path = href.substring(index+1);
		} else {
			path = href;
		}
		if (plugin == null)
			throw new IOException("Invalid parameters supplied to the validate method."); //$NON-NLS-1$
		String key = "/" + plugin + "/" + path; //$NON-NLS-1$ //$NON-NLS-2$
		if (processedTocs.get(key) != null) {
			if (DEBUG)
				System.out.println("Skipping toc because it has already been validated: " + key); //$NON-NLS-1$
			return;
		}
		if (DEBUG)
			System.out.println("Starting toc: " + key); //$NON-NLS-1$
		processedTocs.put(key, new Object());
		TocContribution contribution = parser.parse(new TocFile(plugin,path, true, "en", null, null)); //$NON-NLS-1$
		process(contribution.getToc(), plugin, path, result, filter);
	}

	/* Checks validity of all links in the given IUAElement and recursively calls itself to check all children.
	 * If there are any links to other tocs, calls the processToc method to validate them. If any broken links
	 * are found, an appropriate BrokenLink object will be added to the result ArrayList.
	 */
	private void process(IUAElement element, String plugin, String path, ArrayList<BrokenLink> result, Filter filter) throws SAXException, ParserConfigurationException {
		String href;
		if (element instanceof ILink) {
			href = ((ILink)element).getToc();
			try {
				processToc(href, plugin, result, filter);
			} catch (IOException e) {
				result.add(new BrokenLink("/" + plugin + "/" + path, href)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		else if (element instanceof IHelpResource) {
			if (element instanceof IToc)
				href = ((IToc)element).getTopic(null).getHref();
			else
				href = ((IHelpResource)element).getHref();
			if (href != null && filter.isIncluded(href))
				if (!checkLink(href, plugin)) {
					result.add(new BrokenLink("/" + plugin + "/" + path, href)); //$NON-NLS-1$ //$NON-NLS-2$
				}
		}
		IUAElement [] children = element.getChildren();
		for (int i = 0; i < children.length; i++)
			process(children[i], plugin, path, result, filter);
	}

	/* Checks validity of a given link from a toc in a given plug-in.
	 * returns true if the link is valid.
	 */
	private boolean checkLink(String href, String plugin) {
		if (href.startsWith("http")) { //$NON-NLS-1$
			if (DEBUG)
				System.out.println("    Skipping href: " + href); //$NON-NLS-1$
			return true;
		}
		if (DEBUG)
			System.out.print("    Checking href: " + href + "..."); //$NON-NLS-1$ //$NON-NLS-2$
		boolean result = true;
		InputStream i = null;
		try {
			HelpURLConnection c = new HelpURLConnection(createURL(href, plugin));
			if ((i = c.getInputStream()) == null)
				result = false;
		} catch (Exception e) {
			result = false;
		}
		if (i != null) {
			try { i.close(); } catch(Exception e) { }
			i = null;
		}
		if (DEBUG)
			System.out.println(result?"pass":"fail"); //$NON-NLS-1$ //$NON-NLS-2$
		return result;
	}

	// Builds a URL for a given plug-in/href to create a HelpURLConnection
	private URL createURL(String href, String plugin) throws MalformedURLException {
		StringBuilder url = new StringBuilder("file:/"); //$NON-NLS-1$
		url.append(plugin);
		url.append("/"); //$NON-NLS-1$
		url.append(href);
		return new URL(url.toString());
	}
}
