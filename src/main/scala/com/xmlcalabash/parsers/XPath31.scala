package com.xmlcalabash.parsers

// This file was generated on Thu Mar 18, 2021 17:28 (UTC) by REx v5.52 which is Copyright (c) 1979-2020 by Gunther Rademacher <grd@gmx.net>
// REx command line: XPath31.ebnf -ll 3 -scala -tree

import collection.mutable.ArrayBuffer

class XPath31 {

  def this(string: String, eh: XPath31.EventHandler) = {
    this
    initialize(string, eh)
  }

  def initialize(source: String, parsingEventHandler: XPath31.EventHandler): Unit = {
    eventHandler = parsingEventHandler
    input = source
    size = source.length
    reset(0, 0, 0)
  }

  def getInput: String = {
    return input
  }

  def getTokenOffset: Int = {
    return b0
  }

  def getTokenEnd: Int = {
    return e0
  }

  def reset(l: Int, b: Int, e: Int): Unit = {
            b0 = b; e0 = b
    l1 = l; b1 = b; e1 = e
    l2 = 0; b2 = 0; e2 = 0
    l3 = 0; b3 = 0; e3 = 0
    end = e
    eventHandler.reset(input)
  }

  def reset: Unit = {
    reset(0, 0, 0)
  }

  def parse_XPath: Unit = {
    eventHandler.startNonterminal("XPath", e0)
    lookahead1W(53)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_Expr
    consume(11)                     // EOF
    eventHandler.endNonterminal("XPath", e0)
  }

  private def parse_ParamList: Unit = {
    eventHandler.startNonterminal("ParamList", e0)
    parse_Param
    var c1 = true
    while (c1) {
      lookahead1W(17)               // S^WS | '(:' | ')' | ','
      if (l1 != 21) {               // ','
        c1 = false
      }
      else {
        consume(21)                 // ','
        lookahead1W(2)              // S^WS | '$' | '(:'
        whitespace
        parse_Param
      }
    }
    eventHandler.endNonterminal("ParamList", e0)
  }

  private def parse_Param: Unit = {
    eventHandler.startNonterminal("Param", e0)
    consume(15)                     // '$'
    lookahead1W(42)                 // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_EQName
    lookahead1W(21)                 // S^WS | '(:' | ')' | ',' | 'as'
    if (l1 == 47) {                 // 'as'
      whitespace
      parse_TypeDeclaration
    }
    eventHandler.endNonterminal("Param", e0)
  }

  private def parse_FunctionBody: Unit = {
    eventHandler.startNonterminal("FunctionBody", e0)
    parse_EnclosedExpr
    eventHandler.endNonterminal("FunctionBody", e0)
  }

