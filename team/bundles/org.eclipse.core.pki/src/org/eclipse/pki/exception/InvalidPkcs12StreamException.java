package org.eclipse.pki.exception;

import java.security.cert.CertificateException;

public class InvalidPkcs12StreamException extends
        CertificateException {

    private static final long serialVersionUID = 548968883742412291L;

    public InvalidPkcs12StreamException() {
    }

    public InvalidPkcs12StreamException( String arg0 ) {
        super( arg0 );
    }

    public InvalidPkcs12StreamException( Throwable arg0 ) {
        super( arg0 );
    }

    public InvalidPkcs12StreamException( String arg0, Throwable arg1 ) {
        super( arg0, arg1 );
    }

}
