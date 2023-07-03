/*******************************************************************************
 * Copyright (c) 2005, 2015 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Intel Corporation - initial API and implementation
 *     IBM Corporation - 122967 [Help] Remote help system
 *                       163558 Dynamic content support for all UA
 *******************************************************************************/
package org.eclipse.help.internal.webapp.data;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.IIndex;
import org.eclipse.help.IIndexEntry;
import org.eclipse.help.ITopic;
import org.eclipse.help.internal.HelpPlugin;

/**
 * Helper class for Index view initialization
 */
public class IndexData extends ActivitiesData {
	private IIndex index;

	// images directory
	private String imagesDirectory;

	// plus/minus image file name
	private String plusMinusImage;

	// name of expand/collapse class for IMG, UL tags
	private String expandedCollapsed;

	// use or not expand/collapse feature
	private boolean usePlusMinus;

	// expand all by default flag
	private boolean expandAll;

	// global writer for private generate...() methods
	private Writer out;

	/**
	 * Constructs the data for the index page.
	 * @param context
	 * @param request
	 */
	public IndexData(ServletContext context, HttpServletRequest request,
			HttpServletResponse response) {
		super(context, request, response);

		imagesDirectory = preferences.getImagesDirectory();
		usePlusMinus = preferences.isIndexPlusMinus();
		expandAll = preferences.isIndexExpandAll();
		plusMinusImage = expandAll ? "/minus.svg" : "/plus.svg"; //$NON-NLS-1$ //$NON-NLS-2$
		expandedCollapsed = expandAll ? "expanded" : "collapsed"; //$NON-NLS-1$ //$NON-NLS-2$
		index = HelpPlugin.getIndexManager().getIndex(getLocale());
	}

