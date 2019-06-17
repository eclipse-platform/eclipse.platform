/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.compatible;
import junit.framework.Test;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;

/**
 * @version 	1.0
 * @author 	${user}
 */
public class CompatibleTestSetup extends CVSTestSetup {
	public static final String ECLIPSE_REPOSITORY_LOCATION;
	public static final String REFERENCE_REPOSITORY_LOCATION;
	public static CVSRepositoryLocation referenceClientRepository;
	public static CVSRepositoryLocation eclipseClientRepository;
	
	static {
		REFERENCE_REPOSITORY_LOCATION = System.getProperty("eclipse.cvs.repository1");
		ECLIPSE_REPOSITORY_LOCATION = System.getProperty("eclipse.cvs.repository2");
	}	
	
	/**
	 * Constructor for CompatibleTestSetup.
	 */
	public CompatibleTestSetup(Test test) {
		super(test);
	}
	
	/**
	 * For compatibility testing, we need to set up two repositories
	 */
	@Override
	public void setUp() throws CVSException {
		CVSProviderPlugin.getPlugin().setPruneEmptyDirectories(false);
		CVSProviderPlugin.getPlugin().setFetchAbsentDirectories(false);

		// setup the repositories
		if (referenceClientRepository == null)
			referenceClientRepository = setupRepository(REFERENCE_REPOSITORY_LOCATION);
		if (eclipseClientRepository == null)
			eclipseClientRepository = setupRepository(ECLIPSE_REPOSITORY_LOCATION);
	}
	
	@Override
	public void tearDown() throws CVSException {
		CVSProviderPlugin.getPlugin().setPruneEmptyDirectories(true);
		CVSProviderPlugin.getPlugin().setFetchAbsentDirectories(true);
	}
}
