.model minimal
.dummy __start NonAtomicB AtomicA NonAtomicB__na_result_1 __end __loop
.state graph
s1 __start s2
s2 NonAtomicB s4
s2 AtomicA s5
s4 NonAtomicB__na_result_1 s7
s4 AtomicA s6
s5 NonAtomicB s6
s6 NonAtomicB__na_result_1 s8
s7 AtomicA s8
s8 __end s3
s3 __loop s3
.marking {s1}
.end
