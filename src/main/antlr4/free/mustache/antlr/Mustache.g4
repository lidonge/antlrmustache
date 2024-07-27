grammar Mustache;

// Define the starting point for parsing
template: (statement | variable | text)*;

// Define statements enclosed in {{ }} or {{#...}}
statement: (section | invertedSection | partial | comment) ;

// Define a section, which is used for loops or conditionals
section: sectionHeader (statement | variable | text)* '{{/' footer '}}';

// Define an inverted section, which is used for "if not" conditionals
invertedSection: invertedSectionHeader (statement | variable | text)* '{{/' footer '}}';

// Define a partial, which is a reference to another template
partial: '{{' '>' IDENTIFIER '}}';

// Define variables that are replaced with data
variable: '{{' qualifiedName '}}';
footer: (qualifiedName | first | last) ;
sectionHeader: '{{' '#' footer '}}';
invertedSectionHeader: '{{' '^' footer '}}';

// Define 'first' and 'last' as special variables
first: '-first';
last: '-last';

// Define comments that are ignored
comment: '{{!' .*? '}}';

// Define plain text that is not a Mustache tag
text: (~('{{' | '}}') | WS)+;
qualifiedName
    : IDENTIFIER ('.' IDENTIFIER)*
    ;
// Define an identifier for variable names
IDENTIFIER: [a-zA-Z_][a-zA-Z0-9_]*;

// Define whitespace and newlines
WS: [ \t\r\n]+ -> skip;

// Define any other characters to be handled as text
ANY: .;

// Define error handling
error: .+;