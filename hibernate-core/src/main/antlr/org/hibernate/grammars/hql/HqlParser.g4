parser grammar HqlParser;

options {
	tokenVocab=HqlLexer;
}

@header {
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.grammars.hql;
}

@members {
	protected void logUseOfReservedWordAsIdentifier(Token token) {
	}
}


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Statements

/**
 * Toplevel rule, entrypoint to the whole grammar
 */
statement
	: (selectStatement | updateStatement | deleteStatement | insertStatement) EOF
	;

/**
 * A 'select' query
 */
selectStatement
	: queryExpression
	;

/**
 * A 'select' query that occurs within another statement
 */
subquery
	: queryExpression
	;

/**
 * A declaration of a root entity, with an optional identification variable
 */
targetEntity
	: entityName variable?
	;

/**
 * A 'delete' statement
 */
deleteStatement
	: DELETE FROM? targetEntity whereClause?
	;

/**
 * An 'update' statement
 */
updateStatement
	: UPDATE VERSIONED? targetEntity setClause whereClause?
	;

/**
 * An 'set' list of assignments in an 'update' statement
 */
setClause
	: SET assignment (COMMA assignment)*
	;

/**
 * An assignment to an entity attribute in an 'update' statement
 */
assignment
	: simplePath EQUAL expressionOrPredicate
	;

/**
 * An 'insert' statement
 */
insertStatement
	: INSERT INTO? targetEntity targetFields (queryExpression | valuesList)
	;

/**
 * The list of target entity attributes in an 'insert' statement
 */
targetFields
	: LEFT_PAREN simplePath (COMMA simplePath)* RIGHT_PAREN
	;

/**
 * A 'values' clause in an 'insert' statement, with one of more tuples of values to insert
 */
valuesList
	: VALUES values (COMMA values)*
	;

/**
 * A tuple of values to insert in an 'insert' statement
 */
values
	: LEFT_PAREN expressionOrPredicate (COMMA expressionOrPredicate)* RIGHT_PAREN
	;

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// QUERY SPEC - general structure of root sqm or sub sqm

/**
 * A toplevel query of subquery, which may be a union or intersection of subqueries
 */
queryExpression
	: orderedQuery								# SimpleQueryGroup
	| orderedQuery (setOperator orderedQuery)+	# SetQueryGroup
	;

/**
 * A query with an optional 'order by' clause
 */
orderedQuery
	: query queryOrder?										# QuerySpecExpression
	| LEFT_PAREN queryExpression RIGHT_PAREN queryOrder?	# NestedQueryExpression
	;

/**
 * An operator whose operands are whole queries
 */
setOperator
	: UNION ALL?
	| INTERSECT ALL?
	| EXCEPT ALL?
	;

/**
 * The 'order by' clause and optional subclauses for limiting and pagination
 */
queryOrder
	: orderByClause limitClause? offsetClause? fetchClause?
	;

/**
 * An unordered query, with just projection, restriction, and aggregation
 *
 * - The 'select' clause may come first, in which case 'from' is optional
 * - The 'from' clause may come first, in which case 'select' is optional, and comes last
 */
query
// TODO: add with clause
	: selectClause fromClause? whereClause? (groupByClause havingClause?)?
	| fromClause whereClause? (groupByClause havingClause?)? selectClause?
	;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// FROM clause

/**
 * The 'from' clause of a query
 */
fromClause
	: FROM entityWithJoins (COMMA entityWithJoins)*
	;

/**
 * The declaration of a root entity in 'from' clause, along with its joins
 */
entityWithJoins
	: rootEntity (join | crossJoin | jpaCollectionJoin)*
	;

/**
 * A root entity declaration in the 'from' clause, with optional identification variable
 */
rootEntity
	: entityName variable?
	;

/**
 * An entity name, for identifying the root entity
 */
entityName
	: identifier (DOT identifier)*
	;

/**
 * An identification variable (an entity alias)
 */
variable
	: AS identifier
	| IDENTIFIER
	| QUOTED_IDENTIFIER
	;

/**
 * A 'cross join' to a second root entity (a cartesian product)
 */
crossJoin
	: CROSS JOIN rootEntity variable?
	;

/**
 * Deprecated syntax dating back to EJB-QL prior to EJB 3, required by JPA, never documented in Hibernate
 */
jpaCollectionJoin
	: COMMA IN LEFT_PAREN path RIGHT_PAREN variable?
	;

