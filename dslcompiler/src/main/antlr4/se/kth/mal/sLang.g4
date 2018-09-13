
grammar sLang;

compilationUnit
   :  declarationUnit* EOF
   ;

declarationUnit
   :  categoryDeclaration
   |  associations
   ;

associations
   :  'associations {' associationDeclaration* '}'
   ;

// Assets are sorted in categories, probably for UI reasons.
categoryDeclaration
   :  'category' Identifier description? '{' assetDeclaration* '}'
   ;

// Declare an asset
assetDeclaration
   :  asset Identifier ('extends' Identifier)? description? rationale? assumptions? '{' stepDeclaration* '}'
   ;

stepDeclaration
   :  attackStepDeclaration
   |  existenceStepDeclaration
   ;

asset
   : 'asset'
   | 'abstractAsset'
   ;

// Here, we define associations
associationDeclaration
   :  Identifier '[' Identifier ']' multiplicity leftRelation Identifier rightRelation multiplicity '[' Identifier ']' Identifier description? rationale? assumptions?
   ;

rightRelation
   :  '-->'
   ;

leftRelation
   :  '<--'
   ;

// 1   An asset must be connected to exactly one of the related assets
// 0-1 An asset can be connected to at most one of the related assets
// 1-* An asset must be connected to at least one of the related assets
// *   An asset can be connected to any number of the related assets
multiplicity
   :  '1'
   |  '0-1'
   |  '1-*'
   |  '*'
   ;

// Declare attack steps
attackStepDeclaration
   :  attackStepType Identifier ttc? description? rationale? assumptions? children? containedSteps?
   ;

// Declare existence steps
existenceStepDeclaration
   :  existenceStepType Identifier ttc? description? rationale? assumptions? existenceRequirements? children? containedSteps?
   ;

existenceStepType
   :  'E'
   |  '3'
   ;

// List the existenceRequirements.
existenceRequirements
   :  '<-' Identifier (',' Identifier)*
   ;

// List the children.
children
   :  childOperator expressionName (',' expressionName)*
   ;

// -> indicates that the generalization attack step definition is replaced by the specialization, while +> denotes the specialization is appended
childOperator
   :  '->'
   |  '+>'
   ;

// An attack step can have sub-attack-steps only to better organize the code.
containedSteps
   : '{' attackStepDeclaration* '}'
   ;

// The description is meant for the end user to read
description
   : 'info:' StringLiteral
   ;

// The rationale is meant to justify the concept for securiLang designers.
rationale
   : 'rationale:' StringLiteral
   ;

// The assumptions list attack paths we chose to not include, modeling assumptions, etc.
assumptions
   : 'assumptions:' StringLiteral
   ;

StringLiteral
   :  '"' StringCharacters? '"'
   ;
fragment
StringCharacters
   :  StringCharacter+
   ;
fragment
StringCharacter
   :  ~["\\]
   ;

// The local TTC declaration. (The allowed distributions could be specified here, in the grammar.)
ttc
   : '[' Identifier '(' formalParameters? ')' ']'
   ;

// The attack step(s) whose TTC determine the color of the asset in the UI.
mostImportant
   :  '!'
   ;

// Should the user see this attack step?
visibility
   :  '+'
   |  '-'
   ;

// | OR attack step.
// & AND attack step.
// # defense step.
// t CPT attack step.
attackStepType
   :  '|'
   |  '&'
   |  '#'
   |  't'
   ;

expressionName
   :  Identifier
   |  ambiguousName '.' Identifier
   ;

ambiguousName
   :  Identifier
   |  ambiguousName '.' Identifier
   ;

Identifier
   :  JavaLetter JavaLetterOrDigit*
   ;

formalParameters
   :  DecimalFloatingPointLiteral (',' DecimalFloatingPointLiteral)*
   ;

DecimalFloatingPointLiteral
   :  Digits '.'? Digits?
   ;

Digits
   :  Digit Digit*
   ;

Digit
   :  [0-9]
   ;



fragment
JavaLetter
   :  [a-zA-Z$_] // these are the "java letters" below 0xFF
   |  // covers all characters above 0xFF which are not a surrogate
      ~[\u0000-\u00FF\uD800-\uDBFF]
      {Character.isJavaIdentifierStart(_input.LA(-1))}?
   |  // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
      [\uD800-\uDBFF] [\uDC00-\uDFFF]
      {Character.isJavaIdentifierStart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
   ;

fragment
JavaLetterOrDigit
   :  [a-zA-Z0-9$_] // these are the "java letters or digits" below 0xFF
   |  // covers all characters above 0xFF which are not a surrogate
      ~[\u0000-\u00FF\uD800-\uDBFF]
      {Character.isJavaIdentifierPart(_input.LA(-1))}?
   |  // covers UTF-16 surrogate pairs encodings for U+10000 to U+10FFFF
      [\uD800-\uDBFF] [\uDC00-\uDFFF]
      {Character.isJavaIdentifierPart(Character.toCodePoint((char)_input.LA(-2), (char)_input.LA(-1)))}?
   ;

//
// Whitespace and comments
//

WS  :  [ \t\r\n\u000C]+ -> skip
    ;

LINE_COMMENT
    :   '//' ~[\r\n]* -> skip
    ;

