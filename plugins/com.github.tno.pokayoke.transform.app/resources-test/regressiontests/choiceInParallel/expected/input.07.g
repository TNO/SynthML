.model minimal
.dummy start end c_Turn_Computer_On c_Starting_Computer c_Drink_Coffee c_Drink_Tea c_Start_Working c_Flip_A_Coin c_satisfied
.state graph
loc0 start s1
s1 c_Turn_Computer_On s2
s2 c_Flip_A_Coin s4
s2 c_Starting_Computer s5
s3 end loc0
s4 c_Drink_Tea s7
s4 c_Drink_Coffee s7
s4 c_Starting_Computer s6
s5 c_Flip_A_Coin s6
s6 c_Drink_Tea s8
s6 c_Drink_Coffee s8
s7 c_Starting_Computer s8
s8 c_Start_Working s9
s9 c_satisfied s3
.marking {loc0}
.end
