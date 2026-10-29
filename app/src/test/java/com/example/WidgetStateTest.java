package com.example;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
public class WidgetStateTest {
 @Test public void state() { new com.example.lockwidgets.WidgetStateTest().stateAndAllHomeKindsRenderWithoutSwallowedErrors(); }
}
