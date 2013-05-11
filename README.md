# Vietnamese voice Recognization

## Voice Activity Dectection module

### SIL to SPEECH transition
Create new head SpeechSegment:
 - start_frm point to the first leader frame
 - initialize number of speech frames equal to sum of leader and win_size
 - reset window start_frm to current analysis frame

	<---------------------------------------------------FRAME_SIZE frames--------------------------------------------------->
		   <--------------SpeechSegment.nfrm------------------->
					  <-----------------win_size--------------->
		   <--leader->
	-------------------------------------------------------------------------------------------------------------------------
	|frame|frame|frame|frame|frame|frame|frame|frame|frame|frame|frame|frame|frame|frame|frame|frame| ... |frame|frame|frame|
	-------------------------------------------------------------------------------------------------------------------------
		  ^			  ^											^
		  |			  |										 	|
		spseg	  old window								 current
		start		start								 	 analysis
		frame		frame								 	  frame
																^
															new window
															  start
															  frame


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
