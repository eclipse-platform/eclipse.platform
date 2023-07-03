/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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

package org.eclipse.ui.internal.intro.impl.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.internal.intro.impl.model.loader.IntroContentParser;
import org.eclipse.ui.internal.intro.impl.model.util.BundleUtil;
import org.eclipse.ui.internal.intro.impl.model.util.ModelUtil;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An intro container extension. If the content attribute is defined, then it is
 * assumed that we have XHTML content in an external file. Load content from
 * external DOM. No need to worry about caching here because this is a transient
 * model class. It is used and then disregarded from the model.<br>
 * Just like in a page, the styles and altStyles strings can be a comma
 * separated list of styles. Handle this by storing styles just like pages.
 */
public class IntroExtensionContent extends AbstractIntroElement {

	protected static final String TAG_CONTAINER_EXTENSION = "extensionContent"; //$NON-NLS-1$
	protected static final String TAG_CONTAINER_REPLACE = "replacementContent"; //$NON-NLS-1$

	public static final int TYPE_CONTRIBUTION = 0;
	public static final int TYPE_REPLACEMENT = 1;

	protected static final String ATT_PATH = "path"; //$NON-NLS-1$
	protected static final String ATT_ID = "id"; //$NON-NLS-1$
	private static final String ATT_STYLE = "style"; //$NON-NLS-1$
	private static final String ATT_ALT_STYLE = "alt-style"; //$NON-NLS-1$
	private static final String ATT_CONTENT = "content"; //$NON-NLS-1$

	private static final Element[] EMPTY_ELEMENT_ARRAY = new Element[0];

	private String path;
	private String content;
	private String contentFile;
	private String contentId;
	private String anchorId;

	private Element element;
	private String base;

	private Vector<String> styles = new Vector<>();
	private Map<String, Bundle> altStyles = new HashMap<>();

	IntroExtensionContent(Element element, Bundle bundle, String base, IConfigurationElement configExtElement) {
		super(element, bundle);
		path = getAttribute(element, ATT_PATH);
		content = getAttribute(element, ATT_CONTENT);
		anchorId = getAttribute(element, ATT_ID);
		this.element = element;
		this.base = base;

		// load and resolve styles, first.
		init(element, bundle, base);

		// if content is not null we have XHTML extension.
		if (content != null) {
			// BASE: since content is being loaded from another XHTML file and
			// not this xml file, point the base of this page to be relative to
			// the new xml file location.
			IPath subBase = ModelUtil.getParentFolderPath(content);
			String newBase = IPath.fromOSString(base).append(subBase).toString();
			extractFileAndId(bundle);
			contentFile = BundleUtil.getResolvedResourceLocation(base, contentFile,
				bundle);
			this.base = newBase;
		}

		// Save the mapping between plugin registry id and base/anchor id
		String contributor = configExtElement.getContributor().getName();
		ExtensionMap.getInstance().putPluginId(anchorId, contributor);
	}

	public String getId() {
		return anchorId;
	}


	/**
	 * Initialize styles. Take first style in style attribute and make it the
	 * page style. Then put other styles in styles vectors. Make sure to resolve
	 * each style.
	 *
	 * @param element
	 * @param bundle
	 */
	private void init(Element element, Bundle bundle, String base) {
		String[] styleValues = getAttributeList(element, ATT_STYLE);
		if (styleValues != null && styleValues.length > 0) {
			for (int i = 0; i < styleValues.length; i++) {
				String style = styleValues[i];
				style = BundleUtil.getResolvedResourceLocation(base, style,
					bundle);
				addStyle(style);
			}
		}

		String[] altStyleValues = getAttributeList(element, ATT_ALT_STYLE);
		if (altStyleValues != null && altStyleValues.length > 0) {
			for (int i = 0; i < altStyleValues.length; i++) {
				String style = altStyleValues[i];
				style = BundleUtil.getResolvedResourceLocation(base, style,
					bundle);
				addAltStyle(style, bundle);
			}
		}
	}

