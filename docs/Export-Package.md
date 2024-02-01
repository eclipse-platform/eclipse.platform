Export-Package
==============

The Eclipse top-level project uses the following convention for the Export-Package manifest attribute:

*   Newly added internal packages should not use x-internal exports unless there are very good reasons
*   For existing packages the x-internal export can be removed with PMC approval
*   Using x-friends is OK and should always be preferred over x-internals. The use of x-friends is an indication of tightly coupled bundles because its use is for when one bundle needs "approved" access to the internals of another bundle.
*   The 'Unexported package' Plug-in Manifest compiler option should be set to 'Warning'