/**
 * A 'join', with an optional 'on' or 'with' clause
 */
join
	: joinType JOIN FETCH? joinPath joinRestriction?
	;

/**
 * The inner or outer join type
 */
joinType
	: INNER?
	| (LEFT|RIGHT|FULL)? OUTER?
	;

/**
 * The joined path, with an optional identification variable
 */
joinPath
	: path variable?
	;

/**
 * An extra restriction added to the join condition
 */
joinRestriction
	: (ON | WITH) predicate
	;



// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// SELECT clause

/**
 * The 'select' clause of a query
 */
selectClause
	: SELECT DISTINCT? selectionList
	;

/**
 * A projection list: a list of selected items
 */
selectionList
	: selection (COMMA selection)*
	;

/**
 * An element of a projection list: a selected item, with an optional alias
 */
selection
	: selectExpression variable?
	;

/**
 * A selected item ocurring in the 'select' clause
 */
selectExpression
	: instantiation
	| mapEntrySelection
	| jpaSelectObjectSyntax
	| expressionOrPredicate
	;


/**
 * The special function entry() which may only occur in the 'select' clause
 */
mapEntrySelection
	: ENTRY LEFT_PAREN path RIGHT_PAREN
	;

/**
 * Instantiation using 'select new'
 */
instantiation
	: NEW instantiationTarget LEFT_PAREN instantiationArguments RIGHT_PAREN
	;

/**
 * The type to be instantiated with 'select new', 'list', 'map', or a fuly-qualified Java class name
 */
instantiationTarget
	: LIST
	| MAP
	| simplePath
	;

/**
 * The arguments to a 'select new' instantiation
 */
instantiationArguments
	: instantiationArgument (COMMA instantiationArgument)*
	;

/**
 * A single argument in a 'select new' instantiation, with an optional alias
 */
instantiationArgument
	: instantiationArgumentExpression variable?
	;

/**
 * A single argument in a 'select new' instantiation: an expression, or a nested instantiation
 */
instantiationArgumentExpression
	: expressionOrPredicate
	| instantiation
	;

/**
 * Deprecated syntax dating back to EJB-QL prior to EJB 3, required by JPA, never documented in Hibernate
 */
jpaSelectObjectSyntax
	: OBJECT LEFT_PAREN identifier RIGHT_PAREN
	;



// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Path structures

/**
 * A simple path expression
 *
 * - a reference to an identification variable (not case-sensitive),
 * - followed by a list of period-separated identifiers (case-sensitive)
 */
simplePath
	: identifier simplePathElement*
	;

/**
 * An element of a simple path expression: a period, and an identifier (case-sensitive)
 */
simplePathElement
	: DOT identifier
	;

/**
 * A much more complicated path expression involving operators and functions
 *
 * A path which needs to be resolved semantically.  This recognizes
 * any path-like structure.  Generally, the path is semantically
 * interpreted by the consumer of the parse-tree.  However, there
 * are certain cases where we can syntactically recognize a navigable
 * path; see `syntacticNavigablePath` rule
 */
path
	: syntacticDomainPath pathContinuation?
	| generalPathFragment
	;

/**
 * A continuation of a path expression "broken" by an operator or function
 */
pathContinuation
	: DOT simplePath
	;

/**
 * An operator or function that may occur within a path expression
 *
 * Rule for cases where we syntactically know that the path is a
 * "domain path" because it is one of these special cases:
 *
 * 		* TREAT( path )
 * 		* ELEMENTS( path )
 * 		* INDICES( path )
 *		* VALUE( path )
 * 		* KEY( path )
 * 		* path[ selector ]
 */
syntacticDomainPath
	: treatedNavigablePath
	| collectionElementNavigablePath
	| collectionIndexNavigablePath
	| mapKeyNavigablePath
	| simplePath indexedPathAccessFragment
	;

/**
 * The main path rule
 *
 * Recognition for all normal path structures including
 * class, field and enum references as well as navigable paths.
 *
 * NOTE : this rule does *not* cover the special syntactic navigable path
 * cases: TREAT, KEY, ELEMENTS, VALUES
 */
generalPathFragment
	: simplePath indexedPathAccessFragment?
	;

/**
 * In index operator that "breaks" a path expression
 */
indexedPathAccessFragment
	: LEFT_BRACKET expression RIGHT_BRACKET (DOT generalPathFragment)?
	;

/**
 * A 'treat()' function that "breaks" a path expression
 */
