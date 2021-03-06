# Vietnamese voice Recognization

## Voice Activity Dectection module

### Compute frame power
Pre-emphasis done to remove low-frequency noise

	x[i] = x[i] - x[i-1];

compute power
	
	power = 20 * log10( sqrt ( sum_square / n_samps) )
	
code
	
	for (int i = 0; i < spf; i++){
		x[i] = x[i] - x[i-1];
		sum += v * v;
	}
	
	// trim lower bound
	if (sum < spf)
		sum = spf;
	
	power = (int) ((10.0 * (Math.log10(sumsq) - Math.log10((double) spf))) + 0.5);
	
	// trim lower bound again
	if (power < 0)
		power = 0;
	
	return power;

### Find thresold
Find smallest non-zero histogram entry, but starting at some minimum power

	wave signal
	^
	|<--------speech---------->|				   |<--------------------------speech------->
	|				 --										--
	|			   -   -								  -   -	  --				  --
	|	  --     -      -							    --		--  -		  ---	 -	-  -
	|   -   -  -         -							  - 			 --		-	 -	-	 -  -
	| -      --			  -    |<------noise------>| -				   --  -	  -			 -
	|-					    -  |<-----9 frames---->|-					 --					  -
	|						  ----------------------
	|						   |
	|						   |--> power = 6
	|						   |
	----------------------------------------------------------------------------------------------------------------------->
																														time

Construct power histogram:

	|<----------------------------------------------------Power Histogram-------------------------------------------------->|
	There is no frame has power < 9 	|----------Min signal wave-------------->
	|<--Bad signal--->|
	-------------------------------------------------------------------------------------------------------------------------
	|  0  |  0  |  0  |  0  |  0  |  0  |  9  |  6  |  5  |  2  |  8  |  7  |  3  |  5  |  2  |     |     |     |     |     |
	-------------------------------------------------------------------------------------------------------------------------
	|				  ^					   9  					^
	|				  |					 frames					|
	|				 min				  with				   max
	|				noise			   power = 6			  noise
	|----------------------------------------------------------------------------------------------------------------------->
	+  0  +  1  +  2  +  3  +  4  +  5  + 6(A)+  7  +  8  +  9  +  10 +  11 +  12 +  13 +  14 +  15 + ... +     + max + power(dB)

Find thresold = power index that has largest number of frames in range (20dB from min power signal- A+20). For the example above
 the thresold is 6 (9 frames in range 6 + 20dB that have power is 6dB)

### Circular buffer
To maintain data read until consumed.

	|<----------------------------------------------------Buffer size------------------------------------------------------>|
							|<----------n_sample--------->|
	-------------------------------------------------------------------------------------------------------------------------
	| null| null| null| null| sam | sam | sam | sam | sam | null| null| null| null| null| ... | null| null| null| null| null|
	-------------------------------------------------------------------------------------------------------------------------
	  n-3   n-2   n-1    n  ^  0     1     2     3     4  ^  5     6     7     8     9    ...   n-8   n-7   n-6   n-5   n-4  
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





























