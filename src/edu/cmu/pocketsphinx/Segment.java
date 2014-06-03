/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 2.0.10
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package edu.cmu.pocketsphinx;

public class Segment {
  private long swigCPtr;
  protected boolean swigCMemOwn;

  protected Segment(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(Segment obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        PocketSphinxJNI.delete_Segment(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setWord(String value) {
    PocketSphinxJNI.Segment_word_set(swigCPtr, this, value);
  }

  public String getWord() {
    return PocketSphinxJNI.Segment_word_get(swigCPtr, this);
  }

  public void setAscr(int value) {
    PocketSphinxJNI.Segment_ascr_set(swigCPtr, this, value);
  }

  public int getAscr() {
    return PocketSphinxJNI.Segment_ascr_get(swigCPtr, this);
  }

  public void setLscr(int value) {
    PocketSphinxJNI.Segment_lscr_set(swigCPtr, this, value);
  }

  public int getLscr() {
    return PocketSphinxJNI.Segment_lscr_get(swigCPtr, this);
  }

  public void setLback(int value) {
    PocketSphinxJNI.Segment_lback_set(swigCPtr, this, value);
  }

  public int getLback() {
    return PocketSphinxJNI.Segment_lback_get(swigCPtr, this);
  }

  public void setStartFrame(int value) {
    PocketSphinxJNI.Segment_startFrame_set(swigCPtr, this, value);
  }

  public int getStartFrame() {
    return PocketSphinxJNI.Segment_startFrame_get(swigCPtr, this);
  }

  public void setEndFrame(int value) {
    PocketSphinxJNI.Segment_endFrame_set(swigCPtr, this, value);
  }

  public int getEndFrame() {
    return PocketSphinxJNI.Segment_endFrame_get(swigCPtr, this);
  }

  public static Segment fromIter(SWIGTYPE_p_ps_seg_t itor) {
    long cPtr = PocketSphinxJNI.Segment_fromIter(SWIGTYPE_p_ps_seg_t.getCPtr(itor));
    return (cPtr == 0) ? null : new Segment(cPtr, false);
  }

  public Segment() {
    this(PocketSphinxJNI.new_segment(), true);
  }

}