treatedNavigablePath
	: TREAT LEFT_PAREN path AS simplePath RIGHT_PAREN pathContinuation?
	;

/**
 * A 'values()' or 'elements()' function that "breaks" a path expression
 */
collectionElementNavigablePath
	: (VALUE | ELEMENTS) LEFT_PAREN path RIGHT_PAREN pathContinuation?
	;

/**
 * An 'indices()' function that "breaks" a path expression
 */
collectionIndexNavigablePath
	: INDICES LEFT_PAREN path RIGHT_PAREN
	;

/**
 * A 'key()' function that "breaks" a path expression
 */
mapKeyNavigablePath
	: KEY LEFT_PAREN path RIGHT_PAREN pathContinuation?
	;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// GROUP BY clause

/**
 * The 'group by' clause of a query, controls aggregation
 */
groupByClause
	: GROUP BY groupByExpression (COMMA groupByExpression)*
	;

/**
 * A grouped item that occurs in the 'group by' clause
 *
 * a select item alias, an ordinal position of a select item, or an expression
 */
groupByExpression
	: identifier
	| INTEGER_LITERAL
	| expression
	;

// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
//HAVING clause

/**
 * The 'having' clause of a query, a restriction on the grouped data
 */
havingClause
	: HAVING predicate
	;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// ORDER BY clause

/**
 * The 'order by' clause of a query, controls sorting
 */
orderByClause
	: ORDER BY sortSpecification (COMMA sortSpecification)*
	;

/**
 * Specialized rule for ordered Map and Set `@OrderBy` handling
 */
orderByFragment
	: sortSpecification (COMMA sortSpecification)*
	;

/**
 * A rule for sorting an item in the 'order by' clause
 */
sortSpecification
	: sortExpression sortDirection? nullsPrecedence?
	;

/**
 * A rule for sorting null values
 */
nullsPrecedence
	: NULLS (FIRST | LAST)
	;

/**
 * A sorted item that occurs in the 'order by' clause
 *
 * a select item alias, an ordinal position of a select item, or an expression
 */
sortExpression
	: identifier
	| INTEGER_LITERAL
	| expression
	;

/**
 * The direction in which to sort
 */
sortDirection
	: ASC
	| DESC
	;

/**
 * The special 'collate()' functions
 */
collateFunction
	: COLLATE LEFT_PAREN expression AS collation RIGHT_PAREN
	;

/**
 * The name of a database-defined collation
 *
 * Certain databases allow a period in a collation name
 */
collation
	: simplePath
	;



// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// LIMIT/OFFSET clause

/**
 * A 'limit' on the number of query results
 */
limitClause
	: LIMIT parameterOrIntegerLiteral
	;

/**
 * An 'offset' of the first query result to return
 */
offsetClause
	: OFFSET parameterOrIntegerLiteral (ROW | ROWS)?
	;

/**
 * A much more complex syntax for limits
 */
fetchClause
	: FETCH (FIRST | NEXT) (parameterOrIntegerLiteral | parameterOrNumberLiteral PERCENT) (ROW | ROWS) (ONLY | WITH TIES)
	;

/**
 * An parameterizable integer literal
 */
parameterOrIntegerLiteral
	: parameter
	| INTEGER_LITERAL
	;

/**
 * An parameterizable numeric literal
 */
parameterOrNumberLiteral
	: parameter
	| INTEGER_LITERAL
	| FLOAT_LITERAL
	| DOUBLE_LITERAL
	;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// WHERE clause & Predicates

/**
 * The 'were' clause of a query, update statement, or delete statement
 */
whereClause
	: WHERE predicate
	;

/**
 * A boolean-valued expression, usually used to express a restriction
 */
predicate
	//highest to lowest precedence
	: LEFT_PAREN predicate RIGHT_PAREN											# GroupedPredicate
	| expression IS NOT? NULL													# IsNullPredicate
	| expression IS NOT? EMPTY													# IsEmptyPredicate
	| expression NOT? IN inList													# InPredicate
	| expression NOT? BETWEEN expression AND expression							# BetweenPredicate
	| expression NOT? (LIKE | ILIKE) expression likeEscape?						# LikePredicate
	| expression comparisonOperator expression									# ComparisonPredicate
	| EXISTS (ELEMENTS|INDICES) LEFT_PAREN simplePath RIGHT_PAREN	# ExistsCollectionPartPredicate
	| EXISTS expression															# ExistsPredicate
	| expression NOT? MEMBER OF? path											# MemberOfPredicate
	| NOT predicate																# NegatedPredicate
	| predicate AND predicate													# AndPredicate
	| predicate OR predicate													# OrPredicate
	| expression																# BooleanExpressionPredicate
	;

