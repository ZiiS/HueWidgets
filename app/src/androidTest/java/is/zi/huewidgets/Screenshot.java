package is.zi.huewidgets;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class Screenshot {
    Screenshot(Context context, String name) throws IOException, InterruptedException {
        Thread.sleep(100);
        try (
                FileOutputStream out = new FileOutputStream(
                        new File(
                                context.getExternalFilesDir(null),
                                name + ".png"
                        )
                )
        ) {
            androidx.test.runner.screenshot.Screenshot.capture().getBitmap().compress(Bitmap.CompressFormat.PNG, 100, out);
        }
    }
}
