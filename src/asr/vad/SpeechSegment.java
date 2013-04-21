public class SpeechSegment {

  /*
   * Frame_id in adbuf of start of this segment
   */
  protected int startfrm;
  
  /*
   * Number of frames in segment ( may wrap around adbuf)
   */
  protected int nfrm;
  
  /*
   * Next speech segment
   */
  protected SpeechSegment next;

  public SpeechSegment(){}
  
  public void setStartFrameId(int startFrameId){
    this.startfrm = startFrameId;
  }
  
  public int getStartFrameId(){
    return this.startfrm;
  }
  
  public void setNumberOfFrames(int nfrm){
    this.nfrm = nfrm;
  }
  
  public int getNumberOfFrames(){
    return this.nfrm;
  }
}
