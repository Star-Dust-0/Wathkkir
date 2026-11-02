package com.example.widgethost;
import android.app.Activity;import android.os.Bundle;import android.widget.FrameLayout;import android.graphics.Color;import android.util.Base64;import androidx.wear.tiles.renderer.TileRenderer;import androidx.wear.protolayout.LayoutElementBuilders;import androidx.wear.protolayout.ResourceBuilders;
/** Same verification renderer for both serialized provider layouts. No released app edits. */
public class TileReferenceActivity extends Activity {
 public void onCreate(Bundle b){super.onCreate(b);FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);setContentView(root);
 try{byte[] data=Base64.decode(getIntent().getStringExtra("layout"),Base64.DEFAULT);LayoutElementBuilders.Layout layout=LayoutElementBuilders.Layout.fromByteArray(data);com.google.common.util.concurrent.ListenableFuture<android.view.View> future=new TileRenderer(this,getMainExecutor(),state->{}).inflateAsync(layout,new ResourceBuilders.Resources.Builder().setVersion("1").build(),root);future.addListener(()->{try{future.get();android.util.Log.i("TileReference","render completed children="+root.getChildCount());}catch(Throwable t){android.util.Log.e("TileReference","async failure",t);}},getMainExecutor());}catch(Throwable t){android.util.Log.e("TileReference","inflate",t);}}
}
