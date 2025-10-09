.model minimal
.dummy __start NonAtomicB NonAtomicA NonAtomicB__na_result_2 NonAtomicB__na_result_1 NonAtomicA__na_result_2 NonAtomicA__na_result_1 __end __loop
.state graph
s1 __start s2
s2 NonAtomicB s4
s2 NonAtomicA s5
s4 NonAtomicB__na_result_2 s8
s4 NonAtomicB__na_result_1 s8
s4 NonAtomicA s6
s5 NonAtomicB s6
s5 NonAtomicA__na_result_2 s7
s5 NonAtomicA__na_result_1 s7
s6 NonAtomicB__na_result_2 s10
s6 NonAtomicB__na_result_1 s10
s6 NonAtomicA__na_result_2 s9
s6 NonAtomicA__na_result_1 s9
s7 NonAtomicB s9
s8 NonAtomicA s10
s9 NonAtomicB__na_result_2 s11
s9 NonAtomicB__na_result_1 s11
s10 NonAtomicA__na_result_2 s11
s10 NonAtomicA__na_result_1 s11
s11 __end s3
s3 __loop s3
.marking {s1}
.end
