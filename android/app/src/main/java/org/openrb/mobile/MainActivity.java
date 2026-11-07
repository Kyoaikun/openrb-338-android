package org.openrb.mobile;
import android.app.*;
import android.os.*;
import android.content.*;
import android.media.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.io.*;
public class MainActivity extends Activity {
 static { System.loadLibrary("orb_android"); }
 static native void nativeInit();
 static native void nativeCommand(int kind,int arg);
 static native void nativeParam(int device,int ctrl,int value);
 static native boolean nativeLoad(byte[] data);
 static native int nativeRender(short[] pcm);
 private volatile boolean running=false;
 private Thread audioThread;
 private LinearLayout root;
 private TextView status;
 private static final int PICK_SONG=33;
 @Override public void onCreate(Bundle state){super.onCreate(state);nativeInit();buildUi();startAudio();}
 private TextView title(String name){TextView t=new TextView(this);t.setText(name);t.setTextSize(21);t.setTextColor(Color.WHITE);t.setPadding(14,12,14,8);return t;}
 private Button button(String name,Runnable action){Button b=new Button(this);b.setText(name);b.setOnClickListener(v->action.run());return b;}
 private void buildUi(){
 ScrollView sc=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(18,10,18,16);root.setBackgroundColor(Color.rgb(24,26,32));sc.addView(root);setContentView(sc);
 root.addView(title("OpenRB-338 · Android engine port"));
 LinearLayout transport=new LinearLayout(this);transport.setOrientation(LinearLayout.HORIZONTAL);
 transport.addView(button("Play",()->nativeCommand(0,0)));transport.addView(button("Stop",()->nativeCommand(1,0)));
 transport.addView(button("Open RBS",()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_SONG);}));root.addView(transport);
 status=title("44.1 kHz · Standard ReBirth kit");root.addView(status);
 slider("Tempo",40,300,120,v->nativeCommand(2,v));slider("Shuffle",0,127,0,v->nativeCommand(3,v));
 for(int d=1;d<=4;d++){final int device=d;root.addView(title(d==1?"TB-303 A":d==2?"TB-303 B":d==3?"TR-808":"TR-909"));
 slider("Pattern select",0,127,0,v->nativeParam(device,1,v));slider("On / Off",0,1,1,v->nativeParam(device,0,v));
 if(d<=2){slider("Cutoff",0,127,64,v->nativeParam(device,5,v));slider("Resonance",0,127,64,v->nativeParam(device,6,v));}}
 root.addView(title("Effects and mixer"));
 for(int d=5;d<=8;d++){final int device=d;slider(new String[]{"Delay","Distortion","PCF","Compressor"}[d-5],0,127,64,v->nativeParam(device,0,v));}
 }
 interface Change{void changed(int value);}
 private void slider(String name,int min,int max,int initial,Change change){
 LinearLayout line=new LinearLayout(this);line.setOrientation(LinearLayout.HORIZONTAL);TextView text=new TextView(this);text.setTextColor(Color.LTGRAY);text.setText(name+"  "+initial);text.setWidth(340);line.addView(text);
 SeekBar seek=new SeekBar(this);seek.setMax(max-min);seek.setProgress(initial-min);line.addView(seek,new LinearLayout.LayoutParams(0,70,1));
 seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){if(user){int v=p+min;text.setText(name+"  "+v);change.changed(v);}}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});
 root.addView(line);}
 private void startAudio(){running=true;audioThread=new Thread(()->{
 int min=AudioTrack.getMinBufferSize(44100,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT);if(min<=0)return;
 AudioTrack track=new AudioTrack(AudioManager.STREAM_MUSIC,44100,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT,Math.max(min,8192),AudioTrack.MODE_STREAM);
 short[] pcm=new short[2048];track.play();try{while(running){int n=nativeRender(pcm);if(n<=0)break;track.write(pcm,0,n);}}finally{track.stop();track.release();}
 },"OpenRB audio");audioThread.start();}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
 if(request==PICK_SONG&&result==RESULT_OK&&data!=null)try(InputStream in=getContentResolver().openInputStream(data.getData());ByteArrayOutputStream out=new ByteArrayOutputStream()){
 byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
 status.setText(nativeLoad(out.toByteArray())?"Song loaded":"Unsupported or invalid song");
 }catch(Exception e){status.setText("Open error: "+e.getMessage());}}
 @Override protected void onDestroy(){running=false;super.onDestroy();}
}