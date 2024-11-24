saxon app/pipe.xml app/src/main/resources/com/xmlcalabash/graph2dot.xsl -o:/tmp/graph.dotxml
saxon /tmp/graph.dotxml app/src/main/resources/com/xmlcalabash/dot2txt.xsl -o:/tmp/graph.dot
dot -Tsvg -o/tmp/graph.svg /tmp/graph.dot
