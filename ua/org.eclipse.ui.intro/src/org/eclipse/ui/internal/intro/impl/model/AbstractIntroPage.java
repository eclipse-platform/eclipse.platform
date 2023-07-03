/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
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

import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.UAElementFactory;
import org.eclipse.help.internal.dynamic.DocumentProcessor;
import org.eclipse.help.internal.dynamic.FilterHandler;
import org.eclipse.help.internal.dynamic.ProcessorHandler;
import org.eclipse.ui.internal.intro.impl.IIntroConstants;
import org.eclipse.ui.internal.intro.impl.model.loader.ExtensionPointManager;
import org.eclipse.ui.internal.intro.impl.model.loader.IntroContentParser;
import org.eclipse.ui.internal.intro.impl.model.loader.ModelLoaderUtil;
import org.eclipse.ui.internal.intro.impl.model.util.BundleUtil;
import org.eclipse.ui.internal.intro.impl.model.util.ModelUtil;
import org.eclipse.ui.internal.intro.impl.util.IntroEvaluationContext;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Base class for all Intro pages, inlcuding HomePage. Adds page specific
 * support:
 * <ul>
 * <li>support for page styles, and style inheritance</li>
 * <li>support for XHTML via a DOM instance var. Resolving the page is also
 * handled here.</li>
 * <li>a pge has the concept of being an IFramePage. This is indicated by the
 * isIFrame flag. A page is an IFramePage when it is not defined in any content
 * file, but instead is actually created at runtime. This is done to display a
 * Help System topic embedded in any given div. The current page is cloned, its
 * id is mangled with "_embedDivId", the content of the div pointed to by
 * embedDivId is replaced with an IFrame that loads the Help System topic.</li>
 * </ul>
 */
public abstract class AbstractIntroPage extends AbstractIntroContainer {

	protected static final String TAG_PAGE = "page"; //$NON-NLS-1$
	private static final String ATT_STYLE = "style"; //$NON-NLS-1$
	private static final String ATT_ALT_STYLE = "alt-style"; //$NON-NLS-1$
	private static final String ATT_CONTENT = "content"; //$NON-NLS-1$
	private static final String ATT_SHARED_STYLE = "shared-style"; //$NON-NLS-1$
	private static final String INVALID_CONTENT = "invalidPage/invalidPage.xhtml"; //$NON-NLS-1$
	private static final String INVALID_CONTENT_BASE = "invalidPage"; //$NON-NLS-1$
	protected static final String ATT_URL = "url"; //$NON-NLS-1$

	private String style;
	private String altStyle;
	private String sharedStyle;
	private IntroPageTitle title;
	private String content;
	private String url;

	// if iframe is not null, this indicates that this page was cloned at
	// runtime from another page whose id was "originalId".
	private IntroInjectedIFrame iframe;

	// id of page from which this page was cloned.
	private String originalId;

	// DOM representing XHTML content. DOM is only cached in the case of XHTML
	// content.
	private Document dom;

	private DocumentProcessor domProcessor;

	// set when the content file is loaded (ie: loadChildren is called)
	private boolean isXHTMLPage;

	// Model base attribute is stored in parent class. This base attribute here
	// is to cache the initial location of the content file. When content
	// attribute is defined, the base in the model becomes relative to
	// new xml file. However, in the case of XHTML content, and when
	// presentation is UI forms, we need to reuse initial content file location.
	private String initialBase;

	/**
	 * The vectors to hold all inhertied styles and alt styles from included
	 * elements. They are lazily created when children are resolved (ie:
	 * includes are resolved) OR when extensions are resolved and styles need to
	 * be added to the target page.
	 * <p>
	 * Style Rules:
	 * <ul>
	 * <li>For includes, merge-style controls wether or not the enclosing page
	 * inherits the styles of the target.
	 * <li>If a page is including a shared div, merging target styles into this
	 * page is ignored. Shared divs do not have styles.</li>
	 * <li>For extensions, if the style or alt-style is not defined, that means
	 * that no style inheritence is needed, and the style of the target page are
	 * not updated.
	 * <li>If an extension is extending a shared div, merging the styles of
	 * this extension into the target is ignored. Shared divs do not have
	 * styles.</li>
	 * <li>Shared hashtable has alt-styles as keys and bundles as values.</li>
	 * </ul>
	 */
	private Vector<String> styles;
	private Map<String, Bundle> altStyles;
	private boolean isDynamic = false;
	protected boolean isStandbyPage;

