saxon /tmp/pipeline.xml app/src/main/resources/com/xmlcalabash/pipeline2dot.xsl -o:/tmp/pipeline.dotxml
saxon /tmp/pipeline.dotxml app/src/main/resources/com/xmlcalabash/dot2txt.xsl -o:/tmp/pipeline.dot
dot -Tsvg -o/tmp/pipeline.svg /tmp/pipeline.dot
