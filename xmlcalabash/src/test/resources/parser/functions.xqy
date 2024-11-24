xquery version "1.0";

module namespace f="https://xmlcalabash.com/ns/functions/xqy";

declare default function namespace "http://www.w3.org/2005/xpath-functions";

declare function f:hello(
) as xs:string
{
  "Hello, world."
};
