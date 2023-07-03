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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.ui.internal.intro.impl.model.util.BundleUtil;
import org.osgi.framework.Bundle;
import org.w3c.dom.Element;

/**
 * An intro config component. All config components can get to their defining
 * config element or bundle depending from where the element was loaded.
 * <p>
 * Class Rules:
 * <ul>
 * <li>If an element does not appear as a child under any node, then that
 * element does not need a type to be defined.</li>
 * <li>Each subclass must ensure that it properly supports cloning. This means
 * that if a deep copy is needed, the subclass must override the base behavior
 * here.</li>
 * <li>if cloning is not needed, override clone method and throw an unsupported
 * cloning exception. For now, only pages and targets of includes are cloneable.
 * </li>
 * </ul>
 * <p>
 * Note: This is an abstract base class for all classes in the Intro Model. <br>
 * Clients are not expected to implement or subclass this class, or any of its
 * subclasses.
 */
public abstract class AbstractIntroElement implements Cloneable {

	/**
	 * Type constant which identifies an IntroModelRoot element.
	 */
	public static final int MODEL_ROOT = 1;

	/**
	 * Type constant which identifies an IntroPartPresentation element.
	 */
	public static final int PRESENTATION = 1 << 1;

	/**
	 * Type constant which identifies an IntroHomePage element.
	 */
	public static final int HOME_PAGE = 1 << 2;

	/**
	 * Type constant which identifies the IntroPage element.
	 */
	public static final int PAGE = 1 << 3;

	/**
	 * Type constant which identifies the AbstractIntroPage element.
	 */
	public static final int ABSTRACT_PAGE = HOME_PAGE | PAGE;

	/**
	 * Type constant which identifies an IntroDiv element.
	 */
	public static final int GROUP = 1 << 4;

	/**
	 * Type constant which identifies the AbstractIntroContainer element.
	 */
	public static final int ABSTRACT_CONTAINER = ABSTRACT_PAGE | GROUP
			| MODEL_ROOT;

	/**
	 * Type constant which identifies the IntroHtml element.
	 */
	public static final int HTML = 1 << 5;

	/**
	 * Type constant which identifies the IntroLink element.
	 */
	public static final int LINK = 1 << 6;

	/**
	 * Type constant which identifies the IntroImage element.
	 */
	public static final int IMAGE = 1 << 7;

	/**
	 * Type constant which identifies the IntroInclude element.
	 */
	public static final int INCLUDE = 1 << 8;

	/**
	 * Type constant which identifies the IntroText element.
	 */
	public static final int TEXT = 1 << 9;

	/**
	 * Type constant which identifies the IntroContainerExtension element.
	 */
	public static final int CONTAINER_EXTENSION = 1 << 10;

	/**
	 * Type constant which identifies the IntroHead element.
	 */
	public static final int HEAD = 1 << 11;

	/**
	 * Type constant which identifies the IntroHead element.
	 */
	public static final int PAGE_TITLE = 1 << 12;

	/**
	 * Type constant which identifies the IntroAnchor element.
	 */
	public static final int ANCHOR = 1 << 13;

	/**
	 * Type constant which identifies the IntroContentProvider element.
	 */
	public static final int CONTENT_PROVIDER = 1 << 14;

	/**
	 * Type constant which identifies the LaunchBarElement.
	 */
	public static final int LAUNCH_BAR = 1 << 15;

	/**
	 * Type constant which identifies the launch bar shortcut.
	 */
	public static final int LAUNCH_BAR_SHORTCUT = 1 << 16;

	/**
	 * Type constant which identifies am injected IFrame model element.
	 */
	public static final int INJECTED_IFRAME = 1 << 17;

	/**
	 * Type constant for the theme element.
	 */
	public static final int THEME = 1 << 18;

	/**
	 * Type constant for the hr element.
	 */
	public static final int HR = 1 << 19;


	/**
	 * Type constant which identifies the AbstractText element.
	 */
	public static final int ABSTRACT_TEXT = HTML | LINK | CONTENT_PROVIDER;

	/**
	 * Type constant which identifies the AbstractCommonIntroElement element.
	 */
	public static final int BASE_ELEMENT = ABSTRACT_CONTAINER | ABSTRACT_TEXT
			| IMAGE | TEXT | PAGE_TITLE;

