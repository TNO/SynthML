.model minimal
.dummy __start action __end __loop
.state graph
s1 __start s2
s2 action s4
s4 __end s3
s4 action s4
s3 __loop s3
.marking {s1}
.end