/**
 * An operator which compares values for equality or order
 */
comparisonOperator
	: EQUAL
	| NOT_EQUAL
	| GREATER
	| GREATER_EQUAL
	| LESS
	| LESS_EQUAL
	| IS DISTINCT FROM
	| IS NOT DISTINCT FROM
	;

/**
 * Any right operand of the 'in' operator
 *
 * A list of values, a parameter (for a parameterized list of values), a subquery, or an `elements()` or `indices()` function
 */
inList
	: (ELEMENTS|INDICES) LEFT_PAREN simplePath RIGHT_PAREN				# PersistentCollectionReferenceInList
	| LEFT_PAREN (expressionOrPredicate (COMMA expressionOrPredicate)*)? RIGHT_PAREN# ExplicitTupleInList
	| LEFT_PAREN subquery RIGHT_PAREN												# SubqueryInList
	| parameter 																	# ParamInList
	;

/**
 * A single character used to escape the '_' and '%' wildcards in a 'like' pattern
 */
likeEscape
	: ESCAPE (STRING_LITERAL | parameter)
	;


// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// Expression

/**
 * An expression, excluding boolean expressions
 */
expression
	//highest to lowest precedence
	: LEFT_PAREN expression RIGHT_PAREN												# GroupedExpression
	| LEFT_PAREN expressionOrPredicate (COMMA expressionOrPredicate)+ RIGHT_PAREN	# TupleExpression
	| LEFT_PAREN subquery RIGHT_PAREN												# SubqueryExpression
	| primaryExpression 															# BarePrimaryExpression
	| signOperator numericLiteral													# UnaryNumericLiteralExpression
	| signOperator expression														# UnaryExpression
	| expression datetimeField  													# ToDurationExpression
	| expression BY datetimeField													# FromDurationExpression
	| expression multiplicativeOperator expression									# MultiplicationExpression
	| expression additiveOperator expression										# AdditionExpression
	| expression DOUBLE_PIPE expression												# ConcatenationExpression
	;

/**
 * An expression not involving operators
 */
primaryExpression
	: caseList											# CaseExpression
	| literal											# LiteralExpression
	| parameter											# ParameterExpression
	| entityTypeReference								# EntityTypeExpression
	| entityIdReference									# EntityIdExpression
	| entityVersionReference							# EntityVersionExpression
	| entityNaturalIdReference							# EntityNaturalIdExpression
	| syntacticDomainPath pathContinuation?				# SyntacticPathExpression
	| function											# FunctionExpression
	| generalPathFragment								# GeneralPathExpression
	;

/**
 * Any expression, including boolean expressions
 */
expressionOrPredicate
	: expression
	| predicate
	;

/**
 * A binary operator with the same precedence as *
 */
multiplicativeOperator
	: SLASH
	| PERCENT_OP
	| ASTERISK
	;

/**
 * A binary operator with the same precedence as +
 */
additiveOperator
	: PLUS
	| MINUS
	;

/**
 * A unary prefix operator
 */
signOperator
	: PLUS
	| MINUS
	;

/**
 * The special function 'type()'
 */
entityTypeReference
	: TYPE LEFT_PAREN (path | parameter) RIGHT_PAREN
	;

/**
 * The special function 'id()'
 */
entityIdReference
	: ID LEFT_PAREN path RIGHT_PAREN pathContinuation?
	;

/**
 * The special function 'version()'
 */
entityVersionReference
	: VERSION LEFT_PAREN path RIGHT_PAREN
	;

/**
 * The special function 'naturalid()'
 */
entityNaturalIdReference
	: NATURALID LEFT_PAREN path RIGHT_PAREN pathContinuation?
	;

/**
 * A 'case' expression, which comes in two forms: "simple", and "searched"
 */
caseList
	: simpleCaseList
	| searchedCaseList
	;

/**
 * A simple 'case' expression
 */
simpleCaseList
	: CASE expressionOrPredicate simpleCaseWhen+ caseOtherwise? END
	;

/**
 * The 'when' clause of a simple case
 */
simpleCaseWhen
	: WHEN expression THEN expressionOrPredicate
	;

/**
 * The 'else' clause of a 'case' expression
 */
caseOtherwise
	: ELSE expressionOrPredicate
	;

