package app.supernotes.sendIntent;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;

@CapacitorPlugin
public class SendIntent extends Plugin {

    @Override
    protected void handleOnNewIntent(Intent intent) {
        super.handleOnNewIntent(intent);
        bridge.getActivity().setIntent(intent);
    }

    @PluginMethod
    public void checkSendIntentReceived(PluginCall call) {
        // Log.d("SendIntent", "checkSendIntentReceived called");
        Intent intent = bridge.getActivity().getIntent();
        handleSendIntent(call, intent);
    }

    private void handleSendIntent(PluginCall call, Intent intent) {
        if (intent == null) {
            call.reject("No intent found");
            return;
        }
        String action = intent.getAction();
        String type = intent.getType();
        // Log.d("SendIntent", "Action: " + action + ", Type: " + type);
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            JSObject result = readItemAt(intent, type, 0);
            call.resolve(result);
            // immediately clear intent after processing
            bridge.getActivity().setIntent(new Intent());
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            JSObject result = readItemAt(intent, type, 0);
            List<JSObject> additionalItems = new ArrayList<>();
            // if there are multiple share items, concatenate them into an array
            if (intent.getClipData() != null) {
                for (int index = 1; index < intent.getClipData().getItemCount(); index++) {
                    additionalItems.add(readItemAt(intent, type, index));
                }
            }
            result.put("additionalItems", new JSArray(additionalItems));
            call.resolve(result);
            // immediately clear intent after processing
            bridge.getActivity().setIntent(new Intent());
        } else {
            call.reject("No processing needed");
        }
    }

    @PluginMethod
    public void finish(PluginCall call) {
        // wipe intent regardless of whether there was one and return OK
        bridge.getActivity().setIntent(new Intent());
        call.resolve();
    }

    private JSObject readItemAt(Intent intent, String type, int index) {
        JSObject result = new JSObject();
        String title = intent.getStringExtra(Intent.EXTRA_SUBJECT);
        Uri uri = null;

        if (intent.getClipData() != null && intent.getClipData().getItemAt(index) != null) uri =
            intent.getClipData().getItemAt(index).getUri();

        String url = null;
        // handling web links as url
        if ("text/plain".equals(type) && intent.getStringExtra(Intent.EXTRA_TEXT) != null) {
            url = intent.getStringExtra(Intent.EXTRA_TEXT);
        }
        // handling files as url
        else if (uri != null) {
            final Uri copyfileUri = copyfile(uri);
            url = (copyfileUri != null) ? copyfileUri.toString() : null;
        }
        if (title == null && uri != null) title = readFileName(uri);

        result.put("title", title);
        result.put("description", null);
        result.put("type", type);
        result.put("url", url);
        return result;
    }

    public String readFileName(Uri uri) {
        final String[] projection = { OpenableColumns.DISPLAY_NAME };
        Cursor cursor = null;
        try {
            cursor = getContext().getContentResolver().query(uri, projection, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    return cursor.getString(nameIndex);
                } else {
                    // Fallback to URI extraction if column missing
                    return getFallbackFileName(uri);
                }
            } else {
                return "unnamed_file_" + System.currentTimeMillis();
            }
        } catch (SecurityException e) {
            // Log.e("SendIntent", "Permission error accessing URI", e);
            return "secured_file";
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private String getFallbackFileName(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int cut = path.lastIndexOf('/');
            if (cut != -1) {
                return path.substring(cut + 1);
            }
        }
        return "shared_file_" + System.currentTimeMillis();
    }

    Uri copyfile(Uri uri) {
        final String fileName = readFileName(uri);
        File file = new File(getContext().getFilesDir(), fileName);

        try (
            FileOutputStream outputStream = getContext().openFileOutput(fileName, Context.MODE_PRIVATE);
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri)
        ) {
            IOUtils.copy(inputStream, outputStream);
            return Uri.fromFile(file);
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
        return null;
    }
}
