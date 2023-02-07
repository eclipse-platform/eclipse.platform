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
package org.eclipse.help.internal;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.ExpressionTagNames;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.dynamic.FilterResolver;
import org.eclipse.help.internal.entityresolver.LocalEntityResolver;
import org.eclipse.help.internal.util.ProductPreferences;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
/*
 * Base class for UA model elements.
 */
public class UAElement implements IUAElement {

	private static final String ELEMENT_FILTER = "filter"; //$NON-NLS-1$
	private static final String ATTRIBUTE_FILTER = "filter"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
	private static final String ATTRIBUTE_VALUE = "value"; //$NON-NLS-1$

	private static DocumentBuilder builder;
	private static Document document;

	private Element element;
	private UAElement parent;
	protected List<UAElement> children;
	private Filter[] filters;
	private Expression enablementExpression;
	private IUAElement src;

	private static class Filter {
		public Filter(String name, String value, boolean isNegated) {
			this.name = name;
			this.value = value;
			this.isNegated = isNegated;
		}
		String name;
		String value;
		boolean isNegated;
	}

	public UAElement(Element element) {
		this.element = element;
	}

	public UAElement(String name) {
		this.element = getDocument().createElement(name);
	}

	public UAElement(String name, IUAElement src) {
		this(name);
		if (src instanceof UAElement) {
			copyFilters(src);
		} else {
			this.src = src;
		}
	}

	private void copyFilters(IUAElement src) {
		UAElement sourceElement = (UAElement)src;
		String filter = sourceElement.getAttribute(ATTRIBUTE_FILTER);
		if (filter != null && filter.length() > 0) {
			this.setAttribute(ATTRIBUTE_FILTER, filter);
		}
		filters = sourceElement.getFilterElements();
		this.enablementExpression = sourceElement.enablementExpression;
		this.src = sourceElement.src;
	}

