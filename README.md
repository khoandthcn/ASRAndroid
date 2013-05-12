# Vietnamese voice Recognization

## Voice Activity Dectection module

### Compute frame power
Pre-emphasis done to remove low-frequency noise

	x[i] = x[i] - x[i-1];

compute power
	
	power = 20 * log10( sqrt ( sum_square / n_samps) )
	
code
	:::java
		for (int i = 0; i < spf; i++){
			x[i] = x[i] - x[i-1];
			sum += v * v;
		}
		
		// trim lower bound
		if (sum < spf) sum = spf;
		
		power = (int) ((10.0 * (Math.log10(sumsq) - Math.log10((double) spf))) + 0.5);
		
		// trim lower bound again
		if (power < 0) power = 0;
		
		return power;

### Calibrate audio input source



### Circular buffer
To maintain data read until consumed.

	|<----------------------------------------------------Buffer size------------------------------------------------------>|
							|<----------n_sample--------->|
	-------------------------------------------------------------------------------------------------------------------------
	| n-3 | n-2 | n-1 |  n  |++0++|++1++|++2++|++3++|++4++|  5  |  6  |  7  |  8  |  9  | ... | n-8 | n-7 | n-6 | n-5 | n-4 |
	-------------------------------------------------------------------------------------------------------------------------
							^							  ^
							|							  |
						  head							 tail
						 frame							frame

### SIL to SPEECH transition
Create new head SpeechSegment: 
start_frm point to the first leader frame; 
initialize number of speech frames equal to sum of leader and win_size; 
reset window start_frm to current analysis frame

	|<----------------------------------------------Internal buffer frames------------------------------------------------->|
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

	|<----------------------------------------------Internal buffer frames------------------------------------------------->|
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

	|<----------------------------------------------Internal buffer frames------------------------------------------------->|
		  |<----------------[tail]SpeechSegment.nfrm----------------->|
													|<-----------------win_size-------------->|
													|<----trailer---->|
																	  |<--leader->|
	-------------------------------------------------------------------------------------------------------------------------
	|-sil-|voice|voice|voice|-sil-|voice|voice|voice|-sil-|voice|voice|voice|voice|voice|voice|-sil-|-sil-|-sil-|-sil-| ... |
	-------------------------------------------------------------------------------------------------------------------------
		  ^																			  ^			  ^
		  |																			  |			  |
		spseg																	  new window	current
		start																		start		analysis
		frame																		frame		 frame































