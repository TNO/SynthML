.model minimal
.dummy __start Robot2MoveToLocB HomeRobot2 __end ReleaseProductAtLocC PickProductAtLocB Robot2MoveToLocC __loop
.state graph
s1 __start s2
s2 Robot2MoveToLocB s7
s4 HomeRobot2 s5
s5 __end s3
s6 ReleaseProductAtLocC s4
s7 PickProductAtLocB s8
s8 Robot2MoveToLocC s6
s3 __loop s3
.marking {s1}
.end
