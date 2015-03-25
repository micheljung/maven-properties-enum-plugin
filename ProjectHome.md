Never liked hard-coding your property keys? The maven-properties-enum-plugin allows you to generate enum types, holding all valid keys including javadoc.

Your advantages:

  * You can't access on non-existing keys as it would cause compile errors
  * As it's an enum, you can use your IDE's auto-completion
  * You'll never have to check your properties keys again
  * You can see the property's value in the enum field's javadoc