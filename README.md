# eclipse.platform
This repository contains the basis for the [Eclipse IDE](https://www.eclipse.org/eclipseide/) and a sub-repository of the [eclipse-platform](https://github.com/eclipse-platform) organization. 

It contains multiple modules:

### [eclipse.platform](https://github.com/eclipse-platform/eclipse.platform/tree/master/platform)
Platform provides images like the splash screen.

### [eclipse.resources](https://github.com/eclipse-platform/eclipse.platform/tree/master/resources)
Resources provides Java interfaces like `IResource` and implementations of workspace, folder, file and file system abstraction.

### [eclipse.runtime](https://github.com/eclipse-platform/eclipse.platform/tree/master/runtime) 
Runtime provides Java interfaces like `IJob`, `ISchedulingRule` and implements scheduling of multithreaded jobs with exclusive access to a resource.

### [eclipse.update](https://github.com/eclipse-platform/eclipse.platform/tree/master/update)
Update provides Java interfaces like `IPlatformConfiguration`. `IPlatformConfiguration` represents the runtime configuration.

