package com.example.widgethost;
import android.app.Instrumentation;import android.os.Bundle;import android.view.accessibility.AccessibilityNodeInfo;
public class ScrollAction extends Instrumentation{
 Bundle args;public void onCreate(Bundle a){super.onCreate(a);args=a;start();}
 public void onStart(){Bundle r=new Bundle();try{AccessibilityNodeInfo n=getUiAutomation().getRootInActiveWindow();r.putString("scrolled",String.valueOf(walk(n)));finish(0,r);}catch(Throwable e){r.putString("error",e.toString());finish(1,r);}}
 boolean walk(AccessibilityNodeInfo n){if(n==null)return false;boolean back=args.getString("back","false").equals("true");if(n.isScrollable()&&n.performAction(back?AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD:AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))return true;for(int i=0;i<n.getChildCount();i++)if(walk(n.getChild(i)))return true;return false;}
}
