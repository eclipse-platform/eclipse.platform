/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.webapp.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.scope.ScopeUtils;
import org.eclipse.help.internal.search.HTMLDocParser;
import org.eclipse.help.internal.xhtml.DynamicXHTMLProcessor;

/*
 * Used by the print jsp to access print-related data.
 */
public class PrintData extends RequestData {

	// default max connections for concurrent print
	private static final int defaultMaxConnections = 10;

	// default max topics allowed for one print request
	private static final int defaultMaxTopics = 500;

	// where to inject the section numbers
	private static final Pattern PATTERN_HEADING = Pattern.compile("<body.*?>[\\s]*?([^<\\s])", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

	// to normalize external links to new base href
	private static final Pattern PATTERN_LINK = Pattern.compile("(src|href)=\"(.*?\")", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

	// Where to inject css
	private static final Pattern PATTERN_END_HEAD = Pattern.compile("</head.*?>", Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

	private static boolean initialized = false;

	private static int allowedConnections;

	private static int allowedMaxTopics;

	private boolean confirmed;

	// flag right-to-left direction of text
	private boolean isRTL;

	private AbstractHelpScope scope;

	/*
	 * Constructs the print data for the given request.
	 */
	public PrintData(ServletContext context, HttpServletRequest request, HttpServletResponse response) {
		super(context, request, response);

		if (!initialized) {
			initPreferences(preferences);
		}

		isRTL = UrlUtil.isRTL(request, response);
		scope = RequestScope.getScope(request, response, false);

		String confirmString = request.getParameter("confirmed"); //$NON-NLS-1$
		if ((confirmString != null) && ("true".equals(confirmString))) { //$NON-NLS-1$
			confirmed = true;
		} else {
			confirmed = false;
		}

	}

	/*
	 * Returns the overall topic's title.
	 */
	public String getTitle() {
		return getTopic().getLabel();
	}

	/*
	 * Returns the href of the toc containing the topic(s) to print.
	 */
	public String getTocHref() {
		return getToc().getHref();
	}

	/*
	 * Returns the href of the root topic to print.
	 */
	public String getTopicHref() {
		return getTopic().getHref();
	}

	/*
	 * init properties set in base preference.ini for quick print
	 */
	private static synchronized void initPreferences(WebappPreferences preferences) {

		if (initialized) {
			return;
		}

		// set max connection numbers for concurrent access
		String maxConnections = preferences.getQuickPrintMaxConnections();
		if ((null == maxConnections) || ("".equals(maxConnections.trim()))) { //$NON-NLS-1$
			allowedConnections = defaultMaxConnections;
		} else {
			try {
				allowedConnections = Integer.parseInt(maxConnections);
			} catch (NumberFormatException e) {
				Platform.getLog(PrintData.class).error("Init maxConnections error. Set to default.", e); //$NON-NLS-1$
				allowedConnections = defaultMaxConnections;
			}
		}

		// set max topics allowed to print in one request
		String maxTopics = preferences.getQuickPrintMaxTopics();
		if ((null == maxTopics) || ("".equals(maxTopics.trim()))) { //$NON-NLS-1$
			allowedMaxTopics = defaultMaxTopics;
		} else {
			try {
				allowedMaxTopics = Integer.parseInt(maxTopics);
			} catch (NumberFormatException e) {
				Platform.getLog(PrintData.class).error("Init maxTopics error. Set to default.", e); //$NON-NLS-1$
				allowedMaxTopics = defaultMaxTopics;
			}
		}

		initialized = true;
	}

	/*
	 * Generates resources to print
	 */
	public void generateResources(Writer out) throws IOException, ServletException {
		// check resource allocation
		if (!getConnection()) {
			RequestDispatcher rd = context.getRequestDispatcher("/advanced/printError.jsp"); //$NON-NLS-1$
			request.setAttribute("msg", "noConnection"); //$NON-NLS-1$ //$NON-NLS-2$
			rd.forward(request, response);
			return;
		}


		ITopic topic = getTopic(); // topic selected for print
		int topicRequested = topicsRequested(topic);
		if (topicRequested > allowedMaxTopics) {
			if (!confirmed) {
				releaseConnection();
				RequestDispatcher rd = context.getRequestDispatcher("/advanced/printConfirm.jsp"); //$NON-NLS-1$
				request.setAttribute("topicsRequested", String.valueOf(topicRequested)); //$NON-NLS-1$
				request.setAttribute("allowedMaxTopics", String.valueOf(allowedMaxTopics)); //$NON-NLS-1$
				rd.forward(request, response);
				return;
			}
		}

		try {
			generateToc(out);
			generateContent(out);
		} catch (IOException e) {
			RequestDispatcher rd = context.getRequestDispatcher("/advanced/printError.jsp"); //$NON-NLS-1$
			request.setAttribute("msg", "ioException"); //$NON-NLS-1$ //$NON-NLS-2$
			rd.forward(request, response);
		} finally {
			releaseConnection();
		}
	}

	private static synchronized boolean getConnection() {
		if (allowedConnections > 0) {
			allowedConnections--;
			return true;
		}
		return false;
	}

	private static synchronized void releaseConnection() {
		allowedConnections++;
	}

	/*
	 * Calculate the amount of topics to print in one request
	 */
	private int topicsRequested(ITopic topic) {
		int topicsRequested = 0;
		if (topic.getHref() != null && topic.getHref().length() > 0) {
			topicsRequested++;
		}

		ITopic[] subtopics = ScopeUtils.inScopeTopics(topic.getSubtopics(), scope);
		for (ITopic subtopic : subtopics) {
			topicsRequested += topicsRequested(subtopic);
		}
		return topicsRequested;
	}

	/*
	 * Generates and outputs a table of contents div with links.
	 */
	private void generateToc(Writer out) throws IOException {
		int tocGenerated = 0;

		out.write("<html>\n"); //$NON-NLS-1$
		out.write("<head>\n"); //$NON-NLS-1$
		out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n"); //$NON-NLS-1$
		out.write("<title>" + UrlUtil.htmlEncode(getTitle()) +"</title>\n"); //$NON-NLS-1$ //$NON-NLS-2$
		out.write("<link rel=\"stylesheet\" href=\"print.css\" charset=\"utf-8\" type=\"text/css\">\n"); //$NON-NLS-1$
		out.write("</head>\n"); //$NON-NLS-1$
		out.write("<body dir=\"" + (isRTL ? "right" : "left") + "\" onload=\"print()\">\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		out.write("<div id=\"toc\">\n"); //$NON-NLS-1$
		out.write("<h1>"); //$NON-NLS-1$
		out.write(getTitle());
		out.write("</h1>\n"); //$NON-NLS-1$
		out.write("<h2>"); //$NON-NLS-1$
		out.write(ServletResources.getString("TocHeading", request)); //$NON-NLS-1$
		out.write("</h2>\n"); //$NON-NLS-1$
		out.write("<div id=\"toc_content\">\n"); //$NON-NLS-1$
		ITopic topic = getTopic();

		String href = topic.getHref();
		if (href != null && href.length() > 0) {
			tocGenerated++;
		}
		ITopic[] subtopics = ScopeUtils.inScopeTopics(topic.getSubtopics(), scope);
		for (int i = 0; i < subtopics.length; ++i) {
			tocGenerated = generateToc(subtopics[i], String.valueOf(i + 1), tocGenerated, out);
		}

		out.write("</div>\n"); //$NON-NLS-1$
		out.write("</div>\n"); //$NON-NLS-1$
		out.write("</body>\n"); //$NON-NLS-1$
		out.write("</html>\n"); //$NON-NLS-1$
	}

	/*
	 * Auxiliary method for recursively generating table of contents div.
	 */
	private int generateToc(ITopic topic, String sectionId, int tocGenerated, Writer out) throws IOException {
		if (tocGenerated < allowedMaxTopics) {
			out.write("<div class=\"toc_" + (sectionId.length() > 2 ? "sub" : "") + "entry\">\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			String hrefContent = sectionId + ". " + "<a href=\"#section" + sectionId + "\">" + topic.getLabel() + "</a>\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

			String href = topic.getHref();
			if (href != null && href.length() > 0) {
				tocGenerated++;
				//If the link points to an anchor, use the anchor ID instead of using the section ID
				if (href.contains("#")) //$NON-NLS-1$
					hrefContent = sectionId + ". " + "<a href=\"" + getAnchor(href) + "\">" + topic.getLabel() + "</a>\n"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}

			out.write(hrefContent);

			ITopic[] subtopics = ScopeUtils.inScopeTopics(topic.getSubtopics(), scope);
			for (int i = 0; i < subtopics.length; ++i) {
				String subsectionId = sectionId + "." + (i + 1); //$NON-NLS-1$
				tocGenerated = generateToc(subtopics[i], subsectionId, tocGenerated, out);
			}
			out.write("</div>\n"); //$NON-NLS-1$
			return tocGenerated;
		}
		return tocGenerated;
	}

	/*
	 * Generates the content to print (the merged topics).
	 */
	public void generateContent(Writer out) throws IOException {
		int topicsGenerated = 0;
		generateContent(getTopic(), null, topicsGenerated, new HashSet<>(), out);
	}

	/*
	 * Auxiliary method for recursively generating print content.
	 */
	private int generateContent(ITopic topic, String sectionId, int topicsGenerated, Set<String> generated, Writer out) throws IOException {
		if (topicsGenerated < allowedMaxTopics) {
			String href = topic.getHref();
			if (href != null && href.length() > 0) {
				topicsGenerated++;
				// get the topic content
				href = removeAnchor(href);
				String pathHref = href.substring(0, href.lastIndexOf('/') + 1);
				String baseHref = "../topic" + pathHref; //$NON-NLS-1$
				String content;
				if (!generated.contains(href)) {
					generated.add(href);
					content = getContent(href, locale);
					// root topic doesn't have sectionId
					if (sectionId != null) {
						content = injectHeading(content, sectionId);
					}
					content = normalizeHrefs(content, baseHref);
					content = injectCss(content);
					out.write(content);
				}
			}
			ITopic[] subtopics = ScopeUtils.inScopeTopics(topic.getSubtopics(), scope);
			for (int i = 0; i < subtopics.length; ++i) {
				String subsectionId = (sectionId != null ? sectionId + "." : "") + (i + 1); //$NON-NLS-1$ //$NON-NLS-2$
				topicsGenerated = generateContent(subtopics[i], subsectionId, topicsGenerated, generated, out);
			}
			return topicsGenerated;
		} else {
			return topicsGenerated;
		}
	}

	/*
	 * Injects the sectionId into the document heading.
	 * public static for JUnit Testing
	 */
	public static String injectHeading(String content, String sectionId) {
		Matcher matcher = PATTERN_HEADING.matcher(content);
		if (matcher.find()) {
			String heading = "<a id=\"section" + sectionId + "\">" + sectionId + ". </a>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return content.substring(0, matcher.start(1)) + heading + content.substring(matcher.start(1));
		}
		return content;
	}
	/*
	 *
	 * Injects the sectionId into the document heading.
	 */
	private String injectCss(String content) {
		Matcher matcher = PATTERN_END_HEAD.matcher(content);
		if (matcher.find()) {
			String css = getCssIncludes(); //"<link rel=\"stylesheet\" type=\"text/css\" href=\"../testbook.css\">";
			return content.substring(0, matcher.start(0)) + css + content.substring(matcher.start(0));
		}
		return content;
	}

	/*
	 * Normalizes all external links since we're not at the same base href as the
	 * topics we're printing.
	 */
	private String normalizeHrefs(String content, String baseHref) {
		StringBuilder buf = new StringBuilder();
		Matcher matcher = PATTERN_LINK.matcher(content);
		int prev = 0;
		while (matcher.find()) {
			buf.append(content.substring(prev, matcher.start(2)));
			buf.append(baseHref);
			buf.append(matcher.group(2));
			prev = matcher.end();
		}
		buf.append(content.substring(prev));
		return buf.toString();
	}

	/*
	 * Returns the string content of the referenced topic in UTF-8.
	 */
	@SuppressWarnings("resource")
	private String getContent(String href, String locale) {
		InputStream in = HelpSystem.getHelpContent(href, locale);
		StringBuilder buf = new StringBuilder();
		InputStream rawInput=null;
		if (in != null) {
			try {
				String charset = HTMLDocParser.getCharsetFromHTML(in);
				if (charset == null) {
					charset = "UTF-8"; //$NON-NLS-1$
				}
				rawInput = HelpSystem.getHelpContent(href, locale);
				boolean filter = BaseHelpSystem.getMode() != BaseHelpSystem.MODE_INFOCENTER;
				in = DynamicXHTMLProcessor.process(href, rawInput, locale, filter);

				if (in == null) {
					in = HelpSystem.getHelpContent(href, locale);
				}

				try (Reader reader = new BufferedReader(new InputStreamReader(in, charset))) {
					char[] cbuf = new char[4096];
					int num;
					while ((num = reader.read(cbuf)) > 0) {
						buf.append(cbuf, 0, num);
					}
				}
			}
			catch (Exception e) {
				String msg = "Error retrieving print preview content for " + href; //$NON-NLS-1$
				Platform.getLog(getClass()).error(msg, e);
			}
			finally {
				try {
					in.close();
				}
				catch (IOException e) {
				}
				try {
					if (rawInput != null)
						rawInput.close();
				}
				catch (Exception e) {}
			}
		}
		return buf.toString();
	}

	/*
	 * Returns the toc containing the selected topic(s).
	 */
	private IToc getToc() {
		String tocParam = request.getParameter("toc"); //$NON-NLS-1$
		if (tocParam != null && tocParam.length() > 0) {
			return HelpPlugin.getTocManager().getToc(tocParam, getLocale());
		}
		String topicParam = request.getParameter("topic"); //$NON-NLS-1$
		if (topicParam != null && topicParam.length() > 0) {
			if (topicParam.startsWith("/../nav/")) { //$NON-NLS-1$
				String navPath = topicParam.substring(8);
				StringTokenizer tok = new StringTokenizer(navPath, "_"); //$NON-NLS-1$
				int index = Integer.parseInt(tok.nextToken());
				return HelpPlugin.getTocManager().getTocs(getLocale())[index];
			}
			IToc[] tocs = HelpPlugin.getTocManager().getTocs(getLocale());
			for (IToc toc : tocs) {
				if (toc.getTopic(topicParam) != null) {
					return toc;
				}
			}
		}
		return null;
	}

	/*
	 * Returns the selected topic.
	 */
	private ITopic getTopic() {
		String topicParam = request.getParameter("topic"); //$NON-NLS-1$
		String anchorParam = request.getParameter("anchor"); //$NON-NLS-1$
		if (anchorParam!=null) {
			topicParam = topicParam + '#' + anchorParam;
		}
		if (topicParam != null && topicParam.length() > 0) {
			if (topicParam.startsWith("/../nav/")) { //$NON-NLS-1$
				String navPath = topicParam.substring(8);
				StringTokenizer tok = new StringTokenizer(navPath, "_"); //$NON-NLS-1$
				int index = Integer.parseInt(tok.nextToken());
				ITopic topic = HelpPlugin.getTocManager().getTocs(getLocale())[index].getTopic(null);
				while (tok.hasMoreTokens()) {
					index = Integer.parseInt(tok.nextToken());
					topic = topic.getSubtopics()[index];
				}
				return topic;
			}
			else {
				IToc[] tocs = HelpPlugin.getTocManager().getTocs(getLocale());
				for (IToc toc : tocs) {
					ITopic topic = toc.getTopic(topicParam);
					if (topic != null) {
						return topic;
					}
					// Test for root node as topic
					topic = toc.getTopic(null);
					if (topicParam.equals(topic.getHref())) {
						return topic;
					}
				}
			}
			return null;
		}
		return getToc().getTopic(null);
	}

	private static String removeAnchor(String href) {
		int index = href.indexOf('#');
		if (index != -1) {
			return href.substring(0, index);
		}
		return href;
	}

	private static String getAnchor(String href) {
		int index = href.indexOf('#');
		if (index != -1) {
			return href.substring(index);
		}
		return ""; //$NON-NLS-1$
	}

	private String getCssIncludes() {
		List<String> css = new ArrayList<>();
		CssUtil.addCssFiles("topic_css", css); //$NON-NLS-1$
		return CssUtil.createCssIncludes(css, "../"); //$NON-NLS-1$
	}
}
