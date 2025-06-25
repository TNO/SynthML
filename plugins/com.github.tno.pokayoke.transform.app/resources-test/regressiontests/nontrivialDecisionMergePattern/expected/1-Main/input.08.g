.model minimal
.dummy __start flip right left flip__na_result_2 flip__na_result_1 __end __loop
.state graph
s1 __start s2
s2 flip s4
s2 right s5
s2 left s5
s4 flip__na_result_2 s7
s4 flip__na_result_1 s7
s5 flip s6
s6 flip__na_result_2 s8
s6 flip__na_result_1 s8
s7 right s8
s7 left s8
s8 __end s3
s3 __loop s3
.marking {s1}
.end
