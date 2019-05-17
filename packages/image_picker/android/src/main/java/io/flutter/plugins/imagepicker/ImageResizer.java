// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package io.flutter.plugins.imagepicker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class ImageResizer {
  private final File externalFilesDirectory;
  private final ExifDataCopier exifDataCopier;

  ImageResizer(File externalFilesDirectory, ExifDataCopier exifDataCopier) {
    this.externalFilesDirectory = externalFilesDirectory;
    this.exifDataCopier = exifDataCopier;
  }

  /**
   * If necessary, resizes the image located in imagePath and then returns the path for the scaled
   * image.
   *
   * <p>If no resizing is needed, returns the path for the original image.
   */
  String resizeImageIfNeeded(String imagePath, Double maxWidth, Double maxHeight, int compressionQuality) {
    Bitmap bmp = BitmapFactory.decodeFile(imagePath);
    double originalWidth = bmp.getWidth() * 1.0;
    double originalHeight = bmp.getHeight() * 1.0;

    boolean shouldScale = (maxWidth != null || maxHeight != null) && (maxWidth < originalWidth || maxHeight < originalHeight);

    if (!shouldScale) {
      return imagePath;
    }

    try {
      File scaledImage = resizedImage(imagePath, bmp, maxWidth, maxHeight, compressionQuality);
      exifDataCopier.copyExif(imagePath, scaledImage.getPath());

      return scaledImage.getPath();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private File resizedImage(String path, Bitmap bmp, Double maxWidth, Double maxHeight, int compressionQuality) throws IOException {
    double originalWidth = bmp.getWidth() * 1.0;
    double originalHeight = bmp.getHeight() * 1.0;
    Double width;
    Double height;

    if (maxWidth * originalHeight > maxHeight * originalWidth) {
      width = maxHeight * originalWidth / originalHeight;
      height = maxHeight;
    } else {
      width = maxWidth;
      height = maxWidth * originalHeight / originalWidth;
    }

    Bitmap scaledBmp = Bitmap.createScaledBitmap(bmp, width.intValue(), height.intValue(), false);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    boolean saveAsPNG = bmp.hasAlpha();
    scaledBmp.compress(
        saveAsPNG ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, compressionQuality, outputStream);

    String[] pathParts = path.split("/");
    String imageName = pathParts[pathParts.length - 1];

    File imageFile = new File(externalFilesDirectory, "/scaled_" + imageName);
    FileOutputStream fileOutput = new FileOutputStream(imageFile);
    fileOutput.write(outputStream.toByteArray());
    fileOutput.close();

    return imageFile;
  }
}
