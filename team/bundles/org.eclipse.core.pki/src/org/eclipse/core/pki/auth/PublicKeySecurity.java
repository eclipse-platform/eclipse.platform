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