	/**
	 * Generates values for array of ids of list items
	 * avaliable to be navigated through typein feature.
	 *
	 * Currently only first level items can be navigated.
	 *
	 * @param out
	 * @throws IOException
	 */
	public void generateIds(Writer out) throws IOException {
		boolean first = true;
		IIndexEntry[] entries = index.getEntries();
		for (IIndexEntry entry : entries) {
			if (entry != null && entry.getKeyword() != null && entry.getKeyword().length() > 0) {
				if (first) {
					first = false;
				} else {
					out.write(",\n"); //$NON-NLS-1$
				}
				out.write("\""); //$NON-NLS-1$
				out.write(UrlUtil.JavaScriptEncode(entry.getKeyword()));
				out.write("\""); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Generates the HTML code (a list) for the index.
	 *
	 * @param out
	 * @throws IOException
	 */
	public void generateIndex(Writer out) throws IOException {
		this.out = out;
		IIndexEntry[] entries = index.getEntries();
		for (IIndexEntry entrie : entries) {
			if (EnabledTopicUtils.isEnabled(entrie)) {
				generateEntry(entrie, 0);
			}
		}
	}

	/**
	 * Generates the HTML code for an index entry.
	 *
	 * @param entry
	 * @param level
	 * @throws IOException
	 */
	/*
	 * For advanced UI:
	 *   <li>[ plus_image ]<a ...>...</a>
	 *   [<ul>list of topics</ul>]
	 *   [<ul>nested entries</ul>]
	 *   </li>
	 */
	private void generateEntry(IIndexEntry entry, int level) throws IOException {
		if (entry.getKeyword() != null && entry.getKeyword().length() > 0) {
			ITopic[] topics = EnabledTopicUtils.getEnabled(entry.getTopics());
			IIndexEntry[] subentries = EnabledTopicUtils.getEnabled(entry.getSubentries());
			boolean multipleTopics = topics.length > 1;
			boolean singleTopic = topics.length == 1;

			out.write("<li>"); //$NON-NLS-1$
			if (usePlusMinus) generatePlusImage(multipleTopics);
			generateAnchor(singleTopic, entry, level);
			if (multipleTopics || subentries.length > 0) {
				if (multipleTopics) generateTopicList(entry);
				generateSubentries(entry, level + 1);
			}
			out.write("</li>\n"); //$NON-NLS-1$
		}
	}

	/**
	 * Generates the HTML code for the plus/minus image.
	 *
	 * @param multipleTopics
	 * @throws IOException
	 */
	/*
	 * <img scr="images/plus.svg" class={ "collapsed" | "expanded" | "h" } alt="...">
	 */
	private void generatePlusImage(boolean multipleTopics) throws IOException {
		out.write("<img src=\""); //$NON-NLS-1$
		out.write(imagesDirectory);
		out.write(plusMinusImage);
		out.write("\" class=\""); //$NON-NLS-1$
		if (multipleTopics) {
			out.write(expandedCollapsed);
		} else {
			out.write("h"); //$NON-NLS-1$
		}
		out.write("\" alt=\""); //$NON-NLS-1$
		if (multipleTopics) {
			if (expandAll) {
				out.write(ServletResources.getString("collapseTopicTitles", request)); //$NON-NLS-1$
			} else {
				out.write(ServletResources.getString("expandTopicTitles", request)); //$NON-NLS-1$
			}
		}
		out.write("\">"); //$NON-NLS-1$
	}

	/**
	 * Generates the HTML code for an index entry anchor tag.
	 *
	 * @param singleTopic
	 * @param entry
	 * @param level
	 * @throws IOException
	 */
	/*
	 * For advanced UI:
	 *   <a [ id="..." ] [ class="nolink" ] href="...">...</a>
	 */
	private void generateAnchor(boolean singleTopic, IIndexEntry entry, int level) throws IOException {
		out.write("<a "); //$NON-NLS-1$
		if (level == 0 ) {
			out.write("id=\""); //$NON-NLS-1$
			out.write(entry.getKeyword());
			out.write("\" "); //$NON-NLS-1$
		}
		if (singleTopic) {
			out.write("href=\""); //$NON-NLS-1$
			out.write(UrlUtil.getHelpURL((entry.getTopics()[0]).getHref()));
			out.write("\">"); //$NON-NLS-1$
		} else {
			out.write("class=\"nolink\" "); //$NON-NLS-1$
			out.write("href=\"about:blank\">"); //$NON-NLS-1$
		}
		out.write(UrlUtil.htmlEncode(entry.getKeyword()));
		out.write("</a>\n"); //$NON-NLS-1$
	}

	/**
	 * Generates the HTML code for a list of topics.
	 *
	 * @param entry
	 * @throws IOException
	 */
	/*
	 * For advanced UI:
	 *   <ul class={"collapsed" | "expanded"}>
	 *   <li><img class="h" src="images/plus.svg" alt=""><a href="..."><img src="images/topic.svg" alt="">...</a></li>
	 *   <li>...
	 *   </ul>
	 */
	private void generateTopicList(IIndexEntry entry) throws IOException {
		ITopic[] topics = entry.getTopics();

		out.write("\n<ul class=\""); //$NON-NLS-1$
		out.write(expandedCollapsed);
		out.write("\">\n"); //$NON-NLS-1$
		for (ITopic topic : topics) {
			out.write("<li>"); //$NON-NLS-1$
			if (usePlusMinus) {
				out.write("<img class=\"h\" src=\""); //$NON-NLS-1$
				out.write(imagesDirectory);
				out.write(plusMinusImage);
				out.write("\" alt=\"\">"); //$NON-NLS-1$
			}
			out.write("<a href=\""); //$NON-NLS-1$
			out.write(UrlUtil.getHelpURL(topic.getHref()));
			out.write("\"><img src=\""); //$NON-NLS-1$
			out.write(imagesDirectory);
			out.write("/topic.svg\" "); //$NON-NLS-1$
			out.write("alt=\"\">"); //$NON-NLS-1$
			out.write(UrlUtil.htmlEncode(topic.getLabel()));
			out.write("</a></li>\n"); //$NON-NLS-1$
		}
		out.write("</ul>\n"); //$NON-NLS-1$
	}

	/**
	 * Generates the HTML for nested index entries.
	 *
	 * @param entry
	 * @param level
	 * @throws IOException
	 */
	/*
	 * For advanced UI:
	 *   <ul class="expanded">
	 *   entries...
	 *   </ul>
	 */
	private void generateSubentries(IIndexEntry entry, int level) throws IOException {
		out.write("<ul class=\"expanded\">\n"); //$NON-NLS-1$
		IIndexEntry[] subentries = entry.getSubentries();
		for (IIndexEntry subentrie : subentries) {
			generateEntry(subentrie, level);
		}
		out.write("</ul>\n"); //$NON-NLS-1$
	}
}
