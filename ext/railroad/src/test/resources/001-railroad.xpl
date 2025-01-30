<p:declare-step xmlns:p="http://www.w3.org/ns/xproc"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                name="main" version="3.0">
  <p:import href="https://xmlcalabash.com/ext/library/railroad.xpl"/>
  <p:output port="result" sequence="true"/>

  <cx:railroad parameters="map{'color': '#FFAAAA', 'color-offset': 50, 'stroke-width': 5}">
    <p:with-input>
      <p:inline content-type="text/plain">
script   ::= versionDecl s page s statement ( RS statement )* s
versionDecl
         ::= 'script version ' '0.2' s '.'
page     ::= 'page' RS string s '.'
statement
         ::= simpleStatement s '.'
           | block
           | perform
perform  ::= compoundStatement ( RS 'then' RS compoundStatement )+ s '.'
compoundStatement
         ::= send
           | pause
           | key
           | scroll
           | move
           | 'release'
           | drag
           | click
simpleStatement
         ::= compoundStatement
           | message
           | output
           | set
           | window
           | cookie
           | navigate
           | 'refresh'
           | 'reset'
           | 'close'
           | find
           | call
           | waitReady
block    ::= ifBlock
           | whileBlock
           | untilBlock
           | subroutine
find     ::= 'find' RS ( 'all' RS )? varname RS ( 'by' RS )? findType s '=' s string ( RS wait )? ( RS pause )?
findType ::= 'name'
           | 'selector'
           | 'id'
           | 'link-text'
           | 'partial-link-text'
           | 'tag'
           | 'class'
           | 'xpath'
set      ::= 'set' RS varname RS ( 'to' RS )? ( 'window' RS setWindowParam | 'page' RS setPageParam | 'cookie' RS ( string | name ) | ( 'string' | 'xpath' ) RS string | 'element' RS varname RS property )
setWindowParam
         ::= 'width'
           | 'height'
           | 'x'
           | 'y'
setPageParam
         ::= 'url'
           | 'title'
property ::= name
duration ::= number
           | 'P' ( digit+ 'D' | ( digit+ 'D' )? 'T' ( digit+ 'H' )? ( digit+ 'M' )? ( number 'S' )? )
