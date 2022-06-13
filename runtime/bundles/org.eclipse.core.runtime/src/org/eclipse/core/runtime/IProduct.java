/*******************************************************************************
 *  Copyright (c) 2004, 2007 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime;

import org.osgi.framework.Bundle;

/**
 * Products are the Eclipse unit of branding.  From the runtime point of view they have
 * a name, id and description and identify the Eclipse application to run.
 * <p>
 * Since the bulk of the branding related information is
 * specific to the UI, products also carry an arbitrary set of properties.  The valid set of
 * key-value pairs and their interpretation defined by the UI of the target environment.
 * For example, in the standard Eclipse UI, <code>org.eclipse.ui.branding.IProductConstants</code>
 * the properties of interest to the UI.  Other clients may specify additional properties.
 * </p><p>
 * Products can be defined directly using extensions to the <code>org.eclipse.core.runtime.products</code>
 * extension point or by using facilities provided by IProductProvider implementations.
 * </p><p>
 * For readers familiar with Eclipse 2.1 and earlier, products are roughly equivalent to
 * <i>primary features</i>.
 * </p>
 *
 * @see IProductProvider
 * @since 3.0
 */
public interface IProduct {
	/**
	 * Returns the application associated with this product.  This information is used
	 * to guide the runtime as to what application extension to create and execute.
	 *
	 * @return this product's application or <code>null</code> if none
	 */
	public String getApplication();

	/**
	 * Returns the name of this product.  The name is typically used in the title
	 * bar of UI windows.
	 *
	 * @return the name of this product or <code>null</code> if none
	 */
	public String getName();

	/**
	 * Returns the text description of this product
	 *
	 * @return the description of this product or <code>null</code> if none
	 */
	public String getDescription();

	/** Returns the unique product id of this product.
	 *
	 * @return the id of this product
	 */
	public String getId();

	/**
	 * Returns the property of this product with the given key.
	 * <code>null</code> is returned if there is no such key/value pair.
	 *
	 * @param key the name of the property to return
	 * @return the value associated with the given key or <code>null</code> if none
	 */
	public String getProperty(String key);

	/**
	 * Returns the bundle which is responsible for the definition of this product.
	 * Typically this is used as a base for searching for images and other files
	 * that are needed in presenting the product.
	 *
	 * @return the bundle which defines this product or <code>null</code> if none
	 */
	public Bundle getDefiningBundle();
}