	/**
	 * Adds the given style to the list. Style is not added if it already exists
	 * in the list.
	 *
	 * @param style
	 */
	protected void addStyle(String style) {
		if (styles.contains(style))
			return;
		styles.add(style);
	}


	/**
	 * Adds the given style to the list.Style is not added if it already exists
	 * in the list.
	 *
	 * @param altStyle
	 */
	protected void addAltStyle(String altStyle, Bundle bundle) {
		if (altStyles.containsKey(altStyle))
			return;
		altStyles.put(altStyle, bundle);
	}

	/**
	 * Returns the extension type; either contribution into an anchor or replacement
	 * of an element.
	 */
	public int getExtensionType() {
		return TAG_CONTAINER_REPLACE.equals(element.getNodeName()) ? TYPE_REPLACEMENT : TYPE_CONTRIBUTION;
	}

	/**
	 * @return Returns the path.
	 */
	public String getPath() {
		return path;
	}

	@Override
	public int getType() {
		return AbstractIntroElement.CONTAINER_EXTENSION;
	}

	protected Element[] getChildren() {
		NodeList nodeList = element.getChildNodes();
		Vector<Node> vector = new Vector<>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE)
				vector.add(node);
		}
		Element[] filteredElements = new Element[vector.size()];
		vector.copyInto(filteredElements);
		// free DOM model for memory performance.
		this.element = null;
		return filteredElements;
	}

	public boolean isXHTMLContent() {
		return content != null ? true : false;
	}

	/**
	 * Returns the elements loaded from the content attribute. This is the content
	 * that should be inserted for the extension. If it is a file, all child elements
	 * of body are returned. If it is a file with an id, only the element with the id
	 * is returned.
	 *
	 * @return the elements to be inserted
	 */
	public Element[] getElements() {
		// only applicable when content attribute is specified
		if (isXHTMLContent()) {
			IntroContentParser parser = new IntroContentParser(contentFile);
			Document dom = parser.getDocument();
			if (dom != null) {
				// parser content should be XHTML because defining content here
				// means that we want XHTML extension.
				if (parser.hasXHTMLContent()) {
					if (contentId != null) {
						// id specified, only get that element
						return new Element[] { ModelUtil.getElementById(dom, contentId) };
					}
					else {
						// no id specified, use the whole body
						Element extensionBody = ModelUtil.getBodyElement(dom);
						return ModelUtil.getElementsByTagName(extensionBody, "*"); //$NON-NLS-1$
					}
				}
			}
		}
		return EMPTY_ELEMENT_ARRAY;
	}

	/**
	 * @return Returns the altStyle.
	 */
	protected Map<String, Bundle> getAltStyles() {
		return altStyles;
	}

	/**
	 * @return Returns the style.
	 */
	protected String[] getStyles() {
		String[] stylesArray = new String[styles.size()];
		styles.copyInto(stylesArray);
		return stylesArray;
	}

	/**
	 * @return Returns the content.
	 */
	public String getContent() {
		return content;
	}

	public String getBase() {
		return base;
	}

	/**
	 * Extracts the file and id parts of the content attribute. This attribute has two modes -
	 * if you specify a file, it will include the body of that file (minus the body element itself).
	 * If you append an id after the file, only the element with that id will be included. However
	 * we need to know which mode we're in.
	 *
	 * @param bundle the bundle that contributed this extension
	 */
	private void extractFileAndId(Bundle bundle) {
		// look for the file
		IPath resourcePath = IPath.fromOSString(base + content);
		if (FileLocator.find(bundle, resourcePath, null) != null) {
			// found it, assume it's a file
			contentFile = content;
		}
		else {
			// didn't find the file, assume the last segment is an id
			int lastSlashIndex = content.lastIndexOf('/');
			if (lastSlashIndex != -1) {
				contentFile = content.substring(0, lastSlashIndex);
				contentId = content.substring(lastSlashIndex + 1);
			}
			else {
				// there was no slash, it must be a file
				contentFile = content;
			}
		}
	}
}
