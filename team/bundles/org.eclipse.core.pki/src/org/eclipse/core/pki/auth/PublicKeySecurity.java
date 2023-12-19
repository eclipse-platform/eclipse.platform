/*******************************************************************************
 * Copyright (c) 2023 Eclipse Platform, Security Group and others.
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
package org.eclipse.core.pki.auth;

import java.util.Properties;

public enum PublicKeySecurity {
	INSTANCE;
	public boolean isTurnedOn() {
		return SecurityFileSnapshot.INSTANCE.image();
	}
	public Properties getPkiPropertyFile() {
		return SecurityFileSnapshot.INSTANCE.load();
	}
}