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
import org.eclipse.core.internal.expressions.ExpressionStatus;
import org.eclipse.core.internal.expressions.Expressions;
import org.eclipse.core.internal.expressions.Property;
import org.eclipse.core.internal.expressions.TypeExtensionManager;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

/**
 * @since 3.7
 */
public class TestExpression extends Expression {

	private String fNamespace;
	private String fProperty;
	private Object[] fArgs;
	private Object fExpectedValue;
	private boolean fForcePluginActivation;

	private static final char PROP_SEP = '.';
	private static final String ATT_PROPERTY= "property"; //$NON-NLS-1$
	private static final String ATT_ARGS= "args"; //$NON-NLS-1$
	private static final String ATT_FORCE_PLUGIN_ACTIVATION= "forcePluginActivation"; //$NON-NLS-1$
	/**
	 * The seed for the hash code for all test expressions.
	 */
	private static final int HASH_INITIAL= TestExpression.class.getName().hashCode();

	private static final TypeExtensionManager fgTypeExtensionManager= new TypeExtensionManager("propertyTesters"); //$NON-NLS-1$

	public TestExpression(IConfigurationElement element) throws CoreException {
		String property= element.getAttribute(ATT_PROPERTY);
		int pos= property.lastIndexOf(PROP_SEP);
		if (pos == -1) {
			throw new CoreException(new ExpressionStatus(
				ExpressionStatus.NO_NAMESPACE_PROVIDED,
				ExpressionMessages.TestExpression_no_name_space));
		}
		fNamespace= property.substring(0, pos);
		fProperty= property.substring(pos + 1);
		fArgs= Expressions.getArguments(element, ATT_ARGS);
		fExpectedValue= Expressions.convertArgument(element.getAttribute(ATT_VALUE));
		fForcePluginActivation= Expressions.getOptionalBooleanAttribute(element, ATT_FORCE_PLUGIN_ACTIVATION);
	}

	public TestExpression(Element element) throws CoreException {
		String property= element.getAttribute(ATT_PROPERTY);
		int pos= property.lastIndexOf(PROP_SEP);
		if (pos == -1) {
			throw new CoreException(new ExpressionStatus(
				ExpressionStatus.NO_NAMESPACE_PROVIDED,
				ExpressionMessages.TestExpression_no_name_space));
		}
		fNamespace= property.substring(0, pos);
		fProperty= property.substring(pos + 1);
		fArgs= Expressions.getArguments(element, ATT_ARGS);
		String value = element.getAttribute(ATT_VALUE);
		fExpectedValue = Expressions.convertArgument(value.isEmpty() ? null : value);
		fForcePluginActivation= Expressions.getOptionalBooleanAttribute(element, ATT_FORCE_PLUGIN_ACTIVATION);
	}

	public TestExpression(String namespace, String property, Object[] args, Object expectedValue) {
		this(namespace, property, args, expectedValue, false);
	}

	public TestExpression(String namespace, String property, Object[] args, Object expectedValue, boolean forcePluginActivation) {
		Assert.isNotNull(namespace);
		Assert.isNotNull(property);
		fNamespace= namespace;
		fProperty= property;
		fArgs= args != null ? args : Expressions.EMPTY_ARGS;
		fExpectedValue= expectedValue;
		fForcePluginActivation= forcePluginActivation;
	}

	@Override
	public EvaluationResult evaluate(IEvaluationContext context) throws CoreException {
		Object element= context.getDefaultVariable();
		if (System.class.equals(element)) {
			String str= System.getProperty(fProperty);
			if (str == null)
				return EvaluationResult.FALSE;
			return EvaluationResult.valueOf(str.equals(fArgs[0]));
		}
		Property property= fgTypeExtensionManager.getProperty(element, fNamespace, fProperty, context.getAllowPluginActivation() && fForcePluginActivation);
		if (!property.isInstantiated())
			return EvaluationResult.NOT_LOADED;
		return EvaluationResult.valueOf(property.test(element, fArgs, fExpectedValue));
	}

	@Override
	public void collectExpressionInfo(ExpressionInfo info) {
		info.markDefaultVariableAccessed();
		info.addAccessedPropertyName(fNamespace + PROP_SEP + fProperty);
	}

	@Override
	public boolean equals(final Object object) {
		if (!(object instanceof TestExpression))
			return false;

		final TestExpression that= (TestExpression)object;
		return this.fNamespace.equals(that.fNamespace) && this.fProperty.equals(that.fProperty)
			&& this.fForcePluginActivation == that.fForcePluginActivation
			&& equals(this.fArgs, that.fArgs) && equals(this.fExpectedValue, that.fExpectedValue);
	}

	@Override
	protected int computeHashCode() {
		return HASH_INITIAL * HASH_FACTOR + hashCode(fArgs)
			* HASH_FACTOR + hashCode(fExpectedValue)
			* HASH_FACTOR + fNamespace.hashCode()
			* HASH_FACTOR + fProperty.hashCode()
			* HASH_FACTOR + (fForcePluginActivation ? 1 : 0);
	}

	//---- Debugging ---------------------------------------------------

	@Override
	public String toString() {
		StringBuilder args= new StringBuilder();
		for (int i= 0; i < fArgs.length; i++) {
			Object arg= fArgs[i];
			if (arg instanceof String) {
				args.append('\'');
				args.append(arg);
				args.append('\'');
			} else {
				args.append(arg.toString());
			}
			if (i < fArgs.length - 1)
				args.append(", "); //$NON-NLS-1$
		}
		return "<test property=\"" + fProperty +  "\"" +//$NON-NLS-1$ //$NON-NLS-2$
		(fArgs.length != 0 ? " args=\"" + args + "\"" : "") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		 (fExpectedValue != null ? " value=\"" + fExpectedValue + "\"" : "") + //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		  " plug-in activation: " + (fForcePluginActivation ? "eager" : "lazy") +   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
		  "/>"; //$NON-NLS-1$
	}

	//---- testing ---------------------------------------------------

	public boolean testGetForcePluginActivation() {
		return fForcePluginActivation;
	}

	public static TypeExtensionManager testGetTypeExtensionManager() {
		return fgTypeExtensionManager;
	}
}
