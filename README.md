# How to show Presentation on android Virtual Display
Clone the project with the tag **virtualdisplay** to see how to display [Presentation](https://developer.android.com/reference/android/app/Presentation.html) on Android virutual display.

```shell
git clone https://github.com/andronmobi/PresentationOnVirtualDisplay.git \
-b l01_virtualdisplay
```

Two use-cases will be demonstrated in one simple android application:

* Creating a virtual display rendering the content to a Surface
* Displaying a Presentation on this virtual display

## Creating Virtual Display
A virtual display is created by **createVirtualDisplay** method of [DisplayManager](https://developer.android.com/reference/android/hardware/display/DisplayManager.html). In this method (among other parameters) we have to pass a surface to which the content of the virtual display will be rendered and flags varying the behaviour of the virtual display:

```java
mDisplayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
 
int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION | 
    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
 
VirtualDisplay virtualDisplay = mDisplayManager.createVirtualDisplay("MyVirtualDisplay",
    WIDTH, HEIGHT, DENSITY_DPI, surface, flags);
```

But the code snippet presented above wont work if your application is not system and doesn’t have some permissions (like CAPTURE\_VIDEO\_OUTPUT or CAPTURE\_SECURE\_VIDEO\_OUTPUT) required for certain flags. That’s why we have to use [MediaProjectionManager](https://developer.android.com/reference/android/media/projection/MediaProjectionManager.html) to ask a permission to user to “cast a screen” (to obtain [MediaProjection](https://developer.android.com/reference/android/media/projection/MediaProjection.html) which creates a virtual display):

```java
mProjectionManager = (MediaProjectionManager) getSystemService(
    Context.MEDIA_PROJECTION_SERVICE);
startActivityForResult(mProjectionManager.createScreenCaptureIntent(), 
    PERMISSION_CODE);

```

In case if a user accepts the permission to cast a screen it will be possible to obtain an instance of MediaProjection and to create a virtual display:

```java
@Override
public void onActivityResult(int requestCode, int resultCode, Intent data) {
    ...
    Log.i(TAG, "Get media projection with the new permission");
    mProjection = getProjection();
    createVirtualDisplay();
}
 
private MediaProjection getProjection() {
    MediaProjection projection = mProjectionManager.getMediaProjection(mResultCode,
        mResultData);
    // Add a callback to be informed if the projection
    // will be stopped from the status bar.
    mProjectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            Log.d(TAG, "MediaProjection.Callback onStop obj:" + toString());
            destroyVirtualDisplay();
            mProjection = null;
        }
    };
    projection.registerCallback(mProjectionCallback, null);
    return projection;
}
 
private void createVirtualDisplay() {
    if (mProjection != null && mVirtualDisplay == null) {
        Log.d(TAG, "createVirtualDisplay WxH (px): " + mWidth + "x" + mHeight +
                ", dpi: " + mMetrics.densityDpi);
        int flags = DisplayManager.VIRTUAL_DISPLAY_FLAG_PRESENTATION;
        //flags |= DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
        mVirtualDisplay = mProjection.createVirtualDisplay("MyVirtualDisplay",
                mWidth, mHeight, mMetrics.densityDpi, flags, mSurface,
                null /*Callbacks*/, null /*Handler*/);
        mButtonCreate.setEnabled(false);
        mButtonDestroy.setEnabled(true);
    }
}
 
private void destroyVirtualDisplay() {
    Log.d(TAG, "destroyVirtualDisplay");
    if (mVirtualDisplay != null) {
        Log.d(TAG, "destroyVirtualDisplay release");
        mVirtualDisplay.release();
        mVirtualDisplay = null;
        mButtonDestroy.setEnabled(false);
        mButtonCreate.setEnabled(true);
    }
}
```
## Showing Presentation on Virtual Display
Once a virtual display is created you can display a content on it. In our case, it will be a Presentation (android Dialog for secondary display) with LinearLayout as a content view and animated (rotating) TextView on it. To listen for changes in available display devices a DisplayListener must be registered: