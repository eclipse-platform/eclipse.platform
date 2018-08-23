/**********************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.jsch.internal.core;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS{
  private static final String BUNDLE_NAME="org.eclipse.jsch.internal.core.messages";//$NON-NLS-1$

  public static String JSchSession_5;
  public static String Util_timeout;
  public static String JSchAuthenticationException_detail;
  public static String JSchRepositoryLocation_locationForm;
  public static String JSchRepositoryLocation_invalidFormat;
  public static String KnownRepositories_0;
  public static String JSchRepositoryLocation_73;
  public static String JSchRepositoryLocation_74;
  public static String JSchRepositoryLocation_75;
  
  static{
    // load message values from bundle file
    NLS.initializeMessages(BUNDLE_NAME, Messages.class);
  }
}