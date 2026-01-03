Our query panel should have an autocomplete feature. The feature will use the following and based on this format

SQL VERSION:

`SELECT <ldap attributes> FROM <ou / cn / dn> WHERE <ldap filter>`

1. The autocomplete input should contain a list of ldap attributes that are found through the schema discovery that will be returned in the results.
2. The autocomplete input should contain a list of ldap ou / cn / dn that are found through the ldap schema discovery.
3. The format for the <ou / cn / dn> should just be the value of the <ou / cn / dn> but in the autocomplete list will specify the full path.
4. THe ldap filters will autocomplete attribute names but not values.

UI:
1. The autocomplete input should be a multi-line text field with line numbers.
2. The autocomplete input should have a dropdown list that appears when the user starts typing.
3. The dropdown list should be filtered based on the user's input (fuzzy search).
4. The dropdown list should display the full path for ldap ou / cn / dn but only show the value for ldap attributes.

===========================

LDAP SEARCH SYNTAX VERSION:

The LDAP search syntax is different than the SQl syntax, but the autocomplete will function similarly. The one main difference is that ldap search query syntax doesn't include a list of attributes to retrieve. We'll be populating the attributes within the filter (not the serch value). If the search contains a ou / cn / dn, we can autocomplete the options for that search value.

Key Files:
Everything in teh `ui/components/editor` folder.