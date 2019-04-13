/*******************************************************************************
 * Copyright (c) 2007 JCraft,Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Atsuhiko Yamanaka, JCraft,Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jsch.core;

import org.eclipse.jsch.internal.core.IUserAuthenticator;

/**
 * This interface provides access to the specific portions of
 * the location string for use by ssh2 connection
 * and the user authenticator.
 * 
 * This interface is not intended to be implemented by clients.
 * @see IUserAuthenticator
 * @see IPasswordStore
 * @since 1.1
 */
public interface IJSchLocation{

  /**
   * port value which indicates to a connection method to use the default port
   */
  public static int USE_DEFAULT_PORT=0;

  /**
   * Returns the host where the repository is located
   * @return host name
   */
  public String getHost();

  /**
   * Returns the port to connect to or USE_DEFAULT_PORT if
   * the connection method is to use its default port.
   * @return port number
   */
  public int getPort();

  /**
   * Sets the user information used for this location
   * @param username user name
   */
  public void setUsername(String username);

  /**
   * Return the user name 
   * @return user name
   */
  public String getUsername();

  /**
   * Sets the user password used for this location
   * @param password password
   */
  public void setPassword(String password);

  /**
   * Return the password 
   * @return password
   */
  public String getPassword();

  /**
   * Sets the comment for this location.  This comment will be displayed
   * in prompting for the password.
   * @param comment Comment to be displayed in password prompt.
   */
  public void setComment(String comment);

  /**
   * Return the comment 
   * @return Comment to be displayed in password prompt.
   */
  public String getComment();

  /**
   * Sets the password store used for this location
   * @param store password store
   */
  public void setPasswordStore(IPasswordStore store);

  /**
   * Return the password store.
   * @return password store.
   */
  public IPasswordStore getPasswordStore();
}
