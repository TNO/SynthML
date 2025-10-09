.model minimal
.dummy __start NonAtomicC NonAtomicA NonAtomicC__na_result_2 NonAtomicC__na_result_1 NonAtomicA__na_result_2 NonAtomicA__na_result_1 AtomicD AtomicB __end __loop
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
s6 NonAtomicC__na_result_2 s12
s6 NonAtomicC__na_result_1 s12
s6 NonAtomicA__na_result_2 s10
s6 NonAtomicA__na_result_1 s10
s7 AtomicD s9
s7 NonAtomicC s10
s7 AtomicB s11
s8 NonAtomicA s12
s9 NonAtomicC s15
s9 AtomicB s13
s10 AtomicD s15
s10 NonAtomicC__na_result_2 s16
s10 NonAtomicC__na_result_1 s16
s10 AtomicB s14
s11 AtomicD s13
s11 NonAtomicC s14
s12 NonAtomicA__na_result_2 s16
s12 NonAtomicA__na_result_1 s16
s13 NonAtomicC s17
s14 AtomicD s17
s14 NonAtomicC__na_result_2 s18
s14 NonAtomicC__na_result_1 s18
s15 NonAtomicC__na_result_2 s19
s15 NonAtomicC__na_result_1 s19
s15 AtomicB s17
s16 AtomicD s19
s16 AtomicB s18
s17 NonAtomicC__na_result_2 s20
s17 NonAtomicC__na_result_1 s20
s18 AtomicD s20
s19 AtomicB s20
s20 __end s3
s3 __loop s3
.marking {s1}
.end
