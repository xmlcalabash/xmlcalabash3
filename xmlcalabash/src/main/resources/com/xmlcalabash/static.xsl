<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                version="3.0">

<xsl:variable name="logo" as="xs:string">
R0lGODlhMAAxAPcAAP///wAAAP7+/v39/fz8/Pv7+/r6+vn5+fj4+Pf39/b29vX19fT09PPz8/Ly8vHx
8fDw8O/v7+7u7u3t7ezs7Ovr6+rq6unp6ejo6Ofn5+bm5uXl5eTk5OPj4+Li4uHh4eDg4N/f397e3t3d
3dzc3Nvb29ra2tnZ2djY2NfX19bW1tXV1dTU1NPT09HR0dDQ0M/Pz87Ozs3NzczMzMvLy8rKysnJycjI
yMfHx8bGxsXFxcTExMPDw8LCwsHBwcDAwL+/v76+vr29vbu7u7q6urm5ubi4uLe3t7a2trW1tbS0tLOz
s7KysrGxsbCwsK+vr66urq2traysrKurq6qqqqmpqaioqKenp6ampqWlpaSkpKOjo6GhoaCgoJ+fn56e
np2dnZycnJubm5qampmZmZiYmJeXl5WVlZSUlJKSkpGRkZCQkI+Pj46Ojo2NjYyMjIuLi4qKiomJiYiI
iIaGhoSEhIODg4KCgoGBgX9/f35+fn19fXx8fHt7e3p6enl5eXh4eHd3d3Z2dnV1dXR0dHNzc3JycnFx
cXBwcG9vb25ubm1tbWxsbGtra2pqamlpaWhoaGdnZ2ZmZmVlZWRkZGNjY2JiYmFhYWBgYF9fX15eXlxc
XFtbW1paWllZWVhYWFdXV1ZWVlVVVVRUVFNTU1JSUlFRUVBQUE9PT05OTk1NTUxMTEtLS0pKSklJSUhI
SEdHR0ZGRkVFRURERENDQ0JCQkFBQUBAQD8/Pz4+Pj09PTw8PDs7Ozo6Ojk5OTg4ODc3NzY2NjU1NTQ0
NDMzMzIyMjExMTAwMC8vLy4uLi0tLSwsLCsrKyoqKikpKSgoKCcnJyYmJiUlJSQkJCMjIyIiIiEhISAg
IB8fHx4eHh0dHRwcHBsbGxoaGhgYGBcXFxYWFhUVFRQUFBMTExISEhERERAQEA8PDw4ODg0NDQwMDAsL
CwoKCgkJCQgICAcHBwYGBgUFBQQEBAMDAwICAgEBAf///wAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA
ACH5BAUAAPYALAAAAAAwADEAAAj/AO0JHEiwYEEIM7xYSSFAgA5I0jbJoCBHFKAvJwxq3KhRgSBSIGiE
W4Yp1DIfjM7VenUrVx0yvCY54EizoAMrl2x9AABARzVTtP7wFAZN06IFPAEgQkehZk0Tu4wZ2wWhjDUA
j5gd81EAgA9typYAOOQHwIdzqKAAIeDUoAVpPHvBs4GgCQBbzhYBAFT21zoUAMwFCLWpFq1d3FIJaEvw
kiEVNwQEIJS0wCpqAOZ8AvAnQBmeiaJFYtWqQIFpmxifEOQpkqEf6bDokAUAyqYTKJhRAGAAgBd3ytDw
VCTMmBQAFIJVK+MUhLRqqwapEtLEBwAuEX4EUATAjYikMrjx/zIGiec4UDwLsTP0iUhNM0kIwXOkiwyA
EC4eoDigIwGAPRSEMMoJABBjCiHZ4ACAK0YAgEM6k5RiSioq0CRGFgCk4s0ogABgRgArJLHbA4Eg42EA
GNZhlhMAzNAGT2iYA8kvKJQQyUYChNEMLhAAYI01pNiARTy3DEOIEpgk40ojs+DiySGxdJBUGTYkdUMo
VgDQgi8bYbEKLgHQAsATiOy1zRk1eNJNNIJswYQPG9jzgiXC3CKGColwR8EiBDYgAACV3LIRLC8IUMU1
jvCEQB2dWCAQBjNpVIAMkmCzDgwAlBHAMyvwhEUAfmgkQCxMiNEFAEWgQMEhuDTBmEAzcP/CUy4BnMJT
CMuck4VGKERzSA4b9JaBMdt08epAUexARgCP8IQDOAHAkoBBCZxiyyo0AIDLLa5g88KxAxFASwB28LSF
OerkgoBGPLxghS+SdMDIMDwoAC5BCqjixweTBKCLKOtqpIULcQRQBRLciHGvQQe0oYs61qgRsEYVCLKB
Hh76MsDCG0HQgFMxMALAD5lszDHHBUBwRCBqnMyRBUFkcYcgstBREAKhHGOHI7iY4DJBAuTASjsBwLPM
KbfYTFACceQgRQyghPGzQDd8E8A5acRJkxCGvLFGIiVMbUQAAcRBEwQyDIEDEoy4AQggOuxwgsn3YuFM
AE4QhMAAEUD/McAAa8SBQwQAOBACBxkwwJMDSHgxMWM6JDNZQSMYccQcPUCBhQ0qjADCBDwVIMEKOfxg
hBlgHHtCMc68s0BBBzAgQAMl5IFCBgQkBUABCzAQAQg1UJHIIK8SkEklAShhkAQycNADA0Ec4kUYVeQg
Qgw33JBEE0GI8UgpErzKRh/TWLNYQRYAoYIQGNjzADG6uGIJMfOsQ045AXiDii/TMmaCJWsIQA4MAoAG
bCAEBRgIICTRBU6QLQD0qEc90nGLIhyLE1pghzTORxAG6MAeITDCQJQgCj68IwDrIBvRxjGLDrzqC38o
RABcpREGrGAEMRgIBkLhiQCgIx1XQ0cA/6zxiQkwZgKy2EL+DGAQATTBUSNg4kD6EIp4oGMd8HCHPIY4
igswJhFjYEUAoKAREIChBAogAlsGMoVQPMOHWyRbNkZRgbbQABJXCAA3DiCpjakhCAWRQChKEQB2BKAe
ZBOHKl5XEwKg4gnbSF5NMvCxgpTBE+eARzwO6UNdQMApYmhDZ/a4EQJsjCcGQQEoxuUOTqZjFUakiQQM
8QWy5c0gEIAAAUAAAI4IYhLqIBsi15GKD9QkDFxARQBUsUaCGAAG3wqCCzeCAlbQCh4PfMYMOAIBU6gi
FAHIhs8K8gARICAKRpBDAjlih16kgx7zIBs41sARD3RjFq00FkE2xv8BFtgDBqYIG00eAAxjBCCO8nhG
+zTiiUJkwh3h8IJAILAYCYgBATtYAwwYAwRfGJQeZGOHJhhgEEwIogG9SAY0AOk+e2AABTSQAwnG8EnG
uIEWyyAbPejxjmJIQQMKWAAPapEKEPAiANgQgj0KkIADqKABlHAIHu41CExIg2zzoIc61sGObHCDHucI
RE7dAQeBJEALAICDB54AhKUuDA+r0AYiD/kOdKhDHvNghyGv0YaBCGAHVzCBE0TASI7pYBXeiGM81JEO
drRDgt7YAkEsAAJCPOAAhT3ZAsgAjHXUQx71gCfZkuECghAAAikIwdQKQoAdLCIa3diGN1IRBLoICWQA
zWRMQAAAOw==
</xsl:variable>

