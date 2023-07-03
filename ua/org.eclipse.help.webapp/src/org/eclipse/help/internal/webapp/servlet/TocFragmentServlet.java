/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.help.internal.webapp.servlet;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.base.scope.ScopeUtils;
import org.eclipse.help.internal.toc.Toc;
import org.eclipse.help.internal.webapp.WebappResources;
import org.eclipse.help.internal.webapp.data.IconFinder;
import org.eclipse.help.internal.webapp.data.RequestScope;
import org.eclipse.help.internal.webapp.data.TocData;
import org.eclipse.help.internal.webapp.data.UrlUtil;

/*
 * Creates xml representing selected parts of one or more TOCs  depending on the parameters
 * With no parameters the head of each toc is included
 * With parameter "href" the node and all its ancestors and siblings is included, corresponds to show in toc
 * With parameter "toc" and optionally "path" the node, its ancestors and children are included
 */
public class TocFragmentServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static Map<String, String> locale2Response = new WeakHashMap<>();
	private boolean isErrorSuppress;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// set the character-set to UTF-8 before calling resp.getWriter()
		resp.setContentType("application/xml; charset=UTF-8"); //$NON-NLS-1$
		resp.getWriter().write(processRequest(req, resp));
	}

	protected String processRequest(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String locale = UrlUtil.getLocale(req, resp);
		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
		resp.setHeader("Cache-Control","no-cache");   //$NON-NLS-1$//$NON-NLS-2$
		resp.setHeader("Pragma","no-cache");  //$NON-NLS-1$ //$NON-NLS-2$
		resp.setDateHeader ("Expires", 0); 	 //$NON-NLS-1$
		TocData data = new TocData(this.getServletContext(), req, resp);

		readParameters(req);

		AbstractHelpScope scope = RequestScope.getScope(req, resp, false);
		Serializer serializer = new Serializer(data, UrlUtil.getLocaleObj(req, resp), scope);
		String response = serializer.generateTreeXml();
		locale2Response.put(locale, response);

		return response;
	}

	private void readParameters(HttpServletRequest req) {
		String errorSuppressParam = req.getParameter("errorSuppress"); //$NON-NLS-1$
		isErrorSuppress = "true".equalsIgnoreCase(errorSuppressParam); //$NON-NLS-1$
	}

	/*
	 * Class which creates the xml file based upon the request parameters
	 */
	private class Serializer {

		private TocData tocData;
		private StringBuilder buf;
		private int requestKind;
		private Locale locale;
		private AbstractHelpScope scope;
		private static final int REQUEST_SHOW_IN_TOC = 1;      // Get the path to an element an element based on its href
		private static final int REQUEST_SHOW_TOCS = 2;        // Show all the tocs but not their children
		private static final int REQUEST_SHOW_CHILDREN = 3;    // Show the children of a node
		private static final int REQUEST_EXPAND_PATH = 4;      // Get all the nodes requires to expand a path in the tree

		public Serializer(TocData data, Locale locale, AbstractHelpScope scope) {
			tocData = data;
			buf = new StringBuilder();
			this.locale = locale;
			this.scope = scope;
			if (tocData.isExpandPath()) {
				requestKind = REQUEST_EXPAND_PATH;
			} else if (tocData.getTopicHref() != null) {
				requestKind = REQUEST_SHOW_IN_TOC;
			} else if (tocData.getSelectedToc() == -1) {
				requestKind = REQUEST_SHOW_TOCS;
			} else {
				requestKind = REQUEST_SHOW_CHILDREN;
			}
		}

		public String generateTreeXml() {
			buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); //$NON-NLS-1$
			buf.append("<tree_data>\n"); //$NON-NLS-1$

			// Return an error for show in toc if topic was not found in toc
			if ((requestKind == REQUEST_SHOW_IN_TOC || requestKind == REQUEST_EXPAND_PATH) && tocData.getTopicPath() == null) {
				addError(WebappResources.getString("CannotSync", locale)); //$NON-NLS-1$
			} else if (requestKind == REQUEST_SHOW_IN_TOC) {
				generateNumericPath();
			} else {
				serializeTocs();
			}
			buf.append("</tree_data>\n"); //$NON-NLS-1$
			return buf.toString();
		}


		private void generateNumericPath() {
			int selectedToc = tocData.getSelectedToc();
			if (selectedToc < 0) {
				addError(WebappResources.getString("CannotSync", locale)); //$NON-NLS-1$
			} else {
				// Count the number of enabled tocs
				int enabled = 0;
				for (int i = 0; i <= selectedToc; i++) {
					if (ScopeUtils.showInTree(tocData.getTocs()[i], scope)) {
						enabled++;
					}
				}
				String fullNumericPath = "" + (enabled - 1); //$NON-NLS-1$
				String numericPath = tocData.getNumericPath();
				if (numericPath != null) {
					fullNumericPath = fullNumericPath +  '_' + numericPath;
				}
				buf.append("<numeric_path path=\"" + fullNumericPath + "\"/>\n"); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		private void addError(String message) {
			if (!isErrorSuppress) {
				buf.append("<error>"); //$NON-NLS-1$
				buf.append(XMLGenerator.xmlEscape(message));
				buf.append("</error>"); //$NON-NLS-1$
			}
		}

		private void serializeTocs() {
			ITopic[] topicPath = tocData.getTopicPath();

			int selectedToc = tocData.getSelectedToc();
			// Iterate over all tocs - if there is a selected toc only generate that
			// toc, otherwise generate the root of every toc.
			for (int toc=0; toc< tocData.getTocCount(); toc++) {
				boolean shouldLoad = requestKind == REQUEST_SHOW_TOCS || toc == selectedToc;
				if (shouldLoad) {
					boolean isSelected = false; // Should this node be selected in the tree
					if (requestKind == REQUEST_SHOW_TOCS) {
						isSelected = toc == 0;
					} else if (requestKind == REQUEST_SHOW_CHILDREN) {
						isSelected = tocData.getRootPath() == null;
					}
					serializeToc(tocData.getTocs()[toc], toc, topicPath, isSelected);
				}
			}
		}

		private void serializeToc(IToc toc, int tocIndex, ITopic[] topicPath, boolean isSelected) {

			if (!ScopeUtils.showInTree(toc, scope)) {
				// do not generate toc when there are no leaf topics or if it is filtered out
				return;
			}
			ITopic[] topics = toc.getTopics();

			if (requestKind == REQUEST_SHOW_CHILDREN) {
				topicPath = tocData.getTopicPathFromRootPath(toc);
			}

			buf.append("<node"); //$NON-NLS-1$
			if (toc.getLabel() != null) {
				buf.append('\n' + "      title=\"" + XMLGenerator.xmlEscape(toc.getLabel()) + '"'); //$NON-NLS-1$
			}
			buf.append('\n' + "      id=\"" + XMLGenerator.xmlEscape(toc.getHref()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$

			String href = fixupHref(toc.getTopic(null).getHref(), "" + tocIndex); //$NON-NLS-1$
			buf.append('\n' + "      href=\"" + XMLGenerator.xmlEscape(UrlUtil.getHelpURL(href)) + "\""); //$NON-NLS-1$ //$NON-NLS-2$

			buf.append(createTocImageTag(toc));

			boolean serializeChildren = true;
			if (requestKind == REQUEST_SHOW_TOCS) {
				serializeChildren = false;
			}
			if (requestKind == REQUEST_EXPAND_PATH && topicPath.length == 0) {
				serializeChildren = false;
				buf.append('\n' + "      is_selected=\"true\"" ); //$NON-NLS-1$
				buf.append('\n' + "      is_highlighted=\"true\"" ); //$NON-NLS-1$
			}
			buf.append(">\n"); //$NON-NLS-1$
			if (serializeChildren) {
				serializeChildTopics(topics, topicPath, "", isSelected); //$NON-NLS-1$
			}
			buf.append("</node>\n"); //$NON-NLS-1$

		}

		private void serializeTopic(ITopic topic, ITopic[] topicPath, boolean isSelected, String parentPath)  {
			ITopic[] subtopics = topic.getSubtopics();
			boolean isLeaf = !ScopeUtils.hasInScopeDescendent(topic, scope);
			buf.append("<node"); //$NON-NLS-1$
			if (topic.getLabel() != null) {
				buf.append('\n'	+ "      title=\"" + XMLGenerator.xmlEscape(topic.getLabel()) + '"'); //$NON-NLS-1$
			}

			buf.append('\n' + "      id=\"" + parentPath + "\""); //$NON-NLS-1$ //$NON-NLS-2$

			String href = topic.getHref();
			href = fixupHref(href, "" + tocData.getSelectedToc() + '_' + parentPath); //$NON-NLS-1$
			buf.append('\n' + "      href=\"" + XMLGenerator.xmlEscape( //$NON-NLS-1$
					UrlUtil.getHelpURL(href)) + '"');
			if (isLeaf ) {
				buf.append('\n' + "      is_leaf=\"true\"" ); //$NON-NLS-1$
			}
			if (isSelected && requestKind == REQUEST_EXPAND_PATH) {
				buf.append('\n' + "      is_selected=\"true\"" ); //$NON-NLS-1$
				buf.append('\n' + "      is_highlighted=\"true\"" ); //$NON-NLS-1$
			}
			String imageTags = createTopicImageTags(topic, isLeaf);
			buf.append(imageTags);

			buf.append(">\n"); //$NON-NLS-1$
			serializeChildTopics(subtopics, topicPath, parentPath, isSelected);
			buf.append("</node>\n"); //$NON-NLS-1$
		}

		private String createTocImageTag(IToc toc) {
			if (toc instanceof Toc toc1) {
				String icon = toc1.getIcon();

				if (IconFinder.isIconDefined(icon)) {
					String openIcon = IconFinder.getImagePathFromId(icon, IconFinder.TYPEICON_OPEN);
					String closedIcon = IconFinder.getImagePathFromId(icon, IconFinder.TYPEICON_CLOSED);
					String imageTags = '\n' + "      openImage=\"/"+ openIcon + "\""; //$NON-NLS-1$ //$NON-NLS-2$
					if (!openIcon.equals(closedIcon)) {
						imageTags += '\n' + "      closedImage=\"/" + closedIcon + "\""; //$NON-NLS-1$ //$NON-NLS-2$
					}
					String altText = IconFinder.getIconAltFromId(icon);
					if(altText != null) {
						imageTags += '\n' + "      imageAlt=\""+ altText + "\""; //$NON-NLS-1$ //$NON-NLS-2$
					}
					return imageTags;
				}
			}
			return '\n' + "      image=\"toc_closed\""; //$NON-NLS-1$
		}

		private String createTopicImageTags(ITopic topic, boolean isLeaf) {
			if (topic instanceof Topic topic1) {
				String icon = topic1.getIcon();
				String altText = IconFinder.getIconAltFromId(icon);

				if (IconFinder.isIconDefined(icon)) {
					String imageTags;
					if (isLeaf) {
						imageTags = '\n' + "      openImage=\"/" +IconFinder.getImagePathFromId(icon, IconFinder.TYPEICON_LEAF) + "\"";   //$NON-NLS-1$//$NON-NLS-2$
					} else {
						String openIcon = IconFinder.getImagePathFromId(icon, IconFinder.TYPEICON_OPEN);
						String closedIcon = IconFinder.getImagePathFromId(icon, IconFinder.TYPEICON_CLOSED);
						imageTags = '\n' + "      openImage=\"/" + openIcon+ "\""; //$NON-NLS-1$ //$NON-NLS-2$
						if (!openIcon.equals(closedIcon)) {
							imageTags += '\n' + "      closedImage=\"/" +  closedIcon + "\""; //$NON-NLS-1$ //$NON-NLS-2$
						}
					}
					if(altText != null) {
						imageTags += '\n' + "      imageAlt=\""+ altText + "\""; //$NON-NLS-1$ //$NON-NLS-2$
					}
					return imageTags;
				}
			}
			String icon;
			if (isLeaf) {
				icon = "topic"; //$NON-NLS-1$
			} else if (topic.getHref() == null) {
				icon = "container_obj"; //$NON-NLS-1$
			} else {
				icon = "container_topic"; //$NON-NLS-1$
			}
			String imageTags = '\n' + "      image=\"" + icon + "\""; //$NON-NLS-1$ //$NON-NLS-2$
			return imageTags;
		}

		private void serializeChildTopics(ITopic[] childTopics, ITopic[] topicPath, String parentPath, boolean parentIsSelected) {
			if (parentIsSelected && requestKind == REQUEST_SHOW_CHILDREN) {
				// Show the children of this node
				for (int subtopic = 0; subtopic < childTopics.length; subtopic++) {
					ITopic childTopic = childTopics[subtopic];
					if (ScopeUtils.showInTree(childTopic, scope)) {
						serializeTopic(childTopic, null, false, addSuffix(parentPath, subtopic));
					}
				}
			} else if (topicPath != null) {
				for (int subtopic = 0; subtopic < childTopics.length; subtopic++) {
					ITopic childTopic = childTopics[subtopic];
					if (ScopeUtils.showInTree(childTopic, scope)) {
						if (topicPath[0].getLabel().equals(childTopic.getLabel())) {
							ITopic[] newPath = null;
							if (topicPath.length > 1) {
								newPath = new ITopic[topicPath.length - 1];
								System.arraycopy(topicPath, 1, newPath, 0, topicPath.length - 1);
							}
							serializeTopic(childTopic, newPath, topicPath.length == 1, addSuffix(parentPath, subtopic));
						} else {
							serializeTopic(childTopic, null, false, addSuffix(parentPath, subtopic));
						}
					}
				}
			}
		}

		private String addSuffix(String parentPath, int subtopic) {
			if (parentPath.length() == 0) {
				return parentPath + subtopic;
			}
			return parentPath + '_' + subtopic;
		}
	}

	/*
	 * Add an extra parameter which represents the path within the tree. This enables show
	 * in Toc, print selected topic and search selected topic and all subtopics to work
	 * correctly even if the same page appears more than once in the table of contents, Bug 330868
	 * Static for testing purposes
	 */
	public static String  fixupHref(String href, String path) {
		if (href == null) {
			return "/../nav/" + path; //$NON-NLS-1$
		}
		int aIndex = href.indexOf('#');
		String anchorPart = ""; //$NON-NLS-1$
		String hrefPart = href;
		if (aIndex > 0) {
			anchorPart = href.substring(aIndex);
			hrefPart = href.substring(0, aIndex);
		}

		int questionIndex = href.indexOf('?');
		if  (questionIndex > 0 ) {
			return hrefPart + "&" + TocData.COMPLETE_PATH_PARAM + '=' + path + anchorPart; //$NON-NLS-1$
		} else {
			return hrefPart + "?" + TocData.COMPLETE_PATH_PARAM + '=' + path + anchorPart; //$NON-NLS-1$
		}

	}

}
