.model minimal
.dummy do_a do_b do_b__na_result_1 __start __end __loop
.state graph
s1 __start s2
s2 do_b s4
s2 do_a s5
s4 do_b__na_result_1 s6
s5 __end s3
s6 do_a s5
s3 __loop s3
.marking {s1}
.end
