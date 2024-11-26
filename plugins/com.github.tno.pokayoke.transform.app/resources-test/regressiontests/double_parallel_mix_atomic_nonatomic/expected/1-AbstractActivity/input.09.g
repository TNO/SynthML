.model minimal
.dummy __start NonAtomicC NonAtomicA NonAtomicC__na_result_2 NonAtomicC__na_result_1 NonAtomicA__na_result_2 NonAtomicA__na_result_1 AtomicB AtomicD __end __loop
.state graph
s1 __start s2
s2 NonAtomicC s4
s2 NonAtomicA s5
s4 NonAtomicC__na_result_2 s8
s4 NonAtomicC__na_result_1 s8
s4 NonAtomicA s6
s5 NonAtomicC s6
s5 NonAtomicA__na_result_2 s7
s5 NonAtomicA__na_result_1 s7
s6 NonAtomicC__na_result_2 s11
s6 NonAtomicC__na_result_1 s11
s6 NonAtomicA__na_result_2 s9
s6 NonAtomicA__na_result_1 s9
s7 NonAtomicC s9
s7 AtomicB s10
s8 AtomicD s12
s8 NonAtomicA s11
s9 NonAtomicC__na_result_2 s14
s9 NonAtomicC__na_result_1 s14
s9 AtomicB s13
s10 NonAtomicC s13
s11 AtomicD s15
s11 NonAtomicA__na_result_2 s14
s11 NonAtomicA__na_result_1 s14
s12 NonAtomicA s15
s13 NonAtomicC__na_result_2 s16
s13 NonAtomicC__na_result_1 s16
s14 AtomicD s17
s14 AtomicB s16
s15 NonAtomicA__na_result_2 s17
s15 NonAtomicA__na_result_1 s17
s16 AtomicD s18
s17 AtomicB s18
s18 __end s3
s3 __loop s3
.marking {s1}
.end
