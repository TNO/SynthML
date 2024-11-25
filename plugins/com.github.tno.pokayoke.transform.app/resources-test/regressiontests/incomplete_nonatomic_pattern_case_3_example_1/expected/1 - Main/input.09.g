.model minimal
.dummy __start do_b do_b__na_result_1 do_a __end __loop
.state graph
s1 __start s2
s2 do_b s4
s4 do_b__na_result_1 s5
s4 do_a s6
s5 do_a s7
s6 do_b__na_result_1 s7
s7 __end s3
s3 __loop s3
.marking {s1}
.end