  private def parse_EnclosedExpr: Unit = {
    eventHandler.startNonterminal("EnclosedExpr", e0)
    consume(104)                    // '{'
    lookahead1W(56)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union' | '}'
    if (l1 != 107) {                // '}'
      whitespace
      parse_Expr
    }
    consume(107)                    // '}'
    eventHandler.endNonterminal("EnclosedExpr", e0)
  }

  private def parse_Expr: Unit = {
    eventHandler.startNonterminal("Expr", e0)
    parse_ExprSingle
    var c1 = true
    while (c1) {
      if (l1 != 21) {               // ','
        c1 = false
      }
      else {
        consume(21)                 // ','
        lookahead1W(53)             // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace
        parse_ExprSingle
      }
    }
    eventHandler.endNonterminal("Expr", e0)
  }

  private def parse_ExprSingle: Unit = {
    eventHandler.startNonterminal("ExprSingle", e0)
    l1 match {
    case 70 =>                      // 'if'
      lookahead2W(34)               // S^WS | EOF | '!' | '!=' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' |
                                    // ':' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' | 'and' |
                                    // 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' |
                                    // 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' |
                                    // 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
    case 61                         // 'every'
       | 65                         // 'for'
       | 77                         // 'let'
       | 96 =>                      // 'some'
      lookahead2W(40)               // S^WS | EOF | '!' | '!=' | '#' | '$' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' |
                                    // '/' | '//' | ':' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' |
                                    // ']' | 'and' | 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' |
                                    // 'gt' | 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' |
                                    // 'or' | 'return' | 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
    case _ =>
      lk = l1
    }
    lk match {
    case 1985 =>                    // 'for' '$'
      parse_ForExpr
    case 1997 =>                    // 'let' '$'
      parse_LetExpr
    case 1981                       // 'every' '$'
       | 2016 =>                    // 'some' '$'
      parse_QuantifiedExpr
    case 2118 =>                    // 'if' '('
      parse_IfExpr
    case _ =>
      parse_OrExpr
    }
    eventHandler.endNonterminal("ExprSingle", e0)
  }

  private def parse_ForExpr: Unit = {
    eventHandler.startNonterminal("ForExpr", e0)
    parse_SimpleForClause
    consume(91)                     // 'return'
    lookahead1W(53)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_ExprSingle
    eventHandler.endNonterminal("ForExpr", e0)
  }

  private def parse_SimpleForClause: Unit = {
    eventHandler.startNonterminal("SimpleForClause", e0)
    consume(65)                     // 'for'
    lookahead1W(2)                  // S^WS | '$' | '(:'
    whitespace
    parse_SimpleForBinding
    var c1 = true
    while (c1) {
      if (l1 != 21) {               // ','
        c1 = false
      }
      else {
        consume(21)                 // ','
        lookahead1W(2)              // S^WS | '$' | '(:'
        whitespace
        parse_SimpleForBinding
      }
    }
    eventHandler.endNonterminal("SimpleForClause", e0)
  }

  private def parse_SimpleForBinding: Unit = {
    eventHandler.startNonterminal("SimpleForBinding", e0)
    consume(15)                     // '$'
    lookahead1W(42)                 // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_VarName
    lookahead1W(10)                 // S^WS | '(:' | 'in'
    consume(71)                     // 'in'
    lookahead1W(53)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_ExprSingle
    eventHandler.endNonterminal("SimpleForBinding", e0)
  }

  private def parse_LetExpr: Unit = {
    eventHandler.startNonterminal("LetExpr", e0)
    parse_SimpleLetClause
    consume(91)                     // 'return'
    lookahead1W(53)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_ExprSingle
    eventHandler.endNonterminal("LetExpr", e0)
  }

  private def parse_SimpleLetClause: Unit = {
    eventHandler.startNonterminal("SimpleLetClause", e0)
    consume(77)                     // 'let'
    lookahead1W(2)                  // S^WS | '$' | '(:'
    whitespace
    parse_SimpleLetBinding
    var c1 = true
    while (c1) {
      if (l1 != 21) {               // ','
        c1 = false
      }
      else {
        consume(21)                 // ','
        lookahead1W(2)              // S^WS | '$' | '(:'
        whitespace
        parse_SimpleLetBinding
      }
    }
    eventHandler.endNonterminal("SimpleLetClause", e0)
  }

  private def parse_SimpleLetBinding: Unit = {
    eventHandler.startNonterminal("SimpleLetBinding", e0)
    consume(15)                     // '$'
    lookahead1W(42)                 // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_VarName
    lookahead1W(8)                  // S^WS | '(:' | ':='
    consume(30)                     // ':='
    lookahead1W(53)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_ExprSingle
    eventHandler.endNonterminal("SimpleLetBinding", e0)
  }

  private def parse_QuantifiedExpr: Unit = {
    eventHandler.startNonterminal("QuantifiedExpr", e0)
    l1 match {
    case 96 =>                      // 'some'
      consume(96)                   // 'some'
    case _ =>
      consume(61)                   // 'every'
    }
    lookahead1W(2)                  // S^WS | '$' | '(:'
    consume(15)                     // '$'
    lookahead1W(42)                 // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_VarName
    lookahead1W(10)                 // S^WS | '(:' | 'in'
    consume(71)                     // 'in'
    lookahead1W(53)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_ExprSingle
    var c1 = true
    while (c1) {
      if (l1 != 21) {               // ','
        c1 = false
      }
      else {
        consume(21)                 // ','
        lookahead1W(2)              // S^WS | '$' | '(:'
        consume(15)                 // '$'
        lookahead1W(42)             // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace
        parse_VarName
        lookahead1W(10)             // S^WS | '(:' | 'in'
        consume(71)                 // 'in'
        lookahead1W(53)             // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace
        parse_ExprSingle
      }
    }
    consume(92)                     // 'satisfies'
    lookahead1W(53)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_ExprSingle
    eventHandler.endNonterminal("QuantifiedExpr", e0)
  }

  private def parse_IfExpr: Unit = {
    eventHandler.startNonterminal("IfExpr", e0)
    consume(70)                     // 'if'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(53)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_Expr
    consume(18)                     // ')'
    lookahead1W(12)                 // S^WS | '(:' | 'then'
    consume(99)                     // 'then'
    lookahead1W(53)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_ExprSingle
    consume(58)                     // 'else'
    lookahead1W(53)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_ExprSingle
    eventHandler.endNonterminal("IfExpr", e0)
  }

  private def parse_OrExpr: Unit = {
    eventHandler.startNonterminal("OrExpr", e0)
    parse_AndExpr
    var c1 = true
    while (c1) {
      if (l1 != 86) {               // 'or'
        c1 = false
      }
      else {
        consume(86)                 // 'or'
        lookahead1W(53)             // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace
        parse_AndExpr
      }
    }
    eventHandler.endNonterminal("OrExpr", e0)
  }

  private def parse_AndExpr: Unit = {
    eventHandler.startNonterminal("AndExpr", e0)
    parse_ComparisonExpr
    var c1 = true
    while (c1) {
      if (l1 != 45) {               // 'and'
        c1 = false
      }
      else {
        consume(45)                 // 'and'
        lookahead1W(53)             // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace
        parse_ComparisonExpr
      }
    }
    eventHandler.endNonterminal("AndExpr", e0)
  }

  private def parse_ComparisonExpr: Unit = {
    eventHandler.startNonterminal("ComparisonExpr", e0)
    parse_StringConcatExpr
    if (l1 != 11                    // EOF
     && l1 != 18                    // ')'
     && l1 != 21                    // ','
     && l1 != 27                    // ':'
     && l1 != 42                    // ']'
     && l1 != 45                    // 'and'
     && l1 != 58                    // 'else'
     && l1 != 86                    // 'or'
     && l1 != 91                    // 'return'
     && l1 != 92                    // 'satisfies'
     && l1 != 107) {                // '}'
      l1 match {
      case 60                       // 'eq'
         | 67                       // 'ge'
         | 68                       // 'gt'
         | 76                       // 'le'
         | 78                       // 'lt'
         | 83 =>                    // 'ne'
        whitespace
        parse_ValueComp
      case 32                       // '<<'
         | 38                       // '>>'
         | 74 =>                    // 'is'
        whitespace
        parse_NodeComp
      case _ =>
        whitespace
        parse_GeneralComp
      }
      lookahead1W(53)               // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace
      parse_StringConcatExpr
    }
    eventHandler.endNonterminal("ComparisonExpr", e0)
  }

  private def parse_StringConcatExpr: Unit = {
    eventHandler.startNonterminal("StringConcatExpr", e0)
    parse_RangeExpr
    var c1 = true
    while (c1) {
      if (l1 != 106) {              // '||'
        c1 = false
      }
      else {
        consume(106)                // '||'
        lookahead1W(53)             // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace
        parse_RangeExpr
      }
    }
    eventHandler.endNonterminal("StringConcatExpr", e0)
  }

  private def parse_RangeExpr: Unit = {
    eventHandler.startNonterminal("RangeExpr", e0)
    parse_AdditiveExpr
    if (l1 == 100) {                // 'to'
      consume(100)                  // 'to'
      lookahead1W(53)               // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace
      parse_AdditiveExpr
    }
    eventHandler.endNonterminal("RangeExpr", e0)
  }

  private def parse_AdditiveExpr: Unit = {
    eventHandler.startNonterminal("AdditiveExpr", e0)
    parse_MultiplicativeExpr
    var c1 = true
    while (c1) {
      if (l1 != 20                  // '+'
       && l1 != 22) {               // '-'
        c1 = false
      }
      else {
        l1 match {
        case 20 =>                  // '+'
          consume(20)               // '+'
        case _ =>
          consume(22)               // '-'
        }
        lookahead1W(53)             // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace
        parse_MultiplicativeExpr
      }
    }
    eventHandler.endNonterminal("AdditiveExpr", e0)
  }

  private def parse_MultiplicativeExpr: Unit = {
    eventHandler.startNonterminal("MultiplicativeExpr", e0)
    parse_UnionExpr
    var c1 = true
    while (c1) {
      if (l1 != 19                  // '*'
       && l1 != 55                  // 'div'
       && l1 != 69                  // 'idiv'
       && l1 != 80) {               // 'mod'
        c1 = false
      }
      else {
        l1 match {
        case 19 =>                  // '*'
          consume(19)               // '*'
        case 55 =>                  // 'div'
          consume(55)               // 'div'
        case 69 =>                  // 'idiv'
          consume(69)               // 'idiv'
        case _ =>
          consume(80)               // 'mod'
        }
        lookahead1W(53)             // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace
        parse_UnionExpr
      }
    }
    eventHandler.endNonterminal("MultiplicativeExpr", e0)
  }

  private def parse_UnionExpr: Unit = {
    eventHandler.startNonterminal("UnionExpr", e0)
    parse_IntersectExceptExpr
    var c1 = true
    while (c1) {
      if (l1 != 103                 // 'union'
       && l1 != 105) {              // '|'
        c1 = false
      }
      else {
        l1 match {
        case 103 =>                 // 'union'
          consume(103)              // 'union'
        case _ =>
          consume(105)              // '|'
        }
        lookahead1W(53)             // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace
        parse_IntersectExceptExpr
      }
    }
    eventHandler.endNonterminal("UnionExpr", e0)
  }

  private def parse_IntersectExceptExpr: Unit = {
    eventHandler.startNonterminal("IntersectExceptExpr", e0)
    parse_InstanceofExpr
    var c1 = true
    while (c1) {
      lookahead1W(25)               // S^WS | EOF | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '>' | '>=' | '>>' | ']' | 'and' | 'div' | 'else' | 'eq' | 'except' |
                                    // 'ge' | 'gt' | 'idiv' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' |
                                    // 'return' | 'satisfies' | 'to' | 'union' | '|' | '||' | '}'
      if (l1 != 62                  // 'except'
       && l1 != 73) {               // 'intersect'
        c1 = false
      }
      else {
        l1 match {
        case 73 =>                  // 'intersect'
          consume(73)               // 'intersect'
        case _ =>
          consume(62)               // 'except'
        }
        lookahead1W(53)             // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace
        parse_InstanceofExpr
      }
    }
    eventHandler.endNonterminal("IntersectExceptExpr", e0)
  }

  private def parse_InstanceofExpr: Unit = {
    eventHandler.startNonterminal("InstanceofExpr", e0)
    parse_TreatExpr
    lookahead1W(26)                 // S^WS | EOF | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '>' | '>=' | '>>' | ']' | 'and' | 'div' | 'else' | 'eq' | 'except' |
                                    // 'ge' | 'gt' | 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' |
                                    // 'ne' | 'or' | 'return' | 'satisfies' | 'to' | 'union' | '|' | '||' | '}'
    if (l1 == 72) {                 // 'instance'
      consume(72)                   // 'instance'
      lookahead1W(11)               // S^WS | '(:' | 'of'
      consume(85)                   // 'of'
      lookahead1W(44)               // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace
      parse_SequenceType
    }
    eventHandler.endNonterminal("InstanceofExpr", e0)
  }

  private def parse_TreatExpr: Unit = {
    eventHandler.startNonterminal("TreatExpr", e0)
    parse_CastableExpr
    lookahead1W(27)                 // S^WS | EOF | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '>' | '>=' | '>>' | ']' | 'and' | 'div' | 'else' | 'eq' | 'except' |
                                    // 'ge' | 'gt' | 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' |
                                    // 'ne' | 'or' | 'return' | 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' |
                                    // '}'
    if (l1 == 101) {                // 'treat'
      consume(101)                  // 'treat'
      lookahead1W(9)                // S^WS | '(:' | 'as'
      consume(47)                   // 'as'
      lookahead1W(44)               // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace
      parse_SequenceType
    }
    eventHandler.endNonterminal("TreatExpr", e0)
  }

  private def parse_CastableExpr: Unit = {
    eventHandler.startNonterminal("CastableExpr", e0)
    parse_CastExpr
    lookahead1W(29)                 // S^WS | EOF | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '>' | '>=' | '>>' | ']' | 'and' | 'castable' | 'div' | 'else' |
                                    // 'eq' | 'except' | 'ge' | 'gt' | 'idiv' | 'instance' | 'intersect' | 'is' | 'le' |
                                    // 'lt' | 'mod' | 'ne' | 'or' | 'return' | 'satisfies' | 'to' | 'treat' | 'union' |
                                    // '|' | '||' | '}'
    if (l1 == 50) {                 // 'castable'
      consume(50)                   // 'castable'
      lookahead1W(9)                // S^WS | '(:' | 'as'
      consume(47)                   // 'as'
      lookahead1W(42)               // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace
      parse_SingleType
    }
    eventHandler.endNonterminal("CastableExpr", e0)
  }

  private def parse_CastExpr: Unit = {
    eventHandler.startNonterminal("CastExpr", e0)
    parse_ArrowExpr
    if (l1 == 49) {                 // 'cast'
      consume(49)                   // 'cast'
      lookahead1W(9)                // S^WS | '(:' | 'as'
      consume(47)                   // 'as'
      lookahead1W(42)               // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace
      parse_SingleType
    }
    eventHandler.endNonterminal("CastExpr", e0)
  }

  private def parse_ArrowExpr: Unit = {
    eventHandler.startNonterminal("ArrowExpr", e0)
    parse_UnaryExpr
    var c1 = true
    while (c1) {
      lookahead1W(32)               // S^WS | EOF | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '=>' | '>' | '>=' | '>>' | ']' | 'and' | 'cast' | 'castable' |
                                    // 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' | 'instance' |
                                    // 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' | 'satisfies' |
                                    // 'to' | 'treat' | 'union' | '|' | '||' | '}'
      if (l1 != 35) {               // '=>'
        c1 = false
      }
      else {
        consume(35)                 // '=>'
        lookahead1W(46)             // URIQualifiedName | QName^Token | S^WS | '$' | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace
        parse_ArrowFunctionSpecifier
        lookahead1W(3)              // S^WS | '(' | '(:'
        whitespace
        parse_ArgumentList
      }
    }
    eventHandler.endNonterminal("ArrowExpr", e0)
  }

  private def parse_UnaryExpr: Unit = {
    eventHandler.startNonterminal("UnaryExpr", e0)
    var c1 = true
    while (c1) {
      lookahead1W(53)               // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      if (l1 != 20                  // '+'
       && l1 != 22) {               // '-'
        c1 = false
      }
      else {
        l1 match {
        case 22 =>                  // '-'
          consume(22)               // '-'
        case _ =>
          consume(20)               // '+'
        }
      }
    }
    whitespace
    parse_ValueExpr
    eventHandler.endNonterminal("UnaryExpr", e0)
  }

  private def parse_ValueExpr: Unit = {
    eventHandler.startNonterminal("ValueExpr", e0)
    parse_SimpleMapExpr
    eventHandler.endNonterminal("ValueExpr", e0)
  }

  private def parse_GeneralComp: Unit = {
    eventHandler.startNonterminal("GeneralComp", e0)
    l1 match {
    case 34 =>                      // '='
      consume(34)                   // '='
    case 13 =>                      // '!='
      consume(13)                   // '!='
    case 31 =>                      // '<'
      consume(31)                   // '<'
    case 33 =>                      // '<='
      consume(33)                   // '<='
    case 36 =>                      // '>'
      consume(36)                   // '>'
    case _ =>
      consume(37)                   // '>='
    }
    eventHandler.endNonterminal("GeneralComp", e0)
  }

  private def parse_ValueComp: Unit = {
    eventHandler.startNonterminal("ValueComp", e0)
    l1 match {
    case 60 =>                      // 'eq'
      consume(60)                   // 'eq'
    case 83 =>                      // 'ne'
      consume(83)                   // 'ne'
    case 78 =>                      // 'lt'
      consume(78)                   // 'lt'
    case 76 =>                      // 'le'
      consume(76)                   // 'le'
    case 68 =>                      // 'gt'
      consume(68)                   // 'gt'
    case _ =>
      consume(67)                   // 'ge'
    }
    eventHandler.endNonterminal("ValueComp", e0)
  }

  private def parse_NodeComp: Unit = {
    eventHandler.startNonterminal("NodeComp", e0)
    l1 match {
    case 74 =>                      // 'is'
      consume(74)                   // 'is'
    case 32 =>                      // '<<'
      consume(32)                   // '<<'
    case _ =>
      consume(38)                   // '>>'
    }
    eventHandler.endNonterminal("NodeComp", e0)
  }

  private def parse_SimpleMapExpr: Unit = {
    eventHandler.startNonterminal("SimpleMapExpr", e0)
    parse_PathExpr
    var c1 = true
    while (c1) {
      if (l1 != 12) {               // '!'
        c1 = false
      }
      else {
        consume(12)                 // '!'
        lookahead1W(52)             // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '.' |
                                    // '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' | 'and' |
                                    // 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace
        parse_PathExpr
      }
    }
    eventHandler.endNonterminal("SimpleMapExpr", e0)
  }

  private def parse_PathExpr: Unit = {
    eventHandler.startNonterminal("PathExpr", e0)
    l1 match {
    case 25 =>                      // '/'
      consume(25)                   // '/'
      lookahead1W(57)               // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | EOF | '!' | '!=' | '$' | '(' |
                                    // '(:' | ')' | '*' | '+' | ',' | '-' | '.' | '..' | ':' | '<' | '<<' | '<=' | '=' |
                                    // '=>' | '>' | '>=' | '>>' | '?' | '@' | '[' | ']' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union' | '|' |
                                    // '||' | '}'
      l1 match {
      case 11                       // EOF
         | 12                       // '!'
         | 13                       // '!='
         | 18                       // ')'
         | 19                       // '*'
         | 20                       // '+'
         | 21                       // ','
         | 22                       // '-'
         | 27                       // ':'
         | 31                       // '<'
         | 32                       // '<<'
         | 33                       // '<='
         | 34                       // '='
         | 35                       // '=>'
         | 36                       // '>'
         | 37                       // '>='
         | 38                       // '>>'
         | 42                       // ']'
         | 105                      // '|'
         | 106                      // '||'
         | 107 =>                   // '}'
      case _ =>
        whitespace
        parse_RelativePathExpr
      }
    case 26 =>                      // '//'
      consume(26)                   // '//'
      lookahead1W(51)               // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '.' |
                                    // '..' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' | 'and' | 'array' |
                                    // 'attribute' | 'cast' | 'castable' | 'child' | 'comment' | 'descendant' |
                                    // 'descendant-or-self' | 'div' | 'document-node' | 'element' | 'else' |
                                    // 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace
      parse_RelativePathExpr
    case _ =>
      parse_RelativePathExpr
    }
    eventHandler.endNonterminal("PathExpr", e0)
  }

  private def parse_RelativePathExpr: Unit = {
    eventHandler.startNonterminal("RelativePathExpr", e0)
    parse_StepExpr
    var c1 = true
    while (c1) {
      if (l1 != 25                  // '/'
       && l1 != 26) {               // '//'
        c1 = false
      }
      else {
        l1 match {
        case 25 =>                  // '/'
          consume(25)               // '/'
        case _ =>
          consume(26)               // '//'
        }
        lookahead1W(51)             // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '.' |
                                    // '..' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' | 'and' | 'array' |
                                    // 'attribute' | 'cast' | 'castable' | 'child' | 'comment' | 'descendant' |
                                    // 'descendant-or-self' | 'div' | 'document-node' | 'element' | 'else' |
                                    // 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace
        parse_StepExpr
      }
    }
    eventHandler.endNonterminal("RelativePathExpr", e0)
  }

  private def parse_StepExpr: Unit = {
    eventHandler.startNonterminal("StepExpr", e0)
    l1 match {
    case 66 =>                      // 'function'
      lookahead2W(34)               // S^WS | EOF | '!' | '!=' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' |
                                    // ':' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' | 'and' |
                                    // 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' |
                                    // 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' |
                                    // 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
    case 46                         // 'array'
       | 79 =>                      // 'map'
      lookahead2W(36)               // S^WS | EOF | '!' | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' | ':' |
                                    // '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' | 'and' | 'cast' |
                                    // 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' |
                                    // 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' |
                                    // 'satisfies' | 'to' | 'treat' | 'union' | '{' | '|' | '||' | '}'
    case 43                         // 'ancestor'
       | 44                         // 'ancestor-or-self'
       | 51                         // 'child'
       | 53                         // 'descendant'
       | 54                         // 'descendant-or-self'
       | 63                         // 'following'
       | 64                         // 'following-sibling'
       | 81                         // 'namespace'
       | 87                         // 'parent'
       | 88                         // 'preceding'
       | 89                         // 'preceding-sibling'
       | 95 =>                      // 'self'
      lookahead2W(41)               // S^WS | EOF | '!' | '!=' | '#' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' |
                                    // '//' | ':' | '::' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' |
                                    // ']' | 'and' | 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' |
                                    // 'gt' | 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' |
                                    // 'or' | 'return' | 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
    case 5                          // URIQualifiedName
       | 7                          // QName^Token
       | 45                         // 'and'
       | 49                         // 'cast'
       | 50                         // 'castable'
       | 55                         // 'div'
       | 58                         // 'else'
       | 60                         // 'eq'
       | 61                         // 'every'
       | 62                         // 'except'
       | 65                         // 'for'
       | 67                         // 'ge'
       | 68                         // 'gt'
       | 69                         // 'idiv'
       | 72                         // 'instance'
       | 73                         // 'intersect'
       | 74                         // 'is'
       | 76                         // 'le'
       | 77                         // 'let'
       | 78                         // 'lt'
       | 80                         // 'mod'
       | 83                         // 'ne'
       | 86                         // 'or'
       | 91                         // 'return'
       | 92                         // 'satisfies'
       | 96                         // 'some'
       | 100                        // 'to'
       | 101                        // 'treat'
       | 103 =>                     // 'union'
      lookahead2W(37)               // S^WS | EOF | '!' | '!=' | '#' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' |
                                    // '//' | ':' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' |
                                    // 'and' | 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' |
                                    // 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' |
                                    // 'return' | 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
    case _ =>
      lk = l1
    }
    lk match {
    case 1                          // IntegerLiteral
       | 2                          // DecimalLiteral
       | 3                          // DoubleLiteral
       | 4                          // StringLiteral
       | 15                         // '$'
       | 16                         // '('
       | 23                         // '.'
       | 39                         // '?'
       | 41                         // '['
       | 1797                       // URIQualifiedName '#'
       | 1799                       // QName^Token '#'
       | 1835                       // 'ancestor' '#'
       | 1836                       // 'ancestor-or-self' '#'
       | 1837                       // 'and' '#'
       | 1841                       // 'cast' '#'
       | 1842                       // 'castable' '#'
       | 1843                       // 'child' '#'
       | 1845                       // 'descendant' '#'
       | 1846                       // 'descendant-or-self' '#'
       | 1847                       // 'div' '#'
       | 1850                       // 'else' '#'
       | 1852                       // 'eq' '#'
       | 1853                       // 'every' '#'
       | 1854                       // 'except' '#'
       | 1855                       // 'following' '#'
       | 1856                       // 'following-sibling' '#'
       | 1857                       // 'for' '#'
       | 1859                       // 'ge' '#'
       | 1860                       // 'gt' '#'
       | 1861                       // 'idiv' '#'
       | 1864                       // 'instance' '#'
       | 1865                       // 'intersect' '#'
       | 1866                       // 'is' '#'
       | 1868                       // 'le' '#'
       | 1869                       // 'let' '#'
       | 1870                       // 'lt' '#'
       | 1872                       // 'mod' '#'
       | 1873                       // 'namespace' '#'
       | 1875                       // 'ne' '#'
       | 1878                       // 'or' '#'
       | 1879                       // 'parent' '#'
       | 1880                       // 'preceding' '#'
       | 1881                       // 'preceding-sibling' '#'
       | 1883                       // 'return' '#'
       | 1884                       // 'satisfies' '#'
       | 1887                       // 'self' '#'
       | 1888                       // 'some' '#'
       | 1892                       // 'to' '#'
       | 1893                       // 'treat' '#'
       | 1895                       // 'union' '#'
       | 2053                       // URIQualifiedName '('
       | 2055                       // QName^Token '('
       | 2091                       // 'ancestor' '('
       | 2092                       // 'ancestor-or-self' '('
       | 2093                       // 'and' '('
       | 2097                       // 'cast' '('
       | 2098                       // 'castable' '('
       | 2099                       // 'child' '('
       | 2101                       // 'descendant' '('
       | 2102                       // 'descendant-or-self' '('
       | 2103                       // 'div' '('
       | 2106                       // 'else' '('
       | 2108                       // 'eq' '('
       | 2109                       // 'every' '('
       | 2110                       // 'except' '('
       | 2111                       // 'following' '('
       | 2112                       // 'following-sibling' '('
       | 2113                       // 'for' '('
       | 2114                       // 'function' '('
       | 2115                       // 'ge' '('
       | 2116                       // 'gt' '('
       | 2117                       // 'idiv' '('
       | 2120                       // 'instance' '('
       | 2121                       // 'intersect' '('
       | 2122                       // 'is' '('
       | 2124                       // 'le' '('
       | 2125                       // 'let' '('
       | 2126                       // 'lt' '('
       | 2128                       // 'mod' '('
       | 2129                       // 'namespace' '('
       | 2131                       // 'ne' '('
       | 2134                       // 'or' '('
       | 2135                       // 'parent' '('
       | 2136                       // 'preceding' '('
       | 2137                       // 'preceding-sibling' '('
       | 2139                       // 'return' '('
       | 2140                       // 'satisfies' '('
       | 2143                       // 'self' '('
       | 2144                       // 'some' '('
       | 2148                       // 'to' '('
       | 2149                       // 'treat' '('
       | 2151                       // 'union' '('
       | 13358                      // 'array' '{'
       | 13391 =>                   // 'map' '{'
      parse_PostfixExpr
    case _ =>
      parse_AxisStep
    }
    eventHandler.endNonterminal("StepExpr", e0)
  }

  private def parse_AxisStep: Unit = {
    eventHandler.startNonterminal("AxisStep", e0)
    l1 match {
    case 43                         // 'ancestor'
       | 44                         // 'ancestor-or-self'
       | 87                         // 'parent'
       | 88                         // 'preceding'
       | 89 =>                      // 'preceding-sibling'
      lookahead2W(35)               // S^WS | EOF | '!' | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' | ':' |
                                    // '::' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' | 'and' |
                                    // 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' |
                                    // 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' |
                                    // 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
    case _ =>
      lk = l1
    }
    lk match {
    case 24                         // '..'
       | 3755                       // 'ancestor' '::'
       | 3756                       // 'ancestor-or-self' '::'
       | 3799                       // 'parent' '::'
       | 3800                       // 'preceding' '::'
       | 3801 =>                    // 'preceding-sibling' '::'
      parse_ReverseStep
    case _ =>
      parse_ForwardStep
    }
    lookahead1W(33)                 // S^WS | EOF | '!' | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' | ':' |
                                    // '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' | 'and' | 'cast' |
                                    // 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' |
                                    // 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' |
                                    // 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
    whitespace
    parse_PredicateList
    eventHandler.endNonterminal("AxisStep", e0)
  }

  private def parse_ForwardStep: Unit = {
    eventHandler.startNonterminal("ForwardStep", e0)
    l1 match {
    case 48 =>                      // 'attribute'
      lookahead2W(38)               // S^WS | EOF | '!' | '!=' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' |
                                    // ':' | '::' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' |
                                    // 'and' | 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' |
                                    // 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' |
                                    // 'return' | 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
    case 51                         // 'child'
       | 53                         // 'descendant'
       | 54                         // 'descendant-or-self'
       | 63                         // 'following'
       | 64                         // 'following-sibling'
       | 81                         // 'namespace'
       | 95 =>                      // 'self'
      lookahead2W(35)               // S^WS | EOF | '!' | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' | ':' |
                                    // '::' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' | 'and' |
                                    // 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' |
                                    // 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' |
                                    // 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
    case _ =>
      lk = l1
    }
    lk match {
    case 3760                       // 'attribute' '::'
       | 3763                       // 'child' '::'
       | 3765                       // 'descendant' '::'
       | 3766                       // 'descendant-or-self' '::'
       | 3775                       // 'following' '::'
       | 3776                       // 'following-sibling' '::'
       | 3793                       // 'namespace' '::'
       | 3807 =>                    // 'self' '::'
      parse_ForwardAxis
      lookahead1W(43)               // URIQualifiedName | QName^Token | S^WS | Wildcard | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace
      parse_NodeTest
    case _ =>
      parse_AbbrevForwardStep
    }
    eventHandler.endNonterminal("ForwardStep", e0)
  }

  private def parse_ForwardAxis: Unit = {
    eventHandler.startNonterminal("ForwardAxis", e0)
    l1 match {
    case 51 =>                      // 'child'
      consume(51)                   // 'child'
      lookahead1W(7)                // S^WS | '(:' | '::'
      consume(29)                   // '::'
    case 53 =>                      // 'descendant'
      consume(53)                   // 'descendant'
      lookahead1W(7)                // S^WS | '(:' | '::'
      consume(29)                   // '::'
    case 48 =>                      // 'attribute'
      consume(48)                   // 'attribute'
      lookahead1W(7)                // S^WS | '(:' | '::'
      consume(29)                   // '::'
    case 95 =>                      // 'self'
      consume(95)                   // 'self'
      lookahead1W(7)                // S^WS | '(:' | '::'
      consume(29)                   // '::'
    case 54 =>                      // 'descendant-or-self'
      consume(54)                   // 'descendant-or-self'
      lookahead1W(7)                // S^WS | '(:' | '::'
      consume(29)                   // '::'
    case 64 =>                      // 'following-sibling'
      consume(64)                   // 'following-sibling'
      lookahead1W(7)                // S^WS | '(:' | '::'
      consume(29)                   // '::'
    case 63 =>                      // 'following'
      consume(63)                   // 'following'
      lookahead1W(7)                // S^WS | '(:' | '::'
      consume(29)                   // '::'
    case _ =>
      consume(81)                   // 'namespace'
      lookahead1W(7)                // S^WS | '(:' | '::'
      consume(29)                   // '::'
    }
    eventHandler.endNonterminal("ForwardAxis", e0)
  }

  private def parse_AbbrevForwardStep: Unit = {
    eventHandler.startNonterminal("AbbrevForwardStep", e0)
    if (l1 == 40) {                 // '@'
      consume(40)                   // '@'
    }
    lookahead1W(43)                 // URIQualifiedName | QName^Token | S^WS | Wildcard | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_NodeTest
    eventHandler.endNonterminal("AbbrevForwardStep", e0)
  }

  private def parse_ReverseStep: Unit = {
    eventHandler.startNonterminal("ReverseStep", e0)
    l1 match {
    case 24 =>                      // '..'
      parse_AbbrevReverseStep
    case _ =>
      parse_ReverseAxis
      lookahead1W(43)               // URIQualifiedName | QName^Token | S^WS | Wildcard | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace
      parse_NodeTest
    }
    eventHandler.endNonterminal("ReverseStep", e0)
  }

  private def parse_ReverseAxis: Unit = {
    eventHandler.startNonterminal("ReverseAxis", e0)
    l1 match {
    case 87 =>                      // 'parent'
      consume(87)                   // 'parent'
      lookahead1W(7)                // S^WS | '(:' | '::'
      consume(29)                   // '::'
    case 43 =>                      // 'ancestor'
      consume(43)                   // 'ancestor'
      lookahead1W(7)                // S^WS | '(:' | '::'
      consume(29)                   // '::'
    case 89 =>                      // 'preceding-sibling'
      consume(89)                   // 'preceding-sibling'
      lookahead1W(7)                // S^WS | '(:' | '::'
      consume(29)                   // '::'
    case 88 =>                      // 'preceding'
      consume(88)                   // 'preceding'
      lookahead1W(7)                // S^WS | '(:' | '::'
      consume(29)                   // '::'
    case _ =>
      consume(44)                   // 'ancestor-or-self'
      lookahead1W(7)                // S^WS | '(:' | '::'
      consume(29)                   // '::'
    }
    eventHandler.endNonterminal("ReverseAxis", e0)
  }

  private def parse_AbbrevReverseStep: Unit = {
    eventHandler.startNonterminal("AbbrevReverseStep", e0)
    consume(24)                     // '..'
    eventHandler.endNonterminal("AbbrevReverseStep", e0)
  }

  private def parse_NodeTest: Unit = {
    eventHandler.startNonterminal("NodeTest", e0)
    l1 match {
    case 48                         // 'attribute'
       | 52                         // 'comment'
       | 56                         // 'document-node'
       | 57                         // 'element'
       | 82                         // 'namespace-node'
       | 84                         // 'node'
       | 90                         // 'processing-instruction'
       | 93                         // 'schema-attribute'
       | 94                         // 'schema-element'
       | 98 =>                      // 'text'
      lookahead2W(34)               // S^WS | EOF | '!' | '!=' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' |
                                    // ':' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' | 'and' |
                                    // 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' |
                                    // 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' |
                                    // 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
    case _ =>
      lk = l1
    }
    lk match {
    case 2096                       // 'attribute' '('
       | 2100                       // 'comment' '('
       | 2104                       // 'document-node' '('
       | 2105                       // 'element' '('
       | 2130                       // 'namespace-node' '('
       | 2132                       // 'node' '('
       | 2138                       // 'processing-instruction' '('
       | 2141                       // 'schema-attribute' '('
       | 2142                       // 'schema-element' '('
       | 2146 =>                    // 'text' '('
      parse_KindTest
    case _ =>
      parse_NameTest
    }
    eventHandler.endNonterminal("NodeTest", e0)
  }

  private def parse_NameTest: Unit = {
    eventHandler.startNonterminal("NameTest", e0)
    l1 match {
    case 10 =>                      // Wildcard
      consume(10)                   // Wildcard
    case _ =>
      parse_EQName
    }
    eventHandler.endNonterminal("NameTest", e0)
  }

  private def parse_PostfixExpr: Unit = {
    eventHandler.startNonterminal("PostfixExpr", e0)
    parse_PrimaryExpr
    var c1 = true
    while (c1) {
      lookahead1W(39)               // S^WS | EOF | '!' | '!=' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' |
                                    // ':' | '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '?' | '[' | ']' |
                                    // 'and' | 'cast' | 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' |
                                    // 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' |
                                    // 'return' | 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
      if (l1 != 16                  // '('
       && l1 != 39                  // '?'
       && l1 != 41) {               // '['
        c1 = false
      }
      else {
        l1 match {
        case 41 =>                  // '['
          whitespace
          parse_Predicate
        case 16 =>                  // '('
          whitespace
          parse_ArgumentList
        case _ =>
          whitespace
          parse_Lookup
        }
      }
    }
    eventHandler.endNonterminal("PostfixExpr", e0)
  }

  private def parse_ArgumentList: Unit = {
    eventHandler.startNonterminal("ArgumentList", e0)
    consume(16)                     // '('
    lookahead1W(54)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | ')' | '+' |
                                    // '-' | '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    if (l1 != 18) {                 // ')'
      whitespace
      parse_Argument
      var c1 = true
      while (c1) {
        lookahead1W(17)             // S^WS | '(:' | ')' | ','
        if (l1 != 21) {             // ','
          c1 = false
        }
        else {
          consume(21)               // ','
          lookahead1W(53)           // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
          whitespace
          parse_Argument
        }
      }
    }
    consume(18)                     // ')'
    eventHandler.endNonterminal("ArgumentList", e0)
  }

  private def parse_PredicateList: Unit = {
    eventHandler.startNonterminal("PredicateList", e0)
    var c1 = true
    while (c1) {
      lookahead1W(33)               // S^WS | EOF | '!' | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | '/' | '//' | ':' |
                                    // '<' | '<<' | '<=' | '=' | '=>' | '>' | '>=' | '>>' | '[' | ']' | 'and' | 'cast' |
                                    // 'castable' | 'div' | 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' |
                                    // 'instance' | 'intersect' | 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' |
                                    // 'satisfies' | 'to' | 'treat' | 'union' | '|' | '||' | '}'
      if (l1 != 41) {               // '['
        c1 = false
      }
      else {
        whitespace
        parse_Predicate
      }
    }
    eventHandler.endNonterminal("PredicateList", e0)
  }

  private def parse_Predicate: Unit = {
    eventHandler.startNonterminal("Predicate", e0)
    consume(41)                     // '['
    lookahead1W(53)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_Expr
    consume(42)                     // ']'
    eventHandler.endNonterminal("Predicate", e0)
  }

  private def parse_Lookup: Unit = {
    eventHandler.startNonterminal("Lookup", e0)
    consume(39)                     // '?'
    lookahead1W(23)                 // IntegerLiteral | NCName | S^WS | '(' | '(:' | '*'
    whitespace
    parse_KeySpecifier
    eventHandler.endNonterminal("Lookup", e0)
  }

  private def parse_KeySpecifier: Unit = {
    eventHandler.startNonterminal("KeySpecifier", e0)
    l1 match {
    case 6 =>                       // NCName
      consume(6)                    // NCName
    case 1 =>                       // IntegerLiteral
      consume(1)                    // IntegerLiteral
    case 16 =>                      // '('
      parse_ParenthesizedExpr
    case _ =>
      consume(19)                   // '*'
    }
    eventHandler.endNonterminal("KeySpecifier", e0)
  }

  private def parse_ArrowFunctionSpecifier: Unit = {
    eventHandler.startNonterminal("ArrowFunctionSpecifier", e0)
    l1 match {
    case 15 =>                      // '$'
      parse_VarRef
    case 16 =>                      // '('
      parse_ParenthesizedExpr
    case _ =>
      parse_EQName
    }
    eventHandler.endNonterminal("ArrowFunctionSpecifier", e0)
  }

  private def parse_PrimaryExpr: Unit = {
    eventHandler.startNonterminal("PrimaryExpr", e0)
    l1 match {
    case 5                          // URIQualifiedName
       | 7                          // QName^Token
       | 43                         // 'ancestor'
       | 44                         // 'ancestor-or-self'
       | 45                         // 'and'
       | 49                         // 'cast'
       | 50                         // 'castable'
       | 51                         // 'child'
       | 53                         // 'descendant'
       | 54                         // 'descendant-or-self'
       | 55                         // 'div'
       | 58                         // 'else'
       | 60                         // 'eq'
       | 61                         // 'every'
       | 62                         // 'except'
       | 63                         // 'following'
       | 64                         // 'following-sibling'
       | 65                         // 'for'
       | 67                         // 'ge'
       | 68                         // 'gt'
       | 69                         // 'idiv'
       | 72                         // 'instance'
       | 73                         // 'intersect'
       | 74                         // 'is'
       | 76                         // 'le'
       | 77                         // 'let'
       | 78                         // 'lt'
       | 80                         // 'mod'
       | 81                         // 'namespace'
       | 83                         // 'ne'
       | 86                         // 'or'
       | 87                         // 'parent'
       | 88                         // 'preceding'
       | 89                         // 'preceding-sibling'
       | 91                         // 'return'
       | 92                         // 'satisfies'
       | 95                         // 'self'
       | 96                         // 'some'
       | 100                        // 'to'
       | 101                        // 'treat'
       | 103 =>                     // 'union'
      lookahead2W(15)               // S^WS | '#' | '(' | '(:'
    case _ =>
      lk = l1
    }
    lk match {
    case 1                          // IntegerLiteral
       | 2                          // DecimalLiteral
       | 3                          // DoubleLiteral
       | 4 =>                       // StringLiteral
      parse_Literal
    case 15 =>                      // '$'
      parse_VarRef
    case 16 =>                      // '('
      parse_ParenthesizedExpr
    case 23 =>                      // '.'
      parse_ContextItemExpr
    case 2053                       // URIQualifiedName '('
       | 2055                       // QName^Token '('
       | 2091                       // 'ancestor' '('
       | 2092                       // 'ancestor-or-self' '('
       | 2093                       // 'and' '('
       | 2097                       // 'cast' '('
       | 2098                       // 'castable' '('
       | 2099                       // 'child' '('
       | 2101                       // 'descendant' '('
       | 2102                       // 'descendant-or-self' '('
       | 2103                       // 'div' '('
       | 2106                       // 'else' '('
       | 2108                       // 'eq' '('
       | 2109                       // 'every' '('
       | 2110                       // 'except' '('
       | 2111                       // 'following' '('
       | 2112                       // 'following-sibling' '('
       | 2113                       // 'for' '('
       | 2115                       // 'ge' '('
       | 2116                       // 'gt' '('
       | 2117                       // 'idiv' '('
       | 2120                       // 'instance' '('
       | 2121                       // 'intersect' '('
       | 2122                       // 'is' '('
       | 2124                       // 'le' '('
       | 2125                       // 'let' '('
       | 2126                       // 'lt' '('
       | 2128                       // 'mod' '('
       | 2129                       // 'namespace' '('
       | 2131                       // 'ne' '('
       | 2134                       // 'or' '('
       | 2135                       // 'parent' '('
       | 2136                       // 'preceding' '('
       | 2137                       // 'preceding-sibling' '('
       | 2139                       // 'return' '('
       | 2140                       // 'satisfies' '('
       | 2143                       // 'self' '('
       | 2144                       // 'some' '('
       | 2148                       // 'to' '('
       | 2149                       // 'treat' '('
       | 2151 =>                    // 'union' '('
      parse_FunctionCall
    case 79 =>                      // 'map'
      parse_MapConstructor
    case 41                         // '['
       | 46 =>                      // 'array'
      parse_ArrayConstructor
    case 39 =>                      // '?'
      parse_UnaryLookup
    case _ =>
      parse_FunctionItemExpr
    }
    eventHandler.endNonterminal("PrimaryExpr", e0)
  }

  private def parse_Literal: Unit = {
    eventHandler.startNonterminal("Literal", e0)
    l1 match {
    case 4 =>                       // StringLiteral
      consume(4)                    // StringLiteral
    case _ =>
      parse_NumericLiteral
    }
    eventHandler.endNonterminal("Literal", e0)
  }

  private def parse_NumericLiteral: Unit = {
    eventHandler.startNonterminal("NumericLiteral", e0)
    l1 match {
    case 1 =>                       // IntegerLiteral
      consume(1)                    // IntegerLiteral
    case 2 =>                       // DecimalLiteral
      consume(2)                    // DecimalLiteral
    case _ =>
      consume(3)                    // DoubleLiteral
    }
    eventHandler.endNonterminal("NumericLiteral", e0)
  }

  private def parse_VarRef: Unit = {
    eventHandler.startNonterminal("VarRef", e0)
    consume(15)                     // '$'
    lookahead1W(42)                 // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_VarName
    eventHandler.endNonterminal("VarRef", e0)
  }

  private def parse_VarName: Unit = {
    eventHandler.startNonterminal("VarName", e0)
    parse_EQName
    eventHandler.endNonterminal("VarName", e0)
  }

  private def parse_ParenthesizedExpr: Unit = {
    eventHandler.startNonterminal("ParenthesizedExpr", e0)
    consume(16)                     // '('
    lookahead1W(54)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | ')' | '+' |
                                    // '-' | '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    if (l1 != 18) {                 // ')'
      whitespace
      parse_Expr
    }
    consume(18)                     // ')'
    eventHandler.endNonterminal("ParenthesizedExpr", e0)
  }

  private def parse_ContextItemExpr: Unit = {
    eventHandler.startNonterminal("ContextItemExpr", e0)
    consume(23)                     // '.'
    eventHandler.endNonterminal("ContextItemExpr", e0)
  }

  private def parse_FunctionCall: Unit = {
    eventHandler.startNonterminal("FunctionCall", e0)
    parse_FunctionEQName
    lookahead1W(3)                  // S^WS | '(' | '(:'
    whitespace
    parse_ArgumentList
    eventHandler.endNonterminal("FunctionCall", e0)
  }

  private def parse_Argument: Unit = {
    eventHandler.startNonterminal("Argument", e0)
    l1 match {
    case 39 =>                      // '?'
      lookahead2W(24)               // IntegerLiteral | NCName | S^WS | '(' | '(:' | ')' | '*' | ','
    case _ =>
      lk = l1
    }
    lk match {
    case 2343                       // '?' ')'
       | 2727 =>                    // '?' ','
      parse_ArgumentPlaceholder
    case _ =>
      parse_ExprSingle
    }
    eventHandler.endNonterminal("Argument", e0)
  }

  private def parse_ArgumentPlaceholder: Unit = {
    eventHandler.startNonterminal("ArgumentPlaceholder", e0)
    consume(39)                     // '?'
    eventHandler.endNonterminal("ArgumentPlaceholder", e0)
  }

  private def parse_FunctionItemExpr: Unit = {
    eventHandler.startNonterminal("FunctionItemExpr", e0)
    l1 match {
    case 66 =>                      // 'function'
      parse_InlineFunctionExpr
    case _ =>
      parse_NamedFunctionRef
    }
    eventHandler.endNonterminal("FunctionItemExpr", e0)
  }

  private def parse_NamedFunctionRef: Unit = {
    eventHandler.startNonterminal("NamedFunctionRef", e0)
    parse_FunctionEQName
    lookahead1W(1)                  // S^WS | '#' | '(:'
    consume(14)                     // '#'
    lookahead1W(0)                  // IntegerLiteral | S^WS | '(:'
    consume(1)                      // IntegerLiteral
    eventHandler.endNonterminal("NamedFunctionRef", e0)
  }

  private def parse_InlineFunctionExpr: Unit = {
    eventHandler.startNonterminal("InlineFunctionExpr", e0)
    consume(66)                     // 'function'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(16)                 // S^WS | '$' | '(:' | ')'
    if (l1 == 15) {                 // '$'
      whitespace
      parse_ParamList
    }
    consume(18)                     // ')'
    lookahead1W(19)                 // S^WS | '(:' | 'as' | '{'
    if (l1 == 47) {                 // 'as'
      consume(47)                   // 'as'
      lookahead1W(44)               // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
      whitespace
      parse_SequenceType
    }
    lookahead1W(13)                 // S^WS | '(:' | '{'
    whitespace
    parse_FunctionBody
    eventHandler.endNonterminal("InlineFunctionExpr", e0)
  }

  private def parse_MapConstructor: Unit = {
    eventHandler.startNonterminal("MapConstructor", e0)
    consume(79)                     // 'map'
    lookahead1W(13)                 // S^WS | '(:' | '{'
    consume(104)                    // '{'
    lookahead1W(56)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union' | '}'
    if (l1 != 107) {                // '}'
      whitespace
      parse_MapConstructorEntry
      var c1 = true
      while (c1) {
        if (l1 != 21) {             // ','
          c1 = false
        }
        else {
          consume(21)               // ','
          lookahead1W(53)           // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
          whitespace
          parse_MapConstructorEntry
        }
      }
    }
    consume(107)                    // '}'
    eventHandler.endNonterminal("MapConstructor", e0)
  }

  private def parse_MapConstructorEntry: Unit = {
    eventHandler.startNonterminal("MapConstructorEntry", e0)
    parse_MapKeyExpr
    consume(27)                     // ':'
    lookahead1W(53)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_MapValueExpr
    eventHandler.endNonterminal("MapConstructorEntry", e0)
  }

  private def parse_MapKeyExpr: Unit = {
    eventHandler.startNonterminal("MapKeyExpr", e0)
    parse_ExprSingle
    eventHandler.endNonterminal("MapKeyExpr", e0)
  }

  private def parse_MapValueExpr: Unit = {
    eventHandler.startNonterminal("MapValueExpr", e0)
    parse_ExprSingle
    eventHandler.endNonterminal("MapValueExpr", e0)
  }

  private def parse_ArrayConstructor: Unit = {
    eventHandler.startNonterminal("ArrayConstructor", e0)
    l1 match {
    case 41 =>                      // '['
      parse_SquareArrayConstructor
    case _ =>
      parse_CurlyArrayConstructor
    }
    eventHandler.endNonterminal("ArrayConstructor", e0)
  }

  private def parse_SquareArrayConstructor: Unit = {
    eventHandler.startNonterminal("SquareArrayConstructor", e0)
    consume(41)                     // '['
    lookahead1W(55)                 // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | ']' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    if (l1 != 42) {                 // ']'
      whitespace
      parse_ExprSingle
      var c1 = true
      while (c1) {
        if (l1 != 21) {             // ','
          c1 = false
        }
        else {
          consume(21)               // ','
          lookahead1W(53)           // IntegerLiteral | DecimalLiteral | DoubleLiteral | StringLiteral |
                                    // URIQualifiedName | QName^Token | S^WS | Wildcard | '$' | '(' | '(:' | '+' | '-' |
                                    // '.' | '..' | '/' | '//' | '?' | '@' | '[' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
          whitespace
          parse_ExprSingle
        }
      }
    }
    consume(42)                     // ']'
    eventHandler.endNonterminal("SquareArrayConstructor", e0)
  }

  private def parse_CurlyArrayConstructor: Unit = {
    eventHandler.startNonterminal("CurlyArrayConstructor", e0)
    consume(46)                     // 'array'
    lookahead1W(13)                 // S^WS | '(:' | '{'
    whitespace
    parse_EnclosedExpr
    eventHandler.endNonterminal("CurlyArrayConstructor", e0)
  }

  private def parse_UnaryLookup: Unit = {
    eventHandler.startNonterminal("UnaryLookup", e0)
    consume(39)                     // '?'
    lookahead1W(23)                 // IntegerLiteral | NCName | S^WS | '(' | '(:' | '*'
    whitespace
    parse_KeySpecifier
    eventHandler.endNonterminal("UnaryLookup", e0)
  }

  private def parse_SingleType: Unit = {
    eventHandler.startNonterminal("SingleType", e0)
    parse_SimpleTypeName
    lookahead1W(31)                 // S^WS | EOF | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '>' | '>=' | '>>' | '?' | ']' | 'and' | 'castable' | 'div' |
                                    // 'else' | 'eq' | 'except' | 'ge' | 'gt' | 'idiv' | 'instance' | 'intersect' |
                                    // 'is' | 'le' | 'lt' | 'mod' | 'ne' | 'or' | 'return' | 'satisfies' | 'to' |
                                    // 'treat' | 'union' | '|' | '||' | '}'
    if (l1 == 39) {                 // '?'
      consume(39)                   // '?'
    }
    eventHandler.endNonterminal("SingleType", e0)
  }

  private def parse_TypeDeclaration: Unit = {
    eventHandler.startNonterminal("TypeDeclaration", e0)
    consume(47)                     // 'as'
    lookahead1W(44)                 // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_SequenceType
    eventHandler.endNonterminal("TypeDeclaration", e0)
  }

  private def parse_SequenceType: Unit = {
    eventHandler.startNonterminal("SequenceType", e0)
    l1 match {
    case 59 =>                      // 'empty-sequence'
      lookahead2W(30)               // S^WS | EOF | '!=' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '>' | '>=' | '>>' | '?' | ']' | 'and' | 'div' | 'else' | 'eq' |
                                    // 'except' | 'ge' | 'gt' | 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' |
                                    // 'mod' | 'ne' | 'or' | 'return' | 'satisfies' | 'to' | 'union' | '{' | '|' |
                                    // '||' | '}'
    case _ =>
      lk = l1
    }
    lk match {
    case 2107 =>                    // 'empty-sequence' '('
      consume(59)                   // 'empty-sequence'
      lookahead1W(3)                // S^WS | '(' | '(:'
      consume(16)                   // '('
      lookahead1W(4)                // S^WS | '(:' | ')'
      consume(18)                   // ')'
    case _ =>
      parse_ItemType
      lookahead1W(28)               // S^WS | EOF | '!=' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '>' | '>=' | '>>' | '?' | ']' | 'and' | 'div' | 'else' | 'eq' |
                                    // 'except' | 'ge' | 'gt' | 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' |
                                    // 'mod' | 'ne' | 'or' | 'return' | 'satisfies' | 'to' | 'union' | '{' | '|' |
                                    // '||' | '}'
      l1 match {
      case 19                       // '*'
         | 20                       // '+'
         | 39 =>                    // '?'
        whitespace
        parse_OccurrenceIndicator
      case _ =>
      }
    }
    eventHandler.endNonterminal("SequenceType", e0)
  }

  private def parse_OccurrenceIndicator: Unit = {
    eventHandler.startNonterminal("OccurrenceIndicator", e0)
    l1 match {
    case 39 =>                      // '?'
      consume(39)                   // '?'
    case 19 =>                      // '*'
      consume(19)                   // '*'
    case _ =>
      consume(20)                   // '+'
    }
    eventHandler.endNonterminal("OccurrenceIndicator", e0)
  }

  private def parse_ItemType: Unit = {
    eventHandler.startNonterminal("ItemType", e0)
    l1 match {
    case 46                         // 'array'
       | 48                         // 'attribute'
       | 52                         // 'comment'
       | 56                         // 'document-node'
       | 57                         // 'element'
       | 66                         // 'function'
       | 75                         // 'item'
       | 79                         // 'map'
       | 82                         // 'namespace-node'
       | 84                         // 'node'
       | 90                         // 'processing-instruction'
       | 93                         // 'schema-attribute'
       | 94                         // 'schema-element'
       | 98 =>                      // 'text'
      lookahead2W(30)               // S^WS | EOF | '!=' | '(' | '(:' | ')' | '*' | '+' | ',' | '-' | ':' | '<' | '<<' |
                                    // '<=' | '=' | '>' | '>=' | '>>' | '?' | ']' | 'and' | 'div' | 'else' | 'eq' |
                                    // 'except' | 'ge' | 'gt' | 'idiv' | 'instance' | 'intersect' | 'is' | 'le' | 'lt' |
                                    // 'mod' | 'ne' | 'or' | 'return' | 'satisfies' | 'to' | 'union' | '{' | '|' |
                                    // '||' | '}'
    case _ =>
      lk = l1
    }
    lk match {
    case 2096                       // 'attribute' '('
       | 2100                       // 'comment' '('
       | 2104                       // 'document-node' '('
       | 2105                       // 'element' '('
       | 2130                       // 'namespace-node' '('
       | 2132                       // 'node' '('
       | 2138                       // 'processing-instruction' '('
       | 2141                       // 'schema-attribute' '('
       | 2142                       // 'schema-element' '('
       | 2146 =>                    // 'text' '('
      parse_KindTest
    case 2123 =>                    // 'item' '('
      consume(75)                   // 'item'
      lookahead1W(3)                // S^WS | '(' | '(:'
      consume(16)                   // '('
      lookahead1W(4)                // S^WS | '(:' | ')'
      consume(18)                   // ')'
    case 2114 =>                    // 'function' '('
      parse_FunctionTest
    case 2127 =>                    // 'map' '('
      parse_MapTest
    case 2094 =>                    // 'array' '('
      parse_ArrayTest
    case 16 =>                      // '('
      parse_ParenthesizedItemType
    case _ =>
      parse_AtomicOrUnionType
    }
    eventHandler.endNonterminal("ItemType", e0)
  }

  private def parse_AtomicOrUnionType: Unit = {
    eventHandler.startNonterminal("AtomicOrUnionType", e0)
    parse_EQName
    eventHandler.endNonterminal("AtomicOrUnionType", e0)
  }

  private def parse_KindTest: Unit = {
    eventHandler.startNonterminal("KindTest", e0)
    l1 match {
    case 56 =>                      // 'document-node'
      parse_DocumentTest
    case 57 =>                      // 'element'
      parse_ElementTest
    case 48 =>                      // 'attribute'
      parse_AttributeTest
    case 94 =>                      // 'schema-element'
      parse_SchemaElementTest
    case 93 =>                      // 'schema-attribute'
      parse_SchemaAttributeTest
    case 90 =>                      // 'processing-instruction'
      parse_PITest
    case 52 =>                      // 'comment'
      parse_CommentTest
    case 98 =>                      // 'text'
      parse_TextTest
    case 82 =>                      // 'namespace-node'
      parse_NamespaceNodeTest
    case _ =>
      parse_AnyKindTest
    }
    eventHandler.endNonterminal("KindTest", e0)
  }

  private def parse_AnyKindTest: Unit = {
    eventHandler.startNonterminal("AnyKindTest", e0)
    consume(84)                     // 'node'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(4)                  // S^WS | '(:' | ')'
    consume(18)                     // ')'
    eventHandler.endNonterminal("AnyKindTest", e0)
  }

  private def parse_DocumentTest: Unit = {
    eventHandler.startNonterminal("DocumentTest", e0)
    consume(56)                     // 'document-node'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(22)                 // S^WS | '(:' | ')' | 'element' | 'schema-element'
    if (l1 != 18) {                 // ')'
      l1 match {
      case 57 =>                    // 'element'
        whitespace
        parse_ElementTest
      case _ =>
        whitespace
        parse_SchemaElementTest
      }
    }
    lookahead1W(4)                  // S^WS | '(:' | ')'
    consume(18)                     // ')'
    eventHandler.endNonterminal("DocumentTest", e0)
  }

  private def parse_TextTest: Unit = {
    eventHandler.startNonterminal("TextTest", e0)
    consume(98)                     // 'text'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(4)                  // S^WS | '(:' | ')'
    consume(18)                     // ')'
    eventHandler.endNonterminal("TextTest", e0)
  }

  private def parse_CommentTest: Unit = {
    eventHandler.startNonterminal("CommentTest", e0)
    consume(52)                     // 'comment'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(4)                  // S^WS | '(:' | ')'
    consume(18)                     // ')'
    eventHandler.endNonterminal("CommentTest", e0)
  }

  private def parse_NamespaceNodeTest: Unit = {
    eventHandler.startNonterminal("NamespaceNodeTest", e0)
    consume(82)                     // 'namespace-node'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(4)                  // S^WS | '(:' | ')'
    consume(18)                     // ')'
    eventHandler.endNonterminal("NamespaceNodeTest", e0)
  }

  private def parse_PITest: Unit = {
    eventHandler.startNonterminal("PITest", e0)
    consume(90)                     // 'processing-instruction'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(20)                 // StringLiteral | NCName | S^WS | '(:' | ')'
    if (l1 != 18) {                 // ')'
      l1 match {
      case 6 =>                     // NCName
        consume(6)                  // NCName
      case _ =>
        consume(4)                  // StringLiteral
      }
    }
    lookahead1W(4)                  // S^WS | '(:' | ')'
    consume(18)                     // ')'
    eventHandler.endNonterminal("PITest", e0)
  }

  private def parse_AttributeTest: Unit = {
    eventHandler.startNonterminal("AttributeTest", e0)
    consume(48)                     // 'attribute'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(49)                 // URIQualifiedName | QName^Token | S^WS | '(:' | ')' | '*' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    if (l1 != 18) {                 // ')'
      whitespace
      parse_AttribNameOrWildcard
      lookahead1W(17)               // S^WS | '(:' | ')' | ','
      if (l1 == 21) {               // ','
        consume(21)                 // ','
        lookahead1W(42)             // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace
        parse_TypeName
      }
    }
    lookahead1W(4)                  // S^WS | '(:' | ')'
    consume(18)                     // ')'
    eventHandler.endNonterminal("AttributeTest", e0)
  }

  private def parse_AttribNameOrWildcard: Unit = {
    eventHandler.startNonterminal("AttribNameOrWildcard", e0)
    l1 match {
    case 19 =>                      // '*'
      consume(19)                   // '*'
    case _ =>
      parse_AttributeName
    }
    eventHandler.endNonterminal("AttribNameOrWildcard", e0)
  }

  private def parse_SchemaAttributeTest: Unit = {
    eventHandler.startNonterminal("SchemaAttributeTest", e0)
    consume(93)                     // 'schema-attribute'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(42)                 // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_AttributeDeclaration
    lookahead1W(4)                  // S^WS | '(:' | ')'
    consume(18)                     // ')'
    eventHandler.endNonterminal("SchemaAttributeTest", e0)
  }

  private def parse_AttributeDeclaration: Unit = {
    eventHandler.startNonterminal("AttributeDeclaration", e0)
    parse_AttributeName
    eventHandler.endNonterminal("AttributeDeclaration", e0)
  }

  private def parse_ElementTest: Unit = {
    eventHandler.startNonterminal("ElementTest", e0)
    consume(57)                     // 'element'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(49)                 // URIQualifiedName | QName^Token | S^WS | '(:' | ')' | '*' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    if (l1 != 18) {                 // ')'
      whitespace
      parse_ElementNameOrWildcard
      lookahead1W(17)               // S^WS | '(:' | ')' | ','
      if (l1 == 21) {               // ','
        consume(21)                 // ','
        lookahead1W(42)             // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
        whitespace
        parse_TypeName
        lookahead1W(18)             // S^WS | '(:' | ')' | '?'
        if (l1 == 39) {             // '?'
          consume(39)               // '?'
        }
      }
    }
    lookahead1W(4)                  // S^WS | '(:' | ')'
    consume(18)                     // ')'
    eventHandler.endNonterminal("ElementTest", e0)
  }

  private def parse_ElementNameOrWildcard: Unit = {
    eventHandler.startNonterminal("ElementNameOrWildcard", e0)
    l1 match {
    case 19 =>                      // '*'
      consume(19)                   // '*'
    case _ =>
      parse_ElementName
    }
    eventHandler.endNonterminal("ElementNameOrWildcard", e0)
  }

  private def parse_SchemaElementTest: Unit = {
    eventHandler.startNonterminal("SchemaElementTest", e0)
    consume(94)                     // 'schema-element'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(42)                 // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_ElementDeclaration
    lookahead1W(4)                  // S^WS | '(:' | ')'
    consume(18)                     // ')'
    eventHandler.endNonterminal("SchemaElementTest", e0)
  }

  private def parse_ElementDeclaration: Unit = {
    eventHandler.startNonterminal("ElementDeclaration", e0)
    parse_ElementName
    eventHandler.endNonterminal("ElementDeclaration", e0)
  }

  private def parse_AttributeName: Unit = {
    eventHandler.startNonterminal("AttributeName", e0)
    parse_EQName
    eventHandler.endNonterminal("AttributeName", e0)
  }

  private def parse_ElementName: Unit = {
    eventHandler.startNonterminal("ElementName", e0)
    parse_EQName
    eventHandler.endNonterminal("ElementName", e0)
  }

  private def parse_SimpleTypeName: Unit = {
    eventHandler.startNonterminal("SimpleTypeName", e0)
    parse_TypeName
    eventHandler.endNonterminal("SimpleTypeName", e0)
  }

  private def parse_TypeName: Unit = {
    eventHandler.startNonterminal("TypeName", e0)
    parse_EQName
    eventHandler.endNonterminal("TypeName", e0)
  }

  private def parse_FunctionTest: Unit = {
    eventHandler.startNonterminal("FunctionTest", e0)
    l1 match {
    case 66 =>                      // 'function'
      lookahead2W(3)                // S^WS | '(' | '(:'
      lk match {
      case 2114 =>                  // 'function' '('
        lookahead3W(50)             // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | ')' | '*' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
      case _ =>
      }
    case _ =>
      lk = l1
    }
    lk match {
    case 313410 =>                  // 'function' '(' '*'
      parse_AnyFunctionTest
    case _ =>
      parse_TypedFunctionTest
    }
    eventHandler.endNonterminal("FunctionTest", e0)
  }

  private def parse_AnyFunctionTest: Unit = {
    eventHandler.startNonterminal("AnyFunctionTest", e0)
    consume(66)                     // 'function'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(5)                  // S^WS | '(:' | '*'
    consume(19)                     // '*'
    lookahead1W(4)                  // S^WS | '(:' | ')'
    consume(18)                     // ')'
    eventHandler.endNonterminal("AnyFunctionTest", e0)
  }

  private def parse_TypedFunctionTest: Unit = {
    eventHandler.startNonterminal("TypedFunctionTest", e0)
    consume(66)                     // 'function'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(47)                 // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | ')' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    if (l1 != 18) {                 // ')'
      whitespace
      parse_SequenceType
      var c1 = true
      while (c1) {
        lookahead1W(17)             // S^WS | '(:' | ')' | ','
        if (l1 != 21) {             // ','
          c1 = false
        }
        else {
          consume(21)               // ','
          lookahead1W(44)           // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
          whitespace
          parse_SequenceType
        }
      }
    }
    consume(18)                     // ')'
    lookahead1W(9)                  // S^WS | '(:' | 'as'
    consume(47)                     // 'as'
    lookahead1W(44)                 // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_SequenceType
    eventHandler.endNonterminal("TypedFunctionTest", e0)
  }

  private def parse_MapTest: Unit = {
    eventHandler.startNonterminal("MapTest", e0)
    l1 match {
    case 79 =>                      // 'map'
      lookahead2W(3)                // S^WS | '(' | '(:'
      lk match {
      case 2127 =>                  // 'map' '('
        lookahead3W(45)             // URIQualifiedName | QName^Token | S^WS | '(:' | '*' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
      case _ =>
      }
    case _ =>
      lk = l1
    }
    lk match {
    case 313423 =>                  // 'map' '(' '*'
      parse_AnyMapTest
    case _ =>
      parse_TypedMapTest
    }
    eventHandler.endNonterminal("MapTest", e0)
  }

  private def parse_AnyMapTest: Unit = {
    eventHandler.startNonterminal("AnyMapTest", e0)
    consume(79)                     // 'map'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(5)                  // S^WS | '(:' | '*'
    consume(19)                     // '*'
    lookahead1W(4)                  // S^WS | '(:' | ')'
    consume(18)                     // ')'
    eventHandler.endNonterminal("AnyMapTest", e0)
  }

  private def parse_TypedMapTest: Unit = {
    eventHandler.startNonterminal("TypedMapTest", e0)
    consume(79)                     // 'map'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(42)                 // URIQualifiedName | QName^Token | S^WS | '(:' | 'ancestor' | 'ancestor-or-self' |
                                    // 'and' | 'array' | 'attribute' | 'cast' | 'castable' | 'child' | 'comment' |
                                    // 'descendant' | 'descendant-or-self' | 'div' | 'document-node' | 'element' |
                                    // 'else' | 'empty-sequence' | 'eq' | 'every' | 'except' | 'following' |
                                    // 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' | 'idiv' | 'if' |
                                    // 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' | 'map' | 'mod' |
                                    // 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' | 'parent' | 'preceding' |
                                    // 'preceding-sibling' | 'processing-instruction' | 'return' | 'satisfies' |
                                    // 'schema-attribute' | 'schema-element' | 'self' | 'some' | 'switch' | 'text' |
                                    // 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_AtomicOrUnionType
    lookahead1W(6)                  // S^WS | '(:' | ','
    consume(21)                     // ','
    lookahead1W(44)                 // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_SequenceType
    lookahead1W(4)                  // S^WS | '(:' | ')'
    consume(18)                     // ')'
    eventHandler.endNonterminal("TypedMapTest", e0)
  }

  private def parse_ArrayTest: Unit = {
    eventHandler.startNonterminal("ArrayTest", e0)
    l1 match {
    case 46 =>                      // 'array'
      lookahead2W(3)                // S^WS | '(' | '(:'
      lk match {
      case 2094 =>                  // 'array' '('
        lookahead3W(48)             // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | '*' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
      case _ =>
      }
    case _ =>
      lk = l1
    }
    lk match {
    case 313390 =>                  // 'array' '(' '*'
      parse_AnyArrayTest
    case _ =>
      parse_TypedArrayTest
    }
    eventHandler.endNonterminal("ArrayTest", e0)
  }

  private def parse_AnyArrayTest: Unit = {
    eventHandler.startNonterminal("AnyArrayTest", e0)
    consume(46)                     // 'array'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(5)                  // S^WS | '(:' | '*'
    consume(19)                     // '*'
    lookahead1W(4)                  // S^WS | '(:' | ')'
    consume(18)                     // ')'
    eventHandler.endNonterminal("AnyArrayTest", e0)
  }

  private def parse_TypedArrayTest: Unit = {
    eventHandler.startNonterminal("TypedArrayTest", e0)
    consume(46)                     // 'array'
    lookahead1W(3)                  // S^WS | '(' | '(:'
    consume(16)                     // '('
    lookahead1W(44)                 // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_SequenceType
    lookahead1W(4)                  // S^WS | '(:' | ')'
    consume(18)                     // ')'
    eventHandler.endNonterminal("TypedArrayTest", e0)
  }

  private def parse_ParenthesizedItemType: Unit = {
    eventHandler.startNonterminal("ParenthesizedItemType", e0)
    consume(16)                     // '('
    lookahead1W(44)                 // URIQualifiedName | QName^Token | S^WS | '(' | '(:' | 'ancestor' |
                                    // 'ancestor-or-self' | 'and' | 'array' | 'attribute' | 'cast' | 'castable' |
                                    // 'child' | 'comment' | 'descendant' | 'descendant-or-self' | 'div' |
                                    // 'document-node' | 'element' | 'else' | 'empty-sequence' | 'eq' | 'every' |
                                    // 'except' | 'following' | 'following-sibling' | 'for' | 'function' | 'ge' | 'gt' |
                                    // 'idiv' | 'if' | 'instance' | 'intersect' | 'is' | 'item' | 'le' | 'let' | 'lt' |
                                    // 'map' | 'mod' | 'namespace' | 'namespace-node' | 'ne' | 'node' | 'or' |
                                    // 'parent' | 'preceding' | 'preceding-sibling' | 'processing-instruction' |
                                    // 'return' | 'satisfies' | 'schema-attribute' | 'schema-element' | 'self' |
                                    // 'some' | 'switch' | 'text' | 'to' | 'treat' | 'typeswitch' | 'union'
    whitespace
    parse_ItemType
    lookahead1W(4)                  // S^WS | '(:' | ')'
    consume(18)                     // ')'
    eventHandler.endNonterminal("ParenthesizedItemType", e0)
  }

  private def parse_FunctionEQName: Unit = {
    eventHandler.startNonterminal("FunctionEQName", e0)
    l1 match {
    case 5 =>                       // URIQualifiedName
      consume(5)                    // URIQualifiedName
    case _ =>
      parse_FunctionName
    }
    eventHandler.endNonterminal("FunctionEQName", e0)
  }

  private def parse_EQName: Unit = {
    eventHandler.startNonterminal("EQName", e0)
    l1 match {
    case 5 =>                       // URIQualifiedName
      consume(5)                    // URIQualifiedName
    case _ =>
      parse_QName
    }
    eventHandler.endNonterminal("EQName", e0)
  }

  private def try_Whitespace: Unit = {
    l1 match {
    case 8 =>                       // S^WS
      consumeT(8)                   // S^WS
    case _ =>
      try_Comment
    }
  }

  private def try_Comment: Unit = {
    consumeT(17)                    // '(:'
    var c1 = true
    while (c1) {
      lookahead1(14)                // CommentContents | '(:' | ':)'
      if (l1 == 28) {               // ':)'
        c1 = false
      }
      else {
        l1 match {
        case 9 =>                   // CommentContents
          consumeT(9)               // CommentContents
        case _ =>
          try_Comment
        }
      }
    }
    consumeT(28)                    // ':)'
  }

  private def parse_FunctionName: Unit = {
    eventHandler.startNonterminal("FunctionName", e0)
    l1 match {
    case 7 =>                       // QName^Token
      consume(7)                    // QName^Token
    case 43 =>                      // 'ancestor'
      consume(43)                   // 'ancestor'
    case 44 =>                      // 'ancestor-or-self'
      consume(44)                   // 'ancestor-or-self'
    case 45 =>                      // 'and'
      consume(45)                   // 'and'
    case 49 =>                      // 'cast'
      consume(49)                   // 'cast'
    case 50 =>                      // 'castable'
      consume(50)                   // 'castable'
    case 51 =>                      // 'child'
      consume(51)                   // 'child'
    case 53 =>                      // 'descendant'
      consume(53)                   // 'descendant'
    case 54 =>                      // 'descendant-or-self'
      consume(54)                   // 'descendant-or-self'
    case 55 =>                      // 'div'
      consume(55)                   // 'div'
    case 58 =>                      // 'else'
      consume(58)                   // 'else'
    case 60 =>                      // 'eq'
      consume(60)                   // 'eq'
    case 61 =>                      // 'every'
      consume(61)                   // 'every'
    case 62 =>                      // 'except'
      consume(62)                   // 'except'
    case 63 =>                      // 'following'
      consume(63)                   // 'following'
    case 64 =>                      // 'following-sibling'
      consume(64)                   // 'following-sibling'
    case 65 =>                      // 'for'
      consume(65)                   // 'for'
    case 67 =>                      // 'ge'
      consume(67)                   // 'ge'
    case 68 =>                      // 'gt'
      consume(68)                   // 'gt'
    case 69 =>                      // 'idiv'
      consume(69)                   // 'idiv'
    case 72 =>                      // 'instance'
      consume(72)                   // 'instance'
    case 73 =>                      // 'intersect'
      consume(73)                   // 'intersect'
    case 74 =>                      // 'is'
      consume(74)                   // 'is'
    case 76 =>                      // 'le'
      consume(76)                   // 'le'
    case 77 =>                      // 'let'
      consume(77)                   // 'let'
    case 78 =>                      // 'lt'
      consume(78)                   // 'lt'
    case 80 =>                      // 'mod'
      consume(80)                   // 'mod'
    case 81 =>                      // 'namespace'
      consume(81)                   // 'namespace'
    case 83 =>                      // 'ne'
      consume(83)                   // 'ne'
    case 86 =>                      // 'or'
      consume(86)                   // 'or'
    case 87 =>                      // 'parent'
      consume(87)                   // 'parent'
    case 88 =>                      // 'preceding'
      consume(88)                   // 'preceding'
    case 89 =>                      // 'preceding-sibling'
      consume(89)                   // 'preceding-sibling'
    case 91 =>                      // 'return'
      consume(91)                   // 'return'
    case 92 =>                      // 'satisfies'
      consume(92)                   // 'satisfies'
    case 95 =>                      // 'self'
      consume(95)                   // 'self'
    case 96 =>                      // 'some'
      consume(96)                   // 'some'
    case 100 =>                     // 'to'
      consume(100)                  // 'to'
    case 101 =>                     // 'treat'
      consume(101)                  // 'treat'
    case _ =>
      consume(103)                  // 'union'
    }
    eventHandler.endNonterminal("FunctionName", e0)
  }

  private def parse_QName: Unit = {
    eventHandler.startNonterminal("QName", e0)
    l1 match {
    case 46 =>                      // 'array'
      consume(46)                   // 'array'
    case 48 =>                      // 'attribute'
      consume(48)                   // 'attribute'
    case 52 =>                      // 'comment'
      consume(52)                   // 'comment'
    case 56 =>                      // 'document-node'
      consume(56)                   // 'document-node'
    case 57 =>                      // 'element'
      consume(57)                   // 'element'
    case 59 =>                      // 'empty-sequence'
      consume(59)                   // 'empty-sequence'
    case 66 =>                      // 'function'
      consume(66)                   // 'function'
    case 70 =>                      // 'if'
      consume(70)                   // 'if'
    case 75 =>                      // 'item'
      consume(75)                   // 'item'
    case 79 =>                      // 'map'
      consume(79)                   // 'map'
    case 82 =>                      // 'namespace-node'
      consume(82)                   // 'namespace-node'
    case 84 =>                      // 'node'
      consume(84)                   // 'node'
    case 90 =>                      // 'processing-instruction'
      consume(90)                   // 'processing-instruction'
    case 93 =>                      // 'schema-attribute'
      consume(93)                   // 'schema-attribute'
    case 94 =>                      // 'schema-element'
      consume(94)                   // 'schema-element'
    case 97 =>                      // 'switch'
      consume(97)                   // 'switch'
    case 98 =>                      // 'text'
      consume(98)                   // 'text'
    case 102 =>                     // 'typeswitch'
      consume(102)                  // 'typeswitch'
    case _ =>
      parse_FunctionName
    }
    eventHandler.endNonterminal("QName", e0)
  }

  def getErrorMessage(e: XPath31.ParseException) = {
    var message = e.getMessage
    val tokenSet = XPath31.getExpectedTokenSet(e)
    val found = XPath31.getOffendingToken(e)
    val size = e.end - e.begin
    message += (if (found == null) "" else ", found " + found) + "\nwhile expecting " +
      (if (tokenSet.length == 1) tokenSet(0) else "[" + (tokenSet mkString ", ") + "]") + "\n" +
      (if (size == 0 || found != null) "" else "after successfully scanning " + size + " characters beginning ")
    val prefix = input.substring(0, e.begin)
    val line = prefix.replaceAll("[^\n]", "").length + 1
    val column = prefix.length - prefix.lastIndexOf('\n')
    message +
      "at line " + line + ", column " + column + ":\n..." +
      input.substring(e.begin, math.min(input.length, e.begin + 64)) + "..."
  }

  private def consume(t: Int): Unit = {
    if (l1 == t) {
      whitespace
      eventHandler.terminal(XPath31.TOKEN(l1), b1, e1)
      b0 = b1; e0 = e1; l1 = l2; if (l1 != 0) {
      b1 = b2; e1 = e2; l2 = l3; if (l2 != 0) {
      b2 = b3; e2 = e3; l3 = 0 }}
    }
    else {
      error(b1, e1, 0, l1, t)
    }
  }

  private def consumeT(t: Int): Unit = {
    if (l1 == t) {
      b0 = b1; e0 = e1; l1 = l2; if (l1 != 0) {
      b1 = b2; e1 = e2; l2 = l3; if (l2 != 0) {
      b2 = b3; e2 = e3; l3 = 0 }}
    }
    else {
      error(b1, e1, 0, l1, t)
    }
  }

  private def skip(code: Int): Unit = {
    val b0W = b0; val e0W = e0; val l1W = l1
    val b1W = b1; val e1W = e1; val l2W = l2
    val b2W = b2; val e2W = e2

    l1 = code; b1 = begin; e1 = end
    l2 = 0
    l3 = 0

    try_Whitespace

    b0 = b0W; e0 = e0W; l1 = l1W; if (l1 != 0) {
    b1 = b1W; e1 = e1W; l2 = l2W; if (l2 != 0) {
    b2 = b2W; e2 = e2W }}
  }

  private def whitespace: Unit = {
    if (e0 != b1) {
      eventHandler.whitespace(e0, b1)
      e0 = b1
    }
  }

  private def matchW(tokenSetId: Int): Int = {
    var continue = true
    var code = 0
    while (continue) {
      code = matcher(tokenSetId)
      if (code != 8) {              // S^WS
        if (code != 17) {           // '(:'
          continue = false
        }
        else {
          skip(code)
        }
      }
    }
    code
  }

  private def lookahead1W(tokenSetId: Int): Unit = {
    if (l1 == 0) {
      l1 = matchW(tokenSetId)
      b1 = begin
      e1 = end
    }
  }

  private def lookahead2W(tokenSetId: Int): Unit = {
    if (l2 == 0) {
      l2 = matchW(tokenSetId)
      b2 = begin
      e2 = end
    }
    lk = (l2 << 7) | l1
  }

  private def lookahead3W(tokenSetId: Int): Unit = {
    if (l3 == 0) {
      l3 = matchW(tokenSetId)
      b3 = begin
      e3 = end
    }
    lk |= l3 << 14
  }

  private def lookahead1(tokenSetId: Int): Unit = {
    if (l1 == 0) {
      l1 = matcher(tokenSetId)
      b1 = begin
      e1 = end
    }
  }

  def error(b: Int, e: Int, s: Int, l: Int, t: Int): Int = {
    throw new XPath31.ParseException(b, e, s, l, t)
  }

  private def matcher(tokenSetId: Int) = {
    var nonbmp = false
    begin = end
    var current = end
    var result = XPath31.INITIAL(tokenSetId)
    var state = 0
    var code = result & 1023

    while (code != 0) {
      var charclass = -1
      var c0 = if (current < size) input(current) else 0
      current += 1
      if (c0 < 0x80) {
        charclass = XPath31.MAP0(c0)
      }
      else if (c0 < 0xd800) {
        val c1 = c0 >> 4
        charclass = XPath31.MAP1((c0 & 15) + XPath31.MAP1((c1 & 31) + XPath31.MAP1(c1 >> 5)))
      }
      else {
        if (c0 < 0xdc00) {
          val c1 = if (current < size) input(current) else 0
          if (c1 >= 0xdc00 && c1 < 0xe000) {
            nonbmp = true
            current += 1
            c0 = ((c0 & 0x3ff) << 10) + (c1 & 0x3ff) + 0x10000
          }
        }

        var lo = 0
        var hi = 5
        var m = 3
        while (charclass < 0) {
          if (XPath31.MAP2(m) > c0) hi = m - 1
          else if (XPath31.MAP2(6 + m) < c0) lo = m + 1
          else charclass = XPath31.MAP2(12 + m)
          if (lo > hi) charclass = 0 else m = (hi + lo) >> 1
        }
      }

      state = code
      val i0 = (charclass << 10) + code - 1
      code = XPath31.TRANSITION((i0 & 15) + XPath31.TRANSITION(i0 >> 4))

      if (code > 1023) {
        result = code
        code &= 1023
        end = current
      }
    }

    result >>= 10
    if (result == 0) {
      end = current - 1
      val c1 = if (end < size) input(end) else 0
      if (c1 >= 0xdc00 && c1 < 0xe000) {
        end -= 1
      }
      error(begin, end, state, -1, -1)
    }
    else {
      if (nonbmp) {
        var i = result >> 7
        while (i > 0) {
          end -= 1
          val c1 = if (end < size) input(end) else 0
          if (c1 >= 0xdc00 && c1 < 0xe000) {
            end -= 1
          }
          i -= 1
        }
      }
      else {
        end -= result >> 7
      }
      if (end > size) end = size
      (result & 127) - 1
    }
  }

  var begin = 0
  var end = 0

  var lk = 0
  var b0 = 0
  var e0 = 0
  var l1 = 0
  var b1 = 0
  var e1 = 0
  var l2 = 0
  var b2 = 0
  var e2 = 0
  var l3 = 0
  var b3 = 0
  var e3 = 0
  var eventHandler: XPath31.EventHandler = null
  var input: String = null
  var size = 0
}