/**
 * A searched 'case' expression
 */
searchedCaseList
	: CASE searchedCaseWhen+ caseOtherwise? END
	;

/**
 * The 'when' clause of a searched case
 */
searchedCaseWhen
	: WHEN predicate THEN expressionOrPredicate
	;

/**
 * A literal value
 */
literal
	: STRING_LITERAL
	| NULL
	| booleanLiteral
	| numericLiteral
	| binaryLiteral
	| temporalLiteral
	| generalizedLiteral
	;

/**
 * A boolean literal value
 */
booleanLiteral
	: TRUE
	| FALSE
	;

/**
 * A numeric literal value, including hexadecimal literals
 */
numericLiteral
	: INTEGER_LITERAL
	| LONG_LITERAL
	| BIG_INTEGER_LITERAL
	| FLOAT_LITERAL
	| DOUBLE_LITERAL
	| BIG_DECIMAL_LITERAL
	| HEX_LITERAL
	;

/**
 * A binary literal value, as a SQL-style literal, or a braced list of byte literals
 */
binaryLiteral
	: BINARY_LITERAL
	| LEFT_BRACE HEX_LITERAL (COMMA HEX_LITERAL)* RIGHT_BRACE
	;

/**
 * A literal date, time, or datetime, in HQL syntax, or as a JDBC-style "escape" syntax
 */
temporalLiteral
	: dateTimeLiteral
	| dateLiteral
	| timeLiteral
	| jdbcTimestampLiteral
	| jdbcDateLiteral
	| jdbcTimeLiteral
	;

/**
 * A literal datetime, in braces, or with the 'datetime' keyword
 */
dateTimeLiteral
	: LEFT_BRACE dateTime RIGHT_BRACE
	| DATETIME dateTime
	;

/**
 * A literal date, in braces, or with the 'date' keyword
 */
dateLiteral
	: LEFT_BRACE date RIGHT_BRACE
	| DATE date
	;

/**
 * A literal time, in braces, or with the 'time' keyword
 */
timeLiteral
	: LEFT_BRACE time RIGHT_BRACE
	| TIME time
	;

/**
 * A literal datetime
 */
dateTime
	: date time (zoneId | offset)?
	;

/**
 * A literal date
 */
date
	: year MINUS month MINUS day
	;

/**
 * A literal time
 */
time
	: hour COLON minute (COLON second)?
	;

/**
 * A literal offset
 */
offset
	: (PLUS | MINUS) hour (COLON minute)?
	;

year: INTEGER_LITERAL;
month: INTEGER_LITERAL;
day: INTEGER_LITERAL;
hour: INTEGER_LITERAL;
minute: INTEGER_LITERAL;
second: INTEGER_LITERAL | FLOAT_LITERAL;
zoneId
	: IDENTIFIER (SLASH IDENTIFIER)?
	| STRING_LITERAL;

/**
 * A JDBC-style timestamp escape, as required by JPQL
 */
jdbcTimestampLiteral
	: TIMESTAMP_ESCAPE_START (dateTime | genericTemporalLiteralText) RIGHT_BRACE
	;

/**
 * A JDBC-style date escape, as required by JPQL
 */
jdbcDateLiteral
	: DATE_ESCAPE_START (date | genericTemporalLiteralText) RIGHT_BRACE
	;

/**
 * A JDBC-style time escape, as required by JPQL
 */
jdbcTimeLiteral
	: TIME_ESCAPE_START (time | genericTemporalLiteralText) RIGHT_BRACE
	;

genericTemporalLiteralText
	: STRING_LITERAL
	;

/**
 * A generic format for specifying literal values of arbitary types
 */
generalizedLiteral
	: LEFT_BRACE generalizedLiteralType COLON generalizedLiteralText RIGHT_BRACE
	;

generalizedLiteralType : STRING_LITERAL;
generalizedLiteralText : STRING_LITERAL;


/**
 * A query parameter: a named parameter, or an ordinal parameter
 */
parameter
	: COLON identifier					# NamedParameter
	| QUESTION_MARK INTEGER_LITERAL?	# PositionalParameter
	;

/**
 * A function invocation that may occur in an arbitrary expression
 */
function
	: standardFunction
	| aggregateFunction
	| jpaCollectionFunction
	| hqlCollectionFunction
	| jpaNonstandardFunction
	| genericFunction
	;

/**
 * A syntax for calling user-defined or native database functions, required by JPQL
 */
