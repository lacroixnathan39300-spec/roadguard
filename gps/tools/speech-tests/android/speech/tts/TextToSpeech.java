package android.speech.tts;
/** Explicit speech boundary adapter; no audio is synthesized in these tests. */
public class TextToSpeech {
 public static final int QUEUE_FLUSH=0,SUCCESS=0,ERROR=-1;
 public UtteranceProgressListener listener;public int result=SUCCESS;public String lastId;public int stopCalls;public boolean stopCallback;
 public void setOnUtteranceProgressListener(UtteranceProgressListener listener){this.listener=listener;}
 public int stop(){stopCalls++;if(stopCallback&&listener!=null)listener.onStop(lastId,true);return SUCCESS;}
 public int speak(String text,int mode,Object params,String id){lastId=id;return result;}
}
