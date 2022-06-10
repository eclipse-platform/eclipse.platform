/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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

public interface ContentTypeVisitor {
	int CONTINUE = 0;
	int RETURN = 1;
	int STOP = 2;

	/**
	 * @return CONTINUE, RETURN or STOP
	 */
	int visit(ContentType contentType);
}
