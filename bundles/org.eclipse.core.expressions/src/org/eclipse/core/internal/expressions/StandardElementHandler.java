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
package org.eclipse.core.internal.expressions;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.eclipse.core.expressions.AndExpression;
import org.eclipse.core.expressions.CountExpression;
import org.eclipse.core.expressions.ElementHandler;
import org.eclipse.core.expressions.EqualsExpression;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.ExpressionTagNames;
import org.eclipse.core.expressions.OrExpression;
import org.eclipse.core.expressions.ReferenceExpression;
import org.eclipse.core.expressions.TestExpression;
import org.eclipse.core.expressions.WithExpression;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

public class StandardElementHandler extends ElementHandler {

	@Override
	public Expression create(ExpressionConverter converter, IConfigurationElement element) throws CoreException {
		String name= element.getName();
		if (name == null) {
			return null;
		}

		switch (name) {
		case ExpressionTagNames.INSTANCEOF:
			return new InstanceofExpression(element);
		case ExpressionTagNames.TEST:
			return new TestExpression(element);
		case ExpressionTagNames.OR:
		{
			OrExpression result= new OrExpression();
			processChildren(converter, element, result);
			return result;
		}
		case ExpressionTagNames.AND:
		{
			AndExpression result= new AndExpression();
			processChildren(converter, element, result);
			return result;
		}
		case ExpressionTagNames.NOT:
			return new NotExpression(converter.perform(element.getChildren()[0]));
		case ExpressionTagNames.WITH:
		{
			WithExpression result= new WithExpression(element);
			processChildren(converter, element, result);
			return result;
		}
		case ExpressionTagNames.ADAPT:
		{
			AdaptExpression result= new AdaptExpression(element);
			processChildren(converter, element, result);
			return result;
		}
		case ExpressionTagNames.ITERATE:
		{
			IterateExpression result= new IterateExpression(element);
			processChildren(converter, element, result);
			return result;
		}
		case ExpressionTagNames.COUNT:
			return new CountExpression(element);
		case ExpressionTagNames.SYSTEM_TEST:
			return new SystemTestExpression(element);
		case ExpressionTagNames.RESOLVE:
		{
			ResolveExpression result= new ResolveExpression(element);
			processChildren(converter, element, result);
			return result;
		}
		case ExpressionTagNames.ENABLEMENT:
		{
			EnablementExpression result= new EnablementExpression(element);
			processChildren(converter, element, result);
			return result;
		}
		case ExpressionTagNames.EQUALS:
			return new EqualsExpression(element);
		case ExpressionTagNames.REFERENCE:
			return new ReferenceExpression(element);
		default:
			break;
		}
		return null;
	}

	@Override
	public Expression create(ExpressionConverter converter, Element element) throws CoreException {
		String name= element.getNodeName();
		if (name == null) {
			return null;
		}

		switch (name) {
		case ExpressionTagNames.INSTANCEOF:
			return new InstanceofExpression(element);
		case ExpressionTagNames.TEST:
			return new TestExpression(element);
		case ExpressionTagNames.OR:
		{
			OrExpression result= new OrExpression();
			processChildren(converter, element, result);
			return result;
		}
		case ExpressionTagNames.AND:
		{
			AndExpression result= new AndExpression();
			processChildren(converter, element, result);
			return result;
		}
		case ExpressionTagNames.NOT:
			Node child = element.getFirstChild();
			while (child != null) {
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					return new NotExpression(converter.perform((Element)child));
				}
				child = child.getNextSibling();
			}
			break;
		case ExpressionTagNames.WITH:
		{
			WithExpression result= new WithExpression(element);
			processChildren(converter, element, result);
			return result;
		}
		case ExpressionTagNames.ADAPT:
		{
			AdaptExpression result= new AdaptExpression(element);
			processChildren(converter, element, result);
			return result;
		}
		case ExpressionTagNames.ITERATE:
		{
			IterateExpression result= new IterateExpression(element);
			processChildren(converter, element, result);
			return result;
		}
		case ExpressionTagNames.COUNT:
			return new CountExpression(element);
		case ExpressionTagNames.SYSTEM_TEST:
			return new SystemTestExpression(element);
		case ExpressionTagNames.RESOLVE:
		{
			ResolveExpression result= new ResolveExpression(element);
			processChildren(converter, element, result);
			return result;
		}
		case ExpressionTagNames.ENABLEMENT:
		{
			EnablementExpression result= new EnablementExpression(element);
			processChildren(converter, element, result);
			return result;
		}
		case ExpressionTagNames.EQUALS:
			return new EqualsExpression(element);
		case ExpressionTagNames.REFERENCE:
			return new ReferenceExpression(element);
		default:
			break;
		}
		return null;
	}
}
