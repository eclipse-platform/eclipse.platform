# API removal process

See [Eclipse Project Deprecation Policy](https://github.com/eclipse-platform/eclipse.platform/blob/master/docs/Eclipse_API_Central_Deprecation_Policy.md) for more information.

For new API planned removals use:

* For Java code use the @Deprecated annotation (see below for an example) and optional additional Javadoc. 
An extra entry in the removal document from [removal document](https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fporting%2Fremovals.html) is not necessary anymore
* If appropriate the @noextend @noreference and @noinstantiate Javadoc annotation should be added to code

PMC approval for planned API removal is required, either via the pull request or via the mailing list
After 2 years of announced deletion, the API can be removed

Javadoc generates a detailed [list of forRemoval](https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Freference%2Fapi%2Fdeprecated-list.html&anchor=forRemoval) API which is also linked to in the [removal document](https://help.eclipse.org/latest/index.jsp?topic=%2Forg.eclipse.platform.doc.isv%2Fporting%2Fremovals.html)

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


