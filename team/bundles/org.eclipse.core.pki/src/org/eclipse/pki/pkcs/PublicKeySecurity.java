package org.eclipse.pki.pkcs;

import java.util.Properties;

import org.eclipse.ui.pki.util.SecurityFileSnapshot;

public enum PublicKeySecurity {
	INSTANCE;
	public boolean isTurnedOn() {
		return SecurityFileSnapshot.INSTANCE.image();
	}
	public Properties getPkiPropertyFile() {
		return SecurityFileSnapshot.INSTANCE.load();
	}
}
