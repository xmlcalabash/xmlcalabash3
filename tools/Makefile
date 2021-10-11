paths.xml: paths.txt paths.py
	python paths.py

urify-cases.xml: paths.xml paths.xsl
	saxon $^ -o:$@
