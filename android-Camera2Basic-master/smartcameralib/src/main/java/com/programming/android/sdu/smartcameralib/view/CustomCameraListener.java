package com.programming.android.sdu.smartcameralib.view;

/**
 * Created by grzegorzbaczek on 08/03/2018.
 */

public interface CustomCameraListener {
    void pictureTaken(String filePath, int scale, int preview_width, int preview_height);
    void crossClicked();
    void galleryClicked();
    void useGalleryPhotoClicked(String path);
}