	/**
	 * Type constant which identifies any element in the Intro Model which can
	 * have an id. Note: eventhough IntroStandbyContentPart has an id, it does
	 * not appear as a child in the model, and so it does not have a type.
	 */
	public static final int ID_ELEMENT = BASE_ELEMENT | ANCHOR;

	/**
	 * Type constant which identifies any element in the Intro Model.
	 */
	public static final int ELEMENT = ID_ELEMENT | CONTAINER_EXTENSION | HEAD
			| INCLUDE | PRESENTATION | LAUNCH_BAR | LAUNCH_BAR_SHORTCUT;



	private AbstractIntroElement parent;
	private Object cfgElement;
	private Bundle bundle;
	private String mixinStyle;


	/**
	 * Constructor used when model elements are being loaded from plugin.xml.
	 */
	AbstractIntroElement(IConfigurationElement element) {
		cfgElement = element;
		bundle = BundleUtil.getBundleFromConfigurationElement(element);
	}


	/**
	 * Constructor used when model elements are being loaded from an xml content
	 * file. Bundle is propagated down the model to enable resolving resources
	 * relative to the base of the bundle.
	 *
	 * @param element
	 * @param pd
	 */
	AbstractIntroElement(Element element, Bundle bundle) {
		this.cfgElement = element;
		this.bundle = bundle;
	}


	/**
	 * Constructor used when model elements are being loaded from an xml content
	 * file. Bundle AND base is propagated down the model to enable resolving
	 * resources relative to the xml content file. The base is set to point to
	 * the relative location of the parent folder that holds the content file.
	 * In the case of a configExtension, it is set to point to the relative
	 * position of the parent folder that holds the extension. Only when needed,
	 * the base field is stored in a model element. This saves memory.
	 *
	 * @param element
	 * @param pd
	 */
	AbstractIntroElement(Element element, Bundle bundle, String base) {
		this(element, bundle);
	}




	/**
	 * Returns the configuration element from which this intro element was
	 * loaded. In the case of extension, returns the configuration element of
	 * the defining extension.
	 *
	 * @return
	 */
	public IConfigurationElement getCfgElement() {
		return cfgElement instanceof IConfigurationElement?(IConfigurationElement)cfgElement:null;
	}

	public Element getElement() {
		return cfgElement instanceof Element?(Element)cfgElement:null;
	}

	/**
	 * DOM getAttribute retruns an empty string (not null) if attribute is not
	 * defined. Override this behavior to be consistent with Intro Model, and
	 * IConfiguration element.
	 *
	 * @param element
	 * @param att
	 * @return
	 */
	protected String getAttribute(Element element, String att) {
		if (element.hasAttribute(att)) {
			String value = element.getAttribute(att);
			if (value!=null) {
				IntroModelRoot root = getModelRoot();
				if (root!=null)
					return root.resolveVariables(value);
				return value;
			}
		}
		return null;
	}

	/**
	 * Util method to parse a comma separated list of values
	 *
	 * @param element
	 * @param att
	 * @return
	 */
	protected String[] getAttributeList(Element element, String att) {
		if (element.hasAttribute(att)) {
			String value = element.getAttribute(att);
			if (value!=null) {
				String[] splitValues = value.split(",");  //$NON-NLS-1$
				IntroModelRoot root = getModelRoot();
				if (root!=null) {
					for (int i = 0; i < splitValues.length; i++) {
						splitValues[i] = root.resolveVariables(splitValues[i]);
					}
				}
				return splitValues;
			}
		}
		/*
		if (element.hasAttribute(att))
			return element.getAttribute(att).split(","); //$NON-NLS-1$
			*/
		return null;
	}

	protected void loadFromParent() {
	}


	/**
	 * Returns the plugin descriptor of the plugin from which this intro element
	 * was loaded. In the case of extension, returns the plugin descriptor of
	 * the plugin defining the extension.
	 *
	 * @return
	 */
	public Bundle getBundle() {
		return bundle;
	}



	/**
	 * Returns the specific model type of this intro element. To be implemented
	 * by all subclasses.
	 *
	 * @return returns one of the model class types defined in this class.
	 */
	public abstract int getType();


	/**
	 * Returns the parent of this intro element.
	 * <p>
	 * Rules:
	 * <ul>
	 * <li>For the model root, it retruns null.</li>
	 * <li>For the introPart presentation it returns a model root.</li>
	 * <li>For Pages, it returns an intro model root.</li>
	 * <li>For all other elements, it retruns a subclass of abstract container.
	 * </li>
	 * <li>for divs that are children of configs (shared divs), it returns the
	 * holding model root.</li>
	 * <li>for Head elements that are children of Implementation elements
	 * (shared Heads), it returns the holding presentation element.</li>
	 * </ul>
	 *
	 * @return returns the parent of this intro element. Null only for model
	 *         root.
	 */
	public AbstractIntroElement getParent() {
		return parent;
	}

	/**
	 * @param parent
	 *            The parent to set.
	 */
	public void setParent(AbstractIntroElement parent) {
		this.parent = parent;
		if (parent!=null)
			loadFromParent();
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	/**
	 * Returns the parent page holding this intro element. For the model root
	 * and the introPart presentation it returns null. For Pages, it returns the
	 * page itself. For all other element, returns the holding page.
	 * <p>
	 * Exceptions:
	 * <ul>
	 * <li>for divs that are children of configs (shared divs), it returns
	 * null.</li>
	 * <li>for Head elements that are children of Implementation elements
	 * (shared Heads), it returns null.</li>
	 * </ul>
	 */
	public AbstractIntroPage getParentPage() {
		// return yourself if you are a page.
		if (isOfType(AbstractIntroElement.ABSTRACT_PAGE))
			return (AbstractIntroPage) this;

		AbstractIntroElement parent = getParent();
		if (parent == null)
			return null;

		while (parent != null && parent.getParent() != null
				&& !parent.isOfType(AbstractIntroElement.ABSTRACT_PAGE))
			parent = parent.getParent();
		if (parent.isOfType(ABSTRACT_PAGE))
			return (AbstractIntroPage) parent;
		return null;
	}

	public IntroModelRoot getModelRoot() {
		// return yourself if you are a model root.
		if (isOfType(AbstractIntroElement.MODEL_ROOT))
			return (IntroModelRoot) this;

		AbstractIntroElement parent = getParent();
		if (parent == null)
			return null;

		while (parent != null && parent.getParent() != null
				&& !parent.isOfType(AbstractIntroElement.MODEL_ROOT))
			parent = parent.getParent();
		if (parent.isOfType(MODEL_ROOT))
			return (IntroModelRoot) parent;
		return null;
	}


	/**
	 * Returns whether the element is among the specified element types. An
	 * example of an element mask is as follows:
	 * <p>
	 * <code>
	 *  	int elementMask = IntroElement.ABSTRACT_CONTAINER;
	 * 		int elementMask = IntroElement.DIV | IntroElement.DEFAULT_LINK;
	 * </code>
	 *
	 * @param elementMask
	 *            element mask formed by bitwise OR of element type constants
	 *            defined in this class.
	 * @return <code>true</code> if this element has a matching type, and
	 *         <code>false</code> otherwise.
	 */
	public boolean isOfType(int elementMask) {
		return (getType() & elementMask) != 0;
	}

	/**
	 * Returns whether the types of all the elements in the given array are
	 * among the specified element types. <br>
	 * An example of an element mask is as follows:
	 * <p>
	 * <code>
	 * int elementMask = IntroElement.DIV | IntroElement.DEFAULT_LINK;
	 * </code>
	 *
	 * @return <code>true</code> if all elements are of the right type, and
	 *         <code>false</code> if the list is empty, or at least one
	 *         element is not of the specified types.
	 */
	public static final boolean allElementsAreOfType(
			AbstractIntroElement[] elements, int elementMask) {
		// if we have an empty list, no point going on.
		if (elements.length == 0)
			return false;

		for (int i = 0; i < elements.length; i++) {
			AbstractIntroElement element = elements[i];
			if (!element.isOfType(elementMask))
				return false;
		}
		return true;
	}

	/**
	 * Shallow copy. The design of cloning this model assumes that when a
	 * container is cloned, all its children must be cloned and reparented to
	 * it, hence one clone of this container object. This is why we have a
	 * shallow copy here.
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}



	public String getMixinStyle() {
		return mixinStyle;
	}



	public void setMixinStyle(String mixinStyle) {
		this.mixinStyle = mixinStyle;
	}



}