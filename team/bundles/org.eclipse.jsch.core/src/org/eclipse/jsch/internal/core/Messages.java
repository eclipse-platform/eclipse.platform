/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
package org.eclipse.jsch.internal.core;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS{
  private static final String BUNDLE_NAME="org.eclipse.jsch.internal.core.messages";//$NON-NLS-1$

  public static String JSchSession_5;
  public static String Util_timeout;

  static{
    // load message values from bundle file
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }
}