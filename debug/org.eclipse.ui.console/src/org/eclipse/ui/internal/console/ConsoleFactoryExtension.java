/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
package org.eclipse.ui.internal.console;

import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.ExpressionTagNames;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ResourceLocator;
import org.eclipse.ui.IPluginContribution;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsoleFactory;

/**
 * @since 3.1
 */
public class ConsoleFactoryExtension implements IPluginContribution {

	private IConfigurationElement fConfig;
	private Expression fEnablementExpression;
	private String fLabel;
	private ImageDescriptor fImageDescriptor;
	private IConsoleFactory fFactory;

	ConsoleFactoryExtension(IConfigurationElement config) {
		fConfig = config;
	}

	/**
	 *
	 * @return {@code true} if this is a "New Console" contribution
	 */
	public boolean isNewConsoleExtenson() {
		return ConsoleViewConsoleFactory.class.getName().equals(fConfig.getAttribute("class")); //$NON-NLS-1$
	}

	@Override
	public String getLocalId() {
		return fConfig.getAttribute("id"); //$NON-NLS-1$
	}

	@Override
	public String getPluginId() {
		return fConfig.getContributor().getName();
	}

	public boolean isEnabled() {
		try {
			Expression enablementExpression = getEnablementExpression();
			if (enablementExpression == null) {
				return true;
			}
			EvaluationContext context = new EvaluationContext(null, this);
			EvaluationResult evaluationResult = enablementExpression.evaluate(context);
			return evaluationResult != EvaluationResult.FALSE;
		} catch (CoreException e) {
			ConsolePlugin.log(e);
			return false;
		}
	}

	public Expression getEnablementExpression() throws CoreException {
		if (fEnablementExpression == null) {
			IConfigurationElement[] elements = fConfig.getChildren(ExpressionTagNames.ENABLEMENT);
			IConfigurationElement enablement = elements.length > 0 ? elements[0] : null;

			if (enablement != null) {
				fEnablementExpression = ExpressionConverter.getDefault().perform(enablement);
			}
		}
		return fEnablementExpression;
	}

	/**
	 * @return console label, never null
	 */
	public String getLabel() {
		if (fLabel == null) {
			fLabel = fConfig.getAttribute("label"); //$NON-NLS-1$
		}
		if (fLabel == null) {
			fLabel = "?"; //$NON-NLS-1$
		}
		return fLabel;
	}

	public ImageDescriptor getImageDescriptor() {
		if (fImageDescriptor == null) {
			String path = fConfig.getAttribute("icon"); //$NON-NLS-1$
			if (path != null) {
				fImageDescriptor = ResourceLocator.imageDescriptorFromBundle(fConfig.getNamespaceIdentifier(), path)
						.orElse(null);
			}
		}
		return fImageDescriptor;
	}

	public IConsoleFactory createFactory() throws CoreException {
		if (fFactory == null) {
			fFactory = (IConsoleFactory) fConfig.createExecutableExtension("class"); //$NON-NLS-1$
		}
		return fFactory;
	}
}
