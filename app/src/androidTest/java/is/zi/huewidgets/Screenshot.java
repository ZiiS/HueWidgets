package is.zi.huewidgets;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

class Screenshot {
    Screenshot(Context context, String name) throws IOException, InterruptedException {
        Thread.sleep(100);
        Bitmap bitmap = androidx.test.runner.screenshot.Screenshot.capture().getBitmap();
        if(bitmap != null) {
            //Can't get working on Travis-Ci
            try (
                    FileOutputStream out = new FileOutputStream(
                            new File(
                                    context.getExternalFilesDir(null),
                                    name + ".png"
                            )
                    )
            ) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
        }
    }
}
