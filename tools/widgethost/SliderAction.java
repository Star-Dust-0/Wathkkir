package com.example.widgethost;
import android.app.Instrumentation;
import android.os.Bundle;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
public class SliderAction extends Instrumentation {
 Bundle args;
 @Override public void onCreate(Bundle value) { super.onCreate(value); args=value; start(); }
 @Override public void onStart() {
  Bundle result=new Bundle();
  try {
   AccessibilityNodeInfo root=null;
   for(int i=0;i<20 && root==null;i++) {root=getUiAutomation().getRootInActiveWindow(); if(root==null)android.os.SystemClock.sleep(100);}
   boolean ok=root!=null && change(root,Integer.parseInt(args.getString("y")),Float.parseFloat(args.getString("value")));
   result.putString("changed",String.valueOf(ok));finish(0,result);
  } catch(Throwable t) {result.putString("error",t.toString());finish(1,result);}
 }
 boolean change(AccessibilityNodeInfo node,int y,float value) {
  Rect r=new Rect(); node.getBoundsInScreen(r);
  if(node.getRangeInfo()!=null && r.top<=y && y<r.bottom && "android.widget.SeekBar".contentEquals(node.getClassName())) {
   Bundle b=new Bundle();b.putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE,value);
   return node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.getId(),b);
  }
  for(int i=0;i<node.getChildCount();i++) {AccessibilityNodeInfo c=node.getChild(i);if(c!=null){boolean ok=change(c,y,value);c.recycle();if(ok)return true;}}
  return false;
 }
}