jpaNonstandardFunction
	: FUNCTION LEFT_PAREN jpaNonstandardFunctionName (COMMA genericFunctionArguments)? RIGHT_PAREN
	;

/**
 * The name of a user-defined or native database function, given as a quoted string
 */
jpaNonstandardFunctionName
	: STRING_LITERAL
	;

/**
 * Any function invocation that follows the regular syntax
 *
 * The function name, followed by a parenthesized list of comma-separated expressions
 */
genericFunction
	: genericFunctionName LEFT_PAREN (genericFunctionArguments | ASTERISK)? RIGHT_PAREN filterClause?
	;

/**
 * The name of a generic function, which may contain periods and quoted identifiers
 *
 * Names of generic functions are resolved against the SqmFunctionRegistry
 */
genericFunctionName
	: simplePath
	;

/**
 * The arguments of a generic function
 */
genericFunctionArguments
	: (DISTINCT | datetimeField COMMA)? expressionOrPredicate (COMMA expressionOrPredicate)*
	;

/**
 * The special 'size()' and 'index()' functions defined by JPQL
 */
jpaCollectionFunction
	: SIZE LEFT_PAREN path RIGHT_PAREN					# CollectionSizeFunction
	| INDEX LEFT_PAREN identifier RIGHT_PAREN			# CollectionIndexFunction
	;

/**
 * The special collection functions defined by HQL
 */
hqlCollectionFunction
	: MAXINDEX LEFT_PAREN path RIGHT_PAREN				# MaxIndexFunction
	| MAXELEMENT LEFT_PAREN path RIGHT_PAREN			# MaxElementFunction
	| MININDEX LEFT_PAREN path RIGHT_PAREN				# MinIndexFunction
	| MINELEMENT LEFT_PAREN path RIGHT_PAREN			# MinElementFunction
	;

/**
 * The special `every()`, `all()`, `any()` and `some()` functions defined by HQL
 *
 * May be applied to a subquery or collection reference, or may occur as an aggregate function in the 'select' clause
 */
aggregateFunction
	: everyFunction
	| anyFunction
	;

/**
 * The functions `every()` and `all()` are synonyms
 */
everyFunction
	: (EVERY|ALL) LEFT_PAREN predicate RIGHT_PAREN filterClause?
	| (EVERY|ALL) LEFT_PAREN subquery RIGHT_PAREN
	| (EVERY|ALL) (ELEMENTS|INDICES) LEFT_PAREN simplePath RIGHT_PAREN
	;

/**
 * The functions `any()` and `some()` are synonyms
 */
anyFunction
	: (ANY|SOME) LEFT_PAREN predicate RIGHT_PAREN filterClause?
	| (ANY|SOME) LEFT_PAREN subquery RIGHT_PAREN
	| (ANY|SOME) (ELEMENTS|INDICES) LEFT_PAREN simplePath RIGHT_PAREN
	;

/**
 * A 'filter' clause: a restriction applied to an aggregate function
 */
filterClause
	: FILTER LEFT_PAREN whereClause RIGHT_PAREN
	;

/**
 * Any function with an irregular syntax for the argument list
 *
 * These are all inspired by the syntax of ANSI SQL
 */
standardFunction
	: castFunction
	| extractFunction
	| formatFunction
	| collateFunction
	| substringFunction
	| overlayFunction
	| trimFunction
	| padFunction
	| positionFunction
	| currentDateFunction
	| currentTimeFunction
	| currentTimestampFunction
	| instantFunction
	| localDateFunction
	| localTimeFunction
	| localDateTimeFunction
	| offsetDateTimeFunction
	| cube
	| rollup
	;

/**
 * The 'cast()' function for typecasting
 */
castFunction
	: CAST LEFT_PAREN expression AS castTarget RIGHT_PAREN
	;

/**
 * The target type for a typecast: a typename, together with length or precision/scale
 */
castTarget
	: castTargetType (LEFT_PAREN INTEGER_LITERAL (COMMA INTEGER_LITERAL)? RIGHT_PAREN)?
	;

/**
 * The name of the target type in a typecast
 *
 * Like the `entityName` rule, we have a specialized dotIdentifierSequence rule
 */
castTargetType
	returns [String fullTargetName]
	: (i=identifier { $fullTargetName = _localctx.i.getText(); }) (DOT c=identifier { $fullTargetName += ("." + _localctx.c.getText() ); })*
	;

