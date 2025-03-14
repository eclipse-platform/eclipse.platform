/*******************************************************************************
 * Copyright (c) 2025 Eclipse Platform, Security Group and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Platform - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.security.incoming;

import java.util.Properties;

public class PublicKeySecurity {
	
	private static PublicKeySecurity INSTANCE;
	protected byte[] salt = new byte[16];
	
	private PublicKeySecurity() {}
	public static PublicKeySecurity getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new PublicKeySecurity();
		}
		return INSTANCE;
	}
	
	public boolean isTurnedOn() {
		return SecurityFileSnapshot.getInstance().image();
	}
	public void setupPKIfile() {
		SecurityFileSnapshot.getInstance().createPKI();
	}

	public Properties getPkiPropertyFile(String pin) {
		salt = new String(System.getProperty("user.name") + pin).getBytes(); //$NON-NLS-1$
		return SecurityFileSnapshot.getInstance().load(pin, new String(salt));
	}
}