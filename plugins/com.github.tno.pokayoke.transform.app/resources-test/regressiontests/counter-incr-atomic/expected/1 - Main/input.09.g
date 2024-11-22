.model minimal
.dummy initialize incr1 incr2 __start __end __loop
.state graph
s1 __start s2
s2 initialize s4
s4 incr2 s5
s4 incr1 s5
s5 __end s3
s3 __loop s3
.marking {s1}
.end
