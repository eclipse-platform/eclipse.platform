/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.search;

import org.eclipse.update.core.*;

/**
 * Classes that implement this interface can be used to filter the
 * results of the update search.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 3.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IUpdateSearchFilter {
	/**
	 * Tests a feature according to this filter's criteria.
	 * @param match the feature to test
	 * @return <samp>true</samp> if the feature has been accepted, <samp>false</samp> otherwise.
     * @deprecated In 3.1 only the accept (IFeatureReference) will be used
	 */
	boolean accept(IFeature match);

	/**
	 * Tests a feature reference according to this filter's criteria. 
	 * This is a prefilter that allows rejecting a feature before a potentially lengthy download.
	 * @param match the feature reference to test
	 * @return <samp>true</samp> if the feature reference has been accepted, <samp>false</samp> otherwise.
	 */
	boolean accept(IFeatureReference match);
}
