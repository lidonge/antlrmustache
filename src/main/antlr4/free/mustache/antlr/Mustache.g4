grammar Mustache;

// Define the starting point for parsing
template: (statement | variable | text)*;

// Define statements enclosed in {{ }} or {{#...}}
statement: (section | invertedSection | partial | comment) ;

// Define a section, which is used for loops or conditionals
section: sectionBeg sectionContent* sectionEnd;

// Define an inverted section, which is used for "if not" conditionals
invertedSection: invertedSectionHeader sectionContent* sectionEnd;

// Define a partial, which is a reference to another template
partial: '{{' '>' IDENTIFIER '}}';

// Define variables that are replaced with data
variable: '{{' qualifiedName '}}';
sectionVar: (qualifiedName | first | last) ;
//# means normal section, @ means take map as list
sectionBeg: '{{' ('#' | '@') sectionVar '}}';
sectionContent : statement | variable | text;
// '%' means section is recursive, '/' means section is end
sectionEnd : '{{' ('/'|'%') sectionVar '}}';
invertedSectionHeader: '{{' '^' sectionVar '}}';

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