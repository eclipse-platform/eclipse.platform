/*******************************************************************************
 * Copyright (c) 2007, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.patch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * A patch configuration allows clients to set parameters that control how a
 * patch is applied.
 * 
 * @see IFilePatch2
 * @since 3.3
 * @noextend This class may be instantiated by clients but is not intended to be
 *           subclassed.
 */
public class PatchConfiguration {

	private int fStripPrefixSegments;
	private int fFuzz;
	private boolean fIgnoreWhitespace= false;
	private boolean fReverse= false;
	private HashMap properties = new HashMap();
	private List hunkFilters = new ArrayList();

	/**
	 * Return whether the patch should be reversed when applied.
	 * @return whether the patch should be reversed when applied
	 */
	public boolean isReversed() {
		return this.fReverse;
	}

	/**
	 * Set whether the patch should be reversed when applied.
	 * @param reversed whether the patch should be reversed when applied
	 */
	public void setReversed(boolean reversed) {
		this.fReverse = reversed;
	}

	/**
	 * Return the number of prefix segments to be stripped when attempting 
	 * to apply a patch.
	 * @return the number of prefix segments to be stripped when attempting 
	 * to apply a patch
	 */
	public int getPrefixSegmentStripCount() {
		return this.fStripPrefixSegments;
	}

	/**
	 * Set the number of prefix segments to be stripped when attempting 
	 * to apply a patch.
	 * @param stripCount the number of prefix segments to be stripped when attempting 
	 * to apply a patch.
	 */
	public void setPrefixSegmentStripCount(int stripCount) {
		this.fStripPrefixSegments = stripCount;
	}

	/**
	 * Return the fuzz factor to be used when applying a patch.
	 * If the fuzz factor is set to -1, then the patcher is to make a best
	 * effort to apply the patch by adjusting the fuzz factor
	 * accordingly.
	 * @return the fuzz factor to be used when applying a patch.
	 */
	public int getFuzz() {
		return this.fFuzz;
	}
	
	/**
	 * Set the fuzz factor to be used when applying a patch.
	 * @param fuzz the fuzz factor to be used when applying a patch.
	 */
	public void setFuzz(int fuzz) {
		this.fFuzz = fuzz;
	}

	/**
	 * Return whether whitespace should be ignored.
	 * @return whether whitespace should be ignored
	 */
	public boolean isIgnoreWhitespace() {
		return this.fIgnoreWhitespace;
	}
	
	/**
	 * Set whether whitespace should be ignored
	 * @param ignoreWhitespace whether whitespace should be ignored
	 */
	public void setIgnoreWhitespace(boolean ignoreWhitespace) {
		this.fIgnoreWhitespace = ignoreWhitespace;
	}
	
	/**
	 * Return the property associated with the given key or 
	 * <code>null</code> if there is no property for the key.
	 * @param key the key
	 * @return the property associated with the given key or 
	 * <code>null</code>
	 */
	public Object getProperty(String key) {
		return this.properties.get(key);
	}
	
	/**
	 * Set the property associated with the given key
	 * @param key the key
	 * @param value the value to be associated with the key
	 */
	public void setProperty(String key, Object value) {
		this.properties.put(key, value);
	}

	/**
	 * Adds a hunk filter.
	 * 
	 * @param filter the filter
	 * @since org.eclipse.compare.core 3.5
	 */
	public void addHunkFilter(IHunkFilter filter) {
		this.hunkFilters.add(filter);
	}

	/**
	 * Removes a hunk filter.
	 * 
	 * @param filter the filter
	 * @since org.eclipse.compare.core 3.5
	 */
	public void removeHunkFilter(IHunkFilter filter) {
		this.hunkFilters.remove(filter);
	}

	/**
	 * Return an array of hunk filters that have been added to this
	 * configuration.
	 * 
	 * @return an array of hunk filters that have been added to this configuration
	 * @since org.eclipse.compare.core 3.5
	 */
	public IHunkFilter[] getHunkFilters() {
		return (IHunkFilter[]) this.hunkFilters.toArray(new IHunkFilter[this.hunkFilters
				.size()]);
	}

}
