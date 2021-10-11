print("<paths>")

with open("paths.txt", "r") as paths:
    for line in paths.readlines():
        line = line.rstrip()
        if line.startswith("#") or line == "":
            pass
        else:
            scheme = line[0:1]
            implicit = line[1:2]
            hierarchical = line[2:3]
            absolute = line[3:4]
            drive = line[4:5]
            authority = line[5:6]
            date = line[7:17]
            path = line[18:]

            if scheme == "A":
                a1 = ""
            else:
                if implicit == "I":
                    a1 = f"scheme='file' "
                else:
                    pos = path.index(":")
                    a1 = f"scheme='{path[0:pos]}' "

            if implicit == "I":
                a2 = "implicit='true' "
            else:
                a2 = ""

            if hierarchical == "H":
                a3 = ""
            else:
                a3 = "hierarchical='false' "

            if absolute == "A":
                a4 = "path='absolute' "
            else:
                a4 = "path='relative' "

            if drive == "D":
                a5 = "drive='C' "
            else:
                a5 = ""

            if authority == "A":
                a6 = "authority='true' "
            else:
                a6 = ""

            print(f"<path date='{date}' {a1}{a2}{a3}{a4}{a5}{a6}>{path}</path>")
            
print("</paths>")