integer  ::= ( '+' | '-' )? digit+
number   ::= digit+ ( '.' digit+ )?
digit    ::= [0-9]
send     ::= 'send' RS ( string ( RS ( 'to' RS )? varname )? | ( '¶' [^¶]* '¶' | '⁋' [^⁋]* '⁋' | '§' [^§]* '§' | ff [^#xc]* ff ) RS ( 'to' RS )? varname )
click    ::= ( clickType | clickHold ) ( RS varname )?
clickType
         ::= 'click'
           | 'doubleclick'
clickHold
         ::= 'click' RS ( 'and' RS )? 'hold'
pause    ::= 'pause' RS duration
wait     ::= 'wait' RS duration
waitReady
         ::= 'wait' RS ( 'until' RS )? 'ready'
ifBlock  ::= 'if' RS string RS 'then' ( RS statement )+ s 'endif'
whileBlock
         ::= 'while' RS string RS 'do' ( RS statement )+ s 'done'
untilBlock
         ::= 'until' RS string RS 'do' ( RS statement )+ s 'done'
message  ::= 'message' RS string
output   ::= 'output' RS ( ( ( 'xpath' RS )? string | varname | '¶' [^¶]* '¶' | '⁋' [^⁋]* '⁋' | '§' [^§]* '§' | ff [^#xc]* ff ) RS )? ( 'to' RS )? 'result'
window   ::= 'window' RS ( feature | ( 'size' RS integer s 'x' | 'position' RS integer s ',' ) s integer )
feature  ::= 'minimize'
           | 'maximize'
           | 'fullscreen'
cookie   ::= 'cookie' RS ( string | name ) s '=' s string ( RS 'path' s '=' s string )? ( RS 'duration' s '=' s duration )?
scroll   ::= 'scroll' RS ( ( 'to' RS )? varname | ( 'from' RS varname RS )? ( 'by' RS )? integer-x s ',' s integer-y )
move     ::= 'move' RS ( 'to' RS )? varname
drag     ::= 'drag' RS ( 'and' RS 'drop' RS )? varname RS ( 'to' RS )? varname
navigate ::= 'navigate' RS ( direction | 'to' RS string )
direction
         ::= 'forward'
           | 'back'
           | 'backwards'
key      ::= 'key' RS keydir RS ( keyname | string )
keydir   ::= 'up'
           | 'down'
keyname  ::= 'ADD'
           | 'ALT'
           | 'ARROW_DOWN'
           | 'ARROW_LEFT'
           | 'ARROW_RIGHT'
           | 'ARROW_UP'
           | 'BACK_SPACE'
           | 'CANCEL'
           | 'CLEAR'
           | 'COMMAND'
           | 'CONTROL'
           | 'DECIMAL'
           | 'DELETE'
           | 'DIVIDE'
           | 'DOWN'
           | 'END'
           | 'ENTER'
           | 'EQUALS'
           | 'ESCAPE'
           | 'F1'
           | 'F2'
           | 'F3'
           | 'F4'
           | 'F5'
           | 'F6'
           | 'F7'
           | 'F8'
           | 'F9'
           | 'F10'
           | 'F11'
           | 'F12'
           | 'HELP'
           | 'HOME'
           | 'INSERT'
           | 'LEFT'
           | 'LEFT_ALT'
           | 'LEFT_CONTROL'
           | 'LEFT_SHIFT'
           | 'META'
           | 'MULTIPLY'
           | 'NULL'
           | 'NUMPAD0'
           | 'NUMPAD1'
           | 'NUMPAD2'
           | 'NUMPAD3'
           | 'NUMPAD4'
           | 'NUMPAD5'
           | 'NUMPAD6'
           | 'NUMPAD7'
           | 'NUMPAD8'
           | 'NUMPAD9'
           | 'PAGE_DOWN'
           | 'PAGE_UP'
           | 'PAUSE'
           | 'RETURN'
           | 'RIGHT'
           | 'SEPARATOR'
           | 'SHIFT'
           | 'SPACE'
           | 'SUBTRACT'
           | 'TAB'
           | 'UP'
subroutine
         ::= ( 'sub' | 'subroutine' ) RS name #x20* lf s ( statement ( RS statement )* )? s 'end'
call     ::= ( 'call' | 'gosub' ) RS name
name     ::= namestart namefollower*
namestart
         ::= [_A-Za-z#x00AA#x00B5#x00BA#x00C0-#x00D6#x00D8-#x00F6#x00F8-#x02C1#x02C6-#x02D1#x02E0-#x02E4#x02EC#x02EE#x0370-#x0374#x0376-#x0377#x037A-#x037D#x037F#x0386#x0388-#x038A#x038C#x038E-#x03A1#x03A3-#x03F5#x03F7-#x0481#x048A-#x052F#x0531-#x0556#x0559#x0561-#x0587#x05D0-#x05EA#x05F0-#x05F2#x0620-#x064A#x066E-#x066F#x0671-#x06D3#x06D5#x06E5-#x06E6#x06EE-#x06EF#x06FA-#x06FC#x06FF#x0710#x0712-#x072F#x074D-#x07A5#x07B1#x07CA-#x07EA#x07F4-#x07F5#x07FA#x0800-#x0815#x081A#x0824#x0828#x0840-#x0858#x0860-#x086A#x08A0-#x08B4#x08B6-#x08BD#x0904-#x0939#x093D#x0950#x0958-#x0961#x0971-#x0980#x0985-#x098C#x098F-#x0990#x0993-#x09A8#x09AA-#x09B0#x09B2#x09B6-#x09B9#x09BD#x09CE#x09DC-#x09DD#x09DF-#x09E1#x09F0-#x09F1#x09FC#x0A05-#x0A0A#x0A0F-#x0A10#x0A13-#x0A28#x0A2A-#x0A30#x0A32-#x0A33#x0A35-#x0A36#x0A38-#x0A39#x0A59-#x0A5C#x0A5E#x0A72-#x0A74#x0A85-#x0A8D#x0A8F-#x0A91#x0A93-#x0AA8#x0AAA-#x0AB0#x0AB2-#x0AB3#x0AB5-#x0AB9#x0ABD#x0AD0#x0AE0-#x0AE1#x0AF9#x0B05-#x0B0C#x0B0F-#x0B10#x0B13-#x0B28#x0B2A-#x0B30#x0B32-#x0B33#x0B35-#x0B39#x0B3D#x0B5C-#x0B5D#x0B5F-#x0B61#x0B71#x0B83#x0B85-#x0B8A#x0B8E-#x0B90#x0B92-#x0B95#x0B99-#x0B9A#x0B9C#x0B9E-#x0B9F#x0BA3-#x0BA4#x0BA8-#x0BAA#x0BAE-#x0BB9#x0BD0#x0C05-#x0C0C#x0C0E-#x0C10#x0C12-#x0C28#x0C2A-#x0C39#x0C3D#x0C58-#x0C5A#x0C60-#x0C61#x0C80#x0C85-#x0C8C#x0C8E-#x0C90#x0C92-#x0CA8#x0CAA-#x0CB3#x0CB5-#x0CB9#x0CBD#x0CDE#x0CE0-#x0CE1#x0CF1-#x0CF2#x0D05-#x0D0C#x0D0E-#x0D10#x0D12-#x0D3A#x0D3D#x0D4E#x0D54-#x0D56#x0D5F-#x0D61#x0D7A-#x0D7F#x0D85-#x0D96#x0D9A-#x0DB1#x0DB3-#x0DBB#x0DBD#x0DC0-#x0DC6#x0E01-#x0E30#x0E32-#x0E33#x0E40-#x0E46#x0E81-#x0E82#x0E84#x0E87-#x0E88#x0E8A#x0E8D#x0E94-#x0E97#x0E99-#x0E9F#x0EA1-#x0EA3#x0EA5#x0EA7#x0EAA-#x0EAB#x0EAD-#x0EB0#x0EB2-#x0EB3#x0EBD#x0EC0-#x0EC4#x0EC6#x0EDC-#x0EDF#x0F00#x0F40-#x0F47#x0F49-#x0F6C#x0F88-#x0F8C#x1000-#x102A#x103F#x1050-#x1055#x105A-#x105D#x1061#x1065-#x1066#x106E-#x1070#x1075-#x1081#x108E#x10A0-#x10C5#x10C7#x10CD#x10D0-#x10FA#x10FC-#x1248#x124A-#x124D#x1250-#x1256#x1258#x125A-#x125D#x1260-#x1288#x128A-#x128D#x1290-#x12B0#x12B2-#x12B5#x12B8-#x12BE#x12C0#x12C2-#x12C5#x12C8-#x12D6#x12D8-#x1310#x1312-#x1315#x1318-#x135A#x1380-#x138F#x13A0-#x13F5#x13F8-#x13FD#x1401-#x166C#x166F-#x167F#x1681-#x169A#x16A0-#x16EA#x16F1-#x16F8#x1700-#x170C#x170E-#x1711#x1720-#x1731#x1740-#x1751#x1760-#x176C#x176E-#x1770#x1780-#x17B3#x17D7#x17DC#x1820-#x1877#x1880-#x1884#x1887-#x18A8#x18AA#x18B0-#x18F5#x1900-#x191E#x1950-#x196D#x1970-#x1974#x1980-#x19AB#x19B0-#x19C9#x1A00-#x1A16#x1A20-#x1A54#x1AA7#x1B05-#x1B33#x1B45-#x1B4B#x1B83-#x1BA0#x1BAE-#x1BAF#x1BBA-#x1BE5#x1C00-#x1C23#x1C4D-#x1C4F#x1C5A-#x1C7D#x1C80-#x1C88#x1CE9-#x1CEC#x1CEE-#x1CF1#x1CF5-#x1CF6#x1D00-#x1DBF#x1E00-#x1F15#x1F18-#x1F1D#x1F20-#x1F45#x1F48-#x1F4D#x1F50-#x1F57#x1F59#x1F5B#x1F5D#x1F5F-#x1F7D#x1F80-#x1FB4#x1FB6-#x1FBC#x1FBE#x1FC2-#x1FC4#x1FC6-#x1FCC#x1FD0-#x1FD3#x1FD6-#x1FDB#x1FE0-#x1FEC#x1FF2-#x1FF4#x1FF6-#x1FFC#x2071#x207F#x2090-#x209C#x2102#x2107#x210A-#x2113#x2115#x2119-#x211D#x2124#x2126#x2128#x212A-#x212D#x212F-#x2139#x213C-#x213F#x2145-#x2149#x214E#x2183-#x2184#x2C00-#x2C2E#x2C30-#x2C5E#x2C60-#x2CE4#x2CEB-#x2CEE#x2CF2-#x2CF3#x2D00-#x2D25#x2D27#x2D2D#x2D30-#x2D67#x2D6F#x2D80-#x2D96#x2DA0-#x2DA6#x2DA8-#x2DAE#x2DB0-#x2DB6#x2DB8-#x2DBE#x2DC0-#x2DC6#x2DC8-#x2DCE#x2DD0-#x2DD6#x2DD8-#x2DDE#x2E2F#x3005-#x3006#x3031-#x3035#x303B-#x303C#x3041-#x3096#x309D-#x309F#x30A1-#x30FA#x30FC-#x30FF#x3105-#x312E#x3131-#x318E#x31A0-#x31BA#x31F0-#x31FF#x3400-#x4DB5#x4E00-#x9FEA#xA000-#xA48C#xA4D0-#xA4FD#xA500-#xA60C#xA610-#xA61F#xA62A-#xA62B#xA640-#xA66E#xA67F-#xA69D#xA6A0-#xA6E5#xA717-#xA71F#xA722-#xA788#xA78B-#xA7AE#xA7B0-#xA7B7#xA7F7-#xA801#xA803-#xA805#xA807-#xA80A#xA80C-#xA822#xA840-#xA873#xA882-#xA8B3#xA8F2-#xA8F7#xA8FB#xA8FD#xA90A-#xA925#xA930-#xA946#xA960-#xA97C#xA984-#xA9B2#xA9CF#xA9E0-#xA9E4#xA9E6-#xA9EF#xA9FA-#xA9FE#xAA00-#xAA28#xAA40-#xAA42#xAA44-#xAA4B#xAA60-#xAA76#xAA7A#xAA7E-#xAAAF#xAAB1#xAAB5-#xAAB6#xAAB9-#xAABD#xAAC0#xAAC2#xAADB-#xAADD#xAAE0-#xAAEA#xAAF2-#xAAF4#xAB01-#xAB06#xAB09-#xAB0E#xAB11-#xAB16#xAB20-#xAB26#xAB28-#xAB2E#xAB30-#xAB5A#xAB5C-#xAB65#xAB70-#xABE2#xAC00-#xD7A3#xD7B0-#xD7C6#xD7CB-#xD7FB#xF900-#xFA6D#xFA70-#xFAD9#xFB00-#xFB06#xFB13-#xFB17#xFB1D#xFB1F-#xFB28#xFB2A-#xFB36#xFB38-#xFB3C#xFB3E#xFB40-#xFB41#xFB43-#xFB44#xFB46-#xFBB1#xFBD3-#xFD3D#xFD50-#xFD8F#xFD92-#xFDC7#xFDF0-#xFDFB#xFE70-#xFE74#xFE76-#xFEFC#xFF21-#xFF3A#xFF41-#xFF5A#xFF66-#xFFBE#xFFC2-#xFFC7#xFFCA-#xFFCF#xFFD2-#xFFD7#xFFDA-#xFFDC#x10000-#x1000B#x1000D-#x10026#x10028-#x1003A#x1003C-#x1003D#x1003F-#x1004D#x10050-#x1005D#x10080-#x100FA#x10280-#x1029C#x102A0-#x102D0#x10300-#x1031F#x1032D-#x10340#x10342-#x10349#x10350-#x10375#x10380-#x1039D#x103A0-#x103C3#x103C8-#x103CF#x10400-#x1049D#x104B0-#x104D3#x104D8-#x104FB#x10500-#x10527#x10530-#x10563#x10600-#x10736#x10740-#x10755#x10760-#x10767#x10800-#x10805#x10808#x1080A-#x10835#x10837-#x10838#x1083C#x1083F-#x10855#x10860-#x10876#x10880-#x1089E#x108E0-#x108F2#x108F4-#x108F5#x10900-#x10915#x10920-#x10939#x10980-#x109B7#x109BE-#x109BF#x10A00#x10A10-#x10A13#x10A15-#x10A17#x10A19-#x10A33#x10A60-#x10A7C#x10A80-#x10A9C#x10AC0-#x10AC7#x10AC9-#x10AE4#x10B00-#x10B35#x10B40-#x10B55#x10B60-#x10B72#x10B80-#x10B91#x10C00-#x10C48#x10C80-#x10CB2#x10CC0-#x10CF2#x11003-#x11037#x11083-#x110AF#x110D0-#x110E8#x11103-#x11126#x11150-#x11172#x11176#x11183-#x111B2#x111C1-#x111C4#x111DA#x111DC#x11200-#x11211#x11213-#x1122B#x11280-#x11286#x11288#x1128A-#x1128D#x1128F-#x1129D#x1129F-#x112A8#x112B0-#x112DE#x11305-#x1130C#x1130F-#x11310#x11313-#x11328#x1132A-#x11330#x11332-#x11333#x11335-#x11339#x1133D#x11350#x1135D-#x11361#x11400-#x11434#x11447-#x1144A#x11480-#x114AF#x114C4-#x114C5#x114C7#x11580-#x115AE#x115D8-#x115DB#x11600-#x1162F#x11644#x11680-#x116AA#x11700-#x11719#x118A0-#x118DF#x118FF#x11A00#x11A0B-#x11A32#x11A3A#x11A50#x11A5C-#x11A83#x11A86-#x11A89#x11AC0-#x11AF8#x11C00-#x11C08#x11C0A-#x11C2E#x11C40#x11C72-#x11C8F#x11D00-#x11D06#x11D08-#x11D09#x11D0B-#x11D30#x11D46#x12000-#x12399#x12480-#x12543#x13000-#x1342E#x14400-#x14646#x16800-#x16A38#x16A40-#x16A5E#x16AD0-#x16AED#x16B00-#x16B2F#x16B40-#x16B43#x16B63-#x16B77#x16B7D-#x16B8F#x16F00-#x16F44#x16F50#x16F93-#x16F9F#x16FE0-#x16FE1#x17000-#x187EC#x18800-#x18AF2#x1B000-#x1B11E#x1B170-#x1B2FB#x1BC00-#x1BC6A#x1BC70-#x1BC7C#x1BC80-#x1BC88#x1BC90-#x1BC99#x1D400-#x1D454#x1D456-#x1D49C#x1D49E-#x1D49F#x1D4A2#x1D4A5-#x1D4A6#x1D4A9-#x1D4AC#x1D4AE-#x1D4B9#x1D4BB#x1D4BD-#x1D4C3#x1D4C5-#x1D505#x1D507-#x1D50A#x1D50D-#x1D514#x1D516-#x1D51C#x1D51E-#x1D539#x1D53B-#x1D53E#x1D540-#x1D544#x1D546#x1D54A-#x1D550#x1D552-#x1D6A5#x1D6A8-#x1D6C0#x1D6C2-#x1D6DA#x1D6DC-#x1D6FA#x1D6FC-#x1D714#x1D716-#x1D734#x1D736-#x1D74E#x1D750-#x1D76E#x1D770-#x1D788#x1D78A-#x1D7A8#x1D7AA-#x1D7C2#x1D7C4-#x1D7CB#x1E800-#x1E8C4#x1E900-#x1E943#x1EE00-#x1EE03#x1EE05-#x1EE1F#x1EE21-#x1EE22#x1EE24#x1EE27#x1EE29-#x1EE32#x1EE34-#x1EE37#x1EE39#x1EE3B#x1EE42#x1EE47#x1EE49#x1EE4B#x1EE4D-#x1EE4F#x1EE51-#x1EE52#x1EE54#x1EE57#x1EE59#x1EE5B#x1EE5D#x1EE5F#x1EE61-#x1EE62#x1EE64#x1EE67-#x1EE6A#x1EE6C-#x1EE72#x1EE74-#x1EE77#x1EE79-#x1EE7C#x1EE7E#x1EE80-#x1EE89#x1EE8B-#x1EE9B#x1EEA1-#x1EEA3#x1EEA5-#x1EEA9#x1EEAB-#x1EEBB#x20000-#x2A6D6#x2A700-#x2B734#x2B740-#x2B81D#x2B820-#x2CEA1#x2CEB0-#x2EBE0#x2F800-#x2FA1D]
namefollower
         ::= namestart
           | [-.·‿⁀0-9#x0660-#x0669#x06F0-#x06F9#x07C0-#x07C9#x0966-#x096F#x09E6-#x09EF#x0A66-#x0A6F#x0AE6-#x0AEF#x0B66-#x0B6F#x0BE6-#x0BEF#x0C66-#x0C6F#x0CE6-#x0CEF#x0D66-#x0D6F#x0DE6-#x0DEF#x0E50-#x0E59#x0ED0-#x0ED9#x0F20-#x0F29#x1040-#x1049#x1090-#x1099#x17E0-#x17E9#x1810-#x1819#x1946-#x194F#x19D0-#x19D9#x1A80-#x1A89#x1A90-#x1A99#x1B50-#x1B59#x1BB0-#x1BB9#x1C40-#x1C49#x1C50-#x1C59#xA620-#xA629#xA8D0-#xA8D9#xA900-#xA909#xA9D0-#xA9D9#xA9F0-#xA9F9#xAA50-#xAA59#xABF0-#xABF9#xFF10-#xFF19#x104A0-#x104A9#x11066-#x1106F#x110F0-#x110F9#x11136-#x1113F#x111D0-#x111D9#x112F0-#x112F9#x11450-#x11459#x114D0-#x114D9#x11650-#x11659#x116C0-#x116C9#x11730-#x11739#x118E0-#x118E9#x11C50-#x11C59#x11D50-#x11D59#x16A60-#x16A69#x16B50-#x16B59#x1D7CE-#x1D7FF#x1E950-#x1E959#x0300-#x036F#x0483-#x0487#x0591-#x05BD#x05BF#x05C1-#x05C2#x05C4-#x05C5#x05C7#x0610-#x061A#x064B-#x065F#x0670#x06D6-#x06DC#x06DF-#x06E4#x06E7-#x06E8#x06EA-#x06ED#x0711#x0730-#x074A#x07A6-#x07B0#x07EB-#x07F3#x0816-#x0819#x081B-#x0823#x0825-#x0827#x0829-#x082D#x0859-#x085B#x08D4-#x08E1#x08E3-#x0902#x093A#x093C#x0941-#x0948#x094D#x0951-#x0957#x0962-#x0963#x0981#x09BC#x09C1-#x09C4#x09CD#x09E2-#x09E3#x0A01-#x0A02#x0A3C#x0A41-#x0A42#x0A47-#x0A48#x0A4B-#x0A4D#x0A51#x0A70-#x0A71#x0A75#x0A81-#x0A82#x0ABC#x0AC1-#x0AC5#x0AC7-#x0AC8#x0ACD#x0AE2-#x0AE3#x0AFA-#x0AFF#x0B01#x0B3C#x0B3F#x0B41-#x0B44#x0B4D#x0B56#x0B62-#x0B63#x0B82#x0BC0#x0BCD#x0C00#x0C3E-#x0C40#x0C46-#x0C48#x0C4A-#x0C4D#x0C55-#x0C56#x0C62-#x0C63#x0C81#x0CBC#x0CBF#x0CC6#x0CCC-#x0CCD#x0CE2-#x0CE3#x0D00-#x0D01#x0D3B-#x0D3C#x0D41-#x0D44#x0D4D#x0D62-#x0D63#x0DCA#x0DD2-#x0DD4#x0DD6#x0E31#x0E34-#x0E3A#x0E47-#x0E4E#x0EB1#x0EB4-#x0EB9#x0EBB-#x0EBC#x0EC8-#x0ECD#x0F18-#x0F19#x0F35#x0F37#x0F39#x0F71-#x0F7E#x0F80-#x0F84#x0F86-#x0F87#x0F8D-#x0F97#x0F99-#x0FBC#x0FC6#x102D-#x1030#x1032-#x1037#x1039-#x103A#x103D-#x103E#x1058-#x1059#x105E-#x1060#x1071-#x1074#x1082#x1085-#x1086#x108D#x109D#x135D-#x135F#x1712-#x1714#x1732-#x1734#x1752-#x1753#x1772-#x1773#x17B4-#x17B5#x17B7-#x17BD#x17C6#x17C9-#x17D3#x17DD#x180B-#x180D#x1885-#x1886#x18A9#x1920-#x1922#x1927-#x1928#x1932#x1939-#x193B#x1A17-#x1A18#x1A1B#x1A56#x1A58-#x1A5E#x1A60#x1A62#x1A65-#x1A6C#x1A73-#x1A7C#x1A7F#x1AB0-#x1ABD#x1B00-#x1B03#x1B34#x1B36-#x1B3A#x1B3C#x1B42#x1B6B-#x1B73#x1B80-#x1B81#x1BA2-#x1BA5#x1BA8-#x1BA9#x1BAB-#x1BAD#x1BE6#x1BE8-#x1BE9#x1BED#x1BEF-#x1BF1#x1C2C-#x1C33#x1C36-#x1C37#x1CD0-#x1CD2#x1CD4-#x1CE0#x1CE2-#x1CE8#x1CED#x1CF4#x1CF8-#x1CF9#x1DC0-#x1DF9#x1DFB-#x1DFF#x20D0-#x20DC#x20E1#x20E5-#x20F0#x2CEF-#x2CF1#x2D7F#x2DE0-#x2DFF#x302A-#x302D#x3099-#x309A#xA66F#xA674-#xA67D#xA69E-#xA69F#xA6F0-#xA6F1#xA802#xA806#xA80B#xA825-#xA826#xA8C4-#xA8C5#xA8E0-#xA8F1#xA926-#xA92D#xA947-#xA951#xA980-#xA982#xA9B3#xA9B6-#xA9B9#xA9BC#xA9E5#xAA29-#xAA2E#xAA31-#xAA32#xAA35-#xAA36#xAA43#xAA4C#xAA7C#xAAB0#xAAB2-#xAAB4#xAAB7-#xAAB8#xAABE-#xAABF#xAAC1#xAAEC-#xAAED#xAAF6#xABE5#xABE8#xABED#xFB1E#xFE00-#xFE0F#xFE20-#xFE2F#x101FD#x102E0#x10376-#x1037A#x10A01-#x10A03#x10A05-#x10A06#x10A0C-#x10A0F#x10A38-#x10A3A#x10A3F#x10AE5-#x10AE6#x11001#x11038-#x11046#x1107F-#x11081#x110B3-#x110B6#x110B9-#x110BA#x11100-#x11102#x11127-#x1112B#x1112D-#x11134#x11173#x11180-#x11181#x111B6-#x111BE#x111CA-#x111CC#x1122F-#x11231#x11234#x11236-#x11237#x1123E#x112DF#x112E3-#x112EA#x11300-#x11301#x1133C#x11340#x11366-#x1136C#x11370-#x11374#x11438-#x1143F#x11442-#x11444#x11446#x114B3-#x114B8#x114BA#x114BF-#x114C0#x114C2-#x114C3#x115B2-#x115B5#x115BC-#x115BD#x115BF-#x115C0#x115DC-#x115DD#x11633-#x1163A#x1163D#x1163F-#x11640#x116AB#x116AD#x116B0-#x116B5#x116B7#x1171D-#x1171F#x11722-#x11725#x11727-#x1172B#x11A01-#x11A06#x11A09-#x11A0A#x11A33-#x11A38#x11A3B-#x11A3E#x11A47#x11A51-#x11A56#x11A59-#x11A5B#x11A8A-#x11A96#x11A98-#x11A99#x11C30-#x11C36#x11C38-#x11C3D#x11C3F#x11C92-#x11CA7#x11CAA-#x11CB0#x11CB2-#x11CB3#x11CB5-#x11CB6#x11D31-#x11D36#x11D3A#x11D3C-#x11D3D#x11D3F-#x11D45#x11D47#x16AF0-#x16AF4#x16B30-#x16B36#x16F8F-#x16F92#x1BC9D-#x1BC9E#x1D167-#x1D169#x1D17B-#x1D182#x1D185-#x1D18B#x1D1AA-#x1D1AD#x1D242-#x1D244#x1DA00-#x1DA36#x1DA3B-#x1DA6C#x1DA75#x1DA84#x1DA9B-#x1DA9F#x1DAA1-#x1DAAF#x1E000-#x1E006#x1E008-#x1E018#x1E01B-#x1E021#x1E023-#x1E024#x1E026-#x1E02A#x1E8D0-#x1E8D6#x1E944-#x1E94A#xE0100-#xE01EF]
varname  ::= '$' name
string   ::= '"' dchar+ '"'
           | "'" schar+ "'"
           | '“' qchar+ '”'
dchar    ::= [^"#xa#xd]
           | '"' '"'
schar    ::= [^'#xa#xd]
           | "'" "'"
qchar    ::= [^“”#xa#xd]
s        ::= ( whitespace | comment )*
RS       ::= ( whitespace | comment )+
whitespace
         ::= [ #x00A0#x1680#x2000-#x200A#x202F#x205F#x3000]
           | tab
           | lf
           | cr
tab      ::= #x9
lf       ::= #xa
cr       ::= #xd
ff       ::= #xc
comment  ::= '#' [^#xa]* #xa
      </p:inline>
    </p:with-input>
  </cx:railroad>

</p:declare-step>
