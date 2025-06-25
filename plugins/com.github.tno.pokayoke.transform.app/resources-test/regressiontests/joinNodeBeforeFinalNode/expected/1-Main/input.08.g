.model minimal
.dummy __start action2 action1 __end __loop
.state graph
s1 __start s2
s2 action2 s4
s2 action1 s5
s4 action1 s6
s5 action2 s6
s5 action1 s5
s6 __end s3
s6 action1 s6
s3 __loop s3
.marking {s1}
.end
