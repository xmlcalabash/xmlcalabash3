saxon pipe.xml src/main/resources/com/xmlcalabash/pipeline2dot.xsl -o:/tmp/pipeline.dotxml
saxon /tmp/pipeline.dotxml src/main/resources/com/xmlcalabash/dot2txt.xsl -o:/tmp/pipeline.dot
dot -Tsvg -opipe.pipeline.svg /tmp/pipeline.dot
