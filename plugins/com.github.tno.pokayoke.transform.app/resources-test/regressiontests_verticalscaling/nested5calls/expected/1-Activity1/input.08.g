.model minimal
.dummy __start flip1 flip1__na_result_1 __end __loop
.state graph
s1 __start s2
s2 flip1 s4
s4 flip1__na_result_1 s5
s5 __end s3
s3 __loop s3
.marking {s1}
.end