	private Filter[] getFilterElements() {
		if (filters == null) {
			List<Filter> list = new ArrayList<>();
			if (element.hasChildNodes()) {
				Node node = element.getFirstChild();
				while (node != null) {
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						String elementKind = node.getNodeName();
						if (ExpressionTagNames.ENABLEMENT.equals(elementKind)) {
							Element enablement = (Element)node;
							try {
								enablementExpression = ExpressionConverter.getDefault().perform(enablement);
							}
							catch (CoreException e) {

							}
						} else if (ELEMENT_FILTER.equals(elementKind)) {
							Element filter = (Element)node;
							String filterName = filter.getAttribute(ATTRIBUTE_NAME);
							String value = filter.getAttribute(ATTRIBUTE_VALUE);
							if (filterName.length() > 0 && value.length() > 0) {
								boolean isNegated = false;
								if (value.startsWith("!")) { //$NON-NLS-1$
									isNegated = true;
									value = value.substring(1);
								}
								if (filterName.length() > 0 && value.length() > 0) {
									list.add(new Filter(filterName, value, isNegated));
								}
							}
						}
					}
					node = node.getNextSibling();
				}
			}
			filters = list.toArray(new Filter[list.size()]);
		}
		return filters;
	}

	public void appendChild(UAElement uaElementToAppend) {
		importElement(uaElementToAppend);
		element.appendChild(uaElementToAppend.element);
		uaElementToAppend.parent = this;

		if (children != null) {
			children.add(uaElementToAppend);
		}
	}

	public void appendChildren(IUAElement[] children) {
		if (this.children == null && children.length > 0) {
			this.children = new ArrayList<>(4);
		}
		for (int i=0;i<children.length;i++) {
			appendChild(children[i] instanceof UAElement ? (UAElement)children[i] : UAElementFactory.newElement(children[i]));
		}
	}

	/*
	 * This method is synchronized to fix Bug 232169. When modifying this source be careful not
	 * to introduce any logic which could possibly cause this thread to block.
	 */
	synchronized public String getAttribute(String name) {
		String value = element.getAttribute(name);
		if (value != null && value.length() > 0) {
			return value;
		}
		return null;
	}

	/*
	 * This method is synchronized to fix Bug 230037. A review of the code indicated that there was no
	 * path which could get blocked and cause deadlock. When modifying this source be careful not
	 * to introduce any logic which could possibly cause this thread to block.
	 */
	@Override
	public synchronized IUAElement[] getChildren() {
		if (children == null) {
			if (element.hasChildNodes()) {
				children = new ArrayList<>(4);
				Node node = element.getFirstChild();
				while (node != null) {
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						UAElement uaElement = UAElementFactory.newElement((Element)node);
						if (uaElement != null) {
							uaElement.parent = this;
							children.add(uaElement);
						}
					}
					node = node.getNextSibling();
				}
			} else {
				return new UAElement[0];
			}
		}
		return children.toArray(new UAElement[children.size()]);
	}

	@SuppressWarnings("unchecked")
	public <T> T[] getChildren(Class<T> clazz) {
		IUAElement[] children = getChildren();
		if (children.length > 0) {
			List<Object> list = new ArrayList<>();
			for (int i=0;i<children.length;++i) {
				IUAElement child = children[i];
				if (clazz.isAssignableFrom(child.getClass())) {
					list.add(child);
				}
			}
			return list.toArray((T[]) Array.newInstance(clazz, list.size()));
		}
		return (T[]) Array.newInstance(clazz, 0);
	}

	public String getElementName() {
		return element.getNodeName();
	}

	private static Document getDocument() {
		if (document == null) {
			if (builder == null) {
				try {
					builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					builder.setEntityResolver(new LocalEntityResolver());
				}
				catch (ParserConfigurationException e) {
					String msg = "Error creating document builder"; //$NON-NLS-1$
					Platform.getLog(UAElement.class).error(msg, e);
				}
			}
			document = builder.newDocument();
		}
		return document;
	}

	public UAElement getParentElement() {
		return parent;
	}

	public void insertBefore(UAElement newChild, UAElement refChild) {
		importElement(newChild);
		element.insertBefore(newChild.element, refChild.element);
		newChild.parent = this;
		getChildren();
		if (children != null) {
			int index = children.indexOf(refChild);
			if (index < 0) {
				// cache is now invalid
				children = null;
			} else {
				children.add(index, newChild);
			}
		}
	}

	@Override
	public boolean isEnabled(IEvaluationContext context) {
		if (!ProductPreferences.useEnablementFilters()) {
			return true;
		}
		if (src != null) {
			return src.isEnabled(context);
		}
		String filter = getAttribute(ATTRIBUTE_FILTER);
		if (filter != null) {
			return isEnabledByFilterAttribute(filter);
		}
		Filter[] filterElements = getFilterElements();
		for (int i = 0; i < filterElements.length; i++) {
			if (!isFilterEnabled(filterElements[i])) {
				return false;
			}
		}
		if (enablementExpression != null) {
			try {
				return enablementExpression.evaluate(context) == EvaluationResult.TRUE;
			} catch (CoreException e) {
				return false;
			}
		}
		return true;
	}

	public void removeChild(UAElement elementToRemove) {

		element.removeChild(elementToRemove.element);
		elementToRemove.parent = null;

		if (children != null) {
			if (!children.remove(elementToRemove)) {
				// cache is now invalid
				children = null;
			}
		}
	}

	public void setAttribute(String name, String value) {
		element.setAttribute(name, value);
	}

	private void importElement(UAElement uaElementToImport) {
		Element elementToImport = uaElementToImport.element;
		Document ownerDocument = element.getOwnerDocument();
		if (!ownerDocument.equals(elementToImport.getOwnerDocument()) ) {
			elementToImport = (Element)ownerDocument.importNode(elementToImport, true);
			uaElementToImport.children = null;
		}  else {
			if (elementToImport.getParentNode() != null) {
				elementToImport = (Element)ownerDocument.importNode(elementToImport, true);
				uaElementToImport.children = null;
			} else {
			}
		}
		uaElementToImport.element = elementToImport;
	}

	private boolean isEnabledByFilterAttribute(String filter) {
		return !FilterResolver.getInstance().isFiltered(filter);
	}

	private boolean isFilterEnabled(Filter filter) {
		return !FilterResolver.getInstance().isFiltered(filter.name, filter.value, filter.isNegated);
	}

	public Element getElement() {
		return element;
	}

}