object XPath31 {

  def getOffendingToken(e: ParseException) = {
    if (e.offending < 0) null else TOKEN(e.offending)
  }

  class ParseException(val begin: Int, val end: Int, val state: Int, val offending: Int, val expected: Int) extends RuntimeException {

    override def getMessage = {
      if (offending < 0) "lexical analysis failed" else "syntax error"
    }
  }

  def getExpectedTokenSet(e: ParseException) = {
    if (e.expected < 0) {
      getTokenSet(- e.state)
    }
    else {
      Array(TOKEN(e.expected))
    }
  }

  trait EventHandler {
    def reset(string: String): Unit
    def startNonterminal(name: String, begin: Int): Unit
    def endNonterminal(name: String, end: Int): Unit
    def terminal(name: String, begin: Int, end: Int): Unit
    def whitespace(begin: Int, end: Int): Unit
  }

  class TopDownTreeBuilder extends EventHandler {
    private var input: String = null
    private var stack = new ArrayBuffer[Nonterminal](64)
    private var top = -1

    override def reset(input: String): Unit = {
      this.input = input
      top = -1
    }

    override def startNonterminal(name: String, begin: Int): Unit = {
      val nonterminal = new Nonterminal(name, begin, begin, ArrayBuffer[Symbol]())
      if (top >= 0) addChild(nonterminal)
      top += 1
      if (top == stack.length) stack += nonterminal else stack(top) = nonterminal
    }

