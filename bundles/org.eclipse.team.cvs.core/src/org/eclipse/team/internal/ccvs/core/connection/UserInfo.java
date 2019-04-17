/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.connection;

 
import org.eclipse.team.internal.ccvs.core.IUserInfo;

/**
 * @version 	1.0
 * @author
 */
public class UserInfo implements IUserInfo {

	private String username;
	private String password;
	private boolean isUsernameMutable;
	
	protected UserInfo(String username, String password, boolean isUsernameMutable) {
		this.username = username;
		this.password = password;
		this.isUsernameMutable = isUsernameMutable;
	}
	
	@Override
	public String getUsername() {
		return username;
	}

	protected String getPassword() {
		return password;
	}
	
	@Override
	public boolean isUsernameMutable() {
		return isUsernameMutable;
	}

	@Override
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public void setUsername(String username) {
		this.username = username;
	}

}
