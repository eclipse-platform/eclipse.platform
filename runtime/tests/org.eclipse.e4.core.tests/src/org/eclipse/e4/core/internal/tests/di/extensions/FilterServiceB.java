package org.eclipse.e4.core.internal.tests.di.extensions;

import org.osgi.service.component.annotations.Component;

@Component(property="filtervalue=Test")
public class FilterServiceB implements TestService {

}
