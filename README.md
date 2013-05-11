# Vietnamese voice Recognization

## Voice Activity Dectection module
Processing block:

	<---------------------------------------------------FRAME_SIZE frames--------------------------------------------------->
					  <-----------------win_size--------------->
		   <--leader->
	-------------------------------------------------------------------------------------------------------------------------
	|frame|frame|frame|frame|frame|frame|frame|frame|frame|frame|frame|frame|frame|frame|frame|frame| ... |frame|frame|frame|
	-------------------------------------------------------------------------------------------------------------------------
																^
															 	|
															 current
														 	 analysis
														 	  frame
