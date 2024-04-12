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

import java.util.Iterator;
import java.util.Vector;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.help.UAContentFilter;
import org.eclipse.help.internal.UAElementFactory;
import org.eclipse.ui.internal.intro.impl.model.loader.ExtensionPointManager;
import org.eclipse.ui.internal.intro.impl.util.IntroEvaluationContext;
import org.eclipse.ui.internal.intro.impl.util.Log;
import org.osgi.framework.Bundle;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An intro config component that is a container, ie: it can have children.
 */
public abstract class AbstractIntroContainer extends AbstractBaseIntroElement {

	protected static final String ATT_BG_IMAGE = "bgImage"; //$NON-NLS-1$
	// vector is lazily created when children are loaded in a call to
	// loadChildren().
	protected Vector<AbstractIntroElement> children;
	protected boolean loaded = false;
	protected boolean resolved = false;
	protected Element element;

	// store the base since it will needed later to resolve children.
	protected String base;

	AbstractIntroContainer(IConfigurationElement element) {
		super(element);
	}

	AbstractIntroContainer(Element element, Bundle bundle) {
		super(element, bundle);
		this.element = element;
	}

	AbstractIntroContainer(Element element, Bundle bundle, String base) {
		super(element, bundle);
		this.element = element;
		this.base = base;
	}


	/**
	 * Get the children of this container. Loading children and resolving
	 * includes and extension is delayed until this method call.
	 *
	 * @return Returns all the children of this container.
	 */
	public AbstractIntroElement[] getChildren() {
		if (!loaded)
			loadChildren();

		if (!loaded)
			// if loaded still is false, something went wrong. This could happen
			// when loading content from another external content files.
			return new AbstractIntroElement[0];

		if (!resolved)
			resolveChildren();

		Vector filtered = filterChildren(children);

		AbstractIntroElement[] childrenElements = (AbstractIntroElement[]) convertToModelArray(
			filtered, AbstractIntroElement.ELEMENT);
		return childrenElements;
	}

	/**
	 * Returns all the children of this container that are of the specified
	 * type(s). <br>
	 * An example of an element mask is as follows:
	 * <p>
	 * <code>
	 * 		int elementMask = IntroElement.IMAGE | IntroElement.DEFAULT_LINK;
	 * 		int elementMask = IntroElement.ABSTRACT_CONTAINER;
	 * </code>
	 * The return type is determined depending on the mask. If the mask is a
	 * predefined constant in the IntroElement, and it does not correspond to an
	 * abstract model class, then the object returned can be safely cast to an
	 * array of the corresponding model class. For exmaple, the following code
	 * gets all groups in the given page, in the same order they appear in the
	 * plugin.xml markup:
	 * <p>
	 * <code>
	 * 		Introgroup[] groups  = (IntroGroup[])page.getChildrenOfType(IntroElement.GROUP);
	 * </code>
	 *
	 * However, if the element mask is not homogenous (for example: LINKS |
	 * GROUP) then the returned array must be cast to an array of
	 * IntroElements.For exmaple, the following code gets all images and links
	 * in the given page, in the same order they appear in the plugin.xml
	 * markup:
	 * <p>
	 * <code>
	 * 		int elementMask = IntroElement.IMAGE | IntroElement.DEFAULT_LINK;
	 * 		IntroElement[] imagesAndLinks  =
	 * 			(IntroElement[])page.getChildrenOfType(elementMask);
	 * </code>
	 *
	 * @return An array of elements of the right type. If the container has no
	 *         children, or no children of the specified types, returns an empty
	 *         array.
	 */
	public Object[] getChildrenOfType(int elementMask) {

		AbstractIntroElement[] childrenElements = getChildren();
		// if we have no children, we still need to return an empty array of
		// the correct type.
		Vector<AbstractIntroElement> typedChildren = new Vector<>();
		for (int i = 0; i < childrenElements.length; i++) {
			AbstractIntroElement element = childrenElements[i];
			if (element.isOfType(elementMask))
				typedChildren.addElement(element);
		}
		return convertToModelArray(typedChildren, elementMask);
	}

