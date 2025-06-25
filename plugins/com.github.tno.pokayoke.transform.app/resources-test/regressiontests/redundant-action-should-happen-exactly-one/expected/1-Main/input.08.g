.model minimal
.dummy __start flip right left __end __loop
.state graph
s1 __start s2
s2 flip s4
s2 right s5
s2 left s5
s4 right s6
s4 left s6
s5 flip s6
s6 __end s3
s3 __loop s3
.marking {s1}
.end