/**
 * The two formats for the 'substring() function: one defined by JPQL, the other by ANSI SQL
 */
substringFunction
	: SUBSTRING LEFT_PAREN expression COMMA substringFunctionStartArgument (COMMA substringFunctionLengthArgument)? RIGHT_PAREN
	| SUBSTRING LEFT_PAREN expression FROM substringFunctionStartArgument (FOR substringFunctionLengthArgument)? RIGHT_PAREN
	;

substringFunctionStartArgument
	: expression
	;

substringFunctionLengthArgument
	: expression
	;

/**
 * The ANSI SQL-style 'trim()' function
 */
trimFunction
	: TRIM LEFT_PAREN trimSpecification? trimCharacter? FROM? expression RIGHT_PAREN
	;

trimSpecification
	: LEADING
	| TRAILING
	| BOTH
	;

trimCharacter
	: STRING_LITERAL
	;

/**
 * A 'pad()' function inspired by 'trim()'
 */
padFunction
	: PAD LEFT_PAREN expression WITH padLength padSpecification padCharacter? RIGHT_PAREN
	;

padSpecification
	: LEADING
	| TRAILING
	;

padCharacter
	: STRING_LITERAL
	;

padLength
	: expression
	;

/**
 * The ANSI SQL-style 'overlay()' function
 */
overlayFunction
	: OVERLAY LEFT_PAREN overlayFunctionStringArgument PLACING overlayFunctionReplacementArgument FROM overlayFunctionStartArgument (FOR overlayFunctionLengthArgument)? RIGHT_PAREN
	;

overlayFunctionStringArgument
	: expression
	;

overlayFunctionReplacementArgument
	: expression
	;

overlayFunctionStartArgument
	: expression
	;

overlayFunctionLengthArgument
	: expression
	;

/**
 * The deprecated current_date function required by JPQL
 */
currentDateFunction
	: CURRENT_DATE (LEFT_PAREN RIGHT_PAREN)?
	| CURRENT DATE
	;

/**
 * The deprecated current_time function required by JPQL
 */
currentTimeFunction
	: CURRENT_TIME (LEFT_PAREN RIGHT_PAREN)?
	| CURRENT TIME
	;

/**
 * The deprecated current_timestamp function required by JPQL
 */
currentTimestampFunction
	: CURRENT_TIMESTAMP (LEFT_PAREN RIGHT_PAREN)?
	| CURRENT TIMESTAMP
	;

/**
 * The instant function, and deprecated current_instant function
 */
instantFunction
	: CURRENT_INSTANT (LEFT_PAREN RIGHT_PAREN)? //deprecated legacy syntax
	| INSTANT
	;

/**
 * The 'local datetime' function (or literal if you prefer)
 */
localDateTimeFunction
	: LOCAL_DATETIME (LEFT_PAREN RIGHT_PAREN)?
	| LOCAL DATETIME
	;

/**
 * The 'offset datetime' function (or literal if you prefer)
 */
offsetDateTimeFunction
	: OFFSET_DATETIME (LEFT_PAREN RIGHT_PAREN)?
	| OFFSET DATETIME
	;

/**
 * The 'local date' function (or literal if you prefer)
 */
localDateFunction
	: LOCAL_DATE (LEFT_PAREN RIGHT_PAREN)?
	| LOCAL DATE
	;

/**
 * The 'local time' function (or literal if you prefer)
 */
localTimeFunction
	: LOCAL_TIME (LEFT_PAREN RIGHT_PAREN)?
	| LOCAL TIME
	;

/**
 * The 'format()' function for formatting dates and times according to a pattern
 */
formatFunction
	: FORMAT LEFT_PAREN expression AS format RIGHT_PAREN
	;

/**
 * A format pattern, with a syntax inspired by by java.time.format.DateTimeFormatter
 *
 * see 'Dialect.appendDatetimeFormat()'
 */
format
	: STRING_LITERAL
	;

/**
 * The 'extract()' function for extracting fields of dates, times, and datetimes
 */
extractFunction
	: EXTRACT LEFT_PAREN extractField FROM expression RIGHT_PAREN
	| datetimeField LEFT_PAREN expression RIGHT_PAREN
	;

/**
 * A field that may be extracted from a date, time, or datetime
 */
extractField
	: datetimeField
	| dayField
	| weekField
	| timeZoneField
	| dateOrTimeField
	;

datetimeField
	: YEAR
	| MONTH
	| DAY
	| WEEK
	| QUARTER
	| HOUR
	| MINUTE
	| SECOND
	| NANOSECOND
	;

