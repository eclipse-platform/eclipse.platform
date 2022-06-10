/*******************************************************************************
 * Copyright (c) 2004, 2010 IBM Corporation and others.
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
package org.eclipse.core.internal.content;

/**
 * DO NOT REMOVE, used in a product.
 *
 * A content describer for XML files. This class provides basis for XML-based
 * content describers.
 * <p>
 * The document is detected by the describer as <code>VALID</code>, if it
 * contains an xml declaration with <code>&lt;?xml</code> prefix and the
 * encoding in the declaration is correct.
 * </p>
 * Below are sample declarations recognized by the describer as
 * <code>VALID</code>
 * <ul>
 * <li>&lt;?xml version="1.0"?&gt;</li>
 * <li>&lt;?xml version="1.0"</li>
 * <li>&lt;?xml version="1.0" encoding="utf-16"?&gt;</li>
 * <li>&lt;?xml version="1.0" encoding="utf-16?&gt;</li>
 * </ul>
 *
 * @noinstantiate This class is not intended to be instantiated by clients.
 *                Clients should use it to provide their own XML-based
 *                describers that can be referenced by the "describer"
 *                configuration element in extensions to the
 *                <code>org.eclipse.core.runtime.contentTypes</code> extension
 *                point.
 *
 * @see org.eclipse.core.runtime.content.IContentDescriber
 * @see "http://www.w3.org/TR/REC-xml *"
 *
 * @deprecated As of 3.5, replaced by {@link org.eclipse.core.runtime.content.XMLContentDescriber}
 */
@Deprecated
public class XMLContentDescriber extends org.eclipse.core.runtime.content.XMLContentDescriber {
}
