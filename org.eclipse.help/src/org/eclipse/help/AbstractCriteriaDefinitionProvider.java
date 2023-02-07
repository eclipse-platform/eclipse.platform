/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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
package org.eclipse.help;

/**
 * An <code>AbstractCriteriaDefinitionProvider</code> is a mechanism to provide display name
 * for criteria attached to toc or topic. <code>AbstractCriteriaDefinitionProvider</code>s must be
 * registered via the <code>org.eclipse.help.criteriaDefinition</code> extension point.
 *
 * @since 3.5
 */
public abstract class AbstractCriteriaDefinitionProvider {

	/**
	 * Returns all criteria definition contributions for this provider. Providers
	 * are free to provide any number of contributions (zero or more).
	 *
	 * @param locale the locale for which to get contributions
	 * @return all the criteria definition contributions for this provider
	 */
	public abstract ICriteriaDefinitionContribution[] getCriteriaDefinitionContributions(String locale);
}
