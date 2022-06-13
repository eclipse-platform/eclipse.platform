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

import org.eclipse.core.internal.expressions.ExpressionMessages;
import org.eclipse.core.internal.expressions.StandardElementHandler;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;


/**
 * An element handler converts an {@link IConfigurationElement} into a
 * corresponding expression object.
 * <p>
 * The class should be subclassed by clients wishing to provide an element
 * handler for special expressions.
 * </p>
 * @since 3.0
 */
public abstract class ElementHandler {

	private static final ElementHandler INSTANCE= new StandardElementHandler();

	/**
	 * The default element handler which can cope with all XML expression elements
	 * defined by the common expression language.
	 *
	 * @return the default element handler
	 */
	public static ElementHandler getDefault() {
		return INSTANCE;
	}

	/**
	 * Creates the corresponding expression for the given configuration element.
	 *
	 * @param converter the expression converter used to initiate the
	 *  conversion process
	 *
	 * @param config the configuration element to convert
	 *
	 * @return the corresponding expression
	 *
	 * @throws CoreException if the conversion failed
	 */
	public abstract Expression create(ExpressionConverter converter, IConfigurationElement config) throws CoreException;

	/**
	 * Creates the corresponding expression for the given DOM element. This is
	 * an optional operation that is only required if the handler supports conversion
	 * of DOM elements.
	 *
	 * @param converter the expression converter used to initiate the
	 *  conversion process
	 *
	 * @param element the DOM element to convert
	 *
	 * @return the corresponding expression
	 *
	 * @throws CoreException if the conversion failed
	 *
	 * @since 3.3
	 */
	public Expression create(ExpressionConverter converter, Element element) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, ElementHandler.class,
				IStatus.ERROR,
				ExpressionMessages.ElementHandler_unsupported_element,
				null));
	}

	/**
	 * Converts the children of the given configuration element and adds them
	 * to the given composite expression.
	 * <p>
	 * Note this is an internal method and should not be called by clients.
	 * </p>
	 * @param converter the converter used to do the actual conversion
	 * @param element the configuration element for which the children
	 *  are to be processed
	 * @param expression the composite expression representing the result
	 *  of the conversion
	 *
	 * @throws CoreException if the conversion failed
	 *
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected void processChildren(ExpressionConverter converter, IConfigurationElement element, CompositeExpression expression) throws CoreException {
		converter.processChildren(element, expression);
	}

	/**
	 * Converts the children of the given DOM element and adds them to the
	 * given composite expression.
	 * <p>
	 * Note this is an internal method and should not be called by clients.
	 * </p>
	 * @param converter the converter used to do the actual conversion
	 * @param element the DOM element for which the children are to be processed
	 * @param expression the composite expression representing the result
	 *  of the conversion
	 *
	 * @throws CoreException if the conversion failed
	 *
	 * @since 3.3
	 *
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected void processChildren(ExpressionConverter converter, Element element, CompositeExpression expression) throws CoreException {
		converter.processChildren(element, expression);
	}
}
