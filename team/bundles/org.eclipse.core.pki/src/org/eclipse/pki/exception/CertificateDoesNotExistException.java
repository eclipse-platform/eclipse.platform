package org.eclipse.pki.exception;

public class CertificateDoesNotExistException extends
        InvalidPkcs12StreamException {

    private static final long serialVersionUID = -2415838781812652429L;

    public CertificateDoesNotExistException() {
    }

    public CertificateDoesNotExistException( String arg0 ) {
        super( arg0 );
    }

    public CertificateDoesNotExistException( Throwable arg0 ) {
        super( arg0 );
    }

    public CertificateDoesNotExistException( String arg0, Throwable arg1 ) {
        super( arg0, arg1 );
    }

}
