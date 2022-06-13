/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.core.expressions;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.eclipse.core.internal.expressions.ExpressionMessages;
import org.eclipse.core.internal.expressions.Messages;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * An expression converter converts an XML expression represented by an
 * {@link IConfigurationElement} or {@link Element} (DOM) subtree into a
 * corresponding expression tree.
 *
 * <p>
 * An expression converter manages a list of {@link ElementHandler}s. Element
 * handlers are responsible to do the actual conversion. The element handlers
 * build a chain of responsibility.
 * </p>
 *
 * @since 3.0
 */
public final class ExpressionConverter {

	private ElementHandler[] fHandlers;
	private static final ExpressionConverter INSTANCE = new ExpressionConverter(
			new ElementHandler[] { ElementHandler.getDefault() });

	/**
	 * Returns the default expression converter. The default expression converter
	 * can cope with all expression elements defined by the common expression
	 * language.
	 *
	 * @return the default expression converter
	 */
	public static ExpressionConverter getDefault() {
		return INSTANCE;
	}

	/**
	 * Creates a new expression converter with the given list of element
	 * handlers. The element handlers build a chain of responsibility
	 * meaning that the first handler in the list is first used to
	 * convert the configuration element. If this handler isn't able
	 * to convert the configuration element the next handler in the
	 * array is used.
	 *
	 * @param handlers the array  of element handlers
 	 */
	public ExpressionConverter(ElementHandler[] handlers) {
		Assert.isNotNull(handlers);
		fHandlers= handlers;
	}

	/**
	 * Converts the tree of configuration elements represented by the given
	 * root element and returns a corresponding expression tree.
	 *
	 * @param root the configuration element to be converted
	 *
	 * @return the corresponding expression tree or <code>null</code>
	 *  if the configuration element cannot be converted
	 *
	 * @throws CoreException if the configuration element can't be
	 *  converted. Reasons include: (a) no handler is available to
	 *  cope with a certain configuration element or (b) the XML
	 *  expression tree is malformed.
	 */
	public Expression perform(IConfigurationElement root) throws CoreException {
		for (ElementHandler handler : fHandlers) {
			Expression result= handler.create(this, root);
			if (result != null)
				return result;
		}
		return null;
	}

	/**
	 * Converts the tree of DOM elements represented by the given
	 * root element and returns a corresponding expression tree.
	 *
	 * @param root the element to be converted
	 *
	 * @return the corresponding expression tree or <code>null</code>
	 *  if the element cannot be converted
	 *
	 * @throws CoreException if the element can't be converted.
	 *  Reasons include: (a) no handler is available to cope with
	 *  a certain element or (b) the XML expression tree is malformed.
	 *
	 * @since 3.3
	 */
	public Expression perform(Element root) throws CoreException {
		for (ElementHandler handler : fHandlers) {
			Expression result= handler.create(this, root);
			if (result != null)
				return result;
		}
		return null;
	}

	/* package */ void processChildren(IConfigurationElement element, CompositeExpression result) throws CoreException {
		IConfigurationElement[] children= element.getChildren();
		if (children != null) {
			for (IConfigurationElement configElement : children) {
				Expression child= perform(configElement);
				if (child == null)
					throw new CoreException(new Status(IStatus.ERROR, ExpressionConverter.class,
						IStatus.ERROR,
						Messages.format(
							ExpressionMessages.Expression_unknown_element,
							getDebugPath(configElement)),
						null));
				result.add(child);
			}
		}
	}

	private String getDebugPath(IConfigurationElement configurationElement) {
		StringBuilder buf= new StringBuilder();
		buf.append(configurationElement.getName());
		Object parent= configurationElement.getParent();
		while (parent != null) {
			if (parent instanceof IConfigurationElement) {
				buf.append(" > "); //$NON-NLS-1$
				IConfigurationElement parent2= (IConfigurationElement) parent;
				buf.append(parent2.getName());
				String id= parent2.getAttribute("id"); //$NON-NLS-1$
				if (id != null)
					buf.append(" (id=").append(id).append(')'); //$NON-NLS-1$
				parent= parent2.getParent();
			} else if (parent instanceof IExtension) {
				IExtension parent2= (IExtension) parent;
				buf.append(" : "); //$NON-NLS-1$
				buf.append(parent2.getExtensionPointUniqueIdentifier());
				buf.append(" @ "); //$NON-NLS-1$
				buf.append(parent2.getContributor().getName());
				parent= null;
			} else {
				parent= null;
			}
		}
		return buf.toString();
	}

	/* package */ void processChildren(Element element, CompositeExpression result) throws CoreException {
		Node child = element.getFirstChild();
		while (child != null) {
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Expression exp= perform((Element)child);
				if (exp == null)
					throw new CoreException(new Status(IStatus.ERROR, ExpressionConverter.class,
						IStatus.ERROR,
						Messages.format(
							ExpressionMessages.Expression_unknown_element,
							child.getNodeName()),
						null));
				result.add(exp);
			}
			child = child.getNextSibling();
		}
	}
}
