package com.example.widgethost;
import android.app.Instrumentation;
import android.os.Bundle;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
public class HierarchyDump extends Instrumentation {
 @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
 @Override public void onStart() {
  Bundle result=new Bundle();
  try {
   AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();
   StringBuilder xml=new StringBuilder("<?xml version=\"1.0\"?><hierarchy>");
   if(root!=null) node(root,xml);xml.append("</hierarchy>");
   result.putString("hierarchy",xml.toString());finish(0,result);
  } catch(Throwable t) {result.putString("error",t.toString());finish(1,result);}
 }
 private String esc(CharSequence x) { return x==null?"":x.toString().replace("&","&amp;").replace("\"","&quot;").replace("<","&lt;").replace(">","&gt;").replace("\n","&#10;"); }
 private void node(AccessibilityNodeInfo n,StringBuilder s) {
  Rect r=new Rect();n.getBoundsInScreen(r);
  s.append("<node text=\"").append(esc(n.getText())).append("\" content-desc=\"").append(esc(n.getContentDescription()))
   .append("\" bounds=\"[").append(r.left).append(",").append(r.top).append("][").append(r.right).append(",").append(r.bottom)
   .append("]\" clickable=\"").append(n.isClickable()).append("\" enabled=\"").append(n.isEnabled()).append("\">");
  for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo c=n.getChild(i);if(c!=null){node(c,s);c.recycle();}}
  s.append("</node>");
 }
}