    override def endNonterminal(name: String, end: Int): Unit = {
      var nonterminal = stack(top)
      nonterminal.end = end
      if (top > 0) top -= 1
    }

    override def terminal(name: String, begin: Int, end: Int): Unit = {
      addChild(new Terminal(name, begin, end))
    }

    override def whitespace(begin: Int, end: Int): Unit = {
    }

    private def addChild(s: Symbol): Unit = {
      var current = stack(top)
      current.children += s
    }

    def serialize(e: EventHandler): Unit = {
      e.reset(input)
      stack(0).send(e)
    }
  }

  abstract class Symbol(n: String, b: Int, e: Int) {
    var name = n
    var begin = b
    var end = e

    def send(e: EventHandler): Unit
  }

  class Terminal(name: String, begin: Int, end: Int) extends Symbol(name, begin, end) {
    override def send(e: EventHandler): Unit = {
      e.terminal(name, begin, end)
    }
  }

  class Nonterminal(name: String, begin: Int, end: Int, c: ArrayBuffer[Symbol]) extends Symbol(name, begin, end) {
    var children = c

    override def send(e: EventHandler): Unit = {
      e.startNonterminal(name, begin)
      var pos = begin
      for (c <- children) {
        if (pos < c.begin) e.whitespace(pos, c.begin)
        c.send(e)
        pos = c.end
      }
      if (pos < end) e.whitespace(pos, end)
      e.endNonterminal(name, end)
    }
  }

  private def getTokenSet(tokenSetId: Int) = {
    var expected = ArrayBuffer[String]()
    val s = if (tokenSetId < 0) - tokenSetId else INITIAL(tokenSetId) & 1023
    var i = 0
    while (i < 108) {
      var j = i
      val i0 = (i >> 5) * 828 + s - 1
      val i1 = i0 >> 1
      var f = EXPECTED((i0 & 1) + EXPECTED((i1 & 3) + EXPECTED(i1 >> 2)))
      while (f != 0) {
        if ((f & 1) != 0) {
          expected += TOKEN(j)
        }
        f >>>= 1
        j += 1
      }
      i += 32
    }
    expected.toArray
  }

  private final val MAP0 = Array(
    /*   0 */ "55, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2",
    /*  34 */ "3, 4, 5, 6, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 17, 6, 18, 19",
    /*  62 */ "20, 21, 22, 23, 23, 23, 23, 24, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 25, 23, 23, 23, 23, 23, 23",
    /*  88 */ "23, 23, 23, 26, 6, 27, 6, 23, 6, 28, 29, 30, 31, 32, 33, 34, 35, 36, 23, 23, 37, 38, 39, 40, 41, 42, 43",
    /* 115 */ "44, 45, 46, 47, 48, 49, 50, 23, 51, 52, 53, 6, 6"
  ).flatMap(_.split(", ").map(_.toInt))

  private final val MAP1 = Array(
    /*   0 */ "108, 124, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 156, 181, 181, 181, 181",
    /*  21 */ "181, 214, 215, 213, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214",
    /*  42 */ "214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214",
    /*  63 */ "214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214",
    /*  84 */ "214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214, 214",
    /* 105 */ "214, 214, 214, 247, 261, 277, 293, 309, 331, 370, 386, 422, 422, 422, 414, 354, 346, 354, 346, 354, 354",
    /* 126 */ "354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 439, 439, 439, 439, 439, 439, 439",
    /* 147 */ "315, 354, 354, 354, 354, 354, 354, 354, 354, 400, 422, 422, 423, 421, 422, 422, 354, 354, 354, 354, 354",
    /* 168 */ "354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 422, 422, 422, 422, 422, 422, 422, 422",
    /* 189 */ "422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422, 422",
    /* 210 */ "422, 422, 422, 353, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354",
    /* 231 */ "354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 354, 422, 55, 0, 0, 0, 0, 0, 0, 0",
    /* 255 */ "0, 1, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4, 5, 6, 6, 7, 8, 9, 10, 11",
    /* 289 */ "12, 13, 14, 15, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 17, 6, 18, 19, 20, 21, 22, 23, 23, 23, 23, 24",
    /* 315 */ "23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 6, 23, 23, 25, 23, 23, 23, 23, 23, 23, 23, 23",
    /* 341 */ "23, 26, 6, 27, 6, 23, 23, 23, 23, 23, 23, 23, 6, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23, 23",
    /* 368 */ "23, 23, 6, 28, 29, 30, 31, 32, 33, 34, 35, 36, 23, 23, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48",
    /* 394 */ "49, 50, 23, 51, 52, 53, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 23, 23, 6, 6, 6, 6, 6, 6, 6, 54, 6, 6, 6, 6",
    /* 426 */ "6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54, 54"
  ).flatMap(_.split(", ").map(_.toInt))

  private final val MAP2 = Array(
    /*  0 */ "57344, 63744, 64976, 65008, 65536, 983040, 63743, 64975, 65007, 65533, 983039, 1114111, 6, 23, 6, 23, 23",
    /* 17 */ "6"
  ).flatMap(_.split(", ").map(_.toInt))

  private final val INITIAL = Array(
    /*  0 */ "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28",
    /* 28 */ "29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54",
    /* 54 */ "55, 56, 57, 58"
  ).flatMap(_.split(", ").map(_.toInt))

