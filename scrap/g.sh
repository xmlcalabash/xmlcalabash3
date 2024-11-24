BASE=/tmp/test-case
saxon $BASE.xml xmlcalabash/src/main/resources/com/xmlcalabash/graph2dot.xsl -o:$BASE.dotxml
saxon $BASE.dotxml xmlcalabash/src/main/resources/com/xmlcalabash/dot2txt.xsl -o:$BASE.dot
dot -Tsvg -o$BASE.svg $BASE.dot