<xsl:variable name="graphsvg.js" as="xs:string">
Y29uc3Qgc2NhbGVTcGFuID0gZG9jdW1lbnQucXVlcnlTZWxlY3RvcigiYm9keSBwIHNwYW4u
X19zY2FsZSIpCmlmIChzY2FsZVNwYW4pIHsKICBzY2FsZVNwYW4uaW5uZXJIVE1MID0gIjxz
cGFuPjwvc3Bhbj4gPGJ1dHRvbiBjbGFzcz0nX19taW51cyc+LTwvYnV0dG9uPiAvIDxidXR0
b24gY2xhc3M9J19fcGx1cyc+KzwvYnV0dG9uPiIKICBjb25zdCBzcGFucyA9IGRvY3VtZW50
LnF1ZXJ5U2VsZWN0b3JBbGwoImJvZHkgcCBzcGFuLl9fc2NhbGUgKiIpCiAgY29uc3QgcGVy
Y2VudCA9IHNwYW5zWzBdOwogIGNvbnN0IG1pbnVzID0gc3BhbnNbMV07CiAgY29uc3QgcGx1
cyA9IHNwYW5zWzJdOwoKICBjb25zdCBzdmdEaXYgPSBkb2N1bWVudC5xdWVyeVNlbGVjdG9y
KCJkaXYuc3ZnIik7CiAgY29uc3Qgc3ZnID0gc3ZnRGl2LnF1ZXJ5U2VsZWN0b3IoInN2ZyA+
IGciKTsKICBjb25zdCBwb2x5Z29uID0gc3ZnLnF1ZXJ5U2VsZWN0b3IoInBvbHlnb24iKTsK
CiAgbGV0IGJlZm9yZSA9IG51bGw7CiAgbGV0IGFmdGVyID0gbnVsbDsKICBsZXQgc2NhbGUg
PSAxLjA7CgogIGZ1bmN0aW9uIHJlc2l6ZShmYWN0b3IpIHsKICAgIGlmIChhdHRyKSB7CiAg
ICAgIHNjYWxlID0gTWF0aC5yb3VuZChwYXJzZUZsb2F0KHNjYWxlICogZmFjdG9yICogMTAw
MC4wKSkgLyAxMDAwLjA7CiAgICAgIHN2Zy5zZXRBdHRyaWJ1dGUoInRyYW5zZm9ybSIsIGAk
e2JlZm9yZX0ke3NjYWxlfSAke3NjYWxlfSR7YWZ0ZXJ9YCk7CiAgICAgIHBlcmNlbnQuaW5u
ZXJIVE1MID0gYCR7TWF0aC5yb3VuZChzY2FsZSAqIDEwMC4wKX0lYDsKICAgIH0KICB9Cgog
IGNvbnN0IGF0dHIgPSBzdmcgPyBzdmcuZ2V0QXR0cmlidXRlKCJ0cmFuc2Zvcm0iKSA6IG51
bGw7CiAgaWYgKCFhdHRyKSB7CiAgICAgIGNvbnNvbGUubG9nKCJObyB0cmFuc2Zvcm0gYXR0
cmlidXRlIG9uIHN2Zy9nIik7CiAgfSBlbHNlIHsKICAgIC8vIElmIHRoZSBiYWNrZ3JvdW5k
IGNvbG9yIG9uIHRoZSBkaWdyYXBoIGhhcyBiZWVuIGNoYW5nZWQsCiAgICAvLyBhdHRlbXB0
IHRvIG1hdGNoIHRoZSBjb2xvciBpbiB0aGUgZGl2LgogICAgbGV0IGJnY29sb3IgPSBwb2x5
Z29uICYmIHBvbHlnb24uZ2V0QXR0cmlidXRlKCJmaWxsIik7CiAgICBpZiAoc3ZnRGl2ICYm
IGJnY29sb3IpIHsKICAgICAgc3ZnRGl2LnN0eWxlWyJiYWNrZ3JvdW5kLWNvbG9yIl0gPSBi
Z2NvbG9yOwogICAgfQoKICAgIGxldCBwb3MgPSBhdHRyLmluZGV4T2YoInNjYWxlKCIpOwog
ICAgaWYgKHBvcyA8IC0xKSB7CiAgICAgIGNvbnNvbGUubG9nKCJObyBzY2FsZSgpIGluIHRy
YW5zZm9ybSBhdHRyaWJ1dGUgb24gc3ZnL2ciKTsKICAgICAgYXR0ciA9IG51bGwKICAgIH0g
ZWxzZSB7CiAgICAgIGJlZm9yZSA9IGF0dHIuc3Vic3RyaW5nKDAsIHBvcys2KTsKICAgICAg
YWZ0ZXIgPSBhdHRyLnN1YnN0cmluZyhwb3MrNik7CiAgICAgIHBvcyA9IGFmdGVyLmluZGV4
T2YoIikiKTsKICAgICAgYWZ0ZXIgPSBhZnRlci5zdWJzdHJpbmcocG9zKQogICAgfQogICAg
cmVzaXplKDEpOwogIH0KCiAgbWludXMuYWRkRXZlbnRMaXN0ZW5lcigiY2xpY2siLCBldmVu
dCA9PiB7CiAgICByZXNpemUoMC44KTsKICB9KTsKCiAgcGx1cy5hZGRFdmVudExpc3RlbmVy
KCJjbGljayIsIGV2ZW50ID0+IHsKICAgIHJlc2l6ZSgxLjI1KTsKICB9KTsKfQo=
</xsl:variable>