	/**
	 * Parent class for all pages. Make sure to set the bundle to where the
	 * pages are loaded from. This means that the bundle for root may be
	 * different from the bundle for all the other pages. Only pages do this
	 * logic and so other model objects might have the wrong bundle cached if
	 * the resource was loaded from an nl directory.
	 *
	 * @param element
	 */
	AbstractIntroPage(Element element, Bundle bundle, String base) {
		super(element, bundle, base);
		this.initialBase = base;
		content = getAttribute(element, ATT_CONTENT);
		if (content == null) {
			//Delaying init until we have the model
			// so that we can resolve theme
			//init(element, bundle, base);
		}
		else {
			// Content is not null. Resolve it. Other page attributes (style,
			// alt-style...) will be loaded when xml content file is loaded
			// since we need to pick them up from external xml content file. In
			// the case where this external content file is XHTML and we have
			// HTML presentation, page attributes are simply not loaded. In the
			// case where we have XHTML in a UI forms presentation, we will need
			// to load initial page attributes.
			// BASE: since content is being loaded from another xml file, point
			// the base of this page to be relative to the new xml file
			// location.
			IPath subBase = ModelUtil.getParentFolderPath(content);
			this.base = IPath.fromOSString(base).append(subBase).toString();
			content = BundleUtil.getResolvedResourceLocation(base, content,
				bundle);
		}
		// load shared-style attribure. This is needed in the XML and in the
		// XHTML cases. Default is to include shared style.
		this.sharedStyle = getAttribute(element, ATT_SHARED_STYLE);
		if (sharedStyle == null)
			sharedStyle = "true"; //$NON-NLS-1$
		url = getAttribute(element, ATT_URL);
		if (url == null)
			// if we do not have a URL attribute, then we have dynamic content.
			isDynamic = true;
		else
			// check the url/standby-url attributes and update accordingly.
			url = ModelUtil.resolveURL(base, url, bundle);

	}

	@Override
	public void setParent(AbstractIntroElement parent) {
		super.setParent(parent);
		if (content == null)
			init(element, getBundle(), initialBase);
	}

	/**
	 * Returns unresolved content value as found in the source file.
	 * the source file.
	 * @return the unresolved content value
	 */

	public String getRawContent() {
		return getAttribute(element, ATT_CONTENT);
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
			style = styleValues[0];
			style = BundleUtil.getResolvedResourceLocation(base, style, bundle);
			for (int i = 1; i < styleValues.length; i++) {
				String style = styleValues[i];
				style = BundleUtil.getResolvedResourceLocation(base, style,
					bundle);
				addStyle(style);
			}
		}

