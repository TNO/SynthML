.model minimal
.dummy start end Turn_Computer_On Starting_Computer Drink_Coffee Drink_Tea Start_Working Flip_A_Coin c_satisfied
.state graph
loc0 start s1
s1 Turn_Computer_On s2
s2 Flip_A_Coin s4
s2 Starting_Computer s5
s3 end loc0
s4 Drink_Tea s7
s4 Drink_Coffee s7
s4 Starting_Computer s6
s5 Flip_A_Coin s6
s6 Drink_Tea s8
s6 Drink_Coffee s8
s7 Starting_Computer s8
s8 Start_Working s9
s9 c_satisfied s3
.marking {loc0}
.end