	/**
	 * Utility method to convert all the content of a vector of
	 * AbstractIntroElements into an array of IntroElements cast to the correct
	 * class type. It is assumed that all elements in this vector are
	 * IntroElement instances. If elementMask is a predefined model type (ie:
	 * homogenous), then return array of corresponding type. Else, returns an
	 * array of IntroElements.
	 */
	private Object[] convertToModelArray(Vector vector, int elementMask) {
		int size = vector.size();
		Object[] src = null;
		switch (elementMask) {
		// homogenous vector.
		case AbstractIntroElement.GROUP:
			src = new IntroGroup[size];
			break;
		case AbstractIntroElement.LINK:
			src = new IntroLink[size];
			break;
		case AbstractIntroElement.TEXT:
			src = new IntroText[size];
			break;
		case AbstractIntroElement.IMAGE:
			src = new IntroImage[size];
			break;
		case AbstractIntroElement.HR:
			src = new IntroSeparator[size];
			break;
		case AbstractIntroElement.HTML:
			src = new IntroHTML[size];
			break;
		case AbstractIntroElement.INCLUDE:
			src = new IntroInclude[size];
			break;
		case AbstractIntroElement.PAGE:
			src = new IntroPage[size];
			break;
		case AbstractIntroElement.ABSTRACT_PAGE:
			src = new AbstractIntroPage[size];
			break;
		case AbstractIntroElement.ABSTRACT_CONTAINER:
			src = new AbstractIntroContainer[size];
			break;
		case AbstractIntroElement.HEAD:
			src = new IntroHead[size];
			break;
		case AbstractIntroElement.PAGE_TITLE:
			src = new IntroPageTitle[size];
			break;
		case AbstractIntroElement.ANCHOR:
			src = new IntroAnchor[size];
			break;
		case AbstractIntroElement.CONTENT_PROVIDER:
			src = new IntroContentProvider[size];
			break;

		default:
			// now handle left over abstract types. Vector is not homogenous.
			src = new AbstractIntroElement[size];
			break;
		}

		vector.copyInto(src);
		return src;

	}

