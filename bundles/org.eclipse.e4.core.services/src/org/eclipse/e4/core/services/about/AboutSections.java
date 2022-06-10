/*******************************************************************************
 *  Copyright (c) 2019 ArSysOp and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      Alexander Fedorov <alexander.fedorov@arsysop.ru> - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.services.about;

/**
 * This class consists exclusively of static methods and constants that operate
 * on sections for "about" information.
 *
 * @since 2.2
 */
public final class AboutSections {

	/**
	 * The property to mark the information to be related with specific section
	 */
	public static final String SECTION = "section";

	/**
	 * The value to mark the information to be related with "Installed Bundles"
	 * section
	 */
	public static final String SECTION_INSTALLED_BUNDLES = "section.installed.bundles";

	/**
	 * The value to mark the information to be related with "Installed Features"
	 * section
	 */
	public static final String SECTION_INSTALLED_FEATURES = "section.installed.features";

	/**
	 * The value to mark the information to be related with "System Environment"
	 * section
	 */
	public static final String SECTION_SYSTEM_ENVIRONMENT = "section.system.environment";

	/**
	 * The value to mark the information to be related with "System Properties"
	 * section
	 */
	public static final String SECTION_SYSTEM_PROPERTIES = "section.system.properties";

	/**
	 * The value to mark the information to be related with "User Preferences"
	 * section
	 */
	public static final String SECTION_USER_PREFERENCES = "section.user.preferences";

	/**
	 * Creates a filter for a given section identifier to be used for a
	 * {@link ISystemInformation} service query:
	 *
	 * <pre>
	 * bundleContext.getServiceReferences(ISystemInformationService.class,
	 * 		AboutSections.createSectionFilter(AboutSections.SECTION_SYSTEM_ENVIRONMENT));
	 * </pre>
	 *
	 * @param section the section identifier to use as a filter
	 * @return the section filter string
	 */
	public static String createSectionFilter(String section) {
		return new StringBuilder().append('(').append(SECTION).append('=').append(section).append(')').toString();
	}

}
