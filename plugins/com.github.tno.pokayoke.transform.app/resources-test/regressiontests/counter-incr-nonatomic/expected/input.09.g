.model minimal
.dummy initialize initialize__na_result_1 initialize__na_result_2 incr1 incr2 __start __end __loop
.state graph
s1 __start s2
s2 initialize s4
s4 initialize__na_result_2 s5
s4 initialize__na_result_1 s6
s5 incr1 s7
s6 incr2 s7
s7 __end s3
s3 __loop s3
.marking {s1}
.end