  private final val TRANSITION = Array(
    /*     0 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*    17 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*    34 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*    51 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 3586, 3584, 3584, 3602",
    /*    68 */ "3644, 4355, 3955, 12031, 4774, 4747, 3669, 3893, 3713, 3730, 3894, 3714, 3757, 3796, 3691, 3834, 3681",
    /*    85 */ "3697, 3957, 3863, 3912, 3930, 3910, 3928, 3986, 4149, 4138, 4154, 3946, 3847, 3841, 3973, 4004, 4002",
    /*   102 */ "3878, 4091, 3818, 4020, 4036, 4062, 5179, 12032, 11948, 4077, 4107, 4158, 4123, 4174, 4355, 4355",
    /*   118 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 5259, 4209, 9946, 3644, 4355, 3955",
    /*   135 */ "12031, 4774, 4747, 3669, 3893, 3713, 3730, 3894, 3714, 3757, 3796, 3691, 3834, 3681, 3697, 3957, 3863",
    /*   152 */ "3912, 3930, 3910, 3928, 3986, 4149, 4138, 4154, 3946, 3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818",
    /*   169 */ "4020, 4036, 4062, 5179, 12032, 11948, 4077, 4107, 4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355",
    /*   185 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 6139, 4355, 4244, 4273, 4355, 3955, 12031, 12003",
    /*   201 */ "4747, 3669, 3893, 3713, 3730, 3894, 3714, 3757, 3796, 3691, 3834, 3681, 3697, 3957, 3863, 3912, 3930",
    /*   218 */ "3910, 3928, 3986, 4149, 4138, 4154, 3946, 3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818, 4020, 4036",
    /*   235 */ "4062, 5179, 12032, 11948, 4077, 4107, 4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*   251 */ "4355, 4355, 4355, 4355, 4355, 4298, 4355, 5151, 10656, 3644, 4355, 3955, 12031, 4774, 4747, 3669",
    /*   267 */ "3893, 3713, 3730, 3894, 3714, 3757, 3796, 3691, 3834, 3681, 3697, 3957, 3863, 3912, 3930, 3910, 3928",
    /*   284 */ "3986, 4149, 4138, 4154, 3946, 3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818, 4020, 4036, 4062, 5179",
    /*   301 */ "12032, 11948, 4077, 4107, 4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*   317 */ "4355, 4355, 4355, 4314, 4354, 9015, 4372, 3644, 4355, 3955, 12031, 4774, 4747, 3669, 3893, 3713, 3730",
    /*   334 */ "3894, 3714, 3757, 3796, 3691, 3834, 3681, 3697, 3957, 3863, 3912, 3930, 3910, 3928, 3986, 4149, 4138",
    /*   351 */ "4154, 3946, 3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818, 4020, 4036, 4062, 5179, 12032, 11948",
    /*   367 */ "4077, 4107, 4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*   384 */ "8504, 4355, 4355, 10656, 3644, 4355, 3955, 12031, 4774, 4747, 3669, 3893, 3713, 3730, 3894, 3714",
    /*   400 */ "3757, 3796, 3691, 3834, 3681, 3697, 3957, 3863, 3912, 3930, 3910, 3928, 3986, 4149, 4138, 4154, 3946",
    /*   417 */ "3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818, 4020, 4036, 4062, 5179, 12032, 11948, 4077, 4107",
    /*   433 */ "4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 6256",
    /*   450 */ "4355, 4413, 4442, 4355, 3955, 12031, 4774, 4467, 3669, 3893, 3713, 3730, 3894, 3714, 3757, 3796, 3691",
    /*   467 */ "3834, 3681, 3697, 3957, 3863, 3912, 3930, 3910, 3928, 3986, 4149, 4138, 4154, 3946, 3847, 3841, 3973",
    /*   484 */ "4004, 4002, 3878, 4091, 3818, 4020, 4036, 4062, 5179, 12032, 11948, 4077, 4107, 4158, 4123, 4174",
    /*   500 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4547, 4536, 4563, 4575, 4591",
    /*   517 */ "4355, 3955, 12031, 12059, 4747, 3669, 3893, 3713, 3730, 3894, 3714, 3757, 3796, 3691, 3834, 3681",
    /*   533 */ "3697, 3957, 3863, 3912, 3930, 3910, 3928, 3986, 4149, 4138, 4154, 3946, 3847, 3841, 3973, 4004, 4002",
    /*   550 */ "3878, 4091, 3818, 4020, 4036, 4062, 5179, 12032, 11948, 4077, 4107, 4158, 4123, 4174, 4355, 4355",
    /*   566 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4616, 4656, 4664, 4680, 4709, 4355, 3955",
    /*   583 */ "12031, 3741, 4747, 3669, 3893, 3713, 3730, 3894, 3714, 3757, 3796, 3691, 3834, 3681, 3697, 3957, 3863",
    /*   600 */ "3912, 3930, 3910, 3928, 3986, 4149, 4138, 4154, 3946, 3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818",
    /*   617 */ "4020, 4036, 4062, 5179, 12032, 11948, 4077, 4107, 4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355",
    /*   633 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 5208, 4186, 4193, 4734, 3644, 4355, 3955, 12031, 4774, 4747",
    /*   650 */ "3669, 3893, 3713, 4763, 3894, 3714, 3757, 3796, 3691, 3834, 11976, 3697, 3957, 3863, 3912, 3930, 3910",
    /*   667 */ "3928, 3986, 4149, 4138, 4154, 3946, 3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818, 4020, 4036, 4062",
    /*   684 */ "5179, 12032, 11948, 4077, 4107, 4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*   700 */ "4355, 4355, 4355, 4355, 8504, 4219, 4228, 5933, 3644, 4355, 3955, 12031, 4774, 4747, 3669, 3893, 3713",
    /*   717 */ "3730, 3894, 3714, 4790, 3796, 3691, 3834, 3681, 3697, 3957, 3863, 3912, 3930, 3910, 3928, 3986, 4149",
    /*   734 */ "4138, 4154, 3946, 3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818, 4020, 4036, 4062, 5179, 12032",
    /*   750 */ "11948, 4077, 4107, 4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*   767 */ "4355, 4693, 4830, 4838, 5161, 3644, 4355, 3955, 12031, 4774, 4747, 3669, 3893, 3713, 3730, 3894, 3714",
    /*   784 */ "3757, 3796, 3691, 3834, 3681, 3697, 3957, 3863, 3912, 3930, 3910, 3928, 3986, 4149, 4138, 4154, 3946",
    /*   801 */ "3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818, 4020, 4036, 4062, 5179, 12032, 11948, 4077, 4107",
    /*   817 */ "4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 5113",
    /*   834 */ "5122, 6085, 4854, 4355, 5025, 7717, 9761, 4355, 6261, 6565, 6565, 8989, 8180, 8180, 10959, 7118, 6565",
    /*   851 */ "6565, 4879, 8180, 6464, 7219, 6565, 6565, 8179, 8180, 11771, 9362, 8359, 4901, 11159, 11088, 8179",
    /*   867 */ "11346, 6566, 8180, 7456, 6790, 6812, 4919, 4956, 4972, 7968, 8013, 7275, 4988, 8360, 7157, 7161, 5010",
    /*   884 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 4355, 4355, 5043, 4854",
    /*   901 */ "4355, 5025, 7717, 10119, 4355, 6261, 6565, 6565, 8989, 8180, 8180, 10189, 7118, 6565, 6565, 4879",
    /*   917 */ "8180, 6464, 7219, 6565, 6565, 8179, 8180, 11771, 6565, 8359, 8180, 8389, 6565, 8179, 6409, 6566, 8180",
    /*   934 */ "5021, 8360, 6812, 8358, 4994, 6485, 8358, 8013, 4993, 10587, 8360, 7157, 7161, 5010, 4355, 4355, 4355",
    /*   951 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 4355, 5103, 5138, 3644, 4355, 5177, 12031",
    /*   968 */ "4774, 4747, 3669, 3893, 3713, 3730, 3894, 3714, 3757, 3796, 3691, 3834, 3681, 3697, 3957, 3863, 3912",
    /*   985 */ "3930, 3910, 3928, 3986, 4149, 4138, 4154, 3946, 3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818, 4020",
    /*  1002 */ "4036, 4062, 5179, 12032, 11948, 4077, 4107, 4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355, 4355",
    /*  1018 */ "4355, 4355, 4355, 4355, 4355, 4355, 10000, 9992, 4355, 5195, 4854, 4355, 5025, 7717, 8767, 4355, 6261",
    /*  1035 */ "6565, 6565, 8989, 8180, 8180, 10041, 7118, 6565, 6565, 4879, 8180, 11489, 7219, 6565, 6565, 8179",
    /*  1051 */ "8180, 11771, 6565, 8359, 8180, 8389, 6565, 8179, 6409, 6566, 8180, 5021, 8360, 6812, 8358, 4994, 6485",
    /*  1068 */ "8358, 8013, 4993, 10587, 8360, 7157, 7161, 5010, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*  1085 */ "4355, 4355, 4355, 4520, 5978, 5224, 5234, 5250, 4355, 5635, 5583, 5340, 4747, 5275, 5463, 5303, 5673",
    /*  1102 */ "5684, 5330, 5356, 5395, 5777, 5454, 5287, 5871, 5610, 5439, 5493, 5479, 5860, 5509, 5525, 5571, 5314",
    /*  1119 */ "5607, 5626, 5423, 5555, 5660, 5700, 5725, 5845, 5644, 5733, 5749, 5765, 5540, 5798, 5793, 5591, 5409",
    /*  1136 */ "5814, 5709, 5830, 5887, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504",
    /*  1153 */ "6218, 6227, 6192, 5920, 4355, 3955, 12031, 4774, 4747, 3669, 3893, 3713, 3730, 3894, 3714, 3757, 3796",
    /*  1170 */ "3691, 3834, 3681, 3697, 3957, 3863, 3912, 3930, 3910, 3928, 3986, 4149, 4138, 4154, 3946, 3847, 3841",
    /*  1187 */ "3973, 4004, 4002, 3878, 4091, 3818, 4020, 4036, 4062, 5179, 12032, 11948, 4077, 4107, 4158, 4123",
    /*  1203 */ "4174, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 6312, 5968, 6639",
    /*  1220 */ "5994, 4355, 3955, 12031, 4774, 4747, 3669, 3893, 3713, 3730, 3894, 3714, 3757, 3796, 3691, 3834, 3681",
    /*  1237 */ "3697, 3957, 3863, 3912, 3930, 3910, 3928, 3986, 4149, 4138, 4154, 3946, 3847, 3841, 3973, 4004, 4002",
    /*  1254 */ "3878, 4091, 3818, 4020, 4036, 4062, 5179, 12032, 11948, 4077, 4107, 4158, 4123, 4174, 4355, 4355",
    /*  1270 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 7085, 7094, 6888, 6033, 4356, 3955",
    /*  1287 */ "12031, 4774, 4747, 3669, 3893, 3713, 3730, 3894, 3714, 3757, 3796, 3691, 3834, 3681, 3697, 3957, 3863",
    /*  1304 */ "3912, 3930, 3910, 3928, 3986, 4149, 4138, 4154, 3946, 3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818",
    /*  1321 */ "4020, 4036, 4062, 5179, 12032, 11948, 4077, 4107, 4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355",
    /*  1337 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 5904, 5899, 6072, 3644, 4355, 3955, 12031, 4774, 4747",
    /*  1354 */ "3669, 3893, 3713, 3730, 3894, 3714, 3757, 3796, 3691, 3834, 3681, 3697, 3957, 3863, 3912, 3930, 3910",
    /*  1371 */ "3928, 3986, 4149, 4138, 4154, 3946, 3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818, 4020, 4036, 4062",
    /*  1388 */ "5179, 12032, 11948, 4077, 4107, 4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*  1404 */ "4355, 4355, 4355, 4355, 8504, 4355, 4355, 6126, 3644, 4355, 3955, 12031, 4774, 4747, 3669, 3893, 3713",
    /*  1421 */ "3730, 3894, 3714, 3757, 3796, 3691, 3834, 3681, 3697, 3957, 3863, 3912, 3930, 3910, 3928, 3986, 4149",
    /*  1438 */ "4138, 4154, 3946, 3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818, 4020, 4036, 4062, 5179, 12032",
    /*  1454 */ "11948, 4077, 4107, 4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*  1471 */ "4355, 8504, 6917, 8220, 7990, 4854, 4355, 5025, 7717, 6846, 4355, 3653, 6565, 6565, 7059, 8180, 8180",
    /*  1488 */ "7993, 4718, 6565, 6565, 6160, 8180, 4903, 7219, 6565, 6565, 8179, 8180, 11771, 6565, 8359, 8180, 8389",
    /*  1505 */ "6565, 8179, 6409, 6566, 8180, 5021, 8360, 6812, 8358, 4994, 6485, 8358, 8013, 4993, 10587, 8360, 7157",
    /*  1522 */ "7161, 5010, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 6917, 8220",
    /*  1539 */ "7990, 4854, 4355, 5025, 7717, 10883, 4355, 3653, 6565, 6565, 7059, 8180, 8180, 6524, 4718, 6565, 6565",
    /*  1556 */ "6160, 8180, 4903, 7219, 6565, 6565, 8179, 8180, 11771, 6565, 8359, 8180, 8389, 6565, 8179, 6409, 6566",
    /*  1573 */ "8180, 5021, 8360, 6812, 8358, 4994, 6485, 8358, 8013, 4993, 10587, 8360, 7157, 7161, 5010, 4355, 4355",
    /*  1590 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 6917, 7350, 6182, 4854, 4355, 5025",
    /*  1607 */ "7717, 6846, 4355, 3653, 6565, 6565, 7059, 8180, 8180, 7993, 4718, 6565, 6565, 6160, 8180, 4903, 7219",
    /*  1624 */ "6565, 6565, 8179, 8180, 11771, 6565, 8359, 8180, 8389, 6565, 8179, 6409, 6566, 8180, 5021, 8360, 6812",
    /*  1641 */ "8358, 4994, 6485, 8358, 8013, 4993, 10587, 8360, 7157, 7161, 5010, 4355, 4355, 4355, 4355, 4355, 4355",
    /*  1658 */ "4355, 4355, 4355, 4355, 4355, 4355, 8504, 4355, 6208, 6243, 3644, 4355, 3955, 12031, 4774, 4747, 3669",
    /*  1675 */ "3893, 3713, 3730, 3894, 3714, 3757, 3796, 3691, 3834, 3681, 3697, 3957, 3863, 3912, 3930, 3910, 3928",
    /*  1692 */ "3986, 4149, 4138, 4154, 3946, 3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818, 4020, 4036, 4062, 5179",
    /*  1709 */ "12032, 11948, 4077, 4107, 4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*  1725 */ "4355, 4355, 4355, 8504, 7748, 7757, 3615, 3644, 4355, 3955, 12031, 4774, 4747, 3669, 3893, 3713, 3730",
    /*  1742 */ "3894, 3714, 3757, 3796, 3691, 3834, 3681, 3697, 3957, 3863, 3912, 3930, 3910, 3928, 3986, 4149, 4138",
    /*  1759 */ "4154, 3946, 3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818, 4020, 4036, 4062, 5179, 12032, 11948",
    /*  1775 */ "4077, 4107, 4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*  1792 */ "10050, 6277, 6286, 6302, 4854, 8486, 6328, 6344, 6360, 4355, 3653, 6565, 6565, 7059, 8180, 8180, 7993",
    /*  1809 */ "8811, 6565, 6565, 6400, 8180, 8620, 10968, 6425, 6565, 6441, 6462, 9848, 6565, 6480, 8258, 8389, 9289",
    /*  1826 */ "10235, 6409, 6501, 6548, 5021, 8360, 6812, 8358, 4994, 6485, 8358, 8013, 4993, 10587, 8360, 7157",
    /*  1842 */ "7161, 5010, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 6917, 8220",
    /*  1859 */ "7990, 4854, 4355, 5025, 7717, 6846, 4355, 3653, 6565, 6565, 7059, 8180, 8180, 7993, 4718, 6565, 6565",
    /*  1876 */ "6160, 8180, 4903, 7219, 6565, 6565, 8179, 8180, 7862, 6564, 10531, 8180, 8389, 6565, 8179, 6409, 6566",
    /*  1893 */ "8180, 5021, 8360, 6812, 8358, 4994, 6485, 8358, 10861, 6582, 10587, 8360, 7157, 7161, 5010, 4355",
    /*  1909 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 9098, 6616, 6629, 6655, 4355",
    /*  1926 */ "5025, 6682, 7034, 11418, 4451, 8940, 6565, 10283, 6703, 8180, 7993, 4718, 6728, 9180, 6765, 7267",
    /*  1942 */ "6806, 7219, 6565, 8662, 8179, 11589, 11771, 6565, 8359, 8180, 6779, 10560, 9133, 11658, 10620, 7006",
    /*  1958 */ "5021, 6742, 10822, 8358, 4994, 6485, 8358, 7149, 6828, 10587, 8360, 7157, 6862, 5010, 4355, 4355",
    /*  1974 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 10328, 10337, 6878, 4854, 6904",
    /*  1990 */ "11168, 7833, 6846, 6942, 4600, 6565, 7248, 8950, 8180, 9447, 7993, 4718, 6565, 6565, 6160, 8180, 4903",
    /*  2007 */ "7219, 6958, 6565, 6978, 8180, 11771, 5027, 8359, 6984, 8881, 6565, 7000, 6409, 6566, 8180, 5021, 8360",
    /*  2024 */ "6812, 8358, 4994, 6485, 7022, 7689, 7050, 10587, 8360, 7157, 7161, 5010, 4355, 4355, 4355, 4355, 4355",
    /*  2041 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 6375, 6384, 7075, 4854, 7110, 11357, 7134, 7177, 7217",
    /*  2058 */ "6666, 10998, 7235, 11295, 8579, 7291, 8909, 7319, 7467, 7366, 7404, 7427, 7443, 7502, 8981, 7381",
    /*  2074 */ "10812, 11649, 11771, 11898, 9911, 8180, 9574, 7478, 10462, 8294, 10798, 7539, 11384, 7555, 7580, 9125",
    /*  2090 */ "9544, 7663, 6837, 7613, 7636, 7679, 8360, 7705, 7161, 5010, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*  2107 */ "4355, 4355, 4355, 4355, 4355, 8504, 6917, 10197, 7738, 7773, 4355, 8398, 6110, 6846, 4355, 3653, 6565",
    /*  2124 */ "6565, 7059, 8180, 8180, 7993, 4718, 6565, 6962, 6160, 8180, 11113, 7219, 6565, 6565, 8179, 8180, 7918",
    /*  2141 */ "6565, 7798, 7722, 8389, 6565, 8179, 6409, 6566, 8180, 5021, 8360, 6812, 8358, 4994, 6485, 8358, 8013",
    /*  2158 */ "4993, 10587, 8360, 7821, 7849, 5010, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*  2175 */ "4355, 8504, 3771, 3780, 7878, 4854, 4355, 5025, 7717, 6846, 4355, 3653, 6565, 6565, 7059, 8180, 8180",
    /*  2192 */ "7993, 4718, 6565, 6565, 6160, 8180, 4903, 7219, 6565, 6565, 8179, 8180, 11771, 6565, 8359, 8180, 8389",
    /*  2209 */ "6565, 8179, 6409, 6566, 8180, 8425, 7904, 7951, 7984, 8009, 6485, 8358, 8013, 4993, 10587, 8360",
    /*  2225 */ "11823, 8029, 5010, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 6917",
    /*  2242 */ "8220, 7990, 8054, 4355, 7929, 10684, 6846, 8088, 3653, 6565, 9386, 7059, 8180, 10426, 7993, 4718",
    /*  2258 */ "6565, 6565, 6160, 8180, 4903, 7219, 6565, 6565, 8179, 8180, 11771, 6565, 10228, 9197, 8389, 6565",
    /*  2274 */ "8179, 6409, 6566, 8180, 5021, 8360, 6812, 6591, 8531, 6485, 8358, 8013, 4993, 10587, 8360, 7157, 7161",
    /*  2291 */ "5010, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 5952, 4804, 4814, 8107",
    /*  2308 */ "8142, 4355, 9859, 10513, 6846, 11426, 6098, 8158, 6565, 8197, 8640, 8180, 8213, 10081, 6565, 4940",
    /*  2324 */ "6160, 8180, 10761, 7219, 8236, 6565, 8254, 8180, 11771, 8834, 8359, 10926, 9163, 10724, 8274, 8310",
    /*  2340 */ "9369, 6446, 5021, 8360, 6812, 8358, 4994, 6485, 8337, 8353, 4993, 10587, 8376, 7157, 7161, 8414, 4355",
    /*  2357 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 5370, 5379, 8441, 8476, 8502",
    /*  2374 */ "7589, 9261, 6846, 4355, 3653, 10567, 6565, 8520, 6749, 8784, 7993, 4718, 8555, 6565, 8603, 5079, 4903",
    /*  2391 */ "7219, 6565, 6565, 8179, 8180, 11771, 6565, 8359, 8180, 11852, 6565, 8636, 6409, 6566, 8180, 8656",
    /*  2407 */ "8173, 9272, 8358, 4994, 6485, 8358, 8013, 9817, 11550, 11685, 11454, 8678, 5010, 4355, 4355, 4355",
    /*  2423 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 6008, 6017, 8703, 4854, 4355, 7960, 9620",
    /*  2440 */ "6846, 4355, 3653, 8729, 9903, 8747, 8783, 11306, 8800, 4718, 8827, 8850, 8868, 8897, 4903, 8925, 9226",
    /*  2457 */ "11392, 10874, 11189, 11771, 6565, 8359, 8180, 8389, 6565, 8179, 6409, 6566, 8180, 5021, 8360, 6812",
    /*  2473 */ "8358, 10523, 8966, 8358, 8013, 4993, 10587, 8360, 7157, 7161, 5010, 4355, 4355, 4355, 4355, 4355",
    /*  2489 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 6047, 6056, 9005, 9031, 9085, 9114, 9149, 11033, 4355",
    /*  2506 */ "3653, 11241, 6565, 7059, 4885, 8180, 9456, 4718, 6565, 6565, 6160, 8180, 4903, 6926, 6565, 9487, 9196",
    /*  2523 */ "8285, 9213, 9249, 9305, 9336, 8389, 9385, 9402, 6409, 7651, 11796, 9893, 9436, 9472, 11624, 7564",
    /*  2539 */ "8038, 9508, 9560, 10274, 10587, 8360, 9608, 7161, 9647, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*  2555 */ "4355, 4355, 4355, 4355, 4355, 6144, 9060, 9069, 9674, 4854, 9046, 8321, 9700, 9716, 4355, 3653, 6565",
    /*  2572 */ "9492, 7059, 8180, 6687, 7993, 5056, 6565, 6565, 9752, 8180, 10139, 7219, 7935, 6565, 7805, 8180",
    /*  2588 */ "11771, 6565, 8359, 8180, 9521, 7517, 9777, 6409, 6566, 8180, 5021, 8360, 9795, 7597, 11315, 9811",
    /*  2604 */ "9833, 8013, 4993, 10587, 8360, 7157, 7161, 9882, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*  2621 */ "4355, 4355, 4355, 8504, 6917, 6532, 9927, 4854, 4355, 5025, 7717, 6846, 4355, 3653, 11617, 11235",
    /*  2637 */ "7332, 8758, 8615, 7343, 4718, 6565, 6565, 6160, 8180, 4903, 11883, 11525, 6565, 10485, 8180, 11771",
    /*  2653 */ "11903, 8359, 5087, 8389, 6565, 8179, 6409, 6566, 8180, 5021, 8360, 6812, 8358, 4994, 6485, 8358, 8013",
    /*  2670 */ "4993, 10587, 8360, 7157, 7161, 5010, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*  2687 */ "4355, 8504, 6917, 8220, 7990, 4854, 9962, 9172, 5068, 6846, 4355, 3653, 6565, 6565, 7059, 8180, 8180",
    /*  2704 */ "7993, 4718, 6565, 6565, 6160, 8180, 4903, 7219, 6565, 6565, 8179, 8180, 11771, 6565, 8359, 8180, 8389",
    /*  2721 */ "6565, 8179, 6409, 6566, 8180, 9658, 7620, 6812, 8358, 4994, 6485, 8358, 8013, 4993, 10587, 8360, 7157",
    /*  2738 */ "7161, 5010, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 4329, 4338",
    /*  2755 */ "9982, 4854, 3628, 10016, 10032, 10066, 4355, 4282, 7388, 10359, 9420, 7411, 6166, 7993, 4863, 7486",
    /*  2771 */ "6565, 10109, 10135, 4903, 4426, 6565, 10155, 8179, 10179, 11771, 6565, 8359, 8180, 8389, 6565, 8179",
    /*  2787 */ "6712, 8238, 8180, 5021, 8360, 6812, 8358, 9411, 10213, 8358, 9320, 4993, 10587, 8360, 10251, 7161",
    /*  2803 */ "5010, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 7192, 7201, 10299",
    /*  2820 */ "4854, 10315, 10831, 10093, 6846, 5946, 4257, 10353, 10375, 11007, 10392, 8180, 7993, 4718, 6565, 6565",
    /*  2836 */ "6160, 8180, 4903, 8713, 6565, 10408, 10424, 10750, 10442, 7523, 10478, 11751, 8389, 10501, 6515",
    /*  2851 */ "10547, 6566, 8180, 4934, 10583, 10603, 8358, 10693, 8687, 8358, 11144, 4993, 10636, 7258, 7157, 7161",
    /*  2867 */ "5010, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 9966, 4482, 4497, 4510",
    /*  2884 */ "4854, 10652, 10672, 4397, 6846, 9736, 10709, 6565, 10783, 9592, 8181, 10847, 7993, 4385, 10899, 10376",
    /*  2900 */ "10915, 10949, 10933, 11714, 11570, 8852, 11070, 8180, 10984, 10163, 11265, 11023, 11049, 11086, 11104",
    /*  2915 */ "6600, 11129, 11184, 11205, 11064, 11221, 10738, 11257, 6485, 8358, 8013, 11693, 11281, 11331, 7157",
    /*  2930 */ "7161, 11373, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 4631, 4640",
    /*  2947 */ "11408, 4854, 4355, 9583, 8567, 6846, 4355, 3653, 6565, 6565, 7059, 8180, 8180, 7993, 11442, 11519",
    /*  2963 */ "9866, 11470, 8180, 9789, 7219, 6565, 6565, 8179, 8180, 11771, 6565, 8359, 8180, 9349, 6565, 11486",
    /*  2979 */ "6409, 6566, 8180, 5021, 8360, 10767, 9233, 4994, 6485, 8358, 8013, 4993, 11505, 8360, 7157, 11541",
    /*  2995 */ "5010, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 6917, 8220, 7990",
    /*  3012 */ "4854, 4355, 9281, 9533, 6846, 9731, 3653, 11566, 6565, 7059, 11586, 8180, 7993, 11605, 8731, 6565",
    /*  3028 */ "6160, 8587, 4903, 7219, 6565, 6565, 8179, 8180, 11771, 6565, 8359, 8180, 8389, 6565, 8179, 6409, 6566",
    /*  3045 */ "8180, 5021, 8360, 6812, 8358, 4994, 6485, 8358, 8013, 4993, 10587, 8360, 7157, 7161, 5010, 4355, 4355",
    /*  3062 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 6917, 8220, 7990, 4854, 4355, 5025",
    /*  3079 */ "11640, 7303, 4355, 3653, 6565, 6565, 7059, 8180, 8180, 7993, 4718, 6565, 6565, 6160, 8180, 4903, 7219",
    /*  3096 */ "6565, 6565, 8179, 8180, 11771, 8539, 10455, 11742, 11674, 6565, 8179, 6409, 6566, 8180, 5021, 8360",
    /*  3112 */ "6812, 8358, 4994, 6485, 8358, 8013, 4993, 10587, 8360, 7157, 7161, 5010, 4355, 4355, 4355, 4355, 4355",
    /*  3129 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 6917, 8220, 7990, 4854, 11709, 10612, 10263, 6846",
    /*  3145 */ "4355, 3653, 6565, 6565, 11730, 8180, 8180, 11767, 4718, 6565, 6565, 6160, 8180, 4903, 7219, 6565",
    /*  3161 */ "6565, 8179, 8180, 11771, 6565, 8359, 8180, 8389, 6565, 8179, 6409, 6566, 8180, 5021, 8360, 6812, 8358",
    /*  3178 */ "4994, 6485, 8358, 8013, 4993, 10587, 8360, 7157, 7161, 5010, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*  3195 */ "4355, 4355, 4355, 4355, 4355, 8504, 6917, 8220, 7990, 4854, 4355, 5025, 11787, 9631, 4355, 3653, 6565",
    /*  3212 */ "6565, 7059, 8180, 8180, 7993, 4718, 6565, 6565, 6160, 8180, 4903, 7219, 11812, 6565, 11839, 8180",
    /*  3228 */ "11771, 6565, 8359, 8180, 8389, 6565, 8179, 6409, 6566, 8180, 5021, 8360, 6812, 8358, 4994, 6485, 8358",
    /*  3245 */ "8013, 4993, 10587, 8360, 7157, 7161, 5010, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*  3262 */ "4355, 4355, 8091, 11868, 11919, 10656, 3644, 4355, 11940, 3810, 4774, 4747, 11964, 3893, 3713, 11992",
    /*  3278 */ "3894, 3714, 3757, 3796, 3691, 3834, 3681, 3697, 3957, 3863, 3912, 3930, 3910, 3928, 3986, 4149, 4138",
    /*  3295 */ "4154, 3946, 3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818, 4020, 4036, 4062, 5179, 12032, 11948",
    /*  3311 */ "4077, 4107, 4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355",
    /*  3328 */ "8504, 8117, 8126, 7888, 3644, 11924, 3955, 12031, 4774, 4747, 3669, 3893, 3713, 3730, 3894, 3714",
    /*  3344 */ "3757, 3796, 3691, 3834, 3681, 3697, 3957, 3863, 3912, 3930, 3910, 3928, 3986, 4149, 4138, 4154, 3946",
    /*  3361 */ "3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818, 4020, 4036, 4062, 5179, 12032, 11948, 4077, 4107",
    /*  3377 */ "4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 8451",
    /*  3394 */ "8460, 9684, 3644, 4355, 3955, 12031, 4774, 4747, 12019, 3893, 3713, 12048, 3894, 3714, 3757, 3796",
    /*  3410 */ "3691, 3834, 3681, 3697, 3957, 3863, 3912, 3930, 3910, 3928, 3986, 4149, 4138, 4154, 3946, 3847, 3841",
    /*  3427 */ "3973, 4004, 4002, 3878, 4091, 3818, 4020, 4036, 4062, 5179, 12032, 11948, 4077, 4107, 4158, 4123",
    /*  3443 */ "4174, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8504, 4355, 4355, 4355",
    /*  3460 */ "4854, 4355, 5025, 7717, 6846, 4355, 6261, 6565, 6565, 8989, 8180, 8180, 7993, 7118, 6565, 6565, 4879",
    /*  3477 */ "8180, 4903, 7219, 6565, 6565, 8179, 8180, 11771, 6565, 8359, 8180, 8389, 6565, 8179, 6409, 6566, 8180",
    /*  3494 */ "5021, 8360, 6812, 8358, 4994, 6485, 8358, 8013, 4993, 10587, 8360, 7157, 7161, 5010, 4355, 4355, 4355",
    /*  3511 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 4355, 8063, 8072, 7782, 9937, 4355, 3955, 12031",
    /*  3528 */ "4046, 4747, 11964, 3893, 3713, 11992, 3894, 3714, 3757, 3796, 3691, 3834, 3681, 3697, 3957, 3863",
    /*  3544 */ "3912, 3930, 3910, 3928, 3986, 4149, 4138, 4154, 3946, 3847, 3841, 3973, 4004, 4002, 3878, 4091, 3818",
    /*  3561 */ "4020, 4036, 4062, 5179, 12032, 11948, 4077, 4107, 4158, 4123, 4174, 4355, 4355, 4355, 4355, 4355",
    /*  3577 */ "4355, 4355, 4355, 4355, 4355, 4355, 4355, 9275, 9275, 9275, 9275, 9275, 9275, 9275, 9275, 9275, 9275",
    /*  3594 */ "9275, 9275, 9275, 9275, 9275, 9275, 10308, 9275, 9275, 9275, 9275, 9275, 9275, 9275, 9275, 9275, 9275",
    /*  3611 */ "9275, 9275, 0, 133120, 0, 0, 0, 0, 0, 0, 0, 44032, 0, 44032, 0, 0, 133120, 0, 0, 0, 0, 0, 0, 162, 0",
    /*  3636 */ "0, 0, 0, 0, 0, 168, 0, 0, 0, 0, 0, 10308, 10308, 10308, 71, 72, 138240, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0",
    /*  3663 */ "283, 172, 99, 99, 99, 99, 217088, 220160, 0, 0, 234496, 0, 0, 0, 0, 0, 0, 172, 139264, 139264, 139264",
    /*  3684 */ "139264, 0, 0, 139264, 178176, 139264, 139264, 139264, 139264, 139264, 139264, 188416, 139264, 139264",
    /*  3698 */ "139264, 139264, 139264, 139264, 139264, 198656, 139264, 139264, 139264, 139264, 139264, 210944",
    /*  3710 */ "212992, 214016, 139264, 139264, 207872, 139264, 209920, 211968, 139264, 139264, 139264, 217088",
    /*  3722 */ "139264, 220160, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264",
    /*  3734 */ "234496, 139264, 139264, 139264, 0, 0, 219, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 0",
    /*  3749 */ "133120, 180224, 204800, 219136, 0, 10308, 0, 0, 139264, 139264, 139264, 234496, 139264, 139264",
    /*  3763 */ "139264, 134144, 0, 0, 0, 0, 178176, 188416, 0, 0, 0, 0, 73, 0, 0, 73, 73, 82, 82, 82, 82, 82, 82, 82",
    /*  3787 */ "82, 82, 82, 106, 125, 106, 106, 106, 106, 0, 0, 214016, 0, 0, 0, 0, 0, 0, 0, 139264, 0, 139264",
    /*  3809 */ "178176, 139264, 139264, 139264, 139264, 0, 139264, 139483, 139264, 139264, 139264, 139264, 139264",
    /*  3822 */ "139264, 139264, 139264, 139264, 0, 206848, 226304, 139264, 181248, 139264, 139264, 139264, 139264",
    /*  3835 */ "139264, 210944, 212992, 214016, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264",
    /*  3847 */ "139264, 139264, 139264, 195584, 139264, 139264, 139264, 139264, 139264, 221184, 139264, 139264",
    /*  3859 */ "225280, 139264, 139264, 231424, 233472, 0, 0, 191488, 0, 202752, 0, 0, 0, 0, 0, 0, 0, 182272, 137216",
    /*  3878 */ "139264, 0, 0, 0, 205824, 183296, 176128, 139264, 183296, 139264, 139264, 139264, 139264, 199680",
    /*  3892 */ "205824, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 193536, 139264, 139264, 139264",
    /*  3904 */ "139264, 200704, 201728, 139264, 203776, 139264, 139264, 139264, 139264, 139264, 182272, 139264",
    /*  3916 */ "139264, 139264, 139264, 139264, 191488, 139264, 139264, 139264, 139264, 139264, 202752, 139264",
    /*  3928 */ "202752, 139264, 139264, 208896, 139264, 218112, 139264, 139264, 139264, 139264, 139264, 139264",
    /*  3940 */ "229376, 230400, 139264, 232448, 139264, 139264, 235520, 139264, 237568, 0, 0, 195584, 0, 225280, 0, 0",
    /*  3956 */ "0, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264",
    /*  3969 */ "139264, 139264, 0, 135168, 139264, 139264, 225280, 139264, 139264, 231424, 139264, 190464, 0, 0, 0, 0",
    /*  3985 */ "0, 139264, 139264, 139264, 0, 0, 0, 0, 0, 0, 237568, 0, 235520, 0, 139264, 179200, 139264, 139264",
    /*  4003 */ "139264, 185344, 139264, 139264, 190464, 139264, 139264, 139264, 139264, 139264, 139264, 139264",
    /*  4015 */ "139264, 139264, 139264, 139264, 139264, 196608, 206848, 215040, 222208, 139264, 226304, 139264",
    /*  4027 */ "139264, 139264, 139264, 181248, 139264, 139264, 139264, 196608, 206848, 215040, 222208, 139264",
    /*  4039 */ "226304, 139264, 139264, 139264, 0, 139264, 186368, 139264, 139264, 139264, 139264, 139264, 139264",
    /*  4052 */ "139264, 0, 133120, 180224, 204800, 219136, 0, 0, 0, 0, 139264, 139264, 236544, 139264, 186368, 139264",
    /*  4068 */ "139264, 139264, 139264, 139264, 139264, 139264, 139264, 236544, 0, 139264, 139264, 139264, 139264",
    /*  4081 */ "139264, 189440, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 228352, 139264, 139264",
    /*  4093 */ "139264, 139264, 139264, 139264, 139264, 176128, 139264, 183296, 139264, 139264, 139264, 139264",
    /*  4105 */ "199680, 205824, 192512, 139264, 216064, 139264, 139264, 139264, 228352, 139264, 139264, 192512",
    /*  4117 */ "139264, 216064, 139264, 139264, 139264, 228352, 139264, 227328, 177152, 139264, 139264, 139264",
    /*  4129 */ "139264, 227328, 139264, 197632, 223232, 139264, 139264, 197632, 223232, 139264, 139264, 139264",
    /*  4141 */ "139264, 139264, 235520, 139264, 237568, 139264, 179200, 139264, 139264, 184320, 139264, 139264",
    /*  4153 */ "139264, 139264, 139264, 194560, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264",
    /*  4165 */ "139264, 139264, 139264, 139264, 139264, 177152, 139264, 139264, 139264, 187392, 139264, 187392",
    /*  4177 */ "139264, 139264, 139264, 139264, 139264, 139264, 139264, 224256, 224256, 0, 0, 0, 0, 0, 0, 0, 20480",
    /*  4194 */ "20480, 20480, 20480, 20480, 20480, 20480, 20480, 20480, 20480, 0, 11381, 0, 20480, 0, 0, 76, 13388",
    /*  4211 */ "13388, 13388, 13388, 13388, 13388, 13388, 13388, 13388, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21504, 21504",
    /*  4230 */ "21504, 21504, 21504, 21504, 21504, 21504, 21504, 21504, 0, 0, 0, 0, 0, 0, 0, 0, 0, 71, 71, 71, 71, 71",
    /*  4252 */ "71, 71, 0, 0, 133120, 0, 0, 0, 0, 0, 0, 279, 0, 281, 282, 283, 172, 99, 99, 99, 289, 0, 0, 0, 10308",
    /*  4277 */ "10308, 10308, 5264, 72, 138240, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 283, 172, 99, 287, 99, 99, 0, 15360, 0",
    /*  4301 */ "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10308, 15360, 0, 0, 16384, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10308, 0",
    /*  4330 */ "0, 0, 0, 73, 0, 0, 73, 73, 88, 88, 88, 88, 88, 88, 88, 88, 88, 88, 113, 132, 113, 113, 113, 113",
    /*  4354 */ "16384, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 36864, 0, 0, 0, 16384, 16384, 16384, 16384",
    /*  4379 */ "16384, 16384, 16384, 0, 0, 133120, 0, 0, 0, 0, 0, 0, 379, 0, 381, 382, 283, 383, 99, 99, 99, 99, 0",
    /*  4402 */ "118, 118, 222, 118, 118, 118, 118, 237, 242, 244, 118, 0, 0, 0, 72, 72, 72, 72, 72, 72, 72, 0, 0",
    /*  4425 */ "133120, 0, 0, 0, 0, 0, 0, 455, 456, 0, 0, 0, 0, 0, 0, 383, 99, 0, 0, 0, 10308, 10308, 10308, 71, 5265",
    /*  4450 */ "138240, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 283, 172, 285, 99, 99, 99, 72, 0, 0, 0, 0, 0, 193536, 0, 200704",
    /*  4476 */ "201728, 0, 0, 207872, 209920, 211968, 0, 0, 0, 0, 73, 0, 0, 73, 73, 90, 90, 94, 90, 94, 90, 94, 94",
    /*  4499 */ "94, 94, 94, 94, 94, 94, 94, 94, 115, 134, 115, 115, 115, 115, 134, 134, 134, 134, 134, 134, 134, 0, 0",
    /*  4522 */ "0, 0, 0, 0, 0, 62, 63, 0, 0, 0, 0, 0, 10310, 0, 60, 60, 60, 60, 60, 60, 60, 17468, 17468, 60, 60, 60",
    /*  4548 */ "60, 60, 17468, 60, 60, 60, 60, 60, 60, 60, 60, 60, 60, 10309, 17468, 60, 60, 17468, 60, 60, 17468",
    /*  4569 */ "17468, 17468, 17468, 17468, 60, 60, 17468, 60, 17468, 17468, 17468, 17468, 17468, 17468, 17468, 17468",
    /*  4585 */ "0, 0, 133120, 0, 0, 0, 0, 0, 0, 10382, 10382, 10382, 71, 72, 138240, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0",
    /*  4610 */ "283, 172, 286, 99, 99, 99, 0, 0, 0, 0, 19456, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10308, 0, 0, 0, 0, 73, 0, 0",
    /*  4638 */ "73, 73, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 116, 135, 116, 116, 116, 116, 19456, 19456, 19456, 0",
    /*  4660 */ "19456, 19456, 19456, 0, 19456, 19456, 19456, 19456, 19456, 19456, 19456, 19456, 19456, 19456, 0, 0, 0",
    /*  4677 */ "0, 0, 19456, 0, 19456, 19456, 0, 0, 0, 19456, 0, 0, 19456, 0, 0, 133120, 0, 0, 0, 0, 0, 0, 22528, 0",
    /*  4701 */ "0, 0, 0, 0, 0, 0, 10308, 0, 0, 0, 0, 10308, 10308, 29696, 71, 72, 138240, 0, 0, 0, 0, 0, 0, 0, 0, 0",
    /*  4727 */ "0, 283, 383, 99, 99, 99, 99, 20480, 20480, 20480, 11381, 11381, 11381, 11381, 11381, 11381, 11381, 0",
    /*  4745 */ "0, 133120, 0, 0, 0, 0, 0, 0, 193536, 0, 200704, 201728, 0, 0, 207872, 209920, 211968, 0, 139264",
    /*  4764 */ "139264, 139264, 139264, 234496, 139264, 139264, 139264, 0, 11264, 219, 139264, 139264, 139264, 139264",
    /*  4778 */ "139264, 139264, 139264, 0, 133120, 180224, 204800, 219136, 0, 10308, 10308, 0, 139264, 139264, 139264",
    /*  4793 */ "234496, 139264, 139264, 139264, 134144, 367, 0, 0, 0, 178176, 188416, 0, 0, 0, 0, 73, 0, 0, 73, 73",
    /*  4813 */ "83, 93, 93, 93, 93, 93, 93, 93, 93, 93, 93, 107, 126, 107, 107, 107, 107, 0, 22528, 0, 0, 0, 22528, 0",
    /*  4837 */ "0, 22528, 22528, 22528, 22528, 22528, 22528, 22528, 22528, 22528, 22528, 0, 0, 0, 0, 0, 0, 0, 0, 0",
    /*  4857 */ "10308, 10308, 10308, 71, 72, 73, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 283, 383, 99, 99, 99, 386, 99, 99, 99",
    /*  4882 */ "99, 11589, 0, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 344, 118, 118, 118, 118, 118, 118",
    /*  4902 */ "553, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 0, 0, 685, 99, 686, 687",
    /*  4923 */ "99, 99, 99, 99, 99, 118, 118, 118, 118, 118, 696, 118, 0, 0, 651, 0, 0, 99, 99, 99, 99, 99, 99, 99",
    /*  4947 */ "99, 99, 99, 99, 409, 99, 99, 99, 99, 697, 698, 118, 118, 118, 118, 118, 0, 99, 705, 99, 99, 99, 99",
    /*  4970 */ "99, 711, 99, 99, 99, 118, 715, 118, 118, 118, 118, 118, 721, 118, 118, 118, 0, 725, 99, 99, 99, 118",
    /*  4992 */ "777, 118, 118, 118, 118, 118, 118, 118, 118, 0, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 118, 118, 99",
    /*  5015 */ "118, 99, 118, 99, 118, 99, 118, 0, 0, 0, 0, 0, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99",
    /*  5041 */ "99, 539, 0, 0, 0, 24712, 24712, 24712, 24712, 24712, 24712, 24712, 0, 0, 133120, 0, 0, 0, 0, 0, 378",
    /*  5062 */ "0, 0, 0, 0, 283, 383, 99, 99, 99, 99, 0, 118, 118, 118, 118, 118, 231, 118, 118, 118, 118, 118, 431",
    /*  5085 */ "118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 559, 118, 118, 118, 118, 118, 118, 118, 0, 26722",
    /*  5105 */ "26722, 26722, 26722, 26722, 26722, 26722, 26722, 26722, 0, 0, 0, 0, 0, 0, 0, 0, 0, 23552, 23552",
    /*  5124 */ "23552, 23552, 23552, 23552, 23552, 23552, 23552, 23552, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 26722, 26722",
    /*  5144 */ "26722, 26722, 26722, 0, 0, 0, 133120, 0, 0, 0, 0, 0, 15360, 0, 0, 15360, 15360, 0, 0, 0, 0, 0, 0, 0",
    /*  5168 */ "0, 0, 22528, 0, 0, 133120, 0, 0, 0, 0, 27648, 139264, 139264, 139264, 139264, 139264, 139264, 139264",
    /*  5186 */ "139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 0, 0, 0, 137, 137, 137, 137",
    /*  5202 */ "137, 137, 137, 0, 0, 61, 0, 0, 0, 0, 0, 20480, 0, 0, 0, 0, 0, 0, 0, 0, 10308, 0, 28672, 28672, 28672",
    /*  5227 */ "28734, 28672, 28672, 28734, 28672, 28672, 28734, 0, 0, 0, 0, 0, 0, 0, 0, 0, 28672, 0, 18432, 133120",
    /*  5247 */ "30720, 0, 0, 0, 0, 0, 10383, 18432, 10383, 71, 72, 138240, 0, 0, 0, 0, 0, 0, 0, 0, 0, 76, 76, 76, 76",
    /*  5272 */ "76, 76, 76, 217088, 220160, 0, 0, 234496, 0, 0, 0, 0, 0, 0, 172, 139435, 139435, 139435, 139435, 0, 0",
    /*  5293 */ "139482, 178394, 139482, 139482, 139482, 139482, 139482, 139482, 188634, 139482, 139435, 208043",
    /*  5305 */ "139435, 210091, 212139, 139435, 139435, 139435, 217259, 139435, 220331, 139435, 139435, 139435",
    /*  5317 */ "139435, 139435, 235691, 139435, 237739, 139482, 179418, 139482, 139482, 184538, 139482, 139482",
    /*  5329 */ "139482, 208090, 139482, 210138, 212186, 139482, 139482, 139482, 217306, 139482, 220378, 139482",
    /*  5341 */ "139482, 139482, 139482, 139482, 139482, 139482, 0, 133120, 180224, 204800, 219136, 0, 0, 10383, 0",
    /*  5356 */ "139482, 139482, 139482, 234714, 139482, 139482, 139482, 134144, 0, 0, 0, 0, 178176, 188416, 0, 0, 0",
    /*  5373 */ "0, 73, 0, 0, 73, 73, 84, 84, 84, 84, 84, 84, 84, 84, 84, 84, 108, 127, 108, 108, 108, 108, 0, 0",
    /*  5397 */ "214016, 0, 0, 0, 0, 0, 0, 0, 139264, 0, 139435, 178347, 139435, 139435, 139435, 139482, 139482",
    /*  5414 */ "189658, 139482, 139482, 139482, 139482, 139482, 139482, 139482, 228352, 139435, 139435, 139435",
    /*  5426 */ "195755, 139435, 139435, 139435, 139435, 139435, 221355, 139435, 139435, 225451, 139435, 139435",
    /*  5438 */ "231595, 233472, 0, 0, 191488, 0, 202752, 0, 0, 0, 0, 0, 0, 0, 182272, 137216, 139435, 139435, 211115",
    /*  5457 */ "213163, 214187, 139435, 139435, 139435, 139435, 139435, 139435, 139435, 139435, 139435, 139435",
    /*  5469 */ "139435, 193707, 139435, 139435, 139435, 139435, 200875, 201899, 139435, 203947, 139435, 209067",
    /*  5481 */ "139435, 218283, 139435, 139435, 139435, 139435, 139435, 139435, 229547, 230571, 139435, 232619",
    /*  5493 */ "139435, 139435, 182443, 139435, 139435, 139435, 139435, 139435, 191659, 139435, 139435, 139435",
    /*  5505 */ "139435, 139435, 202923, 139435, 202970, 139482, 139482, 209114, 139482, 218330, 139482, 139482",
    /*  5517 */ "139482, 139482, 139482, 139482, 229594, 230618, 139482, 232666, 139482, 139482, 139482, 0, 0, 0, 0, 0",
    /*  5533 */ "0, 237568, 0, 235520, 0, 139435, 179371, 139435, 139435, 236715, 139482, 186586, 139482, 139482",
    /*  5547 */ "139482, 139482, 139482, 139482, 139482, 139482, 236762, 0, 139435, 139482, 139482, 139482, 139482",
    /*  5560 */ "139482, 139482, 139482, 139482, 195802, 139482, 139482, 139482, 139482, 139482, 221402, 139435",
    /*  5572 */ "184491, 139435, 139435, 139435, 139435, 139435, 194731, 139435, 139435, 139435, 139435, 139435",
    /*  5584 */ "139435, 139435, 139435, 217, 139482, 139482, 139482, 139482, 139482, 139482, 139482, 139482, 139482",
    /*  5597 */ "139482, 139482, 0, 139435, 139435, 189611, 139435, 139435, 139435, 139435, 139482, 139482, 194778",
    /*  5610 */ "139482, 139482, 139482, 139482, 139482, 139482, 139482, 139482, 139482, 139482, 139482, 139482",
    /*  5622 */ "139482, 139482, 0, 135168, 235738, 139482, 237786, 0, 0, 195584, 0, 225280, 0, 0, 0, 139435, 139435",
    /*  5639 */ "139435, 139435, 139435, 139435, 139435, 139435, 139435, 139435, 139435, 139435, 139435, 139435",
    /*  5651 */ "176346, 139482, 183514, 139482, 139482, 139482, 139482, 199898, 206042, 139482, 139482, 225498",
    /*  5663 */ "139482, 139482, 231642, 139482, 190464, 0, 0, 0, 0, 0, 139435, 139435, 139435, 139435, 234667, 139435",
    /*  5679 */ "139435, 139435, 0, 0, 219, 139482, 139482, 139482, 139482, 139482, 139482, 193754, 139482, 139482",
    /*  5693 */ "139482, 139482, 200922, 201946, 139482, 203994, 139482, 185515, 139435, 139435, 190635, 139435",
    /*  5705 */ "139435, 139435, 139435, 139435, 139435, 139435, 139435, 139435, 139435, 139435, 139482, 139482",
    /*  5717 */ "139482, 139482, 139482, 139482, 177323, 139435, 139435, 139435, 139482, 139482, 185562, 139482",
    /*  5729 */ "139482, 190682, 139482, 139482, 139482, 139482, 139482, 139482, 139482, 139482, 139482, 139482, 0",
    /*  5742 */ "206848, 226304, 139435, 181419, 139435, 139435, 139435, 196779, 207019, 215211, 222379, 139435",
    /*  5754 */ "226475, 139435, 139435, 139435, 139482, 181466, 139482, 139482, 139482, 196826, 207066, 215258",
    /*  5766 */ "222426, 139482, 226522, 139482, 139482, 139482, 0, 139435, 186539, 139435, 139435, 139435, 139435",
    /*  5779 */ "139435, 139435, 188587, 139435, 139435, 139435, 139435, 139435, 139435, 139435, 198827, 139435",
    /*  5791 */ "139435, 139435, 139482, 139482, 139482, 0, 139435, 139435, 139435, 139435, 139435, 139435, 139435",
    /*  5804 */ "139435, 139435, 139435, 139482, 139482, 139482, 139482, 139482, 139482, 139482, 192683, 139435",
    /*  5816 */ "216235, 139435, 139435, 139435, 228523, 139482, 139482, 192730, 139482, 216282, 139482, 139482",
    /*  5828 */ "139482, 228570, 139435, 227499, 177370, 139482, 139482, 139482, 139482, 227546, 139435, 197803",
    /*  5840 */ "223403, 139435, 139482, 197850, 223450, 139482, 0, 0, 0, 205824, 183296, 176299, 139435, 183467",
    /*  5854 */ "139435, 139435, 139435, 139435, 199851, 205995, 139435, 139482, 139482, 139482, 182490, 139482",
    /*  5866 */ "139482, 139482, 139482, 139482, 191706, 139482, 139482, 139482, 139482, 139482, 139482, 198874",
    /*  5878 */ "139482, 139482, 139482, 139482, 139482, 211162, 213210, 214234, 139482, 187563, 139435, 187610",
    /*  5890 */ "139482, 139435, 139482, 139435, 139482, 139435, 139482, 224427, 224474, 0, 0, 0, 0, 0, 0, 0, 40960, 0",
    /*  5908 */ "0, 0, 0, 0, 0, 0, 0, 0, 40960, 0, 40960, 40960, 0, 0, 0, 10308, 10308, 10308, 71, 72, 138240, 0, 0, 0",
    /*  5932 */ "33792, 0, 0, 0, 0, 0, 21504, 21504, 21504, 21504, 21504, 0, 0, 133120, 0, 0, 0, 0, 0, 271, 0, 0, 0, 0",
    /*  5956 */ "0, 0, 0, 0, 0, 0, 65, 0, 0, 0, 10308, 0, 35936, 35936, 35936, 35936, 35936, 35936, 35936, 35936",
    /*  5976 */ "35936, 35936, 0, 0, 0, 0, 0, 0, 0, 0, 0, 28672, 28672, 28672, 28672, 28672, 28672, 28672, 0, 0, 0",
    /*  5997 */ "10308, 10308, 10308, 71, 72, 138240, 0, 0, 14336, 34816, 38912, 0, 0, 0, 0, 73, 0, 0, 73, 73, 85, 85",
    /*  6019 */ "85, 85, 85, 85, 85, 85, 85, 85, 109, 128, 109, 109, 109, 109, 0, 0, 0, 10308, 10308, 10308, 71, 72",
    /*  6041 */ "138240, 0, 0, 0, 0, 39936, 0, 0, 0, 0, 73, 0, 0, 73, 73, 86, 86, 86, 86, 86, 86, 86, 86, 86, 86, 110",
    /*  6067 */ "129, 110, 110, 110, 110, 0, 0, 0, 40960, 40960, 40960, 40960, 40960, 40960, 40960, 0, 0, 133120, 0, 0",
    /*  6087 */ "0, 0, 0, 23552, 23552, 23552, 23552, 23552, 0, 0, 133120, 0, 0, 0, 0, 0, 278, 0, 0, 0, 0, 283, 172",
    /*  6110 */ "99, 99, 99, 99, 0, 118, 118, 118, 118, 118, 118, 118, 118, 239, 118, 118, 0, 0, 0, 41984, 41984",
    /*  6131 */ "41984, 41984, 41984, 41984, 41984, 0, 0, 133120, 0, 0, 0, 0, 71, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 66",
    /*  6156 */ "0, 0, 10308, 0, 99, 99, 99, 99, 11589, 383, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 354",
    /*  6177 */ "118, 118, 118, 118, 118, 100, 100, 100, 119, 119, 119, 119, 119, 119, 119, 0, 0, 0, 0, 0, 0, 0, 0, 0",
    /*  6201 */ "32845, 0, 0, 133120, 0, 0, 0, 0, 43008, 43008, 43008, 43008, 43008, 43008, 43008, 43008, 43008, 0, 0",
    /*  6220 */ "0, 0, 0, 0, 0, 0, 0, 32845, 32845, 32845, 32845, 32845, 32845, 32845, 32845, 32845, 32845, 0, 0, 0, 0",
    /*  6241 */ "0, 0, 0, 0, 0, 43008, 43008, 43008, 43008, 43008, 43008, 43008, 0, 0, 133120, 0, 0, 0, 0, 72, 0, 0, 0",
    /*  6264 */ "0, 0, 0, 0, 0, 0, 0, 0, 172, 99, 99, 99, 99, 0, 0, 0, 64, 73, 64, 0, 73, 73, 79, 79, 79, 79, 79, 79",
    /*  6292 */ "79, 79, 79, 79, 101, 120, 101, 101, 101, 101, 101, 101, 101, 120, 120, 120, 120, 120, 120, 120, 0, 0",
    /*  6314 */ "0, 0, 0, 0, 0, 0, 0, 35840, 35840, 35840, 35840, 35840, 35840, 35840, 170, 0, 99, 99, 99, 176, 99, 99",
    /*  6336 */ "99, 99, 99, 99, 198, 200, 99, 204, 99, 207, 99, 99, 0, 118, 118, 118, 223, 118, 118, 118, 118, 118",
    /*  6358 */ "118, 245, 247, 118, 251, 118, 254, 118, 118, 0, 0, 0, 0, 0, 0, 10308, 10308, 0, 0, 0, 0, 73, 0, 74",
    /*  6382 */ "73, 73, 81, 81, 81, 81, 81, 81, 81, 81, 81, 81, 104, 123, 104, 104, 104, 104, 99, 415, 99, 99, 11589",
    /*  6405 */ "383, 118, 118, 419, 118, 118, 118, 118, 118, 118, 118, 0, 0, 0, 0, 0, 0, 99, 99, 99, 99, 99, 465, 99",
    /*  6429 */ "99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 476, 99, 118, 118, 118, 492, 118, 118, 118, 118, 118, 118",
    /*  6452 */ "118, 118, 118, 118, 118, 118, 118, 644, 118, 118, 118, 503, 118, 118, 118, 118, 118, 118, 118, 118",
    /*  6472 */ "118, 118, 118, 118, 118, 118, 0, 135168, 99, 99, 99, 543, 99, 99, 99, 99, 118, 118, 118, 118, 118",
    /*  6493 */ "118, 118, 118, 118, 118, 118, 0, 99, 99, 618, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 629, 99",
    /*  6516 */ "118, 118, 118, 118, 118, 118, 118, 599, 118, 118, 118, 118, 118, 118, 118, 265, 0, 0, 0, 0, 0, 0, 0",
    /*  6539 */ "0, 0, 0, 112, 131, 112, 112, 112, 112, 118, 118, 118, 635, 118, 118, 118, 118, 118, 118, 118, 118",
    /*  6560 */ "118, 118, 118, 646, 526, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 118, 118",
    /*  6583 */ "118, 760, 118, 762, 118, 764, 118, 0, 99, 99, 99, 99, 99, 99, 99, 99, 691, 118, 118, 118, 118, 118",
    /*  6605 */ "118, 118, 0, 0, 0, 0, 0, 0, 99, 616, 99, 97, 97, 97, 97, 97, 97, 97, 97, 97, 97, 102, 121, 102, 102",
    /*  6630 */ "102, 102, 121, 121, 121, 121, 121, 121, 121, 0, 0, 0, 0, 0, 0, 0, 0, 0, 35936, 0, 0, 133120, 0, 31744",
    /*  6654 */ "0, 0, 0, 0, 10308, 10308, 10308, 71, 72, 73, 0, 147, 0, 0, 0, 0, 0, 0, 0, 280, 0, 0, 283, 172, 99, 99",
    /*  6680 */ "99, 99, 99, 208, 99, 99, 0, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 356, 118, 118, 118",
    /*  6702 */ "118, 118, 118, 118, 336, 118, 118, 118, 118, 341, 118, 118, 118, 118, 118, 118, 118, 0, 0, 0, 0, 0, 0",
    /*  6725 */ "615, 99, 99, 99, 99, 99, 390, 99, 99, 99, 99, 99, 99, 99, 99, 99, 398, 99, 99, 99, 99, 99, 99, 665",
    /*  6749 */ "118, 118, 118, 118, 118, 118, 118, 118, 118, 342, 118, 118, 118, 118, 118, 118, 99, 99, 99, 99, 11589",
    /*  6770 */ "383, 118, 118, 118, 118, 118, 118, 118, 424, 118, 118, 118, 0, 0, 0, 0, 0, 0, 572, 0, 99, 99, 99, 99",
    /*  6794 */ "99, 99, 99, 666, 118, 118, 118, 669, 118, 118, 118, 118, 118, 118, 440, 441, 118, 118, 118, 118, 118",
    /*  6815 */ "118, 118, 118, 118, 118, 0, 0, 0, 99, 99, 99, 99, 99, 118, 759, 118, 118, 118, 118, 118, 118, 0, 99",
    /*  6838 */ "99, 99, 99, 99, 99, 99, 99, 734, 118, 118, 118, 118, 118, 118, 118, 0, 0, 0, 0, 0, 0, 10308, 10308, 0",
    /*  6862 */ "99, 99, 118, 118, 118, 118, 118, 118, 99, 99, 99, 818, 118, 118, 118, 820, 103, 103, 103, 122, 122",
    /*  6883 */ "122, 122, 122, 122, 122, 0, 0, 0, 0, 0, 0, 0, 0, 0, 37966, 0, 0, 133120, 0, 0, 0, 0, 0, 155, 0, 0, 0",
    /*  6910 */ "0, 0, 0, 0, 0, 0, 155, 0, 0, 0, 0, 73, 0, 0, 73, 73, 0, 0, 0, 0, 0, 0, 0, 0, 0, 458, 0, 0, 0, 0, 383",
    /*  6941 */ "99, 0, 0, 0, 269, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 275, 99, 99, 99, 466, 99, 99, 99, 99, 99, 99, 99",
    /*  6969 */ "99, 99, 99, 99, 99, 99, 411, 99, 99, 99, 118, 118, 118, 118, 493, 118, 118, 118, 118, 118, 118, 118",
    /*  6991 */ "118, 118, 118, 561, 118, 118, 118, 118, 118, 99, 118, 118, 118, 118, 596, 118, 118, 118, 118, 118",
    /*  7011 */ "118, 118, 118, 118, 118, 641, 642, 118, 118, 118, 118, 99, 727, 99, 99, 99, 99, 99, 99, 99, 118, 118",
    /*  7033 */ "737, 118, 118, 118, 118, 255, 118, 118, 0, 0, 0, 0, 0, 0, 10308, 10308, 0, 118, 118, 118, 761, 118",
    /*  7055 */ "118, 118, 118, 0, 99, 99, 99, 99, 99, 99, 99, 99, 11589, 283, 219, 118, 118, 118, 118, 118, 104, 104",
    /*  7077 */ "104, 123, 123, 123, 123, 123, 123, 123, 0, 0, 0, 0, 0, 0, 0, 0, 0, 37966, 37966, 37966, 37966, 37966",
    /*  7099 */ "37966, 37966, 37966, 37966, 37966, 0, 0, 0, 0, 0, 0, 0, 153, 0, 158, 0, 161, 0, 163, 0, 0, 0, 0, 0, 0",
    /*  7124 */ "0, 0, 0, 0, 283, 0, 99, 99, 99, 99, 206, 209, 212, 99, 0, 118, 118, 118, 118, 226, 118, 118, 236, 118",
    /*  7148 */ "243, 118, 118, 118, 0, 99, 99, 99, 749, 99, 99, 99, 99, 99, 99, 118, 118, 118, 118, 118, 118, 99, 99",
    /*  7171 */ "99, 99, 118, 118, 118, 118, 248, 118, 118, 253, 256, 259, 118, 0, 265, 0, 0, 0, 266, 10308, 10308, 0",
    /*  7193 */ "0, 0, 0, 73, 0, 75, 73, 73, 89, 89, 89, 89, 89, 89, 89, 89, 89, 89, 114, 133, 114, 114, 114, 114, 0",
    /*  7218 */ "267, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 383, 99, 99, 99, 306, 99, 99, 99, 99, 99, 99, 99, 99",
    /*  7246 */ "99, 313, 99, 99, 99, 99, 99, 99, 309, 99, 99, 311, 99, 99, 99, 99, 99, 99, 99, 118, 792, 118, 118",
    /*  7269 */ "118, 118, 118, 118, 118, 432, 118, 118, 118, 118, 118, 118, 118, 118, 0, 99, 768, 99, 99, 99, 99, 99",
    /*  7291 */ "118, 348, 118, 118, 118, 118, 118, 118, 118, 118, 118, 355, 118, 118, 118, 118, 258, 118, 118, 0, 0",
    /*  7312 */ "0, 0, 0, 0, 10308, 10308, 0, 0, 375, 0, 0, 0, 0, 0, 0, 0, 0, 283, 383, 384, 99, 99, 99, 99, 99, 99",
    /*  7338 */ "323, 99, 11589, 283, 219, 118, 118, 118, 118, 118, 365, 118, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 119",
    /*  7362 */ "100, 100, 100, 100, 401, 99, 99, 99, 99, 403, 404, 405, 99, 99, 99, 99, 410, 99, 412, 99, 99, 99, 99",
    /*  7385 */ "99, 480, 481, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 301, 99, 99, 99, 99, 99, 99, 99, 416, 99, 11589",
    /*  7409 */ "383, 418, 118, 118, 118, 118, 118, 118, 118, 118, 118, 343, 118, 118, 118, 118, 118, 118, 118, 427",
    /*  7429 */ "118, 118, 430, 118, 118, 118, 118, 118, 435, 118, 118, 118, 118, 437, 438, 439, 118, 118, 118, 118",
    /*  7449 */ "444, 118, 446, 118, 118, 118, 450, 118, 0, 0, 0, 0, 0, 652, 99, 99, 99, 655, 99, 99, 99, 99, 99, 99",
    /*  7473 */ "99, 393, 99, 99, 396, 99, 99, 99, 99, 99, 99, 99, 585, 99, 99, 99, 99, 99, 99, 99, 99, 99, 395, 99",
    /*  7497 */ "99, 99, 99, 99, 99, 0, 452, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 383, 99, 99, 99, 99, 99, 583, 99, 99",
    /*  7525 */ "99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 536, 99, 99, 99, 118, 634, 118, 118, 118, 118, 637, 118, 118",
    /*  7548 */ "640, 118, 118, 118, 118, 645, 647, 659, 99, 99, 99, 99, 99, 99, 118, 667, 118, 118, 118, 118, 118",
    /*  7569 */ "118, 118, 0, 99, 99, 99, 99, 99, 709, 99, 99, 118, 673, 118, 118, 118, 118, 118, 118, 680, 0, 0, 99",
    /*  7592 */ "99, 99, 99, 99, 182, 99, 99, 99, 99, 99, 99, 99, 99, 99, 692, 118, 118, 118, 118, 118, 118, 118, 118",
    /*  7615 */ "744, 0, 99, 99, 748, 99, 99, 99, 99, 99, 99, 99, 118, 118, 118, 118, 118, 670, 118, 118, 118, 758",
    /*  7637 */ "118, 118, 118, 118, 118, 118, 118, 0, 767, 99, 99, 769, 99, 771, 99, 99, 99, 99, 99, 621, 622, 99, 99",
    /*  7660 */ "99, 626, 99, 99, 99, 99, 118, 118, 118, 717, 118, 118, 118, 118, 118, 118, 118, 724, 99, 99, 99, 99",
    /*  7682 */ "776, 118, 118, 778, 118, 780, 118, 118, 118, 118, 0, 99, 99, 99, 99, 99, 751, 99, 99, 99, 99, 118",
    /*  7704 */ "118, 99, 798, 99, 99, 99, 802, 118, 804, 118, 118, 118, 808, 99, 99, 99, 99, 0, 118, 118, 118, 118",
    /*  7726 */ "118, 118, 118, 118, 118, 118, 118, 118, 118, 564, 118, 118, 105, 105, 105, 124, 124, 124, 124, 124",
    /*  7746 */ "124, 124, 0, 0, 0, 0, 0, 0, 0, 0, 0, 44032, 44032, 44032, 44032, 44032, 44032, 44032, 44032, 44032",
    /*  7766 */ "44032, 0, 0, 0, 0, 0, 0, 0, 140, 0, 10308, 10308, 10308, 71, 72, 73, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12288",
    /*  7792 */ "0, 0, 133120, 0, 0, 0, 99, 99, 542, 99, 99, 99, 99, 99, 118, 118, 118, 118, 118, 118, 118, 118, 118",
    /*  7815 */ "118, 118, 118, 118, 501, 118, 797, 99, 99, 99, 99, 99, 803, 118, 118, 118, 118, 118, 99, 99, 99, 99",
    /*  7837 */ "0, 118, 118, 118, 118, 118, 118, 118, 118, 238, 118, 118, 99, 99, 118, 118, 118, 118, 118, 118, 817",
    /*  7858 */ "99, 99, 99, 819, 118, 118, 118, 0, 0, 0, 0, 0, 0, 0, 0, 0, 523, 99, 99, 525, 106, 106, 106, 125, 125",
    /*  7883 */ "125, 125, 125, 125, 125, 0, 0, 0, 0, 0, 0, 0, 0, 0, 108636, 0, 0, 133120, 0, 0, 0, 99, 660, 99, 99",
    /*  7908 */ "99, 99, 99, 118, 118, 118, 118, 118, 118, 671, 118, 118, 118, 0, 0, 0, 0, 0, 521, 0, 0, 0, 0, 99, 99",
    /*  7933 */ "99, 177, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 474, 99, 99, 99, 118, 118, 674, 118, 118",
    /*  7956 */ "118, 118, 118, 0, 0, 0, 99, 99, 99, 99, 99, 183, 99, 99, 99, 99, 99, 99, 99, 99, 99, 735, 118, 118",
    /*  7980 */ "118, 118, 118, 118, 99, 99, 99, 99, 688, 99, 99, 99, 99, 118, 118, 118, 118, 118, 118, 118, 0, 0, 0",
    /*  8003 */ "0, 0, 0, 0, 0, 0, 118, 118, 699, 118, 118, 118, 118, 0, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 118",
    /*  8028 */ "118, 99, 99, 118, 118, 814, 815, 118, 118, 99, 99, 99, 99, 118, 118, 118, 118, 118, 719, 118, 118",
    /*  8049 */ "118, 118, 118, 0, 99, 0, 0, 141, 10308, 10308, 10308, 71, 72, 73, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12288",
    /*  8073 */ "12288, 12288, 12288, 12288, 12288, 12288, 12288, 12288, 12288, 0, 0, 0, 0, 0, 0, 0, 0, 268, 0, 0, 0",
    /*  8094 */ "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 107520, 10308, 0, 107, 107, 107, 126, 126, 126, 126, 126, 126, 126, 0",
    /*  8118 */ "0, 0, 0, 0, 0, 0, 0, 0, 108636, 108636, 108636, 108636, 108636, 108636, 108636, 108636, 108636",
    /*  8135 */ "108636, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10308, 10308, 10308, 71, 72, 73, 0, 0, 0, 0, 0, 0, 149, 290, 99",
    /*  8160 */ "99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 303, 99, 99, 99, 99, 99, 664, 99, 118, 118, 118, 118",
    /*  8184 */ "118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 347, 99, 99, 320, 99, 99, 99, 99, 324",
    /*  8205 */ "11589, 283, 219, 118, 118, 118, 118, 332, 118, 362, 118, 118, 118, 118, 366, 0, 0, 0, 0, 0, 0, 0, 0",
    /*  8228 */ "0, 0, 99, 118, 99, 99, 99, 99, 99, 464, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99",
    /*  8253 */ "632, 99, 118, 118, 491, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 565",
    /*  8273 */ "118, 591, 118, 118, 118, 118, 118, 118, 118, 118, 118, 600, 118, 118, 118, 118, 118, 118, 506, 118",
    /*  8293 */ "118, 118, 118, 118, 118, 118, 118, 118, 0, 610, 0, 612, 613, 614, 99, 99, 617, 605, 118, 118, 607",
    /*  8314 */ "118, 118, 609, 0, 0, 0, 0, 0, 0, 99, 99, 99, 178, 181, 99, 187, 99, 99, 99, 199, 202, 99, 99, 99, 99",
    /*  8339 */ "99, 729, 99, 731, 732, 733, 99, 118, 118, 118, 118, 739, 118, 741, 742, 743, 118, 0, 99, 99, 99, 99",
    /*  8361 */ "99, 99, 99, 99, 99, 99, 118, 118, 118, 118, 118, 118, 118, 118, 118, 99, 787, 99, 788, 99, 99, 99",
    /*  8383 */ "118, 118, 118, 793, 118, 794, 118, 118, 118, 0, 0, 0, 0, 0, 0, 0, 0, 99, 99, 99, 99, 99, 99, 99, 99",
    /*  8408 */ "192, 99, 99, 99, 99, 99, 99, 99, 118, 118, 823, 824, 99, 118, 99, 118, 99, 118, 0, 0, 0, 0, 0, 99, 99",
    /*  8433 */ "99, 99, 99, 99, 657, 99, 99, 99, 108, 108, 108, 127, 127, 127, 127, 127, 127, 127, 0, 0, 0, 0, 0, 0",
    /*  8457 */ "0, 0, 0, 110592, 110592, 110592, 110592, 110592, 110592, 110592, 110592, 110592, 110592, 0, 0, 0, 0",
    /*  8474 */ "0, 0, 0, 0, 0, 10308, 10308, 10308, 71, 72, 73, 146, 0, 0, 0, 0, 0, 0, 0, 0, 164, 0, 0, 0, 0, 0, 169",
    /*  8501 */ "0, 150, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10308, 0, 318, 99, 99, 99, 99, 99, 99, 99, 11589",
    /*  8529 */ "283, 219, 118, 118, 118, 118, 118, 118, 702, 0, 99, 99, 99, 99, 99, 99, 99, 99, 99, 533, 99, 99, 99",
    /*  8552 */ "99, 99, 99, 99, 388, 99, 99, 99, 99, 99, 99, 99, 99, 99, 397, 99, 99, 99, 99, 0, 118, 118, 118, 118",
    /*  8576 */ "118, 118, 235, 118, 118, 118, 118, 337, 118, 118, 340, 118, 118, 118, 118, 118, 118, 118, 118, 433",
    /*  8596 */ "118, 118, 118, 118, 118, 118, 118, 99, 99, 99, 99, 11589, 383, 118, 118, 118, 118, 118, 422, 118, 118",
    /*  8617 */ "118, 118, 350, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 449, 118, 118, 0, 0, 99, 118",
    /*  8638 */ "118, 594, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 345, 118, 118, 118, 649, 0",
    /*  8659 */ "0, 0, 0, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 485, 99, 99, 99, 99, 99, 118, 813, 118, 118",
    /*  8684 */ "118, 118, 99, 99, 99, 99, 118, 118, 118, 118, 718, 118, 720, 118, 118, 118, 118, 0, 99, 109, 109, 109",
    /*  8706 */ "128, 128, 128, 128, 128, 128, 128, 0, 0, 0, 0, 0, 0, 0, 0, 457, 0, 0, 0, 0, 0, 383, 462, 99, 291, 99",
    /*  8732 */ "99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 399, 99, 99, 319, 99, 99, 99, 99, 99, 99, 11589",
    /*  8756 */ "283, 219, 118, 118, 118, 118, 118, 339, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 264, 137, 0",
    /*  8777 */ "0, 0, 0, 10308, 10308, 0, 333, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118",
    /*  8798 */ "118, 360, 361, 118, 118, 118, 118, 118, 118, 0, 0, 0, 370, 0, 0, 0, 0, 0, 0, 0, 380, 0, 0, 283, 383",
    /*  8823 */ "99, 99, 385, 99, 99, 99, 389, 99, 99, 99, 392, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 534, 99, 99",
    /*  8847 */ "99, 99, 99, 99, 402, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 486, 99, 99, 99, 99, 99",
    /*  8872 */ "11589, 383, 118, 118, 118, 118, 118, 118, 423, 118, 118, 118, 0, 0, 0, 0, 0, 0, 0, 0, 99, 99, 99, 99",
    /*  8896 */ "578, 426, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 436, 118, 118, 118, 118, 364, 118, 118",
    /*  8916 */ "265, 0, 0, 0, 371, 0, 0, 372, 373, 0, 0, 453, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 383, 99, 99, 99, 99",
    /*  8944 */ "294, 99, 99, 99, 99, 299, 99, 99, 99, 99, 99, 99, 99, 99, 11589, 283, 219, 328, 118, 118, 118, 118",
    /*  8966 */ "99, 713, 99, 118, 118, 118, 118, 118, 118, 118, 118, 118, 723, 118, 0, 99, 99, 99, 99, 467, 468, 99",
    /*  8988 */ "470, 99, 99, 99, 99, 99, 99, 99, 99, 0, 0, 219, 118, 118, 118, 118, 118, 110, 110, 110, 129, 129, 129",
    /*  9011 */ "129, 129, 129, 129, 0, 0, 0, 0, 0, 0, 0, 0, 16384, 0, 0, 0, 0, 0, 16384, 0, 139, 0, 0, 10308, 10308",
    /*  9036 */ "10308, 71, 72, 73, 0, 0, 0, 0, 0, 148, 0, 0, 0, 0, 160, 0, 0, 0, 0, 165, 0, 0, 0, 165, 0, 0, 0, 0, 73",
    /*  9065 */ "0, 0, 73, 73, 87, 87, 87, 87, 87, 87, 87, 87, 87, 87, 111, 130, 111, 111, 111, 111, 0, 0, 156, 0, 0",
    /*  9090 */ "0, 0, 0, 0, 0, 166, 0, 167, 0, 0, 0, 0, 73, 0, 0, 73, 73, 0, 0, 0, 0, 95, 0, 95, 0, 0, 99, 99, 173",
    /*  9119 */ "99, 99, 99, 99, 99, 193, 99, 99, 99, 99, 99, 99, 99, 690, 99, 118, 118, 118, 118, 118, 118, 118, 118",
    /*  9142 */ "118, 118, 118, 602, 118, 118, 118, 99, 99, 99, 216, 0, 118, 118, 220, 118, 118, 118, 118, 118, 240",
    /*  9163 */ "118, 118, 118, 0, 0, 0, 0, 0, 571, 0, 0, 99, 99, 99, 99, 99, 184, 99, 99, 99, 99, 99, 99, 99, 99, 406",
    /*  9189 */ "407, 99, 99, 99, 99, 99, 99, 488, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118",
    /*  9210 */ "118, 118, 566, 118, 118, 515, 516, 0, 0, 0, 520, 0, 0, 522, 0, 0, 99, 99, 99, 99, 99, 99, 469, 99, 99",
    /*  9235 */ "99, 99, 99, 99, 99, 99, 99, 118, 118, 118, 118, 695, 118, 118, 99, 99, 527, 528, 99, 530, 99, 99, 99",
    /*  9258 */ "99, 99, 535, 99, 99, 99, 99, 0, 118, 118, 118, 118, 118, 229, 118, 118, 118, 118, 118, 118, 678, 118",
    /*  9280 */ "0, 0, 0, 99, 99, 99, 99, 99, 185, 99, 99, 99, 99, 99, 99, 99, 99, 586, 99, 99, 99, 99, 99, 99, 99, 99",
    /*  9306 */ "541, 99, 99, 99, 99, 99, 99, 118, 118, 118, 118, 118, 549, 550, 118, 118, 118, 0, 99, 747, 99, 99, 99",
    /*  9329 */ "99, 99, 99, 99, 99, 118, 757, 552, 118, 118, 118, 118, 118, 557, 118, 118, 118, 118, 118, 563, 118",
    /*  9350 */ "118, 118, 0, 0, 0, 0, 0, 0, 0, 0, 99, 575, 99, 99, 99, 99, 99, 99, 531, 99, 99, 99, 99, 99, 99, 99",
    /*  9376 */ "99, 99, 99, 99, 627, 99, 99, 99, 118, 579, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99",
    /*  9401 */ "317, 99, 118, 118, 118, 118, 118, 597, 118, 118, 118, 118, 118, 118, 118, 118, 118, 0, 704, 99, 99",
    /*  9422 */ "99, 99, 99, 99, 99, 99, 11589, 283, 219, 118, 329, 118, 118, 118, 99, 99, 661, 99, 99, 99, 99, 118",
    /*  9444 */ "118, 118, 668, 118, 118, 118, 118, 118, 351, 118, 118, 353, 118, 118, 118, 118, 118, 118, 118, 0, 0",
    /*  9465 */ "369, 0, 0, 0, 0, 0, 0, 118, 118, 118, 675, 118, 118, 118, 118, 0, 0, 0, 99, 99, 99, 683, 99, 99, 99",
    /*  9490 */ "99, 479, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 314, 99, 99, 99, 99, 99, 728, 99, 99, 99, 99",
    /*  9515 */ "99, 99, 118, 118, 118, 738, 118, 118, 118, 0, 0, 0, 0, 0, 0, 0, 0, 574, 99, 99, 99, 99, 0, 118, 118",
    /*  9540 */ "118, 118, 118, 232, 118, 118, 118, 118, 118, 701, 118, 0, 99, 99, 99, 707, 99, 99, 99, 99, 118, 118",
    /*  9562 */ "118, 745, 99, 99, 99, 99, 99, 99, 99, 753, 99, 755, 118, 118, 118, 0, 0, 0, 570, 0, 0, 0, 0, 99, 99",
    /*  9587 */ "99, 99, 99, 99, 188, 99, 99, 99, 99, 99, 99, 99, 99, 11589, 283, 219, 118, 118, 330, 118, 118, 99, 99",
    /*  9610 */ "799, 800, 99, 99, 118, 118, 805, 806, 118, 118, 99, 99, 99, 99, 0, 118, 118, 118, 118, 118, 230, 118",
    /*  9632 */ "118, 118, 118, 118, 262, 118, 0, 0, 0, 0, 0, 0, 10308, 10308, 0, 99, 99, 118, 118, 99, 118, 99, 118",
    /*  9655 */ "827, 828, 99, 118, 0, 0, 0, 0, 0, 99, 99, 99, 99, 99, 656, 99, 99, 99, 99, 111, 111, 111, 130, 130",
    /*  9679 */ "130, 130, 130, 130, 130, 0, 0, 0, 0, 0, 0, 0, 0, 110592, 110592, 0, 0, 133120, 0, 0, 0, 99, 210, 213",
    /*  9703 */ "99, 0, 118, 118, 118, 225, 228, 118, 234, 118, 118, 118, 246, 249, 118, 118, 118, 257, 260, 118, 0, 0",
    /*  9725 */ "0, 0, 0, 0, 10308, 10308, 0, 0, 0, 0, 270, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 274, 0, 0, 0, 0, 99, 99",
    /*  9754 */ "99, 417, 11589, 383, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 0, 133120, 0, 0, 0, 0, 10308",
    /*  9775 */ "10308, 0, 99, 592, 118, 118, 118, 118, 118, 118, 118, 118, 118, 601, 118, 118, 118, 118, 442, 118",
    /*  9795 */ "118, 118, 118, 118, 118, 118, 118, 118, 0, 0, 0, 681, 99, 99, 99, 99, 99, 99, 99, 118, 118, 716, 118",
    /*  9818 */ "118, 118, 118, 118, 118, 118, 118, 0, 99, 99, 99, 99, 770, 99, 772, 726, 99, 99, 99, 730, 99, 99, 99",
    /*  9841 */ "99, 118, 736, 118, 118, 118, 740, 118, 118, 118, 0, 517, 0, 0, 0, 0, 0, 0, 0, 0, 99, 99, 99, 99, 180",
    /*  9866 */ "99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 408, 99, 99, 99, 99, 99, 99, 99, 118, 118, 99, 118, 825, 826",
    /*  9890 */ "99, 118, 99, 118, 0, 0, 0, 0, 0, 99, 99, 99, 654, 99, 99, 99, 99, 99, 99, 99, 310, 99, 99, 99, 99, 99",
    /*  9916 */ "99, 99, 99, 118, 118, 118, 118, 118, 118, 118, 551, 112, 112, 112, 131, 131, 131, 131, 131, 131, 131",
    /*  9937 */ "0, 0, 0, 0, 0, 0, 0, 0, 138240, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13388, 0, 0, 133120, 0, 0, 0, 151, 0, 0, 0",
    /*  9966 */ "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 67, 0, 10308, 0, 113, 113, 113, 132, 132, 132, 132, 132, 132, 132",
    /*  9992 */ "0, 0, 0, 0, 0, 0, 0, 61, 61, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 10308, 0, 0, 0, 99, 99, 174, 99",
    /* 10022 */ "99, 99, 99, 99, 99, 99, 99, 99, 203, 205, 99, 99, 214, 99, 0, 118, 118, 221, 118, 118, 118, 118, 118",
    /* 10045 */ "118, 118, 118, 264, 368, 0, 0, 0, 0, 0, 0, 0, 0, 0, 64, 0, 0, 0, 0, 10308, 0, 118, 250, 252, 118, 118",
    /* 10071 */ "261, 118, 0, 0, 0, 0, 0, 0, 10308, 10308, 0, 0, 0, 0, 377, 0, 0, 0, 0, 0, 283, 383, 99, 99, 99, 99, 0",
    /* 10098 */ "118, 118, 118, 118, 118, 118, 118, 118, 241, 118, 118, 99, 99, 99, 99, 11589, 383, 118, 118, 118, 420",
    /* 10119 */ "118, 118, 118, 118, 118, 118, 118, 25600, 133384, 0, 0, 0, 0, 10308, 10308, 0, 118, 118, 118, 429",
    /* 10139 */ "118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 451, 0, 0, 477, 99, 99, 99, 99, 99",
    /* 10161 */ "99, 482, 99, 99, 99, 99, 99, 99, 99, 99, 532, 99, 99, 99, 99, 99, 538, 99, 118, 118, 504, 118, 118",
    /* 10184 */ "118, 118, 118, 118, 509, 118, 118, 118, 118, 118, 118, 118, 134144, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 105",
    /* 10208 */ "124, 105, 105, 105, 105, 712, 99, 99, 714, 118, 118, 118, 118, 118, 118, 118, 722, 118, 118, 0, 99",
    /* 10229 */ "99, 99, 99, 544, 99, 99, 99, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 604",
    /* 10250 */ "118, 99, 99, 99, 99, 801, 99, 118, 118, 118, 118, 807, 118, 99, 99, 99, 99, 0, 118, 118, 118, 118",
    /* 10272 */ "118, 233, 118, 118, 118, 118, 118, 763, 118, 765, 0, 99, 99, 99, 99, 99, 99, 99, 99, 11589, 283, 219",
    /* 10294 */ "327, 118, 118, 118, 118, 114, 114, 114, 133, 133, 133, 133, 133, 133, 133, 0, 0, 0, 0, 0, 138, 0, 0",
    /* 10317 */ "157, 0, 0, 0, 0, 0, 0, 0, 0, 0, 157, 0, 0, 0, 0, 73, 0, 0, 73, 73, 80, 80, 80, 80, 80, 80, 80, 80, 80",
    /* 10346 */ "80, 103, 122, 103, 103, 103, 103, 99, 99, 292, 99, 99, 296, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99",
    /* 10369 */ "99, 312, 99, 99, 99, 99, 304, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 413, 118",
    /* 10393 */ "334, 118, 118, 338, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 346, 99, 99, 478, 99, 99, 99",
    /* 10414 */ "99, 99, 483, 99, 99, 99, 99, 99, 99, 487, 99, 489, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118",
    /* 10436 */ "118, 118, 118, 118, 359, 118, 118, 514, 118, 0, 0, 0, 519, 0, 0, 0, 0, 0, 0, 99, 99, 99, 99, 99, 99",
    /* 10461 */ "545, 99, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 603, 118, 118, 540, 99, 99, 99",
    /* 10482 */ "99, 99, 99, 99, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 500, 118, 118, 99, 99",
    /* 10503 */ "581, 99, 99, 99, 99, 99, 99, 99, 99, 588, 99, 99, 99, 99, 0, 118, 118, 118, 118, 227, 118, 118, 118",
    /* 10526 */ "118, 118, 118, 118, 703, 99, 99, 99, 99, 99, 99, 99, 99, 118, 118, 547, 548, 118, 118, 118, 118, 118",
    /* 10548 */ "606, 118, 118, 118, 118, 118, 0, 0, 0, 0, 0, 0, 99, 99, 99, 99, 99, 99, 584, 99, 99, 99, 99, 99, 99",
    /* 10573 */ "99, 99, 99, 99, 300, 99, 99, 99, 99, 99, 99, 99, 99, 662, 99, 99, 99, 118, 118, 118, 118, 118, 118",
    /* 10596 */ "118, 118, 118, 118, 0, 99, 99, 118, 118, 118, 118, 676, 118, 118, 118, 0, 0, 0, 99, 99, 99, 99, 99",
    /* 10619 */ "186, 99, 99, 99, 99, 99, 99, 99, 99, 624, 625, 99, 99, 99, 99, 99, 118, 773, 99, 99, 118, 118, 118",
    /* 10642 */ "118, 118, 118, 118, 782, 118, 118, 0, 99, 786, 0, 154, 0, 159, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0",
    /* 10668 */ "133120, 0, 0, 0, 0, 0, 99, 99, 175, 99, 99, 99, 99, 190, 195, 197, 99, 99, 99, 99, 0, 118, 118, 118",
    /* 10692 */ "224, 118, 118, 118, 118, 118, 118, 118, 0, 99, 99, 99, 99, 708, 99, 710, 99, 0, 0, 276, 277, 0, 0",
    /* 10715 */ "274, 0, 0, 0, 283, 172, 99, 99, 288, 99, 99, 99, 99, 582, 99, 99, 99, 99, 99, 587, 99, 99, 589, 99",
    /* 10739 */ "99, 99, 99, 99, 99, 689, 99, 99, 118, 118, 693, 118, 118, 118, 118, 505, 118, 118, 118, 118, 118, 510",
    /* 10761 */ "118, 118, 118, 118, 118, 443, 118, 118, 118, 118, 118, 118, 118, 118, 0, 0, 0, 99, 99, 99, 99, 684",
    /* 10783 */ "305, 99, 99, 307, 99, 99, 99, 99, 99, 99, 99, 99, 99, 315, 316, 99, 99, 99, 99, 620, 99, 99, 623, 99",
    /* 10807 */ "99, 99, 99, 628, 630, 99, 118, 118, 118, 118, 118, 494, 495, 118, 497, 118, 118, 118, 118, 118, 118",
    /* 10828 */ "118, 679, 0, 0, 0, 99, 99, 99, 99, 99, 99, 99, 99, 194, 99, 99, 99, 99, 99, 118, 118, 349, 118, 118",
    /* 10852 */ "118, 118, 118, 118, 118, 118, 118, 357, 358, 118, 118, 118, 0, 99, 99, 99, 99, 750, 99, 752, 99, 754",
    /* 10874 */ "99, 118, 118, 118, 118, 118, 118, 118, 496, 118, 118, 118, 118, 118, 118, 118, 0, 265, 0, 0, 0, 0",
    /* 10896 */ "10308, 10308, 0, 387, 99, 99, 99, 99, 99, 99, 99, 394, 99, 99, 99, 99, 99, 99, 400, 414, 99, 99, 99",
    /* 10919 */ "11589, 383, 118, 118, 118, 118, 421, 118, 118, 118, 118, 118, 556, 118, 118, 118, 118, 118, 118, 118",
    /* 10939 */ "118, 118, 118, 447, 448, 118, 118, 118, 0, 0, 118, 118, 428, 118, 118, 118, 118, 118, 118, 434, 118",
    /* 10960 */ "118, 118, 118, 118, 118, 118, 134144, 367, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 459, 0, 461, 461, 383, 99",
    /* 10984 */ "513, 118, 118, 0, 0, 518, 0, 0, 0, 0, 0, 0, 0, 524, 99, 99, 99, 99, 99, 295, 99, 99, 298, 99, 99, 99",
    /* 11010 */ "99, 99, 99, 99, 99, 11589, 283, 219, 118, 118, 118, 331, 118, 118, 118, 118, 554, 118, 118, 118, 118",
    /* 11031 */ "118, 560, 118, 118, 118, 118, 118, 118, 263, 0, 0, 0, 0, 0, 0, 10308, 10308, 0, 118, 118, 118, 568, 0",
    /* 11054 */ "0, 0, 0, 0, 0, 0, 99, 99, 99, 577, 99, 99, 99, 99, 663, 99, 99, 118, 118, 118, 118, 118, 118, 118",
    /* 11078 */ "118, 118, 118, 118, 118, 118, 118, 502, 99, 580, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99",
    /* 11101 */ "99, 590, 99, 99, 118, 118, 118, 595, 118, 118, 598, 118, 118, 118, 118, 118, 118, 118, 118, 445, 118",
    /* 11122 */ "118, 118, 118, 118, 118, 0, 0, 99, 99, 619, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 631, 118, 118",
    /* 11146 */ "118, 0, 746, 99, 99, 99, 99, 99, 99, 99, 99, 99, 756, 118, 118, 118, 0, 569, 0, 0, 0, 0, 0, 0, 99, 99",
    /* 11172 */ "99, 99, 99, 99, 99, 99, 191, 99, 99, 99, 99, 99, 633, 118, 118, 118, 636, 118, 118, 118, 118, 118",
    /* 11194 */ "118, 118, 118, 118, 118, 118, 511, 118, 118, 118, 118, 648, 0, 650, 0, 0, 0, 99, 99, 99, 99, 99, 99",
    /* 11217 */ "99, 99, 99, 658, 672, 118, 118, 118, 118, 677, 118, 118, 0, 0, 0, 99, 99, 682, 99, 99, 99, 99, 99",
    /* 11240 */ "308, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 302, 99, 99, 99, 99, 118, 118, 118, 118, 700, 118",
    /* 11263 */ "118, 0, 99, 99, 99, 99, 99, 99, 99, 99, 546, 118, 118, 118, 118, 118, 118, 118, 99, 99, 775, 118, 118",
    /* 11286 */ "118, 118, 118, 118, 118, 118, 118, 784, 0, 99, 99, 99, 99, 99, 322, 99, 99, 11589, 283, 219, 118, 118",
    /* 11308 */ "118, 118, 118, 118, 352, 118, 118, 118, 118, 118, 118, 118, 118, 118, 0, 99, 99, 706, 99, 99, 99, 99",
    /* 11330 */ "99, 99, 99, 99, 99, 789, 790, 99, 118, 118, 118, 118, 118, 118, 795, 796, 118, 118, 118, 118, 608",
    /* 11351 */ "118, 118, 0, 0, 0, 0, 0, 0, 99, 99, 99, 99, 179, 99, 99, 189, 99, 196, 99, 201, 99, 99, 99, 821, 118",
    /* 11376 */ "822, 99, 118, 99, 118, 99, 118, 99, 118, 0, 0, 0, 0, 0, 99, 653, 99, 99, 99, 99, 99, 99, 99, 99, 99",
    /* 11401 */ "484, 99, 99, 99, 99, 99, 99, 116, 116, 116, 135, 135, 135, 135, 135, 135, 135, 0, 0, 0, 0, 0, 0, 0",
    /* 11425 */ "272, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 273, 0, 0, 0, 0, 0, 0, 0, 0, 376, 0, 0, 0, 0, 0, 0, 283, 383, 99",
    /* 11455 */ "99, 99, 99, 99, 99, 118, 118, 118, 118, 118, 118, 99, 809, 99, 99, 99, 99, 99, 99, 11589, 383, 118",
    /* 11477 */ "118, 118, 118, 118, 118, 118, 118, 118, 425, 99, 118, 593, 118, 118, 118, 118, 118, 118, 118, 118",
    /* 11497 */ "118, 118, 118, 118, 118, 118, 368, 368, 99, 774, 99, 118, 118, 118, 118, 118, 118, 118, 118, 783, 118",
    /* 11518 */ "0, 99, 99, 99, 99, 99, 391, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 473, 99, 99, 99, 99, 812, 99",
    /* 11543 */ "118, 118, 118, 118, 816, 118, 99, 99, 99, 99, 118, 118, 118, 118, 779, 118, 781, 118, 118, 118, 0",
    /* 11564 */ "785, 99, 99, 99, 99, 293, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 475, 99, 99, 118, 118",
    /* 11588 */ "335, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 512, 118, 374, 0, 0, 0, 0",
    /* 11610 */ "0, 0, 0, 0, 0, 283, 383, 99, 99, 99, 99, 99, 99, 297, 99, 99, 99, 99, 99, 99, 99, 99, 99, 118, 118",
    /* 11635 */ "118, 694, 118, 118, 118, 99, 211, 99, 99, 0, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118, 118",
    /* 11656 */ "507, 508, 118, 118, 118, 118, 118, 118, 118, 0, 0, 611, 0, 0, 0, 99, 99, 99, 118, 567, 118, 0, 0, 0",
    /* 11680 */ "0, 0, 0, 0, 0, 99, 99, 99, 99, 99, 99, 99, 791, 118, 118, 118, 118, 118, 118, 118, 118, 766, 99, 99",
    /* 11704 */ "99, 99, 99, 99, 99, 152, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 460, 0, 0, 383, 99, 99, 99, 99",
    /* 11733 */ "321, 99, 99, 99, 99, 11589, 283, 219, 118, 118, 118, 118, 118, 555, 118, 118, 118, 118, 118, 118, 118",
    /* 11754 */ "118, 118, 118, 118, 558, 118, 118, 118, 562, 118, 118, 118, 118, 118, 118, 363, 118, 118, 118, 118, 0",
    /* 11775 */ "0, 0, 0, 0, 0, 0, 0, 0, 0, 99, 99, 99, 99, 99, 215, 99, 0, 118, 118, 118, 118, 118, 118, 118, 118",
    /* 11800 */ "118, 118, 118, 638, 639, 118, 118, 118, 643, 118, 118, 118, 463, 99, 99, 99, 99, 99, 99, 99, 99, 471",
    /* 11822 */ "472, 99, 99, 99, 99, 99, 99, 118, 118, 118, 118, 118, 118, 99, 99, 810, 811, 99, 118, 490, 118, 118",
    /* 11844 */ "118, 118, 118, 118, 118, 118, 498, 499, 118, 118, 118, 0, 0, 0, 0, 0, 0, 0, 573, 99, 99, 576, 99, 99",
    /* 11868 */ "0, 0, 0, 107520, 0, 0, 0, 0, 0, 0, 0, 0, 107520, 0, 107520, 0, 0, 0, 0, 454, 0, 0, 0, 0, 0, 0, 0, 0",
    /* 11896 */ "0, 383, 99, 99, 99, 99, 529, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 99, 537, 99, 99, 0, 0, 0",
    /* 11922 */ "0, 107520, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 109568, 0, 0, 0, 0, 0, 0, 139264, 139436, 139264, 139264",
    /* 11946 */ "139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 0, 139264, 139264",
    /* 11959 */ "189440, 139264, 139264, 139264, 139264, 217088, 220160, 0, 0, 234496, 0, 0, 0, 0, 0, 0, 0, 139264",
    /* 11977 */ "139264, 139264, 139264, 0, 11264, 139264, 178176, 139264, 139264, 139264, 139264, 139264, 139264",
    /* 11990 */ "188416, 139264, 139264, 139264, 139264, 139264, 234496, 139264, 139264, 139264, 0, 0, 0, 139264",
    /* 12004 */ "139264, 139264, 139264, 139264, 139264, 139264, 0, 133120, 180224, 204800, 219136, 0, 10308, 10308",
    /* 12018 */ "71, 217088, 220160, 0, 0, 234496, 0, 0, 0, 0, 0, 0, 284, 139264, 139264, 139264, 139264, 0, 139264",
    /* 12037 */ "139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264, 139264",
    /* 12049 */ "139264, 139264, 139264, 234496, 139264, 139264, 139264, 0, 0, 326, 139264, 139264, 139264, 139264",
    /* 12063 */ "139264, 139264, 139264, 0, 133120, 180224, 204800, 219136, 0, 10382, 10382, 0"
  ).flatMap(_.split(", ").map(_.toInt))

