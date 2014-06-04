package asr.service;

public interface AudioRecordListener {

	public void utteranceStart();
	
	public void readRaw(short[] buf, int length);
	
	public void utteranceEnd(int utt_length);
}