		String[] altStyleValues = getAttributeList(element, ATT_ALT_STYLE);
		if (altStyleValues != null && altStyleValues.length > 0) {
			altStyle = altStyleValues[0];
			altStyle = BundleUtil.getResolvedResourceLocation(base, altStyle,
				bundle);
			for (int i = 1; i < altStyleValues.length; i++) {
				String style = altStyleValues[i];
				style = BundleUtil.getResolvedResourceLocation(base, style,
					bundle);
				addAltStyle(style, bundle);
			}
		}
	}




	/**
	 * The page's title. Each page can have one title.
	 *
	 * @return Returns the title of this page.
	 */
	public String getTitle() {
		// title is a child of the page, and so we have to load children first.
		// We also have to resolve children because someone might be including a
		// title. Update title instance after all includes and extensions have
		// been resolved.
		getChildren();
		if (title == null) {
			// there should only be one title child per page. safe to cast.
			IntroPageTitle[] titles = (IntroPageTitle[]) getChildrenOfType(AbstractIntroElement.PAGE_TITLE);
			if (titles.length > 0)
				title = titles[0];
		}

		if (title == null)
			// still null. no title.
			return null;
		return title.getTitle();
	}

	/**
	 * @return Returns the style.
	 */
	public String getStyle() {
		return style;
	}

	/**
	 * @return Returns the alt_style.
	 */
	public String getAltStyle() {
		return altStyle;
	}

	/**
	 * Gets all the inherited styles of this page. Styles can be inherited from
	 * includes or from configExtensions.
	 * <p>
	 * Note: this call needs to get all the children of this page, and so it
	 * will resolve this page. might be expensive.
	 *
	 * @return Returns all the inherited styles of this page. Returns an empty
	 *         array if page is not expandable or does not have inherited
	 *         styles.
	 */
	public String[] getStyles() {
		// call get children first to resolve includes and populate styles
		// vector. Resolving children will initialize the style vectors.
		getChildren();
		if (styles == null)
			// style vector is still null because page does not have styles.
			return new String[0];
		String[] stylesArray = new String[styles.size()];
		styles.copyInto(stylesArray);
		return stylesArray;
	}

	/**
	 * Gets all the inherited alt-styles of this page (ie: styles for the SWT
	 * presentation). A hashtable is returned that has inhertied alt-styles as
	 * keys, and plugin descriptors as values. This is needed to be able to load
	 * resources from the inherited target plugin. Note: this call needs to get
	 * all the children of this page, and so its will resolve this page. might
	 * be expensive.
	 *
	 * @return Returns all the inherited styles of this page. Returns an empty
	 *         hashtable if page is not expandable, does not have any includes,
	 *         or has includes that do not merge styles.
	 */
	public Map<String, Bundle> getAltStyles() {
		// call get children first to resolve includes and populate hashtable.
		// Resolving children will initialize the style vectors.
		getChildren();
		return altStyles;
	}

	/**
	 * Adds the given style to the list. Style is not added if it already exists
	 * in the list.
	 *
	 * @param style
	 */
	protected void addStyle(String style) {
		if (!initStyles(style))
			return;
		if (styles.contains(style))
			return;
		styles.add(style);
	}

	public void insertStyle(String style, int location) {
		if (!initStyles(style))
			return;
		if (styles.contains(style))
			return;
		styles.add(location, style);
	}



	/**
	 * Adds the given style to the list.Style is not added if it already exists
	 * in the list.
	 *
	 * @param altStyle
	 */
	protected void addAltStyle(String altStyle, Bundle bundle) {
		if (!initAltStyles(altStyle))
			return;
		if (altStyles.containsKey(altStyle))
			return;
		altStyles.put(altStyle, bundle);
	}


	/**
	 * Util method to add given styles to the list.
	 *
	 */
	protected void addStyles(String[] styles) {
		if (styles == null)
			return;
		for (int i = 0; i < styles.length; i++)
			addStyle(styles[i]);
	}

	/**
	 * Util method to add map of altstyles to list.
	 */
	protected void addAltStyles(Map<String, Bundle> altStyles) {
		if (altStyles == null)
			return;
		if (this.altStyles == null)
			// delay creation until needed.
			this.altStyles = new Hashtable<>();
		this.altStyles.putAll(altStyles);
	}


	private boolean initStyles(String style) {
		if (style == null)
			return false;
		if (this.styles == null)
			// delay creation until needed.
			this.styles = new Vector<>();
		return true;
	}

	private boolean initAltStyles(String style) {
		if (style == null)
			return false;
		if (this.altStyles == null)
			// delay creation until needed.
			this.altStyles = new Hashtable<>();
		return true;
	}


	@Override
	public int getType() {
		return AbstractIntroElement.ABSTRACT_PAGE;
	}

	/*
	 * Override parent behavior to lazily initialize styles vectors. This will
	 * only be called once, if resolved == false. In the case of DOM model,
	 * resolve this page's full DOM.
	 *
	 * @see org.eclipse.ui.internal.intro.impl.model.AbstractIntroContainer#resolveChildren()
	 */
	@Override
	protected void resolveChildren() {
		// flag would be set
		if (isXHTMLPage)
			resolvePage();
		else
			super.resolveChildren();
	}



	/**
	 * Override parent behavior to add support for HEAD &amp; Title element in pages
	 * only, and not in divs.
	 *
	 * @see org.eclipse.ui.internal.intro.impl.model.AbstractIntroContainer#getModelChild(org.eclipse.core.runtime.IConfigurationElement)
	 */
	@Override
	protected AbstractIntroElement getModelChild(Element childElement,
			Bundle bundle, String base) {
		AbstractIntroElement child = null;
		if (childElement.getNodeName().equalsIgnoreCase(IntroHead.TAG_HEAD)) {
			child = new IntroHead(childElement, bundle, base);
		} else if (childElement.getNodeName().equalsIgnoreCase(
			IntroPageTitle.TAG_TITLE)) {
			// if we have a title, only add it as a child if we did not load one
			// before. A page can only have one title.
			if (title == null) {
				child = new IntroPageTitle(childElement, bundle);
			}
		}
		if (child != null)
			return child;
		return super.getModelChild(childElement, bundle, base);
	}

	/**
	 * Returns all head contributions in this page. There can be more than one
	 * head contribution in the page;
	 *
	 * @return
	 */
	public IntroHead[] getHTMLHeads() {
		return (IntroHead[]) getChildrenOfType(AbstractIntroElement.HEAD);
	}


	/**
	 * load the children of this container. Override parent behavior because we
	 * want to support loading content from other xml files. The design is that
	 * only the id and content from the existing page are honored. All other
	 * attributes are what they are defined in the external paget. For XHTML
	 * content, all info is in the xhtml page. If we fail to load the page,
	 * display the Invalid Page page.
	 */
	@Override
	protected void loadChildren() {
		if (content == null) {
			// no content. do regular loading.
			super.loadChildren();
			return;
		}

		// content attribute is defined. It either points to an XHTML file, or
		// an introContent.xml file. Process each case. Assume it is an
		// introContent file.
		// INTRO: XHTML file is loaded needlessly when we have XHTML content and
		// SWT presentation.
		IntroContentParser parser = new IntroContentParser(content);
		Document dom = parser.getDocument();
		if (dom == null) {
			// bad xml. This could be bad XHTML or bad intro XML. Parser would
			// have logged fact. Load dom for invalid page, and make sure to
			// force an extract on parent folder to enabling jarring.
			Bundle introBundle = Platform.getBundle(IIntroConstants.PLUGIN_ID);
			ModelUtil.ensureFileURLsExist(introBundle, INVALID_CONTENT);

			String invalidContentFilePath = BundleUtil
				.getResolvedResourceLocation(INVALID_CONTENT, introBundle);
			parser = new IntroContentParser(invalidContentFilePath);
			dom = parser.getDocument();
			// make sure to override all attributes to resolve the Invalid
			// Page page correctly.
			content = invalidContentFilePath;
			this.base = INVALID_CONTENT_BASE;
			setBundle(introBundle);
		}

		// parse content depending on type. Make sure to set the loaded flag
		// accordingly, otherwise content file will always be parsed.
		if (parser.hasXHTMLContent()) {
			loadXHTMLContent(dom);
			// make sure to use correct base.
			init(element, getBundle(), initialBase);
			super.loadChildren();
		} else
			// load the first page with correct id, from content xml file.
			loadXMLContent(dom);
	}

	/**
	 * Load the xml content from the introContent.xml file pointed to by the
	 * content attribute, and loaded into the passed DOM. Load the first page
	 * with correct id from this content file.
	 *
	 * @param dom
	 */
	private void loadXMLContent(Document dom) {
		Element[] pages = ModelUtil.getElementsByTagName(dom,
			AbstractIntroPage.TAG_PAGE);
		if (pages.length == 0) {
			Log.warning("Content file has no pages."); //$NON-NLS-1$
			return;
		}
		// point the element of this page to the new element. Pick first page
		// with matching id. Make sure to disable loading of children of current
		// element if a matching page in the external content file is not found.
		boolean foundMatchingPage = false;
		for (int i = 0; i < pages.length; i++) {
			Element pageElement = pages[i];
			if (pageElement.getAttribute(AbstractIntroIdElement.ATT_ID).equals(
				getId())) {
				this.element = pageElement;
				// call init on the new element. the filtering and the style-id
				// are loaded by the parent class.
				init(pageElement, getBundle(), base);
				// INTRO: revisit. Special processing here should be made more
				// general. we know id is correct.
				style_id = getAttribute(element,
					AbstractBaseIntroElement.ATT_STYLE_ID);
				filteredFrom = getAttribute(element,
					AbstractBaseIntroElement.ATT_FILTERED_FROM);
				sharedStyle = getAttribute(element, ATT_SHARED_STYLE);
				if (sharedStyle == null)
					sharedStyle = "true"; //$NON-NLS-1$
				foundMatchingPage = true;
			}
		}
		if (foundMatchingPage)
			// now do children loading as usual.
			super.loadChildren();
		else {
			// page was not found in content file. Perform load actions, and log
			// fact. init the children vector.
			children = new Vector<>();
			loaded = true;
			// free DOM model for memory performance.
			element = null;
			Log.warning("Content file does not have page with id= " + getId()); //$NON-NLS-1$
		}
	}

	private void loadXHTMLContent(Document dom) {
		// no need to load any children since we use XSLT to print XHTML
		// content. Simply cache DOM.
		this.dom = dom;
		this.isXHTMLPage = true;
		// init empty children vector.
		children = new Vector<>();
		loaded = true;
	}

	/**
	 * Returns the DOM representing an external XHTML file. May return null if
	 * extension content is 3.0 format. The page is resolved before returning,
	 * meaning includes are resolved, and the base of the page is defined.
	 *
	 * @return
	 */
	public Document getResolvedDocument() {
		// we need to force a getChildren to resolve the page.
		getChildren();
		return dom;
	}


	/**
	 * Returns the DOM representing an external XHTML file. May return null if
	 * extension content is 3.0 format. The page is NOT resolved before
	 * returning. It is retruned as given by the dom parser.
	 *
	 * @return
	 */
	public Document getDocument() {
		// we only need to load children here.
		if (!loaded)
			loadChildren();
		return dom;
	}


	/**
	 * Returns whether or not we have an XHTML page as the content for this
	 * page. The XHTML page is defined through the content attribute. This
	 * method forces the content file to be parsed and loaded in memory.
	 *
	 * @return
	 */
	public boolean isXHTMLPage() {
		// we need to force loading of children since we need to determine
		// content type. Load the children without resolving (for performance),
		// this will set the XHTML flag at the page level.
		if (!loaded)
			loadChildren();
		return isXHTMLPage;
	}


	/**
	 * Deep searches all children in this container's DOM for the first child
	 * with the given id. The element retrieved must have the passed local name.
	 * Note that in an XHTML file (aka DOM) elements should have a unique id
	 * within the scope of a document. We use local name because this allows for
	 * finding intro anchors, includes and dynamic content element regardless of
	 * whether or not an xmlns was used in the xml. note: could not have used
	 * inheritance from parent container because return type for parent is intro
	 * legacy model.
	 *
	 */
	public Element findDomChild(String id, String localElementName) {
		if (!loaded)
			loadChildren();
		// using getElementById is tricky and we need to have intro XHTML
		// modules to properly use this method.
		return ModelUtil.getElementById(dom, id, localElementName);
	}

	/**
	 * Search for any element with the given id.
	 *
	 * @param id
	 * @return
	 */
	public Element findDomChild(String id) {
		return findDomChild(id, "*"); //$NON-NLS-1$

	}



	/**
	 * Resolves the full page. It is called just before the page needs to be
	 * displayed.
	 * <ul>
	 * <li>adds a BASE child to the DOM HEAD element, if one is not defined.
	 * All intro documents must have a base defined to resolve all urls.</li>
	 * <li>resolves all includes in the page. This means importing target DOM.
	 * </li>
	 * <li>resolves all XHTML attributes for resources, eg: src, href
	 * attributes.</li>
	 * </ul>
	 */
	protected void resolvePage() {
		// insert base meta-tag,
		ModelUtil.insertBase(dom, ModelUtil.getParentFolderOSString(content));

		// resolve all relative resources relative to content file. Do it before
		// inserting shared style to enable comparing fully qualified styles.
		ModelUtil.updateResourceAttributes(dom.getDocumentElement(), this);

		// now add shared style.
		IntroModelRoot modelRoot = (IntroModelRoot)getParent();
		IntroPartPresentation presentation = modelRoot.getPresentation();
		String [] styles = presentation!=null?presentation.getImplementationStyles():null;
		if (styles != null && injectSharedStyle()) {
			for (int i=0; i<styles.length; i++)
				ModelUtil.insertStyle(dom, styles[i]);
		}

		// filter the content
		if (domProcessor == null) {
			domProcessor = new DocumentProcessor(new ProcessorHandler[] { new FilterHandler(IntroEvaluationContext.getContext()) });
		}
		UAElement element = UAElementFactory.newElement(dom.getDocumentElement());
		domProcessor.process(element, null);

		// and resolve includes.
		resolveIncludes();

		// now remove all anchors from this page.
		ModelUtil.removeAllElements(dom, IntroAnchor.TAG_ANCHOR);
		resolved = true;
	}

	/**
	 * Resolves all includes in this page. This means importing the DOM of the
	 * target path into the current page DOM, and resolving XHTML attributes for
	 * resources.
	 */
	protected void resolveIncludes() {
		// get all includes elements in DOM.
		NodeList includes = dom.getElementsByTagNameNS("*", //$NON-NLS-1$
			IntroInclude.TAG_INCLUDE);
		// get the array version of the include nodelist to work around
		// replaceChild() DOM api design.
		Node[] nodes = ModelUtil.getArray(includes);
		for (int i = 0; i < nodes.length; i++) {
			Element includeElement = (Element) nodes[i];
			IntroInclude include = new IntroInclude(includeElement, getBundle());
			// result[0] is target parent page, result[1] is target element.
			Object[] results = findDOMIncludeTarget(include);
			Element targetElement = (Element) results[1];
			if (targetElement == null) {
				String message = "Could not resolve following include:  " //$NON-NLS-1$
						+ ModelLoaderUtil.getLogString(getBundle(),
							includeElement, IntroInclude.ATT_PATH);
				Log.warning(message);
				return;
			}

			// insert the target element instead of the include.
			Node targetNode = dom.importNode(targetElement, true);
			// update the src attribute of this node, if defined by w3
			// specs.
			AbstractIntroPage page = ((AbstractIntroPage) results[0]);
			ModelUtil.updateResourceAttributes((Element) targetNode, page);
			// use of replace API to remove include element is tricky. It
			// confuses the NodeList used in the loop above. Removing an include
			// removes it from the NodeList. Used cloned Array instead.
			includeElement.getParentNode().replaceChild(targetNode,
				includeElement);
		}
	}


	/**
	 * Find the target Element pointed to by the path in the include. It is
	 * assumed that configId always points to an external config, and not the
	 * same config of the include.
	 *
	 * @param include
	 * @param path
	 * @return
	 */
	private Object[] findDOMIncludeTarget(IntroInclude include) {
		String path = include.getPath();
		IntroModelRoot targetModelRoot = (IntroModelRoot) getParentPage()
			.getParent();
		String targetConfigID = include.getConfigId();
		if (targetConfigID != null)
			targetModelRoot = ExtensionPointManager.getInst().getModel(
				targetConfigID);
		if (targetModelRoot == null)
			// if the target config was not found, skip this include.
			return null;
		return findDOMTarget(targetModelRoot, path);

	}



	/**
	 * Finds the child element that corresponds to the given path in the passed
	 * model.
	 *
	 * @param model
	 *            model containing target path.
	 * @param path
	 *            the path to look for
	 * @param results
	 *            two object array that will return the target intro page as the
	 *            first result, and the actual target DOM Element as the second
	 *            result. It is guaranteed to not be null. Content may be null.
	 * @return target DOM element
	 */
	public Object[] findDOMTarget(IntroModelRoot model, String path) {
		Object[] results = new Object[2];
		// path must be pageId/anchorID in the case of of XHTML pages.
		// pages.
		String[] pathSegments = path.split("/"); //$NON-NLS-1$
		if (pathSegments.length != 2)
			// path does not have correct format. Return empty results.
			return results;

		// save to cast.
		AbstractIntroPage targetPage = (AbstractIntroPage) model.findChild(
			pathSegments[0], ABSTRACT_PAGE);

		if (targetPage != null) {
			results[0] = targetPage;
			Element targetElement = targetPage.findDomChild(pathSegments[1]);
			if (targetElement != null)
				results[1] = targetElement;
		}
		return results;
	}


	/**
	 * @return Returns the content.
	 */
	public String getContent() {
		return content;
	}



	/**
	 * Deep copy since class has mutable objects.
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		AbstractIntroPage clone = (AbstractIntroPage) super.clone();
		if (title != null) {
			IntroPageTitle clonedTitle = (IntroPageTitle) title.clone();
			clonedTitle.setParent(clone);
			clone.title = clonedTitle;
		}
		// styles are safe for a shallow copy.
		if (styles != null)
			clone.styles = new Vector<>(styles);
		if (altStyles != null)
			clone.altStyles = new Hashtable<>(altStyles);
		return clone;
	}

	/**
	 * Used when cloning pages to assign a unique id. Cache original id before
	 * setting.
	 *
	 * @param id
	 */
	public void setId(String id) {
		this.originalId = this.id;
		this.id = id;
	}

	/*
	 * Creates an IFrame and injects it as the only child of the specified path.
	 */
	public boolean injectIFrame(String url, String embedTarget) {
		// embed url as IFrame in target div. We need to find target div in
		// this cloned page not in the original page.
		IntroGroup divToReplace = (IntroGroup) findTarget(embedTarget);
		if (divToReplace == null) {
			// we failed to find embed div, log and exit.
			Log.warning("Failed to find embedTarget: " + embedTarget //$NON-NLS-1$
					+ " in page " + getId()); //$NON-NLS-1$
			return false;
		}

		this.iframe = new IntroInjectedIFrame(getElement(), getBundle());
		this.iframe.setParent(divToReplace);
		this.iframe.setIFrameURL(url);
		divToReplace.clearChildren();
		divToReplace.addChild(iframe);
		return true;
	}

	/**
	 * Return true if this page is a cloned page that has an IFrame.
	 *
	 * @return
	 */
	public boolean isIFramePage() {
		return (iframe != null) ? true : false;
	}


	public String getUnmangledId() {
		if (isIFramePage())
			return originalId;
		return id;
	}


	/**
	 * Set the url of the embedded IFrame, if this page is an IFrame page.
	 *
	 * @param url
	 */
	public void setIFrameURL(String url) {
		if (!isIFramePage())
			return;
		this.iframe.setIFrameURL(url);
	}

	/**
	 * Return the url of the embedded IFrame, if this page is an IFrame page.
	 *
	 * @param url
	 */
	public String getIFrameURL() {
		if (!isIFramePage())
			return null;
		return this.iframe.getIFrameURL();
	}

	/**
	 * Returns the raw or unprocessed base location.
	 */
	public String getInitialBase() {
		return initialBase;
	}

	/**
	 * Return the url of the embedded IFrame, if this page is an IFrame page.
	 *
	 * @param url
	 */
	public boolean injectSharedStyle() {
		return this.sharedStyle.equals("true") ? true : false; //$NON-NLS-1$
	}

	/**
	 * Returns true if this is a dynamicpage or not.
	 * @return Returns the isDynamic.
	 */
	public boolean isDynamic() {
		return isDynamic;
	}

	/**
	 * @return Returns the url.
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * @return Returns the isStandbyPage.
	 */
	public boolean isStandbyPage() {
		return isStandbyPage;
	}

	/**
	 * @param isStandbyPage
	 *            The isStandbyPage to set.
	 */
	public void setStandbyPage(boolean isStandbyPage) {
		this.isStandbyPage = isStandbyPage;
	}

}
