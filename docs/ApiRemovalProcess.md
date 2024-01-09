# API removal process

See [Eclipse Project Deprecation Policy](https://wiki.eclipse.org/Eclipse/API_Central/Deprecation_Policy) for more information.

For new API planned removals use:

* For Java code use the @Deprecated annotation (see below for an example) and optional additional Javadoc. This can only be applied for bundles using a minimum BREE of Java 11. An extra entry in the removal document from [removal document](https://github.com/eclipse-platform/eclipse.platform.common/blob/master/bundles/org.eclipse.platform.doc.isv/porting/removals.html) is not necessary anymore
* Keep the [removal document](https://github.com/eclipse-platform/eclipse.platform.common/blob/master/bundles/org.eclipse.platform.doc.isv/porting/removals.html) for cases in which the bundle is below Java 11 or for other special which do not fit into the Java code. For such cases a new entry is necessary for planned API removals
* If appropriate the @noextend @noreference and @noinstantiate Javadoc annotation should be added to code

PMC approval for planned API removal is required, either via the pull request or via the mailing list
After 2 years of announced deletion, the API can be removed
Javadoc generates a detailed [list of forRemoval](http://help.eclipse.org/latest/topic/org.eclipse.platform.doc.isv/reference/api/deprecated-list.html#forRemoval) API which is also link to in the [removal document](https://github.com/eclipse-platform/eclipse.platform.common/blob/master/bundles/org.eclipse.platform.doc.isv/porting/removals.html)

Example of a deprecation comment:

``` 
* XXX
 * @noreference
 * @noextend
 * @noimplement
 * @deprecated This XXX (class/method/field) will be removed in a future release. Use XXX instead.
 */
@Deprecated(forRemoval = true, since = "4.16")
``` 
* The PMC may decide to back out of an API removal
* In general, removing a deprecated API does NOT cause the increase of the major version segment.

Software tests and test utilities are not considered API and can be changed and deleted at any time.


