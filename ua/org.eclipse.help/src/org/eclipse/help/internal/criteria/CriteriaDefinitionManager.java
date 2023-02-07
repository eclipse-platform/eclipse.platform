/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
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
package org.eclipse.help.internal.criteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.AbstractCriteriaDefinitionProvider;
import org.eclipse.help.ICriteriaDefinition;
import org.eclipse.help.ICriteriaDefinitionContribution;
import org.eclipse.help.ICriterionDefinition;
import org.eclipse.help.ICriterionValueDefinition;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.UAElementFactory;

public class CriteriaDefinitionManager {

	private static final String EXTENSION_POINT_ID_CRITERIA_DEFINITION = HelpPlugin.PLUGIN_ID + ".criteriaDefinition"; //$NON-NLS-1$
	private static final String ELEMENT_NAME_CRITERIA_DEFINITION_PROVIDER = "criteriaDefinitionProvider"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME_CLASS = "class"; //$NON-NLS-1$

	private Map<String, CriteriaDefinitionContribution[]> criteriaDefinitionContributionsByLocale = new HashMap<>();
	private Map<String, CriteriaDefinition> criteriaDefinitionsByLocale = new HashMap<>();
	private AbstractCriteriaDefinitionProvider[] criteriaDefinitionProviders;

	public synchronized ICriteriaDefinition getCriteriaDefinition(String locale) {
		CriteriaDefinition criteriaDefinition = criteriaDefinitionsByLocale.get(locale);
		if (null == criteriaDefinition) {
			HelpPlugin.getTocManager().getTocs(locale);
			long start = System.currentTimeMillis();
			if (HelpPlugin.DEBUG_CRITERIA) {
				System.out.println("Start to update criteria definition for locale " + locale); //$NON-NLS-1$
			}
			List<CriteriaDefinitionContribution> contributions = new ArrayList<>(
					Arrays.asList(readCriteriaDefinitionContributions(locale)));
			CriteriaDefinitionAssembler assembler = new CriteriaDefinitionAssembler();
			criteriaDefinition = assembler.assemble(contributions);
			criteriaDefinitionsByLocale.put(locale, criteriaDefinition);
			long stop = System.currentTimeMillis();
			if (HelpPlugin.DEBUG_CRITERIA) {
				System.out.println("Milliseconds to update criteria definition for locale " + locale +  " = " + (stop - start)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return criteriaDefinition;
	}

	/*
	 * Returns all criteria definition contributions for the given locale, from all providers.
	 */
	public synchronized CriteriaDefinitionContribution[] getCriteriaDefinitionContributions(String locale) {
		CriteriaDefinitionContribution[] contributions = criteriaDefinitionContributionsByLocale.get(locale);
		if (contributions == null) {
			contributions = readCriteriaDefinitionContributions(locale);
			criteriaDefinitionContributionsByLocale.put(locale, contributions);
		}
		return contributions;
	}

	private CriteriaDefinitionContribution[] readCriteriaDefinitionContributions(String locale) {
		CriteriaDefinitionContribution[] cached;
		List<CriteriaDefinitionContribution> contributions = new ArrayList<>();
		AbstractCriteriaDefinitionProvider[] providers = getCriteriaDefinitionProviders();
		for (int i=0;i<providers.length;++i) {
			ICriteriaDefinitionContribution[] contrib;
			try {
				contrib = providers[i].getCriteriaDefinitionContributions(locale);
			}
			catch (Throwable t) {
				// log, and skip the offending provider
				String msg = "Error getting help criteria definition data from provider: " + providers[i].getClass().getName() + " (skipping provider)"; //$NON-NLS-1$ //$NON-NLS-2$
				Platform.getLog(getClass()).error(msg, t);
				continue;
			}

			// check for nulls and root element
			for (int j=0;j<contrib.length;++j) {
				if (contrib[j] == null) {
					String msg = "Help criteria definition provider \"" + providers[i].getClass().getName() + "\" returned a null contribution (skipping)"; //$NON-NLS-1$ //$NON-NLS-2$
					Platform.getLog(getClass()).error(msg);
				}
				else if (contrib[j].getCriteriaDefinition() == null) {
					String msg = "Help criteria definition provider \"" + providers[i].getClass().getName() + "\" returned a contribution with a null root element (expected a \"" + CriteriaDefinition.NAME + "\" element; skipping)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					Platform.getLog(getClass()).error(msg);
				}
				else {
					CriteriaDefinitionContribution contribution = new CriteriaDefinitionContribution();
					contribution.setId(contrib[j].getId());
					contribution.setLocale(contrib[j].getLocale());
					ICriteriaDefinition criteria = contrib[j].getCriteriaDefinition();
					contribution.setCriteriaDefinition(criteria instanceof CriteriaDefinition ? (CriteriaDefinition)criteria  : (CriteriaDefinition)UAElementFactory.newElement(criteria));
					contributions.add(contribution);
				}
			}
		}
		cached = contributions.toArray(new CriteriaDefinitionContribution[contributions.size()]);
		return cached;
	}

	/*
	 * Clears all cached contributions, forcing the manager to query the
	 * providers again next time a request is made.
	 */
	public void clearCache() {
		criteriaDefinitionContributionsByLocale.clear();
		criteriaDefinitionsByLocale.clear();
	}

	/*
	 * Internal hook for unit testing.
	 */
	public AbstractCriteriaDefinitionProvider[] getCriteriaDefinitionProviders() {
		if (null == criteriaDefinitionProviders) {
			List<AbstractCriteriaDefinitionProvider> providers = new ArrayList<>();
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IConfigurationElement[] elements = registry.getConfigurationElementsFor(EXTENSION_POINT_ID_CRITERIA_DEFINITION);
			for (int i=0;i<elements.length;++i) {
				IConfigurationElement elem = elements[i];
				if (elem.getName().equals(ELEMENT_NAME_CRITERIA_DEFINITION_PROVIDER)) {
					try {
						AbstractCriteriaDefinitionProvider provider = (AbstractCriteriaDefinitionProvider)elem.createExecutableExtension(ATTRIBUTE_NAME_CLASS);
						providers.add(provider);
					}
					catch (CoreException e) {
						// log and skip
						String msg = "Error instantiating help keyword index provider class \"" + elem.getAttribute(ATTRIBUTE_NAME_CLASS) + '"'; //$NON-NLS-1$
						Platform.getLog(getClass()).error(msg, e);
					}
				}
			}
			criteriaDefinitionProviders = providers.toArray(new AbstractCriteriaDefinitionProvider[providers.size()]);
		}
		return criteriaDefinitionProviders;
	}

	public boolean isCriteriaDefinitionLoaded(String locale) {
		return criteriaDefinitionsByLocale.get(locale) != null;
	}

	/*
	 * Internal hook for unit testing.
	 */
	public void setCriteriaDefinitionProviders(AbstractCriteriaDefinitionProvider[] criteriaDefinitionProviders) {
		this.criteriaDefinitionProviders = criteriaDefinitionProviders;
	}

	public String getCriterionName(String id, String locale) {
		ICriteriaDefinition definition = getCriteriaDefinition(locale);
		ICriterionDefinition[] criterionDefinitions = definition.getCriterionDefinitions();
		for(int i = 0; i < criterionDefinitions.length; i++) {
			CriterionDefinition criterionDefinition = (CriterionDefinition) criterionDefinitions[i];
			if(null != criterionDefinition.getId() && criterionDefinition.getId().equalsIgnoreCase(id)){
				String name = criterionDefinition.getName();
				if(null != name && 0 != name.length()) {
					return name;
				}
			}
		}
		return id;
	}

	public String getCriterionValueName(String criterionId, String criterionValueId, String locale) {
		ICriteriaDefinition definition = getCriteriaDefinition(locale);
		ICriterionDefinition[] criterionDefinitions = definition.getCriterionDefinitions();
		for(int i = 0; i < criterionDefinitions.length; ++ i) {
			CriterionDefinition criterionDefinition = (CriterionDefinition) criterionDefinitions[i];
			if(null != criterionDefinition.getId() && criterionDefinition.getId().equalsIgnoreCase(criterionId)){
				ICriterionValueDefinition[] valueDefinitions = criterionDefinition.getCriterionValueDefinitions();
				for(int j = 0; j < valueDefinitions.length; ++ j){
					CriterionValueDefinition valueDefinition = (CriterionValueDefinition) valueDefinitions[j];
					if(null != valueDefinition.getId() && valueDefinition.getId().equals(criterionValueId)){
						String name = valueDefinition.getName();
						if(null != name && 0 != name.length()) {
							return name;
						}
					}
				}
			}
		}
		return criterionValueId;
	}
}
