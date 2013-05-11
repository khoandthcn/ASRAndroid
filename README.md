# Vietnamese voice Recognization

## Voice Activity Dectection module

### SIL to SPEECH transition
Create new head SpeechSegment: 
start_frm point to the first leader frame; 
initialize number of speech frames equal to sum of leader and win_size; 
reset window start_frm to current analysis frame

	|<---------------------------------------------------FRAME_SIZE frames------------------------------------------------->|
		  |<--------------[head]SpeechSegment.nfrm------------->|
					  |<-----------------win_size-------------->|
		  |<--leader->|
	-------------------------------------------------------------------------------------------------------------------------
	|-sil-|voice|voice|voice|-sil-|voice|voice|voice|-sil-|voice|voice|     |     |     |     |     | ... |     |     |     |
	-------------------------------------------------------------------------------------------------------------------------
		  ^			  ^											^
		  |			  |										 	|
		spseg		  |										 current
		start		  |									 	 analysis
		frame		  |									 	  frame
					  |											^
				  old window								new window
					start	------------------------------ >  start
					frame									  frame

### In SPEECH state
Staying there and add this frame to segment

	|<---------------------------------------------------FRAME_SIZE frames------------------------------------------------->|
		  |<-[tail]SpeechSegment.nfrm-->|
										|<-----------------win_size-------------->|
	-------------------------------------------------------------------------------------------------------------------------
	|-sil-|voice| ... |voice|-sil-|voice|voice|voice|-sil-|voice|voice|voice|voice|     |     |     | ... |     |     |     |
	-------------------------------------------------------------------------------------------------------------------------
		  ^								^										  ^
		  |								|										  |
		spseg						  window									current
		start						  start										analysis
		frame						  frame										 frame

### SPEECH to SIL transition

	|<---------------------------------------------------FRAME_SIZE frames------------------------------------------------->|
		  |<----------------[tail]SpeechSegment.nfrm----------------->|
													|<-----------------win_size-------------->|
													|<----trailer---->|
																	  |<--leader->|
	-------------------------------------------------------------------------------------------------------------------------
	|-sil-|voice|voice|voice|-sil-|voice|voice|voice|-sil-|voice|voice|voice|voice|voice|voice|-sil-|-sil-|-sil-|-sil-| ... |
	-------------------------------------------------------------------------------------------------------------------------
	  ^																			  ^			  ^
		  |																		  |			  |
		spseg																  new window	current
		start																	start		analysis
		frame																	frame		 frame