  private final val EXPECTED = Array(
    /*    0 */ "414, 418, 422, 426, 430, 434, 438, 442, 446, 450, 471, 471, 456, 828, 743, 480, 462, 466, 470, 471",
    /*   20 */ "471, 741, 828, 828, 828, 828, 828, 478, 480, 480, 480, 480, 481, 964, 471, 741, 828, 828, 828, 828",
    /*   40 */ "458, 480, 480, 480, 480, 832, 471, 714, 828, 828, 828, 828, 501, 480, 480, 480, 485, 825, 828, 828",
    /*   60 */ "828, 480, 480, 480, 485, 826, 828, 828, 501, 480, 490, 825, 828, 498, 480, 480, 486, 828, 498, 480",
    /*   80 */ "480, 826, 828, 501, 490, 828, 500, 502, 828, 501, 492, 829, 480, 827, 500, 491, 498, 480, 829, 496",
    /*  100 */ "831, 830, 506, 508, 976, 579, 515, 519, 522, 525, 529, 977, 778, 538, 471, 550, 557, 471, 856, 471",
    /*  120 */ "976, 916, 565, 471, 541, 571, 575, 471, 471, 471, 692, 602, 583, 471, 471, 471, 643, 592, 541, 601",
    /*  140 */ "885, 471, 471, 471, 600, 884, 585, 471, 471, 866, 713, 606, 610, 578, 471, 553, 609, 577, 471, 777",
    /*  160 */ "776, 881, 614, 471, 945, 623, 578, 777, 878, 632, 578, 560, 633, 471, 779, 631, 642, 637, 641, 471",
    /*  180 */ "647, 578, 561, 642, 843, 651, 844, 652, 951, 843, 656, 671, 670, 657, 664, 663, 670, 669, 668, 675",
    /*  200 */ "683, 544, 545, 546, 594, 596, 471, 471, 690, 658, 696, 697, 701, 702, 706, 691, 712, 718, 722, 471",
    /*  220 */ "729, 733, 725, 738, 690, 712, 747, 751, 471, 471, 686, 755, 759, 578, 471, 471, 763, 767, 771, 471",
    /*  240 */ "775, 783, 471, 471, 474, 792, 797, 471, 471, 473, 791, 796, 471, 801, 785, 471, 708, 806, 811, 471",
    /*  260 */ "707, 805, 810, 659, 785, 471, 815, 819, 471, 957, 836, 659, 842, 957, 836, 471, 958, 837, 841, 627",
    /*  280 */ "849, 472, 848, 853, 957, 871, 627, 860, 864, 870, 734, 875, 532, 626, 893, 891, 890, 898, 534, 533",
    /*  300 */ "890, 889, 897, 892, 533, 902, 903, 904, 908, 912, 914, 471, 920, 939, 924, 928, 931, 933, 965, 471",
    /*  320 */ "471, 787, 937, 471, 567, 471, 471, 943, 964, 471, 471, 949, 471, 471, 471, 471, 473, 955, 471, 471",
    /*  340 */ "471, 471, 452, 619, 471, 823, 471, 471, 471, 471, 511, 471, 471, 471, 471, 510, 962, 821, 471, 471",
    /*  360 */ "471, 472, 970, 471, 471, 471, 969, 821, 471, 471, 471, 974, 471, 471, 617, 471, 471, 471, 678, 471",
    /*  380 */ "471, 679, 471, 471, 588, 471, 587, 471, 471, 588, 471, 981, 471, 587, 471, 588, 586, 471, 982, 471",
    /*  400 */ "471, 471, 471, 471, 471, 471, 471, 471, 471, 471, 471, 471, 471, 471, 986, 988, 990, 992, 994, 995",
    /*  420 */ "995, 997, 999, 1001, 1003, 1005, 1007, 1008, 1008, 1012, 1009, 1015, 1010, 1014, 1017, 1019, 1021",
    /*  437 */ "1023, 1025, 1027, 1029, 1032, 1030, 1115, 1034, 1131, 1038, 1253, 1074, 1063, 1047, 1173, 1038, 1038",
    /*  454 */ "1040, 1050, 1076, 1185, 1184, 1184, 1087, 1089, 1089, 1089, 1089, 1090, 1084, 1038, 1253, 1134, 1051",
    /*  471 */ "1038, 1038, 1038, 1038, 1039, 1041, 1070, 1180, 1088, 1089, 1089, 1089, 1089, 1082, 1089, 1181, 1038",
    /*  488 */ "1038, 1184, 1089, 1089, 1089, 1181, 1184, 1184, 1089, 1089, 1184, 1184, 1184, 1283, 1089, 1089, 1089",
    /*  505 */ "1085, 1184, 1089, 1283, 1283, 1038, 1038, 1040, 1211, 1183, 1218, 1113, 1094, 1095, 1097, 1099, 1101",
    /*  522 */ "1102, 1103, 1102, 1105, 1105, 1105, 1106, 1107, 1108, 1110, 1038, 1038, 1226, 1250, 1035, 1038, 1117",
    /*  539 */ "1247, 1119, 1038, 1038, 1227, 1038, 1038, 1240, 1038, 1038, 1240, 1038, 1260, 1264, 1038, 1038, 1245",
    /*  556 */ "1194, 1125, 1127, 1037, 1038, 1038, 1245, 1259, 1154, 1199, 1130, 1038, 1038, 1048, 1282, 1193, 1195",
    /*  573 */ "1229, 1198, 1242, 1143, 1035, 1037, 1038, 1038, 1038, 1092, 1243, 1224, 1036, 1038, 1038, 1038, 1047",
    /*  590 */ "1038, 1038, 1247, 1234, 1038, 1038, 1059, 1038, 1059, 1059, 1038, 1246, 1194, 1228, 1197, 1248, 1227",
    /*  607 */ "1245, 1194, 1228, 1197, 1249, 1078, 1035, 1249, 1156, 1036, 1038, 1044, 1046, 1066, 1060, 1038, 1229",
    /*  624 */ "1154, 1268, 1035, 1038, 1038, 1039, 1213, 1147, 1259, 1197, 1249, 1269, 1037, 1038, 1245, 1259, 1197",
    /*  641 */ "1249, 1079, 1038, 1038, 1038, 1113, 1146, 1150, 1153, 1155, 1158, 1155, 1037, 1038, 1038, 1154, 1244",
    /*  658 */ "1038, 1038, 1038, 1131, 1236, 1038, 1240, 1155, 1038, 1038, 1241, 1244, 1038, 1038, 1239, 1241, 1244",
    /*  675 */ "1038, 1240, 1244, 1038, 1044, 1047, 1038, 1038, 1038, 1239, 1160, 1038, 1049, 1062, 1046, 1038, 1162",
    /*  692 */ "1038, 1038, 1038, 1192, 1164, 1166, 1166, 1166, 1166, 1166, 1168, 1168, 1168, 1168, 1168, 1038, 1038",
    /*  709 */ "1038, 1210, 1070, 1038, 1131, 1038, 1038, 1038, 1065, 1072, 1170, 1151, 1160, 1252, 1038, 1172, 1038",
    /*  726 */ "1052, 1054, 1056, 1053, 1055, 1057, 1141, 1237, 1038, 1038, 1038, 1222, 1058, 1142, 1238, 1038, 1069",
    /*  743 */ "1184, 1184, 1180, 1081, 1062, 1133, 1176, 1194, 1140, 1143, 1038, 1191, 1179, 1255, 1257, 1195, 1229",
    /*  760 */ "1231, 1208, 1187, 1048, 1042, 1064, 1190, 1202, 1256, 1258, 1228, 1230, 1207, 1143, 1188, 1038, 1136",
    /*  777 */ "1038, 1038, 1038, 1112, 1038, 1038, 1133, 1204, 1252, 1071, 1038, 1038, 1067, 1086, 1070, 1254, 1174",
    /*  794 */ "1195, 1206, 1206, 1233, 1143, 1188, 1038, 1038, 1131, 1132, 1236, 1070, 1254, 1196, 1232, 1078, 1078",
    /*  811 */ "1187, 1037, 1038, 1038, 1039, 1213, 1263, 1232, 1078, 1187, 1038, 1038, 1085, 1132, 1038, 1038, 1085",
    /*  828 */ "1184, 1184, 1184, 1184, 1089, 1089, 1089, 1061, 1215, 1233, 1143, 1217, 1038, 1235, 1144, 1038, 1038",
    /*  845 */ "1038, 1239, 1158, 1213, 1263, 1233, 1187, 1038, 1136, 1223, 1071, 1038, 1124, 1126, 1128, 1263, 1233",
    /*  862 */ "1224, 1131, 1235, 1252, 1038, 1038, 1112, 1076, 1222, 1220, 1251, 1035, 1038, 1220, 1251, 1035, 1136",
    /*  879 */ "1038, 1038, 1147, 1194, 1139, 1197, 1248, 1250, 1156, 1036, 1200, 1131, 1038, 1039, 1267, 1200, 1131",
    /*  896 */ "1131, 1267, 1200, 1137, 1038, 1038, 1038, 1271, 1200, 1038, 1271, 1200, 1039, 1250, 1039, 1250, 1076",
    /*  913 */ "1076, 1077, 1077, 1038, 1038, 1112, 1173, 1261, 1038, 1038, 1114, 1273, 1273, 1122, 1122, 1275, 1122",
    /*  930 */ "1122, 1277, 1277, 1277, 1277, 1279, 1038, 1265, 1038, 1038, 1038, 1120, 1121, 1281, 1066, 1038, 1038",
    /*  947 */ "1148, 1259, 1068, 1132, 1038, 1038, 1177, 1155, 1064, 1183, 1038, 1038, 1210, 1262, 1215, 1183, 1038",
    /*  964 */ "1043, 1038, 1038, 1038, 1043, 1039, 1041, 1046, 1066, 1038, 1045, 1183, 1038, 1038, 1218, 1038, 1038",
    /*  981 */ "1038, 1182, 1038, 1038, 1038, 131330, 147712, 164096, 196864, 393472, 655616, 2228480, 537002240",
    /*  994 */ "1073873152, 131328, 131328, 268567040, 213248, 426240, 2490624, 393472, 131328, 393552, 2490624",
    /* 1005 */ "393472, 721218, 3080514, -2004997888, -2004997888, -1904330496, -1904248576, -2004932352, -2004997888",
    /* 1014 */ "-1367394048, -1904264960, -1367459584, -1904215808, -1367377664, 131488, 132512, 197024, 655776",
    /* 1023 */ "229792, 459168, 721312, 917920, 983456, 25396670, 126059966, 131302846, -1980252738, 131564990",
    /* 1033 */ "131302846, 2, 536870912, 1073741824, -2147483648, 0, 0, 1, 2, 4, 8, 0, 2, 32, 64, 0, 3, 4, 16, 0, 7",
    /* 1054 */ "24, 3936, 28672, 98304, 1966080, 4194304, 0, 8, 8, 16, 16, 32, 128, 0, 16, 128, 32, 256, 0, 24, 131584",
    /* 1075 */ "268435968, 0, 67108864, 67108864, 134217728, -2147483648, 1184, 1152, 12, 14, 0, 128, 1024, 1056, 1152",
    /* 1090 */ "1152, 16777228, 128, 32768, 0, 1417684087, 1417684087, 1417684215, 1417946231, 1417684215, 1417946359",
    /* 1101 */ "1418077311, 1418077823, 1418077823, 1418077951, -34816, -34816, -33920, -33920, -32896, -33920, -32769",
    /* 1112 */ "0, 33554432, 0, 256, 131072, 3, 96, 1409286144, 0, 3728, 3760, 3760, 0, 96256, 1966080, 31457280",
    /* 1128 */ "2113929216, -2147483648, 268435456, 1073741824, 0, 32, 512, 16, 0, 1073741824, 1073741824, 262144",
    /* 1140 */ "524288, 4194304, 125829120, 134217728, 268435456, 256, 0, 262144, 6144, 16384, 6144, 65536, 524288",
    /* 1153 */ "262144, 6291456, 16777216, 134217728, 536870912, 65536, 6291456, 4194304, 134217728, 128, 2097152, 0",
    /* 1165 */ "407459384, 407459640, 407459640, -2097281, -2097281, 1568, 20480, 1824, 0, 8192, 32768, 1024, 4096",
    /* 1178 */ "6291456, 768, 1024, 1152, 0, 64, 128, 128, 160, 268435456, 1610612736, -2147483648, 64, 768, 0, 14336",
    /* 1194 */ "16384, 65536, 393216, 1048576, 6291456, 8388608, 67108864, 536870912, 1024, 2048, 65536, 134217728",
    /* 1206 */ "1048576, 8388608, 117440512, 134217728, 1, 4, 32, 4, 256, 393216, 8388608, 1610612736, 0, 32768",
    /* 1220 */ "393216, 50331648, 1, 512, 268435456, 536870912, 1, 262144, 393216, 524288, 1048576, 4194304, 8388608",
    /* 1233 */ "50331648, 67108864, 1073741824, 512, 134217728, -268435456, 0, 4096, 4194304, 16777216, 100663296",
    /* 1244 */ "134217728, 0, 6144, 8192, 8388608, 16777216, 33554432, 67108864, 268435456, 0, 512, 2048, 12288, 16384",
    /* 1258 */ "32768, 65536, 262144, 8, 256, 512, 393216, 0, 48, 262144, 33554432, 134217728, 1073741824, 1, 33554432",
    /* 1273 */ "3984, 3760, 4016, 3760, 247, 247, 2295, 3831, 3, 116, 128, 1152"
  ).flatMap(_.split(", ").map(_.toInt))

