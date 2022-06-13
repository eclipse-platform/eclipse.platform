/*******************************************************************************
 *  Copyright (c) 2008, 2010 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.services.contributions;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.osgi.framework.Bundle;

// TBD only used by Languages block, move it there?
public interface IContributionFactorySpi {

	Object create(Bundle bundle, String className, IEclipseContext context);

	Object call(Object object, String methodName, IEclipseContext context,
			Object defaultValue);
}
