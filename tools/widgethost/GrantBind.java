import android.os.IBinder;
import java.lang.reflect.Method;

// Runs as shell via app_process: grants the widget-bind allowlist to the host package
// (shell holds android.permission.MODIFY_APPWIDGET_BIND_PERMISSIONS).
public class GrantBind {
    public static void main(String[] args) throws Exception {
        String pkg = args[0];
        IBinder b = (IBinder) Class.forName("android.os.ServiceManager")
                .getMethod("getService", String.class).invoke(null, "appwidget");
        Object svc = Class.forName("com.android.internal.appwidget.IAppWidgetService$Stub")
                .getMethod("asInterface", IBinder.class).invoke(null, b);
        Method m = svc.getClass().getMethod("setBindAppWidgetPermission",
                String.class, int.class, boolean.class);
        m.invoke(svc, pkg, 0, true);
        System.out.println("granted bind permission to " + pkg);
    }
}
