package org.eclipse.core.pki.exception;

import java.security.cert.CertificateException;

public class WrongPasswordException extends
        CertificateException {

    private static final long serialVersionUID = 6845666941431518872L;

    public WrongPasswordException() {
    }

    public WrongPasswordException( String arg0 ) {
        super( arg0 );
    }

    public WrongPasswordException( Throwable arg0 ) {
        super( arg0 );
    }

    public WrongPasswordException( String arg0, Throwable arg1 ) {
        super( arg0, arg1 );
    }

}
