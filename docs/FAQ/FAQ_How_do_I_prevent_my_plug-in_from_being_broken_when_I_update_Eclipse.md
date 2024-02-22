

FAQ How do I prevent my plug-in from being broken when I update Eclipse?
========================================================================

Eclipse makes a careful distinction between published APIs and internal implementation details. The APIs are designed to reduce coupling between plug-ins to a small, stable interface. This insulates clients of the interface from being affected by implementation changes, and it allows the plug-in that publishes the interface to continue to innovate and grow without breaking existing clients. If your plug-in uses only published API and carefully follows the API contracts defined in the API javadoc, your plug-in should continue to work after migrating to a new Eclipse release.

In the Eclipse Platform, the API of a plug-in includes all public classes and interfaces that do not have the word _internal_ in their package names and all public and protected methods in those classes and interfaces. The API also includes all extension points that are not explicitly described as for internal use only in their documentation; there is only a small handful of such internal extension points.


