

FAQ When do I use a launch delegate?
====================================

A launch configuration captures all the information required to launch a particular application, but the launching is performed by a launch delegate. This separation allows a single launch configuration to be used by several launch delegates to launch an application in different ways using the same launch information. For example, there is a single launch-configuration type for launching Java programs but different launch delegates for launching in run mode versus debug mode. One could define more delegates for launching Java programs by associating with the existing Java launch-configuration type. This example shows a declaration of a launch delegate for launching Java applications in a special profiling mode:

      <extension point="org.eclipse.debug.core.launchDelegates">
         <launchDelegate
            id="org.eclipse.faq.example.traceDelegate"
            delegate="org.eclipse.faq.example.TraceLauncher"
            type="org.eclipse.jdt.launching.localJavaApplication"
            modes="trace"/>
      </extension>

For more information on launch delegates, see the documentation for the org.eclipse.debug.core.launchDelegates extension point and the javadoc for ILaunchConfigurationDelegate in the debug core plug-in.

