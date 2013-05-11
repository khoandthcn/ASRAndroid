# Vietnamese voice Recognization

## Voice Activity Dectection module

### SIL to SPEECH transition
Create new head SpeechSegment:
	start_frm point to the first leader frame
	Initialize number of speech frames equal to sum of leader and win_size
	Reset window start_frm to current analysis frame

	|<---------------------------------------------------FRAME_SIZE frames------------------------------------------------->|
		  |<--------------SpeechSegment.nfrm------------------->|
					  |<-----------------win_size-------------->|
		  |<--leader->|
	-------------------------------------------------------------------------------------------------------------------------
	|-sil-|voice|voice|-sil-|-sil-|voice|voice|voice|-sil-|voice|     |     |     |     |     |     | ... |     |     |     |
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


### In SPEECH state: 
Staying there and add this frame to segment

	|<---------------------------------------------------FRAME_SIZE frames------------------------------------------------->|
					  |<-----------------win_size-------------->|
		  |<--leader->|
	-------------------------------------------------------------------------------------------------------------------------
	|-sil-|voice|voice|-sil-|-sil-|voice|voice|voice|-sil-|voice|voice|voice|     |     |     |     | ... |     |     |     |
	-------------------------------------------------------------------------------------------------------------------------
																			^
																		 	|
																		 current
																	 	 analysis
																	 	  frame
														 	  
														 	  
														 	  
														 	  
														 	  
