package com.example.widgethost;
import android.content.*;import android.os.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;import java.util.*;
import androidx.wear.tiles.*;
import android.support.wearable.complications.*;
/** Registered instrumentation application host. Actual binding/IPC, not a native watch face. */
public final class ProviderServiceProbe extends android.app.Instrumentation {
 android.os.Bundle args;static StringBuilder results;
 public void onCreate(Bundle value){super.onCreate(value);args=value;start();}
 static Context context;static Handler main;static String pkg;static ArrayList<ServiceConnection> connections=new ArrayList<>();
 static synchronized void report(String k,String v){String line="PROBE "+k+"="+v.replace('\n',' ');results.append(line).append("\n");android.util.Log.i("ProviderServiceProbe",line);}
 interface Work {void run()throws Exception;}
 static void ui(Work work)throws Exception {CountDownLatch latch=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();main.post(()->{try{work.run();}catch(Throwable e){error.set(e);}finally{latch.countDown();}});if(!latch.await(15,TimeUnit.SECONDS))throw new Exception("main dispatch timeout");if(error.get()!=null)throw new Exception(error.get());}
 static IBinder bind(String cls,String action)throws Exception {
  CountDownLatch latch=new CountDownLatch(1);AtomicReference<IBinder> result=new AtomicReference<>();
  ServiceConnection c=new ServiceConnection(){public void onServiceConnected(ComponentName n,IBinder b){result.set(b);latch.countDown();}public void onServiceDisconnected(ComponentName n){}public void onNullBinding(ComponentName n){latch.countDown();}};
  Intent i=new Intent(action).setComponent(new ComponentName(pkg,"com.wathakker.wear."+cls));
  ui(()->{boolean ok=context.bindService(i,c,Context.BIND_AUTO_CREATE);report(cls+"_bind",String.valueOf(ok));});connections.add(c);
  if(!latch.await(15,TimeUnit.SECONDS)||result.get()==null)throw new Exception("No actual bound service binder "+cls);return result.get();
 }
 static void tile()throws Exception {
  TileProvider p=TileProvider.Stub.asInterface(bind("PrayerTileService",TileService.ACTION_BIND_TILE_PROVIDER));report("tile_api",String.valueOf(p.getApiVersion()));
  CountDownLatch latch=new CountDownLatch(1);AtomicReference<byte[]> bytes=new AtomicReference<>();
  p.onTileAddEvent(new TileAddEventData(new EventBuilders.TileAddEvent.Builder().setTileId(41).build().toProto().toByteArray(),1));p.onTileEnterEvent(new TileEnterEventData(new EventBuilders.TileEnterEvent.Builder().setTileId(41).build().toProto().toByteArray(),1));
  byte[] request=new RequestBuilders.TileRequest.Builder().setDeviceConfiguration(new androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters.Builder().setScreenWidthDp(227).setScreenHeightDp(227).setScreenDensity(2f).setFontScale(1f).setRendererSchemaVersion(new androidx.wear.protolayout.expression.VersionBuilders.VersionInfo.Builder().setMajor(1).setMinor(200).build()).build()).build().toProto().toByteArray();
  p.onTileRequest(41,new TileRequestData(request,TileRequestData.VERSION_PROTOBUF),new TileCallback.Stub(){public void updateTileData(TileData d){bytes.set(d.getContents());latch.countDown();}});
  if(!latch.await(15,TimeUnit.SECONDS)||bytes.get()==null)throw new Exception("Tile actual service request timeout");report("tile_payload",android.util.Base64.encodeToString(bytes.get(),2));androidx.wear.tiles.TileBuilders.Tile tile=androidx.wear.tiles.TileBuilders.Tile.fromProto(androidx.wear.tiles.proto.TileProto.Tile.parseFrom(bytes.get()));report("tile_proto",tile.toProto().toString());report("tile_layout",android.util.Base64.encodeToString(tile.getTileTimeline().getTimelineEntries().get(0).getLayout().toByteArray(),2));
  CountDownLatch resources=new CountDownLatch(1);
  p.onResourcesRequest(41,new ResourcesRequestData(new RequestBuilders.ResourcesRequest.Builder().setVersion("1").build().toProto().toByteArray(),1),new ResourcesCallback.Stub(){public void updateResources(ResourcesData d){report("tile_resources",android.util.Base64.encodeToString(d.getContents(),2));resources.countDown();}});
  if(!resources.await(15,TimeUnit.SECONDS))throw new Exception("Resources actual service request timeout");p.onTileLeaveEvent(new TileLeaveEventData(new EventBuilders.TileLeaveEvent.Builder().setTileId(41).build().toProto().toByteArray(),1));p.onTileRemoveEvent(new TileRemoveEventData(new EventBuilders.TileRemoveEvent.Builder().setTileId(41).build().toProto().toByteArray(),1));
 }
 static void complication(String cls,int type,int id)throws Exception {
  IComplicationProvider p=IComplicationProvider.Stub.asInterface(bind(cls,"android.support.wearable.complications.ACTION_COMPLICATION_UPDATE_REQUEST"));CountDownLatch latch=new CountDownLatch(1);
  IComplicationManager callback=new IComplicationManager.Stub(){public void updateComplicationData(int actual,ComplicationData d){report(cls+"_id",String.valueOf(actual));report(cls+"_type",String.valueOf(d.getType()));report(cls+"_data",d.toStringNoRedaction());report(cls+"_tap",String.valueOf(d.getTapAction()));try{if(d.getTapAction()!=null)d.getTapAction().send();report(cls+"_tap_sent","true");}catch(Exception e){report(cls+"_tap_error",e.toString());}latch.countDown();}};
  report(cls+"_api",String.valueOf(p.getApiVersion()));p.onComplicationActivated(id,type,callback.asBinder());p.onUpdate(id,type,callback.asBinder());if(!latch.await(20,TimeUnit.SECONDS))throw new Exception("Complication actual service callback timeout "+cls);p.onComplicationDeactivated(id);
 }
 public void onStart(){
  results=new StringBuilder();context=getTargetContext().getApplicationContext();main=new Handler(Looper.getMainLooper());pkg=args.getString("pkg");Bundle result=new Bundle();
  try{
   report("host_package",context.getPackageName());report("host_uid",String.valueOf(android.os.Process.myUid()));report("host_context",context.getClass().getName());
   getUiAutomation().adoptShellPermissionIdentity("com.google.android.wearable.permission.BIND_TILE_PROVIDER","com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER");
   report("tile_permission",String.valueOf(context.checkSelfPermission("com.google.android.wearable.permission.BIND_TILE_PROVIDER")));report("complication_permission",String.valueOf(context.checkSelfPermission("com.google.android.wearable.permission.BIND_COMPLICATION_PROVIDER")));
   boolean ok=true;
   try{tile();}catch(Throwable e){ok=false;report("tile_error",android.util.Log.getStackTraceString(e));}
   String[] names={"PrayerComplicationService","DateGregComplication","DateHijriComplication","DateBothComplication","NextTimeComplication","UpcomingCountdownComplication"};int[] types={5,3,3,4,3,3};
   for(int i=0;i<names.length;i++){try{complication(names[i],types[i],100+i);}catch(Throwable e){ok=false;report(names[i]+"_error",android.util.Log.getStackTraceString(e));}}
   report("complete",String.valueOf(ok));
  }catch(Throwable e){report("error",android.util.Log.getStackTraceString(e));}
  finally{try{ui(()->{for(ServiceConnection c:connections)context.unbindService(c);});}catch(Exception ignored){}getUiAutomation().dropShellPermissionIdentity();result.putString("probe",results.toString());finish(0,result);}
 }
}
