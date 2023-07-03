/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.help.search;

/**
 * The generic search scope object. This is a tagging interface
 * since each search engine is expected to have its own non-overlapping
 * set of scopes that users can include in or exclude from the search.
 * Clients are expected to create scope objects that implement this
 * interface and pass them to the search engine.
 *
 * @since 3.1
 */
public interface ISearchScope {
}