dayField
	: DAY OF MONTH
	| DAY OF WEEK
	| DAY OF YEAR
	;

weekField
	: WEEK OF MONTH
	| WEEK OF YEAR
	;

timeZoneField
	: OFFSET (HOUR | MINUTE)?
	| TIMEZONE_HOUR | TIMEZONE_MINUTE
	;

dateOrTimeField
	: DATE
	| TIME
	;

/**
 * The ANSI SQL-style 'position()' function
 */
positionFunction
	: POSITION LEFT_PAREN positionFunctionPatternArgument IN positionFunctionStringArgument RIGHT_PAREN
	;

positionFunctionPatternArgument
	: expression
	;

positionFunctionStringArgument
	: expression
	;

/**
 * The 'cube()' function specific to the 'group by' clause
 */
cube
	: CUBE LEFT_PAREN expressionOrPredicate (COMMA expressionOrPredicate)* RIGHT_PAREN
	;

/**
 * The 'rollup()' function specific to the 'group by' clause
 */
rollup
	: ROLLUP LEFT_PAREN expressionOrPredicate (COMMA expressionOrPredicate)* RIGHT_PAREN
	;

/**
 * Support for "soft" keywords which may be used as identifiers
 *
 * The `identifier` rule is used to provide "keyword as identifier" handling.
 *
 * The lexer hands us recognized keywords using their specific tokens.  This is important
 * for the recognition of sqm structure, especially in terms of performance!
 *
 * However we want to continue to allow users to use most keywords as identifiers (e.g., attribute names).
 * This parser rule helps with that.  Here we expect that the caller already understands their
 * context enough to know that keywords-as-identifiers are allowed.
 */
identifier
	: IDENTIFIER
	| QUOTED_IDENTIFIER
	| (ALL
	| AND
	| ANY
	| AS
	| ASC
	| BETWEEN
	| BOTH
	| BY
	| CASE
	| CAST
	| COLLATE
	| CROSS
	| CUBE
	| CURRENT
	| CURRENT_DATE
	| CURRENT_INSTANT
	| CURRENT_TIME
	| CURRENT_TIMESTAMP
	| DATE
	| DAY
	| DATETIME
	| DELETE
	| DESC
	| DISTINCT
	| ELEMENTS
	| ELSE
	| EMPTY
	| END
	| ENTRY
	| ESCAPE
	| EVERY
	| EXCEPT
	| EXISTS
	| EXTRACT
	| FETCH
	| FILTER
	| FIRST
	| FOR
	| FORMAT
	| FROM
	| FULL
	| FUNCTION
	| GROUP
	| HAVING
	| HOUR
	| ID
	| ILIKE
	| IN
	| INDEX
	| INDICES
	| INNER
	| INSERT
	| INSTANT
	| INTERSECT
	| INTO
	| IS
	| JOIN
	| KEY
	| LAST
	| LEADING
	| LEFT
	| LIKE
	| LIMIT
	| LIST
	| LOCAL
	| LOCAL_DATE
	| LOCAL_DATETIME
	| LOCAL_TIME
	| MAP
	| MAXELEMENT
	| MAXINDEX
	| MEMBER
	| MICROSECOND
	| MILLISECOND
	| MINELEMENT
	| MININDEX
	| MINUTE
	| MONTH
	| NANOSECOND
	| NATURALID
	| NEW
	| NEXT
	| NOT
	| NULLS
	| OBJECT
	| OF
	| OFFSET
	| OFFSET_DATETIME
	| ON
	| ONLY
	| OR
	| ORDER
	| OUTER
	| OVERLAY
	| PAD
	| PERCENT
	| PLACING
	| POSITION
	| QUARTER
	| RIGHT
	| ROLLUP
	| ROW
	| ROWS
	| SECOND
	| SELECT
	| SET
	| SIZE
	| SOME
	| SUBSTRING
	| THEN
	| TIES
	| TIME
	| TIMESTAMP
	| TIMEZONE_HOUR
	| TIMEZONE_MINUTE
	| TRAILING
	| TREAT
	| TRIM
	| TYPE
	| UNION
	| UPDATE
	| VALUE
	| VALUES
	| VERSION
	| VERSIONED
	| WEEK
	| WHEN
	| WHERE
	| WITH
	| YEAR) {
		logUseOfReservedWordAsIdentifier( getCurrentToken() );
	}
	;