	/**
	 * Load all the children of this container. A container can have other
	 * containers, links, htmls, text, image, include. Load them in the order
	 * they appear in the xml content file.
	 */
	protected void loadChildren() {
		// init the children vector. old children are disposed automatically.
		children = new Vector<>();


		NodeList nodeList = element.getChildNodes();
		Vector<Node> vector = new Vector<>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE)
				vector.add(node);
		}
		Element[] filteredElements = new Element[vector.size()];
		vector.copyInto(filteredElements);
		// add the elements at the end children's vector.
		insertElementsBefore(filteredElements, getBundle(), base, children
			.size(), null);
		loaded = true;
		// we cannot free DOM model element because a page's children may be
		// nulled when reflowing a content provider.
	}

	/**
	 * Adds the given elements as children of this container, before the
	 * specified index.
	 */
	protected void insertElementsBefore(Element[] childElements, Bundle bundle,
			String base, int index, String mixinStyle) {
		for (int i = 0; i < childElements.length; i++) {
			Element childElement = childElements[i];
			AbstractIntroElement child = getModelChild(childElement, bundle,
				base);
			if (child != null) {
				child.setParent(this);
				child.setMixinStyle(mixinStyle);
				children.add(index, child);
				// index is only incremented if we actually added a child.
				index++;
			}
		}
	}

	/**
	 * Adds the given elements as children of this container, before the
	 * specified element. The element must be a direct child of this container.
	 */
	protected void insertElementsBefore(Element[] childElements, Bundle bundle,
			String base, AbstractIntroElement child, String mixinStyle) {
		int childLocation = children.indexOf(child);
		if (childLocation == -1)
			// bad reference child.
			return;
		insertElementsBefore(childElements, bundle, base, childLocation, mixinStyle);
	}



	/**
	 * Adds a child to this container, depending on its type. Subclasses may
	 * override if there is a child specific to the subclass.
	 */
	protected AbstractIntroElement getModelChild(Element childElement,
			Bundle bundle, String base) {

		AbstractIntroElement child = null;
		if (childElement.getNodeName().equalsIgnoreCase(IntroGroup.TAG_GROUP))
			child = new IntroGroup(childElement, bundle, base);
		else if (childElement.getNodeName()
			.equalsIgnoreCase(IntroLink.TAG_LINK))
			child = new IntroLink(childElement, bundle, base);
		else if (childElement.getNodeName()
			.equalsIgnoreCase(IntroText.TAG_TEXT))
			child = new IntroText(childElement, bundle);
		else if (childElement.getNodeName().equalsIgnoreCase(
			IntroImage.TAG_IMAGE))
			child = new IntroImage(childElement, bundle, base);
		else if (childElement.getNodeName().equalsIgnoreCase(
				IntroSeparator.TAG_HR))
			child = new IntroSeparator(childElement, bundle, base);
		else if (childElement.getNodeName()
			.equalsIgnoreCase(IntroHTML.TAG_HTML))
			child = new IntroHTML(childElement, bundle, base);
		else if (childElement.getNodeName().equalsIgnoreCase(
			IntroInclude.TAG_INCLUDE))
			child = new IntroInclude(childElement, bundle);
		else if (childElement.getNodeName().equalsIgnoreCase(
			IntroAnchor.TAG_ANCHOR))
			child = new IntroAnchor(childElement, bundle);
		else if (childElement.getNodeName().equalsIgnoreCase(
			IntroContentProvider.TAG_CONTENT_PROVIDER))
			child = new IntroContentProvider(childElement, bundle);
		return child;
	}


	/**
	 * Resolve each include in this container's children. Includes are lazily
	 * resolved on a per container basis, when the container is resolved.
	 */
	protected void resolveChildren() {
		AbstractIntroElement[] array = children.toArray(new AbstractIntroElement[children.size()]);
		for (int i=0;i<array.length;++i) {
			AbstractIntroElement child = array[i];
			if (UAContentFilter.isFiltered(UAElementFactory.newElement(child.getElement()), IntroEvaluationContext.getContext())) {
				children.remove(child);
			}
			else if (child.getType() == AbstractIntroElement.INCLUDE) {
				resolveInclude((IntroInclude) child);
			}
		}
		resolved = true;
	}

	/**
	 * Resolves an include. Gets the intro element pointed to by the include,
	 * and adds it as a child of this current container. If target is not a
	 * group, or any element that can be included in a group, ignore this
	 * include.
	 */
	private void resolveInclude(IntroInclude include) {
		AbstractIntroElement target = findIncludeTarget(include);
		if (target == null)
			// target could not be found.
			return;
		if (target.isOfType(AbstractIntroElement.GROUP
				| AbstractIntroElement.ABSTRACT_TEXT
				| AbstractIntroElement.IMAGE | AbstractIntroElement.TEXT
				| AbstractIntroElement.PAGE_TITLE))
			// be picky about model elements to include. Can not use
			// BASE_ELEMENT model class because pages can not be included.
			insertTarget(include, target);
	}

	/**
	 * Filters the appropriate elements from the given Vector, according to the current
	 * environment. For example, if one of the elements has a tag to filter for os=linux and
	 * the os is win32, the element will not be returned in the resulting Vector.
	 *
	 * @param unfiltered the unfiltered elements
	 * @return a new Vector with elements filtered
	 */
	private <T> Vector<T> filterChildren(Vector<T> unfiltered) {
		Vector<T> filtered = new Vector<>();
		Iterator<T> iter = unfiltered.iterator();
		while (iter.hasNext()) {
			T element = iter.next();
			if (!UAContentFilter.isFiltered(element, IntroEvaluationContext.getContext())) {
				filtered.add(element);
			}
		}
		return filtered;
	}

	/**
	 * Find the target element pointed to by the path in the include. It is
	 * assumed that configId always points to an external config, and not the
	 * same config of the include.
	 */
	private AbstractIntroElement findIncludeTarget(IntroInclude include) {
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
		AbstractIntroElement target = findTarget(targetModelRoot, path);
		return target;
	}

	/**
	 * Finds the child element that corresponds to the given path in the passed
	 * model.<br>
	 * ps: This method could be a static method, but left as instance for model
	 * enhancements.
	 */
	public AbstractIntroElement findTarget(AbstractIntroContainer container,
			String path) {
		// extract path segments. Get first segment to start search.
		String[] pathSegments = path.split("/"); //$NON-NLS-1$
		if (container == null)
			return null;

		AbstractIntroElement target = container.findChild(pathSegments[0]);
		if (target == null)
			// there is no direct child with the specified first path segment.
			return null;

		// found parent segment. now find each child segment.
		for (int i = 1; i < pathSegments.length; i++) {
			if (!(target instanceof AbstractIntroContainer)) {
				// parent is not a container, so no point going on.
				return null;
			}
			String pathSegment = pathSegments[i];
			target = ((AbstractIntroContainer) target).findChild(pathSegment);
			if (target == null)
				// tried to find next segment and failed.
				return null;
		}
		return target;
	}

	public AbstractIntroElement findTarget(AbstractIntroContainer container,
			String path, String extensionId) {
		// resolve path segments if they are incomplete.
		if (path.contains("@")) { //$NON-NLS-1$
			// new in 3.2: dynamic resolution of incomplete target paths
			IntroModelRoot root = getModelRoot();
			if (root!=null) {
				path = root.resolvePath(extensionId, path);
				if (path==null)
					return null;
			}

		}
		return this.findTarget(container, path);
	}


	public AbstractIntroElement findTarget(String path) {
		return findTarget(this, path);
	}



	/*
	 * searches direct children for the first child with the given id. The type
	 * of the child can be any model element that has an id. ie:
	 * AbstractIntroIdElement
	 *
	 * @see org.eclipse.ui.internal.intro.impl.model.IntroElement#getType()
	 */
	public AbstractIntroElement findChild(String elementId) {
		return findChild(elementId, ID_ELEMENT);
	}

	/*
	 * searches direct children for the first child with the given id. The type
	 * of the child must be of the passed model types mask. This method handles
	 * the 3.0 style model for content. Pages enhance this behavior with DOM
	 * apis.
	 *
	 * @see org.eclipse.ui.internal.intro.impl.model.IntroElement#getType()
	 */
	public AbstractIntroElement findChild(String elementId, int elementMask) {
		if (!loaded)
			loadChildren();

		for (int i = 0; i < children.size(); i++) {
			AbstractIntroElement aChild = children.elementAt(i);
			if (!aChild.isOfType(ID_ELEMENT))
				// includes and heads do not have ids, and so can not be
				// referenced directly. This means that they can not be
				// targets for other includes. Skip, just in case someone
				// adds an id to it! Also, this applies to all elements in
				// the model that do not have ids.
				continue;
			AbstractIntroIdElement child = (AbstractIntroIdElement) aChild;
			if (child.getId() != null && child.getId().equals(elementId)
					&& child.isOfType(elementMask))
				return child;
		}
		// no child with given id and type found.
		return null;
	}



	private void insertTarget(IntroInclude include, AbstractIntroElement target) {
		int includeLocation = children.indexOf(include);
		if (includeLocation == -1)
			// should never be here.
			return;
		children.remove(includeLocation);
		// handle merging target styles first, before changing target parent to
		// enable inheritance of styles.
		handleIncludeStyleInheritence(include, target);
		// now clone the target node because original model should be kept
		// intact.
		AbstractIntroElement clonedTarget = null;
		try {
			clonedTarget = (AbstractIntroElement) target.clone();
		} catch (CloneNotSupportedException ex) {
			// should never be here.
			Log.error("Failed to clone Intro model node.", ex); //$NON-NLS-1$
			return;
		}
		// set parent of cloned target to be this container.
		clonedTarget.setParent(this);
		children.insertElementAt(clonedTarget, includeLocation);
	}

	/**
	 * Updates the inherited styles based on the merge-style attribute. If we
	 * are including a shared group, or if we are including an element from the
	 * same page, do nothing. For inherited alt-styles, we have to cache the pd
	 * from which we inherited the styles to be able to access resources in that
	 * plugin. Also note that when including a container, it must be resolved
	 * otherwise reparenting will cause includes in this target container to
	 * fail.
	 */
	private void handleIncludeStyleInheritence(IntroInclude include,
			AbstractIntroElement target) {

		if (!include.getMergeStyle())
			// target styles are not needed. nothing to do.
			return;

		if (target.getParent().getType() == AbstractIntroElement.MODEL_ROOT
				|| target.getParentPage().equals(include.getParentPage()))
			// If we are including from this same page ie: target is in the
			// same page, OR if we are including a shared group, defined
			// under a config, do not include styles.
			return;

		// Update the parent page styles. skip style if it is null. Note,
		// include both the target page styles and inherited styles. The full
		// page styles need to be include.
		String style = target.getParentPage().getStyle();
		if (style != null)
			getParentPage().addStyle(style);

		// for alt-style cache bundle for loading resources.
		style = target.getParentPage().getAltStyle();
		if (style != null) {
			Bundle bundle = target.getBundle();
			getParentPage().addAltStyle(style, bundle);
		}

		// now add inherited styles. Race condition could happen here if Page A
		// is including from Page B which is in turn including from Page A.
		getParentPage().addStyles(target.getParentPage().getStyles());
		getParentPage().addAltStyles(target.getParentPage().getAltStyles());

	}

	/**
	 * Creates a clone of the given target node. A clone is create by simply
	 * recreating that portion of the model.
	 *
	 * Note: looked into the clonable interface in Java, but it was not used
	 * because it makes modifications/additions to the model harder to maintain.
	 * Will revisit later.
	 */
	protected AbstractIntroElement cloneTarget(AbstractIntroElement target) {
		return null;
	}

	@Override
	public int getType() {
		return AbstractIntroElement.ABSTRACT_CONTAINER;
	}

	/**
	 * Deep copy since class has mutable objects. Leave DOM element as a shallow
	 * reference copy since DOM is immutable.
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		AbstractIntroContainer clone = (AbstractIntroContainer) super.clone();
		clone.children = new Vector<>();
		if (children != null) {
			for (int i = 0; i < children.size(); i++) {
				AbstractIntroElement cloneChild = (AbstractIntroElement) children
					.elementAt(i).clone();
				cloneChild.setParent(clone);
				clone.children.add(i, cloneChild);
			}
		}
		return clone;
	}

	/**
	 * Returns the element.
	 */
	@Override
	public Element getElement() {
		return this.element;
	}

	public String getBase() {
		return base;
	}


	/*
	 * Clears this container. This means emptying the children, and resetting
	 * flags.
	 */
	public void clearChildren() {
		this.children.clear();
	}


	/**
	 * Adds a model element as a child. Caller is responsible for inserting
	 * model elements that rea valid as children.
	 */
	public void addChild(AbstractIntroElement child) {
		children.add(child);
	}

	public void removeChild(AbstractIntroElement child) {
		children.remove(child);
	}

	public String getBackgroundImage() {
		return getAttribute(element, ATT_BG_IMAGE);
	}


}