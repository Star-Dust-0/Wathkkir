package com.example.widgethost;
import android.app.Instrumentation;import android.os.Bundle;import android.view.accessibility.AccessibilityNodeInfo;
public class TextInput extends Instrumentation {
 Bundle args;public void onCreate(Bundle a){super.onCreate(a);args=a;start();}
 public void onStart(){Bundle r=new Bundle();try{AccessibilityNodeInfo root=null;for(int i=0;i<20&&root==null;i++){root=getUiAutomation().getRootInActiveWindow();if(root==null)android.os.SystemClock.sleep(100);}String text=new String(android.util.Base64.decode(args.getString("base64",""),0),java.nio.charset.StandardCharsets.UTF_8);r.putString("set",String.valueOf(walk(root,text)));finish(0,r);}catch(Throwable e){r.putString("error",e.toString());finish(1,r);}}
 boolean walk(AccessibilityNodeInfo n,String text){if(n==null)return false;if("android.widget.EditText".contentEquals(n.getClassName())){Bundle b=new Bundle();b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,text);n.performAction(AccessibilityNodeInfo.ACTION_FOCUS);n.performAction(AccessibilityNodeInfo.ACTION_CLICK);android.os.SystemClock.sleep(300);return n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,b);}for(int i=0;i<n.getChildCount();i++)if(walk(n.getChild(i),text))return true;return false;}
}
