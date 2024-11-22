.model minimal
.dummy do_a do_b do_b__na_result_1 reset __start __end __loop
.state graph
s1 __start s2
s2 do_b s4
s2 do_a s5
s4 do_b__na_result_1 s8
s4 do_a s9
s5 reset s6
s6 do_b s7
s7 do_b__na_result_1 s10
s8 do_a s10
s9 reset s7
s9 do_b__na_result_1 s10
s10 __end s3
s3 __loop s3
.marking {s1}
.end