<xsl:variable name="css">
  <style type="text/css" xsl:expand-text="no">
:root {
  background-color: #ffffff;
  font-size: 16pt;
  --symbol-fonts: "Arial Unicode", "Apple Symbols", "Symbol", "Symbola_hint";
  --body-family: serif, var(--symbol-fonts);
  --title-family: sans-serif, var(--symbol-fonts);
  --mono-family: monospace, var(--symbol-fonts);
}

html {
  height: 100%;
}

body {
  font-family: var(--body-family);
  margin-left: auto;
  margin-right: auto;
  margin-bottom: 4rem;
  padding-right: 1rem;
  padding-left: 1rem;
  height: 100%;
  display: flex;
  flex-direction: column;
  margin-top: 0;
  padding-top: 0.5rem;
}

html.graph, body.graph {
  background-color: #f0f0f0;
}

header {
  max-width: 50rem;
}
h1 {
  font-family: var(--title-family);
}
code {
  font-size: 90%;
}
table {
  layout: fixed;
  width: 100%;
  font-size: 16pt;
  border: 1px solid #7f7f7f;
  border-spacing: 0px;
  border-collapse: collapse;
}
table thead tr {
  background-color: #cfcfcf;
  font-family: var(--title-family);
}
table thead tr th {
  border-bottom: 1px solid #7f7f7f;
}
table thead th {
  text-align: left;
}
th, td {
  padding: 0.25em;
  border-right: 1px solid #7f7f7f;
}
table tbody tr:nth-child(even) td {
  background-color: #f0f0f0;
}

.svg {
  border: 1px solid black;
  overflow: scroll;
  flex-grow: 1;
  margin-bottom: 1rem;
  background-color: white;
}

.svg img {
}

body p span button {
 cursor: pointer;
}

body p span.__scale {
  float: right;
}

.__logo {
  padding-right: 0.5em;
  vertical-align: baseline;
  display: inline;
}
  </style>
</xsl:variable>

</xsl:stylesheet>
