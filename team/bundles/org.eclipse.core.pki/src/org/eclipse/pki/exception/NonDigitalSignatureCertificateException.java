package org.eclipse.pki.exception;

import java.security.cert.CertificateException;

public class NonDigitalSignatureCertificateException extends
        CertificateException {

    private static final long serialVersionUID = 8626994851137646007L;

    public NonDigitalSignatureCertificateException() {
    }

    public NonDigitalSignatureCertificateException( String arg0 ) {
        super( arg0 );
    }

    public NonDigitalSignatureCertificateException( Throwable arg0 ) {
        super( arg0 );
    }

    public NonDigitalSignatureCertificateException( String arg0, Throwable arg1 ) {
        super( arg0, arg1 );
    }

}
