.model minimal
.dummy __start initialize __end flip __loop
.state graph
s1 __start s2
s2 initialize s4
s4 __end s3
s4 flip s5
s5 __end s3
s3 __loop s3
.marking {s1}
.end