  private final val TOKEN = Array(
    "(0)",
    "IntegerLiteral",
    "DecimalLiteral",
    "DoubleLiteral",
    "StringLiteral",
    "URIQualifiedName",
    "NCName",
    "QName",
    "S",
    "CommentContents",
    "Wildcard",
    "EOF",
    "'!'",
    "'!='",
    "'#'",
    "'$'",
    "'('",
    "'(:'",
    "')'",
    "'*'",
    "'+'",
    "','",
    "'-'",
    "'.'",
    "'..'",
    "'/'",
    "'//'",
    "':'",
    "':)'",
    "'::'",
    "':='",
    "'<'",
    "'<<'",
    "'<='",
    "'='",
    "'=>'",
    "'>'",
    "'>='",
    "'>>'",
    "'?'",
    "'@'",
    "'['",
    "']'",
    "'ancestor'",
    "'ancestor-or-self'",
    "'and'",
    "'array'",
    "'as'",
    "'attribute'",
    "'cast'",
    "'castable'",
    "'child'",
    "'comment'",
    "'descendant'",
    "'descendant-or-self'",
    "'div'",
    "'document-node'",
    "'element'",
    "'else'",
    "'empty-sequence'",
    "'eq'",
    "'every'",
    "'except'",
    "'following'",
    "'following-sibling'",
    "'for'",
    "'function'",
    "'ge'",
    "'gt'",
    "'idiv'",
    "'if'",
    "'in'",
    "'instance'",
    "'intersect'",
    "'is'",
    "'item'",
    "'le'",
    "'let'",
    "'lt'",
    "'map'",
    "'mod'",
    "'namespace'",
    "'namespace-node'",
    "'ne'",
    "'node'",
    "'of'",
    "'or'",
    "'parent'",
    "'preceding'",
    "'preceding-sibling'",
    "'processing-instruction'",
    "'return'",
    "'satisfies'",
    "'schema-attribute'",
    "'schema-element'",
    "'self'",
    "'some'",
    "'switch'",
    "'text'",
    "'then'",
    "'to'",
    "'treat'",
    "'typeswitch'",
    "'union'",
    "'{'",
    "'|'",
    "'||'",
    "'}'"
  )
}

// End
