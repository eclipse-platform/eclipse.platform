package org.eclipse.pki.auth;

import java.util.Properties;
import org.eclipse.pki.auth.SecurityFileSnapshot;

public enum PublicKeySecurity {
	INSTANCE;
	public boolean isTurnedOn() {
		return SecurityFileSnapshot.INSTANCE.image();
	}
	public Properties getPkiPropertyFile() {
		return SecurityFileSnapshot.INSTANCE.load();
	}
}