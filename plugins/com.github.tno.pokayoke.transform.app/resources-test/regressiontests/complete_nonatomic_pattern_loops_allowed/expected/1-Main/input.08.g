.model minimal
.dummy __start do_b do_a __end __loop
.state graph
s1 __start s2
s2 do_b s4
s2 do_a s2
s4 __end s3
s4 do_b s4
s4 do_a s2
s3 __loop s3
.marking {s1}
.end
