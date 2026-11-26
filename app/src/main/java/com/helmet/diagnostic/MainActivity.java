package com.helmet.diagnostic;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.*;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    AudioManager am; TextView log; LinearLayout layout; Spinner a,b;
    List<AudioDeviceInfo> outputs = new ArrayList<>();
    AtomicBoolean running = new AtomicBoolean(false); Thread worker;
    static final int RATE=16000;
    @Override public void onCreate(Bundle state){super.onCreate(state);
        am=getSystemService(AudioManager.class); ScrollView scroll=new ScrollView(this);
        layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(24,24,24,24);scroll.addView(layout);setContentView(scroll);
        TextView title=new TextView(this);title.setText("Helmet Intercom Lab — Phase 1");title.setTextSize(23);layout.addView(title);
        TextView hint=new TextView(this);hint.setText("Pair both headsets in Android settings. Keep volumes low. Test while stationary. A and B must be distinct. No audio is saved.");layout.addView(hint);
        a=new Spinner(this);b=new Spinner(this);layout.addView(a);layout.addView(b);
        button("Refresh Bluetooth routes",()->{if(!running.get())refresh();});
        button("Test A microphone + speaker",()->start(false,false,false));
        button("Test B microphone + speaker",()->start(false,false,true));
        button("Test dual microphones (10 seconds)",()->start(true,false,false));
        button("Test dual speakers (alternating tones)",()->start(true,true,false));
        button("Stop test",()->running.set(false));
        button("Copy diagnostic log",()->{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("diagnostics",log.getText()));});
        log=new TextView(this);log.setTextIsSelectable(true);layout.addView(log);
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO,Manifest.permission.BLUETOOTH_CONNECT},1);else refresh();
    }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);refresh();}
    void button(String s,Runnable action){Button v=new Button(this);v.setText(s);v.setOnClickListener(w->action.run());layout.addView(v);}
    void say(String s){runOnUiThread(()->log.append(s+"\n"));}
    String desc(AudioDeviceInfo d){return d==null?"NONE":d.getProductName()+" id="+d.getId()+" type="+d.getType()+" addr="+d.getAddress();}
    boolean bt(AudioDeviceInfo d){int t=d.getType();return t==AudioDeviceInfo.TYPE_BLUETOOTH_SCO||t==AudioDeviceInfo.TYPE_BLE_HEADSET;}
    void refresh(){try{outputs.clear();List<String> names=new ArrayList<>();for(AudioDeviceInfo d:am.getAvailableCommunicationDevices()){say("Communication: "+desc(d));if(bt(d)){outputs.add(d);names.add(desc(d));}}for(AudioDeviceInfo d:am.getDevices(AudioManager.GET_DEVICES_INPUTS))say("Input: "+desc(d));for(AudioDeviceInfo d:am.getDevices(AudioManager.GET_DEVICES_OUTPUTS))say("Output: "+desc(d));for(Spinner s:new Spinner[]{a,b})s.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,names));if(names.size()>1)b.setSelection(1);say("Selectable Bluetooth communication routes: "+names.size());}catch(Exception e){say(e.toString());}}
    AudioDeviceInfo inputFor(AudioDeviceInfo out){for(AudioDeviceInfo d:am.getDevices(AudioManager.GET_DEVICES_INPUTS))if(bt(d)&&d.getType()==out.getType()&&!out.getAddress().isEmpty()&&out.getAddress().equals(d.getAddress()))return d;return null;}
    AudioRecord record(AudioDeviceInfo d){int n=AudioRecord.getMinBufferSize(RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT);AudioRecord r=new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,RATE,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT,Math.max(n,3200));say("Record preferred accepted="+r.setPreferredDevice(d));return r;}
    AudioTrack track(AudioDeviceInfo d){int n=AudioTrack.getMinBufferSize(RATE,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT);AudioTrack t=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setAudioFormat(new AudioFormat.Builder().setSampleRate(RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(Math.max(n,3200)).build();say("Playback preferred accepted="+t.setPreferredDevice(d));return t;}
    void start(boolean dual,boolean speakers,boolean useB){if(worker!=null&&worker.isAlive()){say("Previous test still stopping or running");return;}if(outputs.isEmpty()||a.getSelectedItemPosition()<0||b.getSelectedItemPosition()<0){say("No selectable Bluetooth route");return;}AudioDeviceInfo x=outputs.get(useB?b.getSelectedItemPosition():a.getSelectedItemPosition()),y=outputs.get(b.getSelectedItemPosition());if(dual&&x.getId()==y.getId()){say("Select distinct A and B routes");return;}if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){say("Microphone permission required");return;}running.set(true);worker=new Thread(()->test(x,y,dual,speakers));worker.start();}
    void test(AudioDeviceInfo x,AudioDeviceInfo y,boolean dual,boolean speakers){AudioRecord r1=null,r2=null;AudioTrack t1=null,t2=null;int old=am.getMode();try{am.setMode(AudioManager.MODE_IN_COMMUNICATION);say("Select A: "+am.setCommunicationDevice(x));SystemClock.sleep(1000);if(!running.get())return;
        if(speakers){t1=track(x);t2=track(y);t1.play();t2.play();short[] p=new short[320],q=new short[320];for(int k=0;k<500&&running.get();k++){for(int i=0;i<320;i++){p[i]=(short)((k/50%2==0)?1000*Math.sin(2*Math.PI*440*i/RATE):0);q[i]=(short)((k/50%2==1)?1000*Math.sin(2*Math.PI*660*i/RATE):0);}t1.write(p,0,p.length);t2.write(q,0,q.length);if(k%50==0)say("Speaker A="+desc(t1.getRoutedDevice())+" | B="+desc(t2.getRoutedDevice()));}return;}
        AudioDeviceInfo ix=inputFor(x),iy=inputFor(y);say("Matched input A: "+desc(ix)+" | B: "+desc(iy));if(ix==null||(dual&&iy==null)){say("Cannot identify distinct requested mic routes; test inconclusive/unsupported by exposed routes.");return;}
        r1=record(ix);r1.startRecording();if(dual){r2=record(iy);r2.startRecording();}else{t1=track(x);t1.play();short[] tone=new short[1600];for(int i=0;i<tone.length;i++)tone[i]=(short)(900*Math.sin(2*Math.PI*440*i/RATE));t1.write(tone,0,tone.length);}
        short[] p=new short[320],q=new short[320];for(int k=0;k<500&&running.get();k++){int n=r1.read(p,0,p.length,AudioRecord.READ_NON_BLOCKING);int m=dual?r2.read(q,0,q.length,AudioRecord.READ_NON_BLOCKING):0;if(n<0||m<0){say("Read error A="+n+" B="+m);break;}if(k%10==0)say("Mic A="+desc(r1.getRoutedDevice())+" RMS="+rms(p,n)+" | "+(dual?"Mic B="+desc(r2.getRoutedDevice())+" RMS="+rms(q,m):"Speaker="+desc(t1.getRoutedDevice())));SystemClock.sleep(20);}
    }catch(Exception e){say("Test error: "+e);}finally{close(r1);close(r2);close(t1);close(t2);am.clearCommunicationDevice();am.setMode(old);running.set(false);say("Test finished. Compare actual route IDs and physical audio; preferred routes are not proof.");}}
    long rms(short[] p,int n){if(n<=0)return 0;double s=0;for(int i=0;i<n;i++)s+=(double)p[i]*p[i];return Math.round(Math.sqrt(s/n));}
    void close(AudioRecord r){if(r!=null){try{r.stop();}catch(Exception ignored){}r.release();}}
    void close(AudioTrack t){if(t!=null){try{t.stop();}catch(Exception ignored){}t.release();}}
    @Override protected void onStop(){running.set(false);super.onStop();}
}
