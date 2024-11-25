.model minimal
.dummy __start Turn_Computer_On Flip_A_Coin Starting_Computer Drink_Tea Drink_Coffee Start_Working __end __loop
.state graph
s1 __start s2
s2 Turn_Computer_On s4
s4 Flip_A_Coin s5
s4 Starting_Computer s6
s5 Drink_Tea s9
s5 Drink_Coffee s9
s5 Starting_Computer s7
s6 Flip_A_Coin s7
s7 Drink_Tea s8
s7 Drink_Coffee s8
s8 Start_Working s10
s9 Starting_Computer s8
s10 __end s3
s3 __loop s3
.marking {s1}
.end
