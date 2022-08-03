/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
 *     Atsuhiko Yamanaka, JCraft,Inc. - copying this class from cvs.core plug-in
 *******************************************************************************/
package org.eclipse.jsch.internal.core;
/**
 * Instances of this class represent a user name password pair.
 * Both values can be set and the user name can be retrieved.
 * However, it is possible that the user name is not mutable.
 * Users must check before trying to set the user name.
 * 
 * Clients are not expected to implement this interface
 * @since 1.1
 */
public interface IUserInfo{
  /**
   * Get the user name for this user.
   * @return user name
   */
  public String getUsername();

  /**
   * Sets the user name for this user. This should not be called if
   * isUsernameMutable() returns false.
   * @param username a user name for this user
   */
  public void setUsername(String username);

  /**
   * Return true if the user name is mutable. If not, setUsername should not be called.
   * @return a flag for isUsernameMutable
   */
  public boolean isUsernameMutable();

  /**
   * Sets the password for this user.
   * @param password a password for this user
   */
  public void setPassword(String password);
}
